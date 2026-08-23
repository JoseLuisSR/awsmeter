# ARCHITECTURE.md

This document describes the architecture of **AWS Meter**: its entry points, main modules, and the design patterns that let it support five unrelated AWS services with very little duplicated logic.

## 1. What This Project Is

AWS Meter is not a standalone application — it is a **JMeter plugin**: a set of Java classes packaged into a single "uber JAR" that JMeter loads at runtime and exposes as selectable "Java Request" samplers in the JMeter GUI (Kinesis, SQS, SNS, Cognito, EventBridge). JMeter drives the test lifecycle (threads, loops, timers, reporting); this project only supplies the per-service request/response logic.

## 2. Entry Points

There is no `main()` method. The entry points are the classes JMeter instantiates via reflection because they implement `org.apache.jmeter.protocol.java.sampler.JavaSamplerClient`:

- `kinesis.KinesisProducerSampler`
- `sqs.SQSProducerStandardQueue`, `sqs.SQSProducerFifoQueue`
- `sns.SNSProducerStandardTopic`, `sns.SNSProducerFifoTopic`
- `cognito.CognitoProducerAdminCreateUser`, `cognito.CognitoProducerAdminLoginUser`
- `eventbus.EventBusProducerSampler`

For each thread in a JMeter test plan, JMeter calls these `JavaSamplerClient` lifecycle methods on the sampler instance, in order:

1. `getDefaultParameters()` — declares the parameter fields shown in the JMeter GUI (also used the first time a sampler is dropped into a test plan).
2. `setupTest(JavaSamplerContext)` — runs once per thread; reads all JMeter parameters into a `Map<String, String>` and builds the AWS SDK client.
3. `runTest(JavaSamplerContext)` — runs once per sample/iteration; builds the AWS request, executes it, and returns a populated `SampleResult` (success/failure, timing, request/response data) that JMeter uses for reporting.
4. `teardownTest(JavaSamplerContext)` — runs once per thread; closes the AWS SDK client.

The build artifact itself (`./gradlew uberJar` → `build/libs/awsmeter-<version>.jar`) is the deployable unit: it bundles this project's classes plus all AWS SDK / Jackson dependencies (JMeter itself is `compileOnly`, since it's provided by the host JMeter installation). `awsmeter.jmx` is a ready-made JMeter test plan used to exercise every sampler manually or against LocalStack.

## 3. Package / Module Map

```
org.apache.jmeter.protocol.aws                 shared foundation (no AWS service specifics)
├── AWSSampler          abstract base sampler: JMeter parameter names, SampleResult helpers
├── AWSClient            credential/region resolution shared by both SDK generations
├── AWSClientSDK1        AWS SDK v1 credential-provider wiring (interface, default methods)
├── AWSClientSDK2        AWS SDK v2 credential-provider wiring (interface, default methods)
└── MessageAttribute     POJO for JSON-deserialized SQS/SNS message attributes

org.apache.jmeter.protocol.aws.kinesis          Kinesis Data Streams (SDK v2)
org.apache.jmeter.protocol.aws.sqs              SQS Standard & FIFO queues (SDK v2)
org.apache.jmeter.protocol.aws.sns              SNS Standard & FIFO topics (SDK v1)
org.apache.jmeter.protocol.aws.cognito          Cognito user creation & admin login (SDK v2)
org.apache.jmeter.protocol.aws.eventbus         EventBridge event publishing (SDK v2)
```

Each service package is self-contained: it depends only on the shared foundation package, never on another service package. `src/test/java/...` mirrors this exact structure, one `*Test.java` per production class, using JUnit 5 + Mockito to mock AWS SDK clients (no real network calls in tests).

## 4. Design Patterns

### 4.1 Template Method (sampler lifecycle)

`AWSSampler` is the template: it fixes the vocabulary of JMeter parameter names (`aws_access_key_id`, `aws_region`, `aws_endpoint_custom`, …) and provides finished helper methods (`newSampleResult`, `sampleResultStart`, `sampleResultSuccess`, `sampleResultFail`, `readMsgAttributes`) that every concrete sampler reuses verbatim. Concrete classes only fill in the AWS-specific steps (`createSdkClient`/`createAWSClient`, `getDefaultParameters`, `runTest`).

Within most service packages this template is split into two levels:
- An **abstract `*ProducerSampler`** (`SQSProducerSampler`, `SNSProducerSampler`, `CognitoProducerSampler`) that owns the SDK client field and implements the identical `setupTest`/`teardownTest` boilerplate (collect JMeter parameters into a map → build client; close client).
- **Concrete leaf classes** (`SQSProducerStandardQueue`/`SQSProducerFifoQueue`, `SNSProducerStandardTopic`/`SNSProducerFifoTopic`, `CognitoProducerAdminCreateUser`/`CognitoProducerAdminLoginUser`) that supply `getDefaultParameters()` and `runTest()` for one specific request shape (e.g. FIFO adds `sqs_msg_group_id`/`sqs_msg_deduplication_id` parameters and populates them on the request).

Kinesis and EventBridge have only one request shape each, so their sampler is a single concrete class rather than an abstract-plus-leaves pair — the pattern is applied only where variation actually exists.

### 4.2 Interface Segregation / Strategy (SDK v1 vs v2)

AWS Meter straddles two generations of the AWS SDK for Java because SNS support was written against SDK v1 while newer integrations use SDK v2. Rather than branching on SDK version, `AWSClient` factors out the version-independent logic (region resolution) and two sibling interfaces, `AWSClientSDK1` and `AWSClientSDK2`, each provide default-method implementations of *credentials provider* construction against their respective SDK's types (`AWSCredentialsProvider` vs `AwsCredentialsProvider`) and a factory method a sampler must implement (`createAWSClient` vs `createSdkClient`). A sampler picks its strategy simply by declaring `implements AWSClientSDK1` or `implements AWSClientSDK2` — the rest of `AWSSampler` and the JMeter lifecycle are unaffected by which one is chosen.

### 4.3 Chain of Responsibility-style credential & region resolution

Both `AWSClientSDK1.getAWSCredentialsProvider` and `AWSClientSDK2.getAwsCredentialsProvider` (and `AWSClient.getAWSRegion`) resolve their value through the same ordered fallback, implemented as a short-circuiting chain of `Optional`/predicate checks rather than object-oriented handler objects:

1. Explicit JMeter parameters (`aws_access_key_id` + `aws_secret_access_key`, with `aws_session_token` promoting basic credentials to session credentials).
2. A named profile (`aws_configure_profile`, when set to something other than `default`) → `ProfileCredentialsProvider`.
3. The SDK's own default provider chain → environment variables → shared credentials file (`~/.aws/credentials`) → ECS task role → EC2 instance profile.

This is what makes the same sampler code work unmodified against a developer's local AWS CLI profile, LocalStack (`test`/`test` credentials + `aws_endpoint_custom`), and production ECS/EC2 workloads with IAM roles — the caller never needs to know which source actually supplied the credentials.

### 4.4 Builder Pattern

All AWS request/response objects (`PutRecordRequest`, `SendMessageRequest`, `PublishRequest`, `PutEventsRequest`, `AdminInitiateAuthRequest`, SDK client builders themselves) are constructed via the AWS SDK's fluent builder APIs. Samplers' `create*Request` methods are thin builder-assembly functions driven by `JavaSamplerContext.getParameter(...)` values.

### 4.5 Functional composition for message attributes

SQS and SNS both accept typed message attributes (String, Number, Binary, and for SNS, String Array) supplied by the user as a JSON array (`sqs_msg_attributes` / `sns_msg_attributes`). `AWSSampler.readMsgAttributes` deserializes this JSON into `List<MessageAttribute>` via Jackson. Each producer sampler then partitions that list into per-type maps using small `Predicate<MessageAttribute>` / `Function<MessageAttribute, MessageAttributeValue>` fields (`isStringDataType`/`createStringAttribute`, etc.) composed with the Streams API, and merges the per-type maps into the single attributes map the AWS request expects. SQS and SNS each keep their own copy of this logic because their attribute-value types differ (`software.amazon.awssdk...MessageAttributeValue` vs `com.amazonaws...MessageAttributeValue`), which is a direct consequence of the SDK v1/v2 split described above.

## 5. Runtime Data Flow (example: SQS Standard Queue)

```
JMeter GUI/plan          SQSProducerStandardQueue          AWS SDK v2              AWS / LocalStack
     │  parameters              │                              │                        │
     ├─ getDefaultParameters ──►│                              │                        │
     │                          │                              │                        │
     ├─ setupTest ─────────────►│─ createSdkClient() ─────────►│ SqsClient.builder()    │
     │                          │  (region, endpoint,          │  .credentialsProvider  │
     │                          │   credentials resolved)      │                        │
     │                          │                              │                        │
     ├─ runTest ───────────────►│─ createSendMessageRequest()  │                        │
     │                          │  (body, attributes, delay)   │                        │
     │                          │─ sqsClient.sendMessage() ───►│──── SendMessage ──────►│
     │                          │◄─────────────────────────────│◄─── response/error ────│
     │                          │─ sampleResultSuccess/Fail()  │                        │
     │◄──────── SampleResult ───│                              │                        │
     │                          │                              │                        │
     └─ teardownTest ──────────►│─ sqsClient.close()           │                        │
```

Every other service (Kinesis, SNS, Cognito, EventBridge) follows this same shape; only the SDK client type, request builder, and success/failure message formatting differ.

## 6. Extending the Architecture

To add a new AWS service sampler:

1. Create a new package `org.apache.jmeter.protocol.aws.<service>`.
2. Write an abstract (or, if there's only one request shape, concrete) sampler extending `AWSSampler` and implementing `AWSClientSDK1` or `AWSClientSDK2` depending on which SDK generation the target service client belongs to.
3. Implement the client factory method (`createSdkClient`/`createAWSClient`), reusing `getAWSRegion`/`getAWSEndpoint`/`getAwsCredentialsProvider` (or SDK1 equivalents) — no new credential-resolution logic should be needed.
4. Implement `getDefaultParameters()` (service-specific JMeter fields, appended to the shared `AWS_PARAMETERS`), `setupTest`/`teardownTest` (or inherit them from the abstract sampler), and `runTest()` (build request → call SDK → map to `SampleResult` via `sampleResultSuccess`/`sampleResultFail`).
5. Add matching tests under `src/test/java/...` mocking the SDK client, following the existing per-class test layout; keep coverage at or above the 95% threshold enforced by `build.gradle` and CI (see `CLAUDE.md` for exact commands).

For a visual class diagram, see `doc/img/awsmeter-class-diagram.png` (referenced from `README.md`).

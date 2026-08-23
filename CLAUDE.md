# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

AWS Meter is a JMeter plugin (Java Request Samplers) for load-testing AWS services: Kinesis Data Streams, SQS (Standard & FIFO), SNS (Standard & FIFO), Cognito, and EventBridge. It's packaged as a single uber JAR dropped into `$JMETER_HOME/lib/ext/`. All services also work against LocalStack via a custom endpoint parameter.

## Commands

Build and packaging:
```bash
./gradlew uberJar          # Build the shaded plugin JAR -> build/libs/awsmeter-<version>.jar
```

Testing:
```bash
./gradlew test                              # Run all tests (JUnit 5 + Mockito), generates JaCoCo report
./gradlew test --tests "*.SQSProducerSamplerTest"          # Run a single test class
./gradlew test --tests "*.SQSProducerSamplerTest.testName" # Run a single test method
./gradlew unitTest                          # Same tests with verbose stdout/stderr logging
./gradlew jacocoTestReport                  # HTML/XML coverage report (build/reports/jacoco)
./gradlew jacocoTestCoverageVerification    # Enforce coverage thresholds (fails build if unmet)
./gradlew ciTest                            # unitTest + coverage verification, mirrors CI
```

Coverage requirements (enforced in `build.gradle` and CI): 95% overall line coverage, 75% minimum per class (50% in the CI-specific verification task). PRs are gated on this in `.github/workflows/pr-tests.yml`, which also requires 95% coverage on changed files. Keep new/changed code covered accordingly.

Java 21 is the compile target (`sourceCompatibility`/`targetCompatibility` in `build.gradle`); CI runs against Java 24 (Corretto). Mockito/Byte Buddy needs `-Dnet.bytebuddy.experimental=true` on newer JVMs — already wired into the `test` task's `jvmArgs`.

## Architecture

Every AWS service integration follows the same three-layer pattern; understanding it lets you extend to a new service without reading every existing sampler.

1. **`AWSSampler`** (`src/main/java/org/apache/jmeter/protocol/aws/AWSSampler.java`) — abstract base implementing JMeter's `JavaSamplerClient`. Defines the shared JMeter parameter names (`aws_access_key_id`, `aws_secret_access_key`, `aws_session_token`, `aws_region`, `aws_configure_profile`, `aws_endpoint_custom`), `SampleResult` helpers (`newSampleResult`, `sampleResultStart`, `sampleResultSuccess`, `sampleResultFail`), and message-attribute JSON deserialization (`readMsgAttributes`, backed by `MessageAttribute`).

2. **`AWSClient`** (credentials/region resolution shared logic) with two SDK-specific extensions:
   - **`AWSClientSDK1`** — for services still on AWS SDK v1 (currently SNS, STS). Builds `AWSCredentialsProvider` and exposes `createAWSClient(...)` returning an `AwsSyncClientBuilder`.
   - **`AWSClientSDK2`** — for services on AWS SDK v2 (Kinesis, SQS, Cognito, EventBridge). Builds `AwsCredentialsProvider` and exposes `createSdkClient(...)` returning an `SdkClient`.
   
   Both resolve credentials/region with the same precedence: explicit JMeter parameters → named profile (`ProfileCredentialsProvider`) → default provider chain (env vars → shared credentials file → ECS task role → EC2 instance profile). This is what makes the plugin "just work" both locally and inside ECS/EC2 with IAM roles.

3. **Per-service sampler package** under `org.apache.jmeter.protocol.aws.<service>` (`kinesis`, `sqs`, `sns`, `cognito`, `eventbus`). Each package has:
   - An abstract `*ProducerSampler` extending `AWSSampler` and implementing `AWSClientSDK1` or `AWSClientSDK2` — owns the client field, `setupTest`/`teardownTest` (build/close the SDK client from JMeter parameters), and an abstract request-building method.
   - One or more concrete subclasses (e.g. `SQSProducerStandardQueue`, `SQSProducerFifoQueue`; `SNSProducerStandardTopic`, `SNSProducerFifoTopic`) implementing `getDefaultParameters()` (declares the JMeter GUI fields) and `runTest(JavaSamplerContext)` (executes the actual AWS call and maps the response into a `SampleResult`).

To add a new AWS service: create a new package following this convention, extend `AWSSampler`, implement `AWSClientSDK1` or `AWSClientSDK2` depending on which AWS SDK the service client uses, and provide `getDefaultParameters()` + `runTest()` in the concrete class(es).

Tests mirror this structure 1:1 under `src/test/java/...` (one `*Test.java` per production class), using JUnit 5 and Mockito to mock AWS SDK clients rather than making real network calls.

`awsmeter.jmx` at the repo root is a working JMeter test plan demonstrating parameter configuration for each sampler — useful as a reference for what `getDefaultParameters()` should expose.

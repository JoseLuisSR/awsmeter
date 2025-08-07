# 🌊 AWS Kinesis Data Stream

Amazon Kinesis Data Streams is a **real-time data streaming service** that enables you to collect, process, and analyze streaming data at scale. Think of it as a high-speed highway for your data - where millions of events can flow simultaneously from various sources to multiple destinations.

This guide provides specific instructions for testing AWS Kinesis Data Stream using the `awsmeter` plugin with Apache JMeter. For general installation and setup instructions, please refer to the [instructions](../../../../../../../../../README.md).

## 📋 Overview

Amazon Kinesis Data Streams is a powerful, serverless service designed to handle high-volume data ingestion from multiple sources including:
- 📱 Social networks
- 🏠 IoT devices  
- 📊 Application logs
- 🔄 Real-time event streams

This service implements a **publish-subscribe pattern** where multiple producers can publish data streams, and Kinesis reliably delivers events to subscribed consumers in near real-time.

### 🏗️ Key Architecture Benefits
- **🔧 Serverless Management**: AWS handles infrastructure, replication across 3 AZs, and auto-scaling
- **⚡ High Throughput**: Process gigabytes of data per second
- **🔗 Native Integrations**: Works seamlessly with Lambda, S3, and other AWS services
- **📈 Scalable**: Manual or automatic scaling with [AWS Application Auto Scaling](https://aws.amazon.com/blogs/big-data/scaling-amazon-kinesis-data-streams-with-aws-application-auto-scaling/)

## ✅ Prerequisites

Before using awsmeter for Kinesis load testing, ensure you have:

### 🛠️ Required Tools
- ☕ **JMeter** with awsmeter plugin installed (see [general prerequisites](../../../../../../../../../README.md))
- 🔑 **AWS CLI** configured with appropriate credentials
- 📋 **AWS Account** with Kinesis access permissions

### 🔐 IAM Permissions
Your AWS user/role needs these minimum permissions:
- `kinesis:PutRecord`
- `kinesis:DescribeStream`
- `kinesis:ListStreams`

## 🚀 Setup

### Step 1: Create Kinesis Data Stream 🎯

1. **Sign in** to your AWS Console
2. Navigate to **Amazon Kinesis** → **Data Streams** → **Create data stream**
3. **Configure Stream Settings**:
   - **Stream Name**: 1-128 characters (e.g., `test-stream`)
   - **Capacity Mode**: Choose between:
     - 📊 **Provisioned**: Specify shard count
     - 🔄 **On-demand**: AWS manages capacity automatically

### Step 2: Capacity Planning 📐

Use the **Shard Estimator** to determine optimal configuration:

![Kinesis Shard Estimator](https://raw.githubusercontent.com/JoseLuisSR/awsmeter/main/doc/img/kinesis/KinesisShardEstimator.png)

**Input Parameters**:
- 📏 **Average record size** (bytes)
- ⚡ **Records per second**
- 👥 **Number of consumers**

**Shard Throughput Limits**:
- ✍️ **Write**: 1 MiB/sec, 1,000 records/sec per shard
- 📖 **Read**: 2 MiB/sec per shard

### Step 3: Platform-Specific Setup

#### 🏠 LocalStack Setup
```bash
# Start LocalStack with Kinesis
localstack start -d

# Create stream via CLI
aws --endpoint-url=http://localhost:4566 kinesis create-stream \
    --stream-name test-stream \
    --shard-count 1
```

For detailed setup instructions, refer to the [main awsmeter installation guide](../../../../../../../../../README.md).

## ⚙️ Configuration

> 💡 **AWS Credentials:** Configure your AWS credentials as described in the [Authentication section](../../../../../../../../../README.md#aws-authentication).

### JMeter Test Plan Configuration 🎯

![Kinesis Producer Sampler](https://raw.githubusercontent.com/JoseLuisSR/awsmeter/main/doc/img/kinesis/KinesisProducerJavaSampler.png)

#### Required Parameters

| Parameter | Description | Example |
|-----------|-------------|---------|
| 🏷️ **Stream Name** | Target Kinesis stream | `test-stream` |
| 🔑 **Partition Key** | Data distribution key (max 256 chars) | `${__counter()}` |
| 📦 **Data Record** | Event payload (max 1 MB) | `{"timestamp": "${__time()}", "data": "test"}` |


### Best Practices for Load Testing 🎯

#### Partition Key Strategy
```jmeter
# Use Counter for even distribution
${__counter(TRUE,)}

# Use UUID for unique keys
${__UUID()}

# Use custom logic for specific patterns
custom-key-${__threadNum}-${__counter()}
```

#### Data Record Patterns
```json
{
  "timestamp": "${__time(yyyy-MM-dd'T'HH:mm:ss.SSS'Z')}",
  "threadId": "${__threadNum}",
  "iteration": "${__counter()}",
  "payload": "${__RandomString(100,abcdefghijklmnopqrstuvwxyz)}"
}
```

## 📊 Testing & Monitoring

### Performance Testing Scenario 🏃‍♂️

**Test Configuration**:
- 🧵 **Threads**: 1
- ⏱️ **Duration**: 300 seconds (5 minutes)
- 📈 **Throughput**: 1 TPS

### Execution Steps

1. **Configure JMeter Test Plan** 📝
   - Open the provided test plan from this repository
   - Fill in AWS credentials and stream parameters

2. **Execute Load Test** ▶️
   - Run the Kinesis Thread Group
   - Monitor real-time results

3. **Analyze Results** 📈
   ![Test Results](https://raw.githubusercontent.com/JoseLuisSR/awsmeter/main/doc/img/kinesis/awsmeter-kinesis-producer-test.png)

### Data Verification 🔍

#### Retrieve Records via AWS CLI

1. **Get Shard Iterator** 🎯
```bash
aws kinesis get-shard-iterator \
    --stream-name {stream-name} \
    --shard-id {shard-id} \
    --shard-iterator-type AT_SEQUENCE_NUMBER \
    --starting-sequence-number {sequence-number}
```

2. **Fetch Records** 📥
```bash
aws kinesis get-records \
    --shard-iterator {shard-iterator}
```

![CLI Results](https://raw.githubusercontent.com/JoseLuisSR/awsmeter/main/doc/img/kinesis/aws-cli-kinesis-get-records.png)

### CloudWatch Monitoring 📊

Monitor these key metrics for performance insights:

![CloudWatch Metrics](https://raw.githubusercontent.com/JoseLuisSR/awsmeter/main/doc/img/kinesis/kinesis-cloudwatch-metrics.png)

#### Critical Metrics

| Metric | Description | Use Case |
|--------|-------------|----------|
| 📈 **IncomingRecords** | Successfully published records | Validate test throughput |
| ✅ **PutRecord.Success** | Success rate percentage | Monitor error rates |
| 📦 **IncomingBytes** | Total bytes ingested | Capacity planning |
| 💾 **PutRecord.Bytes** | Bytes per PutRecord operation | Size optimization |

#### Metric Analysis 🔍
- **Capacity Planning**: Use metrics to validate shard estimator settings
- **Performance Optimization**: Identify bottlenecks and scaling needs
- **Cost Management**: Monitor usage patterns for cost optimization

For comprehensive monitoring setup, visit [CloudWatch Metrics for Kinesis](https://docs.aws.amazon.com/streams/latest/dev/monitoring-with-cloudwatch.html).

### Alternative Testing Tools 🛠️

#### Kinesis Data Generator
For additional testing capabilities, consider the [Kinesis Data Generator](https://awslabs.github.io/amazon-kinesis-data-generator/web/help.html):

**Features**:
- 🎨 **Web-based UI** for easy configuration
- 📊 **Real-time metrics** and monitoring
- 🔧 **Template-based** data generation

**Deployment**: Uses CloudFormation with Lambda, Cognito, and IAM services.

---

**Happy Testing!** 🎉 This guide should help you efficiently test AWS Kinesis Data streams using awsmeter and JMeter.
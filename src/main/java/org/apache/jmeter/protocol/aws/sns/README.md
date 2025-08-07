# 📨 AWS SNS (Simple Notification Service)

AWS SNS is a fully managed message broker service that enables publish-subscribe (pub/sub) messaging patterns for both application-to-application (A2A) and application-to-person (A2P) communication scenarios.

This guide provides specific instructions for testing AWS SNS (Simple Notification Service) using the `awsmeter` plugin with Apache JMeter. For general installation and setup instructions, please refer to the [instructions](../../../../../../../../../README.md).

## 🏗️ Architecture Overview

### Application-to-Application (A2A) Messaging
SNS publishes messages to topics, stores them reliably, and delivers them to all registered subscribers. This pattern enables loose coupling between distributed applications and services.

### Application-to-Person (A2P) Messaging  
SNS delivers notifications directly to end users through multiple channels including mobile applications, SMS, and email addresses.

### 🔧 Key Features
- **🌍 Multi-AZ Replication:** Messages replicated across three availability zones
- **⚡ Auto-scaling:** Handles varying throughput automatically  
- **🔒 Server-side Encryption (SSE):** Protects sensitive data at rest
- **🎯 Message Filtering:** Routes messages based on attributes
- **📊 CloudWatch Integration:** Comprehensive monitoring and metrics

## 📋 SNS Topic Types

### 🔄 Standard Topics
- **Delivery:** At-least-once delivery (possible duplicates)
- **Ordering:** Best-effort ordering
- **Throughput:** Unlimited API calls per second
- **Subscribers:** SQS Standard Queues, Kinesis Data Firehose, Lambda, HTTP/HTTPS endpoints

### 📥 FIFO Topics  
- **Delivery:** Exactly-once delivery guarantee
- **Ordering:** Strict first-in-first-out message ordering
- **Throughput:** Limited by FIFO constraints
- **Subscribers:** Only SQS FIFO Queues (to maintain ordering guarantees)

> ⚠️ **Important:** FIFO topics cannot deliver to customer-managed endpoints (email, mobile apps, SMS, HTTP endpoints) as these don't guarantee message ordering.

## 🛠️ Prerequisites

Before you begin, ensure you have completed the [general prerequisites](../../../../../../../../../README.md#prerequisites) and:

- ✅ AWS account with SNS permissions
- ✅ Basic understanding of AWS IAM and SQS concepts
- ✅ AWS CLI configured (optional, for LocalStack testing)

## ☁️ AWS SNS Setup

### Create SNS Topic
1. **Sign in** to AWS Management Console
2. **Navigate** to SNS → Topics → Create topic
3. **Select** topic type (Standard or FIFO)
4. **Enter** topic name
   - Standard: Any valid name
   - FIFO: Must end with `.fifo` suffix
5. **Configure** FIFO options (if applicable):
   - Enable content-based deduplication (optional)
6. **Enable** server-side encryption (recommended)
7. **Set** access permissions for your IAM user
8. **Create** the topic

### Configure IAM Permissions
Your IAM user needs the following policy for SNS access:

```json
{
    "Version": "2012-10-17",
    "Statement": [
        {
            "Effect": "Allow",
            "Action": [
                "sns:Publish",
                "sns:GetTopicAttributes"
            ],
            "Resource": "arn:aws:sns:*:*:*"
        }
    ]
}
```

## 🐳 LocalStack Setup (Development & Testing)

LocalStack provides a local AWS cloud stack perfect for development and testing without AWS costs.

### Create Standard Topic

```bash
# Create a standard SNS topic
aws --endpoint-url=http://localhost:4566 sns create-topic \
    --name my-standard-topic

# Response example:
# {
#     "TopicArn": "arn:aws:sns:us-east-1:000000000000:my-standard-topic"
# }
```

### Create FIFO Topic

```bash
# Create a FIFO SNS topic
aws --endpoint-url=http://localhost:4566 sns create-topic \
    --name my-fifo-topic.fifo \
    --attributes FifoTopic=true,ContentBasedDeduplication=true

# Response example:
# {
#     "TopicArn": "arn:aws:sns:us-east-1:000000000000:my-fifo-topic.fifo"
# }
```

### Create SQS Queue for Testing (Optional)

```bash
# Create standard SQS queue
aws --endpoint-url=http://localhost:4566 sqs create-queue \
    --queue-name my-test-queue

# Create FIFO SQS queue
aws --endpoint-url=http://localhost:4566 sqs create-queue \
    --queue-name my-test-queue.fifo \
    --attributes FifoQueue=true,ContentBasedDeduplication=true
```

### Create SNS Subscription

```bash
# Subscribe SQS queue to SNS topic
aws --endpoint-url=http://localhost:4566 sns subscribe \
    --topic-arn arn:aws:sns:us-east-1:000000000000:my-standard-topic \
    --protocol sqs \
    --notification-endpoint arn:aws:sqs:us-east-1:000000000000:my-test-queue
```

> 💡 **Pro Tip:** Use the topic ARNs returned by LocalStack in your JMeter test configuration. They follow the format: `arn:aws:sns:us-east-1:000000000000:topic-name`

## ⚙️ Configuration

> 💡 **AWS Credentials:** Configure your AWS credentials as described in the [Authentication section](../../../../../../../../../README.md#aws-authentication).

### Standard Topic Configuration

![Standard Topic Sampler](https://raw.githubusercontent.com/JoseLuisSR/awsmeter/main/doc/img/sns/SNSStandardProducerJavaSampler.png)

**Required Parameters:**

| Parameter | Description | Example |
|-----------|-------------|---------|
| `sns_topic_arn` | Topic Amazon Resource Name | `arn:aws:sns:us-east-1:123456789012:my-topic` |
| `sns_msg_body` | Message content (1 char - 256 KB) | `{"event": "user_signup", "userId": "12345"}` |
| `sns_msg_attributes` | Message metadata (JSON format) | See example below |

**Message Attributes Format:**
```json
[
    {
        "name": "environment",
        "type": "String",
        "value": "production"
    },
    {
        "name": "priority",
        "type": "Number", 
        "value": "1"
    },
    {
        "name": "categories",
        "type": "String.Array",
        "value": "[\"notification\", \"user-event\"]"
    },
    {
        "name": "signature",
        "type": "Binary",
        "value": "aGVsbG8gd29ybGQ="
    }
]
```

> 📝 **Note:** Maximum 10 attributes per message. Total message size (body + attributes) cannot exceed 256 KB.

### FIFO Topic Configuration

![FIFO Topic Sampler](https://raw.githubusercontent.com/JoseLuisSR/awsmeter/main/doc/img/sns/SNSFIFOProducerJavaSampler.png)

**Additional FIFO Parameters:**

| Parameter | Description | Required | Max Length |
|-----------|-------------|----------|------------|
| `sns_msg_group_id` | Groups related messages for ordered processing | ✅ Yes | 128 characters |
| `sns_msg_deduplication_id` | Prevents duplicate messages (5-minute window) | ✅ Yes* | 128 characters |

> *Not required if content-based deduplication is enabled on the topic.

## 📊 Monitoring & Performance Analysis

### CloudWatch Integration

SNS automatically publishes comprehensive metrics to CloudWatch for monitoring message publishing and delivery performance.

![CloudWatch Metrics](https://raw.githubusercontent.com/JoseLuisSR/awsmeter/main/doc/img/sns/SNSCloudWatchMetrics.png)

### Key Performance Metrics

| Metric | Description | Performance Indicator |
|--------|-------------|---------------------|
| `NumberOfMessagesPublished` | Total messages published to topics | Publishing throughput |
| `NumberOfNotificationsDelivered` | Successful message deliveries | Delivery success rate |
| `NumberOfNotificationsFailed` | Failed message deliveries | Error rate monitoring |
| `PublishSize` | Average message payload size | Throughput optimization |

### 🎯 Performance Analysis Guidelines
- **✅ Equal publish/delivery counts:** Healthy message flow
- **⚠️ Delivery failures:** Check subscriber availability and permissions
- **📈 High message sizes:** Monitor impact on throughput and costs
- **🔄 FIFO constraints:** Expect lower throughput compared to Standard topics

---

**Happy Testing!** 🎉 This guide should help you efficiently test AWS SNS topics using awsmeter and JMeter.
# 📬 AWS SQS (Simple Queue Service)

AWS Simple Queue Service (SQS) is a fully managed message queuing service that enables you to decouple and scale microservices, distributed systems, and serverless applications. 

This guide provides specific instructions for testing AWS Simple Queue Service (SQS) using the `awsmeter` plugin with Apache JMeter. For general installation and setup instructions, please refer to the [instructions](../../../../../../../../../README.md).

## 🔍 Overview

### What is AWS SQS? 
AWS SQS is a serverless message queuing service that acts as a middleware between distributed components. It provides:

- **Decoupling**: Producers and consumers don't need to know about each other
- **Scalability**: AWS automatically scales to handle your throughput needs
- **Reliability**: Messages are replicated across multiple Availability Zones
- **Security**: Integrated with AWS IAM for fine-grained access control

### Queue Types 📦

**Standard Queues** 
- ✅ Unlimited throughput
- ✅ At-least-once delivery
- ⚠️ Best-effort ordering (messages may arrive out of order)
- ⚠️ Possible duplicate messages

**FIFO Queues**
- ✅ Exactly-once processing
- ✅ First-in-first-out delivery
- ✅ No duplicates
- ⚠️ Limited to 300 API calls per second (3,000 with batching)
- ⚠️ Must end with `.fifo` suffix

## 🛠️ Prerequisites

Before you begin, ensure you have completed the [general prerequisites](../../../../../../../../../README.md#prerequisites) and:

- ✅ AWS account with SQS permissions
- ✅ Basic understanding of AWS IAM and SQS concepts
- ✅ AWS CLI configured (optional, for LocalStack testing)

## ☁️ AWS SQS Setup

### Step 1: Create SQS Queue 🏗️

1. **Access AWS Console**
   - Navigate to SQS service
   - Click "Create queue"

2. **Choose Queue Type**
   - Select **Standard** for high throughput scenarios
   - Select **FIFO** for guaranteed message ordering

3. **Configure Queue Settings**
   ```
   Queue name: test-queue (add .fifo for FIFO queues)
   Visibility timeout: 30 seconds (recommended starting point)
   Delivery delay: 0 seconds (unless delayed processing needed)
   Message retention: 4 days (default)
   Receive message wait time: 0 seconds (short polling)
   ```

4. **Set Access Permissions** 🔐
   Create an IAM policy with minimum required permissions:
   ```json
   {
     "Version": "2012-10-17",
     "Statement": [
       {
         "Effect": "Allow",
         "Action": [
           "sqs:SendMessage",
           "sqs:ReceiveMessage",
           "sqs:DeleteMessage",
           "sqs:GetQueueAttributes",
           "sqs:GetQueueUrl"
         ],
         "Resource": "arn:aws:sqs:region:account-id:queue-name"
       }
     ]
   }
   ```

### Step 2: Create IAM User for Testing 👤

1. **Create programmatic access user**
   - Navigate to IAM > Users > Create user
   - Enable "Programmatic access" only
   - Attach the SQS policy created above

2. **Securely store credentials**
   - Access Key ID
   - Secret Access Key
   - Session Token (if using temporary credentials)

## 🐳 LocalStack Setup (Development & Testing)

LocalStack provides a local AWS cloud stack perfect for development and testing without AWS costs.

### Create Test Queues
```bash
# Set LocalStack endpoint
export AWS_ENDPOINT_URL=http://localhost:4566

# Create Standard queue
aws --endpoint-url=http://localhost:4566 sqs create-queue \
    --queue-name test-standard-queue

# Create FIFO queue
aws --endpoint-url=http://localhost:4566 sqs create-queue \
    --queue-name test-fifo-queue.fifo \
    --attributes FifoQueue=true,ContentBasedDeduplication=true

# Verify queues were created
aws --endpoint-url=http://localhost:4566 sqs list-queues
```

## ⚙️ Configuration

> 💡 **AWS Credentials:** Configure your AWS credentials as described in the [Authentication section](../../../../../../../../../README.md#aws-authentication).

### Standard Queue Configuration 📊

![Standard Queue Sampler](https://raw.githubusercontent.com/JoseLuisSR/awsmeter/main/doc/img/sqs/SQSStandardProducerJavaSampler.png)

**Required Parameters:**
- **sqs_queue_name**: Queue name (include `.fifo` suffix for FIFO queues)
- **sqs_msg_body**: Message content (1 character - 256 KB)
- **access_key_id**: AWS Access Key ID
- **secret_access_key**: AWS Secret Access Key
- **region**: AWS region (e.g., `us-east-1`)

**Optional Parameters:**
- **session_token**: For temporary credentials or federated access
- **profile**: AWS CLI profile name (alternative to access keys)
- **endpoint_url**: Custom endpoint (`http://localhost:4566` for LocalStack)

### FIFO Queue Configuration 🔄

![FIFO Queue Sampler](https://raw.githubusercontent.com/JoseLuisSR/awsmeter/main/doc/img/sqs/SQSFIFOProducerJavaSampler.png)

**Additional Required Parameters for FIFO:**
- **sqs_msg_group_id**: Groups messages for FIFO processing (max 128 characters)
- **sqs_msg_deduplication_id**: Prevents duplicates (can be auto-generated)

### Message Attributes Format 📝

Use this JSON structure for message attributes:

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
    "name": "correlation-id",
    "type": "String",
    "value": "abc-123-def"
  },
  {
    "name": "binary-data",
    "type": "Binary",
    "value": "SGVsbG8gV29ybGQ="
  }
]
```

**Constraints & Considerations:**
- Maximum 10 attributes per message
- Attributes count towards the 256 KB message size limit
- Supported types: `String`, `Number`, `Binary`
- Custom data types allowed: `Binary.gif`, `String.custom`, etc.

### Delay Configuration ⏱️

**sqs_delay_seconds Parameter:**
- **Range**: 0-900 seconds (15 minutes maximum)
- **Purpose**: Delays message delivery to consumers
- **Use cases**: Retry scenarios, scheduled processing, rate limiting
- **FIFO limitation**: Can only be set at queue level, not per message

## 📊 Monitoring & Observability

### CloudWatch Metrics Dashboard 📈

![CloudWatch Metrics](https://raw.githubusercontent.com/JoseLuisSR/awsmeter/main/doc/img/sqs/SQSCloudWatchMetrics.png)

**Key Metrics to Monitor:**

1. **NumberOfMessagesSent** 📤
   - **Description**: Messages successfully published to queue
   - **Expected**: Should match your test plan's message count
   - **Alert threshold**: Drops below expected rate

2. **NumberOfMessagesReceived** 📥  
   - **Description**: Messages retrieved by consumers
   - **Expected**: Should be ≤ NumberOfMessagesSent
   - **Alert threshold**: Significantly lower than sent messages

3. **NumberOfMessagesDeleted** 🗑️
   - **Description**: Successfully processed and removed messages
   - **Expected**: Should closely match NumberOfMessagesReceived
   - **Alert threshold**: Processing rate drops significantly

4. **ApproximateNumberOfVisibleMessages** 👁️
   - **Description**: Messages available for processing
   - **Expected**: Should remain low during normal operation
   - **Alert threshold**: Sustained high values indicate consumer issues

### Interpreting Metrics 🔍

**Healthy System Indicators:**
- Sent ≈ Received ≈ Deleted (within 5% variance)
- Visible message count remains low (<100 for high-throughput systems)
- Consistent message processing rate
- Error rates below 0.1%

**Problem Indicators:**
- High visible message count (consumer bottleneck or failure)
- Sent > Received (authentication, permissions, or connectivity issues)
- Received > Deleted (message processing failures)
- Irregular spikes in error rates

### JMeter Performance Monitoring

**Key JMeter Metrics:**
- **Response Time**: Should be <200ms for SQS operations
- **Throughput**: Actual vs expected message rate
- **Error Rate**: Should be <1% under normal conditions
- **Active Threads**: Monitor for thread starvation

---

**Happy Testing!** 🎉 This guide should help you efficiently test AWS SQS queues using awsmeter and JMeter.
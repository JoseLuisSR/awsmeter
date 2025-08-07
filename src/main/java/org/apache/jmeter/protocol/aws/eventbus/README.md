# 🎫 AWS EventBridge 

Amazon EventBridge is a **serverless event bus service** that enables seamless integration between applications using real-time data streams. This service facilitates the creation of **event-driven architectures** that are loosely coupled, scalable, and fault-tolerant.

This guide provides specific instructions for testing AWS EventBridge using the `awsmeter` plugin with Apache JMeter. For general installation and setup instructions, please refer to the [instructions](../../../../../../../../../README.md).

## Overview 📋

EventBridge acts as a central hub for routing events between AWS services, SaaS applications, and custom applications without managing infrastructure.

Key capabilities include:

- 🔄 **Event Routing** Automatically routes events from multiple sources to designated targets
- 📈 **Auto Scaling** Handles varying event volumes with automatic scaling across availability zones
- 🛡️ **Built-in Reliability** Includes retry mechanisms and dead letter queues for failed deliveries
- 🎯 **Rule-based Filtering** Uses flexible patterns to match and route specific events
- 🔌 **Native Integrations** Connects with 90+ AWS services and popular SaaS applications
- ⚡ **Real-time Processing** Delivers events with low latency for time-sensitive applications
- 🏗️ **Serverless Architecture** No infrastructure to provision or manage
- 🔒 **Secure by Default** Built-in encryption and IAM integration for access control

EventBridge eliminates the need to manage infrastructure for event processing, allowing you to focus on building robust, distributed systems.

## Prerequisites ✅

Before using `awsmeter` with EventBridge, ensure you have:

### AWS Environment
- 🔐 **AWS Account** with appropriate permissions
- 👤 **IAM User** with `events:PutEvents` permission
- 🔑 **AWS Credentials** (Access Key ID, Secret Access Key, and optionally Session Token)

### Development Environment  
- ☕ **JMeter** with `awsmeter` plugin installed ([installation guide](../../../../../../../../../README.md))
- 📁 **AWS CLI** (optional, for verification commands)
- 🌐 **LocalStack** (optional, for local testing)

## Setup 🛠️

### AWS EventBridge Setup

1. **Create Event Bus** 🏗️
   ```bash
   # Using AWS CLI
   aws events create-event-bus --name "your-custom-eventbus"
   
   # Or use the default event bus (no setup required)
   ```

2. **Configure Event Rules** (Optional) 📝
   - Navigate to: **Amazon EventBridge > Rules > Create rule**
   - Define event patterns to match specific events
   - Set targets (Lambda, SQS, SNS, etc.)

3. **Set IAM Permissions** 🔒
   ```json
   {
     "Version": "2012-10-17",
     "Statement": [
       {
         "Effect": "Allow",
         "Action": "events:PutEvents",
         "Resource": "arn:aws:events:*:*:event-bus/*"
       }
     ]
   }
   ```

### LocalStack Setup (Local Testing) 🏠

1. **Start LocalStack**
   ```bash
   docker run -d -p 4566:4566 localstack/localstack
   ```

2. **Configure AWS CLI for LocalStack**
   ```bash
   aws configure set endpoint-url http://localhost:4566
   ```

3. **Create Local Event Bus**
   ```bash
   aws events create-event-bus --name "local-test-bus" --endpoint-url http://localhost:4566
   ```

## Configuration ⚙️

> 💡 **AWS Credentials:** Configure your AWS credentials as described in the [Authentication section](../../../../../../../../../README.md#aws-authentication).

### JMeter Test Plan Configuration

![EventBridge Configuration](https://raw.githubusercontent.com/JoseLuisSR/awsmeter/main/doc/img/eventbus/EventBridgeStreamJavaSampler.png)

#### Required Parameters

| Parameter | Description | Example |
|-----------|-------------|---------|
| 🏷️ **event_bus_name** | Target event bus name | `"default"` or `"my-custom-bus"` |
| 📍 **event_source** | Event source identifier | `"myapp.orders"` or `"com.example.service"` |
| 🏷️ **event_detail_type** | Event type description | `"Order Placed"` or `"User Registration"` |
| 📄 **event_detail** | JSON event payload (max 256KB) | `{"orderId": "12345", "amount": 99.99}` |

### Sample Event Configuration

```json
{
  "event_bus_name": "my-application-bus",
  "event_source": "ecommerce.orders",
  "event_detail_type": "Order Processed",
  "event_detail": {
    "orderId": "ORD-2025-001",
    "customerId": "CUST-12345",
    "amount": 149.99,
    "currency": "USD",
    "timestamp": "2025-08-07T10:30:00Z"
  }
}
```

## Monitoring 📈

### CloudWatch Metrics

EventBridge automatically publishes performance metrics to CloudWatch:

| Metric | Description | Use Case |
|--------|-------------|----------|
| 📥 **IngestedEvents** | Events received by EventBridge | Monitor event volume |
| 🎯 **MatchedRules** | Rules matched by incoming events | Track routing efficiency |
| ✅ **SuccessfulInvocations** | Successful target invocations | Measure delivery success |
| ❌ **FailedInvocations** | Failed target invocations | Identify delivery issues |

### Monitoring Best Practices

1. **Set Up Alarms** 🚨
   - Configure CloudWatch alarms for failed invocations
   - Monitor unusual spikes in event volume
   - Track rule matching efficiency

2. **Performance Analysis** 📊
   - Use JMeter's built-in reporters for load testing metrics
   - Correlate JMeter results with CloudWatch metrics
   - Analyze event patterns over time

3. **Troubleshooting** 🔍
   - Check event IDs in JMeter responses
   - Verify target configurations for failed invocations
   - Review CloudWatch Logs for detailed error information

For comprehensive monitoring setup, visit [CloudWatch Metrics for EventBridge](https://docs.aws.amazon.com/eventbridge/latest/userguide/eb-monitoring.html).

---

**Happy Testing!** 🎉 This guide should help you efficiently test AWS Event Bridge using awsmeter and JMeter.
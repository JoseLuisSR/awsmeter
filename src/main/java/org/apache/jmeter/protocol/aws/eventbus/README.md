# Event Bus (Amazon EventBridge)

Amazon EventBridge is a serverless event bus service that makes it easy to connect applications using data from your own applications, integrated SaaS applications, and AWS services. EventBridge delivers a stream of real-time data from event sources such as AWS services, your own applications, and SaaS applications and routes that data to targets such as AWS Lambda.

EventBridge allows you to build event-driven architectures, which are loosely coupled and distributed. When an event occurs, EventBridge routes the event to the appropriate targets based on rules you define. This enables you to decouple your applications and build scalable, fault-tolerant systems.

EventBridge is a serverless service where AWS manages the infrastructure needed for event routing and delivery across multiple availability zones. The service automatically scales to handle varying event volumes and provides built-in retry logic and dead letter queues for failed event deliveries.

# Setting Up

Before using `awsmeter` to publish events to EventBridge, you need to create an event bus and configure event rules. Below are the steps to set up EventBridge:

1. Sign-on to your AWS account.

2. Go to Amazon EventBridge > Event buses > Create event bus.

3. Enter the event bus name. You can use the default event bus or create a custom one.

4. (Optional) Set up event rules to route events to targets like Lambda functions, SQS queues, or SNS topics.

5. Configure IAM permissions to allow your application to publish events to the event bus. The IAM user needs the `events:PutEvents` permission.

6. Create the event bus.

More details: [Creating and Managing Event Buses](https://docs.aws.amazon.com/eventbridge/latest/userguide/eb-create-event-bus.html).

# Getting Started

![Screenshot](https://raw.githubusercontent.com/JoseLuisSR/awsmeter/main/doc/img/eventbus/EventBridgeStreamJavaSampler.png)

After you installed `awsmeter` in JMeter, you can start using it to publish events to EventBridge. You need to fill the fields to connect to AWS using an IAM user with programmatic access. [awsmeter install details](https://github.com/JoseLuisSR/awsmeter).

You can find a JMeter Test Plan in this repository that was configured with EventBridge Producer. You just need to fill the below fields to execute the test:

* **event_bus_name**: The name of the event bus where you want to publish events. Use "default" for the default event bus or specify your custom event bus name.

* **event_source**: The source of the event. This is a string that identifies the application or service that generated the event. For example, "myapp.orders" or "com.example.app".

* **event_detail_type**: A string that describes the type of event. This helps consumers understand what kind of event they're receiving. For example, "Order Placed" or "User Registration".

* **event_detail**: The event payload in JSON format. This contains the actual data of your event. The maximum size for an event is 256 KB.

# Testing

It's time to test. We are going to execute a single thread to publish events to EventBridge:

1. Open JMeter test plan (present in this repository) in JMeter and go to EventBridge Thread Group > EventBridge Producer.

2. Fill the parameters to connect to AWS using **Access key id**, **Secret Access Key** and **Session Token**. If you have [credentials file](https://docs.aws.amazon.com/cli/latest/userguide/cli-configure-files.html) in your work area, you just need to enter the name of the **profile** to get the access key id, secret access key and aws region from credentials and config file.

3. Enter the parameters described in **Getting Started** section.

4. Execute EventBridge Thread. When it finishes, you can see details per event including the time spent publishing the event to EventBridge, the response data, and metrics about the average, min and max time of the requests to EventBridge.

![Screenshot](https://raw.githubusercontent.com/JoseLuisSR/awsmeter/main/doc/img/eventbus/awsmeter-eventbridge-stream-test.png)

Now you can verify that events were published successfully:

5. Check the response of the requests to see the event IDs that were assigned by EventBridge.

6. If you have configured event rules and targets, check those targets to verify that events were routed correctly.

7. You can also use AWS CLI to describe your event bus and rules:

```
aws events describe-event-bus --name {eventBusName}
aws events list-rules --event-bus-name {eventBusName}
```

# CloudWatch Metrics

EventBridge sends information to CloudWatch about the events published, matched rules, and successful/failed invocations. CloudWatch generates metrics that help you monitor the performance and reliability of your event-driven applications.

The CloudWatch metrics for EventBridge include:

* **IngestedEvents**: The number of events received by EventBridge.

* **MatchedRules**: The number of rules that were matched by incoming events.

* **SuccessfulInvocations**: The number of successful invocations of targets.

* **FailedInvocations**: The number of failed invocations of targets.

You can use this information to monitor the health of your event-driven architecture, identify bottlenecks, and set up alerts for failed event deliveries. The CloudWatch metrics are very useful for analyzing event patterns over time and for troubleshooting issues in your event routing.

For more details, visit [CloudWatch Metrics for EventBridge](https://docs.aws.amazon.com/eventbridge/latest/userguide/eb-monitoring.html).
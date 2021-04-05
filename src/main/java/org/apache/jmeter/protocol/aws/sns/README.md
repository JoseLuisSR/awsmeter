# SNS (Simple Notification Service)

AWS SNS is message broker using Publish and Subscribe messaging style to asynchronous communication between applications (A2A) and application to person (A2P).

Using Amazon SNS for application-to-application (A2A) messaging, the messages are publishing in SNS topic, SNS store and deliver the message to all topic subscribers, also SNS register the subscribers by a topic.

Using Amazon SNS for application-to-person (A2P) messaging, for user notifications with subscribers such as mobile applications, mobile phone numbers, and email addresses.

AWS SNS is serverless service, AWS replicate the topics and messages in three availability zones, auto scaling to handle input and output throughput, Server-side encryption (SSE) storing sensitive data in encrypted topics. Less administrative tasks for you

Amazon SNS support message filtering. Using message filtering simplifies your architecture by offloading the message routing logic from your publisher systems and the message filtering logic from your subscriber systems and this is a great feature of SNS to use Event Driven Architecture.

There are two types of SNS Topics:

* **Standard:** You can publish messages in Standard Topic and subscribe to it, the messages are delivered at least once, is possible the message can repeat, also AWS tries to delivery the message in the same order were sent.  The throughput for this topic is unlimited support many API calls per second.
Standard Topic support different kind of subscribers like SQS Standard Queue, Kinesis Fire Hose, Lambda and HTTP end-point (HTTP POST Request).
  

* **FIFO:** All messages published on FIFO Topic are Exactly-once message delivery to subscribers and in the same order the message was published. To preserve strict message ordering, Amazon SNS restricts the set of supported delivery protocols for Amazon SNS FIFO topics.
  SNS FIFO topics can't deliver messages to customer managed endpoints, such as email addresses, mobile apps, phone numbers for text messaging (SMS), or HTTP(S) endpoints. These endpoint types aren't guaranteed to preserve strict message ordering.

# Setting up

To start using `awsmeter` to produce message and publish in a topic you need first create SNS topic, follow the below steps:

1. Sig-on AWS account.


2. Go to SNS > Topics > Create topic.


3. Choose the queue type, Standard or FIFO.


4. Enter the topic name, for FIFO topic must end with the `.fifo` suffix.


5. For FIFO Topics you can enable Content-based message deduplication option where AWS use message body to calculate deduplication id, before use it make sure the content of the message is always different.


6. (Optional) Enable a server side encryption adds at-rest encryption to your topic. Amazon SNS encrypts your message as soon as it is received. The message is decrypted immediately prior to delivery.


6. The next step is define who can publish messages to Topic, since `awsmeter` use AWS IAM User with programmatic access you need to enable it user to Publish a message, add Policy for SNS Service with the action Publish.


7. Create the Topic. You can leave the rest fields with default values, for more details of each one go to [Creating an Amazon SNS Topic](https://docs.aws.amazon.com/sns/latest/dg/sns-getting-started.html).

# Getting Started

After you installed `awsmeter` in JMeter you can start using it to connect and publish messages on SNS topics. There are two Java Request Sampler per topic type (Standard and FIFO) each one has fields to set up the topic arn, message body, message attributes and others parameters, also you need to
fill the fields to connect an AWS account in a region and IAM user with programmatic access.

You can find JMeter Test Plan in this repository that was configured with SNS Standard and FIFO Thread Group ready to use, you just need to fill few fields to execute test.

The fields of each Java Request Sampler per SNS topic type are:

## Standard

![Screenshot](https://raw.githubusercontent.com/JoseLuisSR/awsmeter/main/doc/img/sns/SNSStandardProducerJavaSampler.png)

* **sns_topic_arn:** Amazon Resource Name (arn) of the Topic, you can find it in Details section of the Topic.


* **sns_msg_body:** The content of the message that you want to publish. A message can include only XML, JSON, and unformatted text. The minimum size of the message body is one character, and the maximum size is 256 KB.


* **sns_msg_attributes:** You can send message metadata with this parameter like the number of times reprocessing a message or geography location of the producer and  just use the message body for business information. The parts of the message attributes are the Name,
  Type and Value, only the types String, String.Array, Number and Binary are allowed, the maximum number of attributes is ten. Message attributes and body are part of the message size restriction (256 KB or 262,144 bytes).

  You need to use the below JSON structure that `awsmeter` used to send message attributes:

````
[
    {
        "name": "attribute-1-name",
        "type": "String",
        "value": "aws"
    },
    {
        "name": "attribute-1-name",
        "type": "String.Array",
        "value": "aws"
    },    
    {
        "name": "attribute-2-name",
        "type": "Number",
        "value": "meter"
    },
    {
        "name": "attribute-3-name",
        "type": "Binary",
        "value": "sqs"
    }...
]

````

## FIFO


![Screenshot](https://raw.githubusercontent.com/JoseLuisSR/awsmeter/main/doc/img/sns/SNSFIFOProducerJavaSampler.png)


There are some fields share between Standard and FIFO topic like topic arn, message body and message attributes, but there are parameters that only apply for FIFO queue, those are:

* **sns_msg_group_id:** [Message group id](https://docs.aws.amazon.com/sns/latest/dg/fifo-message-grouping.html), all the messages that belongs to the same message group are process in FIFO manner (First-In-First-Out), messages with different messages groups might be process out of order. This field is mandatory. 
  If you have SQS FIFO queue subscribed to SNS FIFO topic then the SNS FIFO topic passes the group ID to the subscribed Amazon SQS FIFO queues. The length of MessageGroupId is 128 characters.


* **sns_msg_deduplication_id:** [Message deduplication id](https://docs.aws.amazon.com/sns/latest/dg/fifo-message-dedup.html), AWS uses this parameter to check the message is not repeated in a period of five minutes, any message with the same deduplication id and published during the five minutes deduplication interval is accepted successfully but is not delivered to subscriber.
  When you publish a message to an SNS FIFO topic, the message must include a deduplication ID. This ID is included in the message that the SNS FIFO topic delivers to the subscribed SQS FIFO queues. You can put it in the message,  or you can enable Content-based message deduplication in FIFO topic to use the message body and apply SHA-256 hash to generate the value.
  
# Testing

We are going publish messages in FIFO topic and subscribe a FIFO queue to it topic to receive the messages. The steps to subscribe FIFO topic are: 

1. Create FIFO topic and FIFO queue.


2. Set up access policy on FIFO queue to enable FIFO topic can delivery messages to FIFO queue. You can use [AWS Policy Generator](https://awspolicygen.s3.amazonaws.com/policygen.html) to create SQS policy, specify sqs:SendMessage action over SQS FIFO queue ARN and add condition ArnEquals with the value SNS FIFO topic ARN, you will get the following JSON:

````
    {
      "Sid": "Stmt1616637180289",
      "Effect": "Allow",
      "Principal": {
        "Service": "sns.amazonaws.com"
      },
      "Action": "sqs:SendMessage",
      "Resource": "arn:aws:sqs:{aws-region}:{aws-account-id}:{sqs-fifo-queue-name}",
      "Condition": {
        "ArnEquals": {
          "aws:SourceArn": "arn:aws:sns:{aws-region}:{aws-account-id}:{sns-fifo-topic-name}"
        }
      }
    }
````

Add access policy.

![Screenshot](https://raw.githubusercontent.com/JoseLuisSR/awsmeter/main/doc/img/sns/SQSAccessPolicy.png)

3. Subscribe SQS FIFO queue to SNS FIFO topic. Go to your FIFO topic and select Create subscription option, then choose the protocol (only Amazon SQS is available to guarantee the message order) and input the SQS FIFO queue ARN. Enable raw message delivery send the message without Message Group id, If you want sends the message in order please don't check it option. 
   
   You can set up [Subscription filter policy](https://docs.aws.amazon.com/sns/latest/dg/sns-message-filtering.html) to receive messages with specific attributes. When you configure the message filtering, SNS FIFO topics support at-most-once delivery, as messages can be filtered out based on your subscription filter policies.

![Screenshot](https://raw.githubusercontent.com/JoseLuisSR/awsmeter/main/doc/img/sns/SNSFIFOTopicSubscription.png)


Now we can start execute test.

4. Open JMeter test plan (present in this repository) in JMeter go to SNS FIFO Topic Thread Group > SNS FIFO Topic Producer.


5. Fill the parameters to connect to AWS using Access key id, Secret Access Key and Session Token (optional). If you have credentials file in you work area you just need to enter the name of the profile to get the access key id, secret access key and aws region from credentials and config file.


6. Enter the Topic ARN that you created. Update the message body and attributes for the values you want. Remember to use the AWS region where you created the topic.


7. Execute SNS FIFO Topic Thread Group.

![Screenshot](https://raw.githubusercontent.com/JoseLuisSR/awsmeter/main/doc/img/sns/awsmeter-sns-fifo-topic-producer-test.png)


8. Go to AWS Management Console > Amazon SQS Queues, then Choose the FIFO queue subscribed to SNS FIFO Topic.


9. Poll for messages.

![Screenshot](https://raw.githubusercontent.com/JoseLuisSR/awsmeter/main/doc/img/
/SQSFIFOQueuePollingMessages.png)


# CloudWatch Metrics

SNS sends information to CloudWatch about the messages published, number of messages delivery successful and failed, size of the messages published and more. CloudWatch generate metrics by dimensions like Topic Metrics in a period of time, we can use this information to identify issues publishing or delivery messages. Let's analyze the below image:

![Screenshot](https://raw.githubusercontent.com/JoseLuisSR/awsmeter/main/doc/img/sns/SNSCloudWatchMetrics.png)


We can see two metrics in period of time of 1 minute, the metrics are:

* **NumberOfMessagesPublished:** The number of messages published to your Amazon SNS topics.


* **NumberOfNotificationsDelivered:** The number of messages successfully delivered from your Amazon SNS topics to subscribing endpoints.

These two metrics are showing the same numbers, the amount of messages published is the same the messages delivered, means the SQS FIFO Queue was available to receive the message from SNS FIFO Topic.

There are other metrics, more information go to [Amazon SNS Metrics](https://docs.aws.amazon.com/sns/latest/dg/sns-monitoring-using-cloudwatch.html).
# SQS 

AWS SQS is service to interchange information between systems through messages and queues. Producer application publish messages in queue and consumer application listen the queue to get message and after processed delete it. This is a serverless service where AWS manages the availability, scalability and security (Shared Responsibility Model) of the queue.

Queues is a great middleware for integration between components decoupling the communication between them, producer doesn't know who and where is the consumer, and the consumer doesn't care where and who is the producer.

There are two kinds of SQS queues:

* **Standard:** It is the default queue where you can publish the message in queue and AWS deliver it at least once, that's mean that is possible the message can repeat also AWS try to delivery the message in the same order were sent.
  

* **FIFO:** Messages are publishing in this queue and AWS guarantees deliver it only once and in the same order the message were sent.


# Setting up

To start using `awsmeter` to produce message and publish in queue you need first create SQS queue, follow the below steps:

1. Sig-on AWS account.

   
2. Go to SQS > Create Queue.

   
3. Choose the queue type, Standard or FIFO.

   
4. Enter the queue name, for FIFO queue must end with the `.fifo` suffix.

   
5. I recommend understand and use the fields of the configuration section where you can set up:


* **Visibility timeout:** This is the time that AWS wait receive delete message action before enable the message to process again. If the consumer fails to process the message or delete it then the message become available to process by other consumer.
  
* **Delivery delay:** This is helpful when you need to reprocess the message, you can set up the amount of time the message will be hidden for consumers, AWS control the time and deliver the message to consumer when the time is up.

6. The next step is define who can send and receive messages to the queue, since `awsmeter` use AWS IAM User with programmatic access you need to enable it user to access to queue and add the policy to publish SQS Messages with the actions SendMessage at least.
   

7. Create the queue. You can leave the rest fields with default values, for more details of each one go to [Creating an Amazon SQS Queue (Console)](https://docs.aws.amazon.com/AWSSimpleQueueService/latest/SQSDeveloperGuide/sqs-configure-create-queue.html).

# Getting Started

After you installed `awsmeter` in JMeter you can start using it to connect and sent messages to SQS queues. There are two Java Request Sampler per queue type (Standard and FIFO) each one has fields to set up the queue name, message body and others parameters, also you need to
fill the fields to connect AWS account in a region and IAM user with programmatic access.

You can find JMeter Test Plan in this repository that was configured with SQS Standard and FIFO Thread Group ready to use, you just need to fill few fields to execute test.

The fields of each Java Request Sampler per SQS queue type are:

## Standard

![Screenshot](https://raw.githubusercontent.com/JoseLuisSR/awsmeter/main/doc/img/sqs/SQSStandardProducerJavaSampler.png)

* **sqs_queue_name:** Name of the queue, remember for FIFO queue you need to use `.fifo` suffix at the end.


* **sqs_msg_body:** The content of the message that you want to send to consumer. A message can include only XML, JSON, and unformatted text. The minimum size of the message body is one character and the maximum size is 256 KB.


* **sqs_msg_attributes:** You can send metadata with this parameter like the number of times reprocessing a message or geography location of the producer and  just use the message body for business information. The parts of the message attributes are the Name, 
  Type and Value, only the types String, Number and Binary are allowed, the maximum number of attributes is ten. Message attributes and body are part of the message size restriction (256 KB or 262,144 bytes). 
  
  You need to use the below JSON structure that `awsmeter` used to send message attributes:

[

    {
        "name": "attribute-1-name",
        "type": "String",
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

You can define custom data type label with the format `.custom-data-type` like Binary.gif and Binary.png can help distinguish between file types. [Details of Message Attributes here](https://docs.aws.amazon.com/AWSSimpleQueueService/latest/SQSDeveloperGuide/sqs-message-metadata.html#sqs-message-attributes).

* **sqs_delay_seconds:** When you publish a message in queue, you can set up the time in seconds to delay the delivery of the message to consumer. AWS control the time and validate it before deliver the message. This is  helpful when you need handle reprocessing a message after a period of time, the maximum time is 900 seconds (15 minutes).
  If you don't specify a value, the default value of **Delivery delay** for the queue applies.


## FIFO


![Screenshot](https://raw.githubusercontent.com/JoseLuisSR/awsmeter/main/doc/img/sqs/SQSFIFOProducerJavaSampler.png)


There are some fields share between Standard and FIFO queue like queue name, body and message attributes, but there are parameters that only apply for FIFO queue, those are:

* **sqs_msg_group_id:** Message group id, all the messages that belongs to the same message group are process in FIFO manner (First-In-First-Out), messages with different messages groups might be process out of order. This field is mandatory, you can use it to achieve goals like 
  [Avoid processing duplicate messages in a multiple-Producer/Consumer system](https://docs.aws.amazon.com/AWSSimpleQueueService/latest/SQSDeveloperGuide/using-messagegroupid-property.html). For FIFO queues, there can be a maximum of 20,000 inflight messages, you need to use it parameter in the right way to 
  [Avoid having a large backlog of messages with the same message group ID](https://docs.aws.amazon.com/AWSSimpleQueueService/latest/SQSDeveloperGuide/using-messagegroupid-property.html). The length of MessageGroupId is 128 characters. 


* **sqs_msg_deduplication_id:** AWS use this parameter to check the message is not repeat in a period of five minutes, any message with the same deduplication id and published during the five minutes deduplication interval is accepted successfully but is not delivered to consumer. 
  Every message must have a unique Message Deduplication Id, this is mandatory, you can put it in the message or you can enable Content Based Deduplication in FIFO queue to use the message body and apply SHA-256 hash to generate the value. See [Exactly-once processing](https://docs.aws.amazon.com/AWSSimpleQueueService/latest/SQSDeveloperGuide/FIFO-queues.html#FIFO-queues-exactly-once-processing).


* **sqs_delay_seconds:** Is the same as Standard queue, but you can set this parameter only on a queue level with Delivery delay in the section of configuration.

# Testing

¿Ready to test? Let's do it. We are going to use `awsmeter` to produce and publish messages in Standard queue and use AWS Management Console to receive the messages:

1. Open JMeter test plan (present in this repository) in JMeter go to SQS Standard Queue Thread Group > SQS Producer.


2. Fill the parameters to connect to AWS using **Access key id**, **Secret Access Key** and **Session Token**. If you have [credentials file](https://docs.aws.amazon.com/cli/latest/userguide/cli-configure-files.html) in you work area you just need to enter the name of the **profile** to get the access key id, secret access key and aws region from credentials and config file.


3. Enter the name of the queue that you created. Update the message body and attributes for the values you want. Remember to use the AWS region where you created the queue.


4. Execute SQS Standard Queue Thread.

![Screenshot](https://raw.githubusercontent.com/JoseLuisSR/awsmeter/main/doc/img/sqs/awsmeter-sqs-producer-test.png)

5. Go to AWS Management Console > Amazon SQS Queues, then Choose the queue you want to test and use the option `Send and receive message`.

![Screenshot](https://raw.githubusercontent.com/JoseLuisSR/awsmeter/main/doc/img/sqs/SQSAWSManagementConsole.png)

5. Poll for messages.

![Screenshot](https://raw.githubusercontent.com/JoseLuisSR/awsmeter/main/doc/img/sqs/SQSPollingMessages.png)
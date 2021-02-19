# SQS 

AWS SQS is service to interchange information between systems through messages and queues. Producer application publish messages on queue and consumer application listen the queue the get the new message and after processed delete it. This is a serverless service where AWS manages the availability, scalability and security (Shared Responsibility Model) of the queue.

Queues is a great middleware for integration between components decoupling the communication between them, producer doesn't know who and where is the consumer, and the consumer doesn't care where and who is the producer.

There are two kinds of SQS queues:

* **Standard**: It is the default queue where you can publish the message on queue and AWS deliver it at least once, that's mean that is possible the message can repeat also AWS try to delivery the message in the same order were sent.
  

* **FIFO**: Messages are publishing in this queue and AWS guarantees deliver it only once and the same order the message were sent.


# Setting up

To start using awsmeter to produce message and publish on queue you need first create SQS queue, follow the below steps:

1. Sig-on AWS account

   
2. Go to SQS > Create Queue

   
3. Choose the queue type, Standard or FIFO

   
4. Enter the queue name, for FIFO queue must end with the `.fifo` suffix.

   
5. I recommend understand the fields of the configuration section where you can set up:


* **Visibility timeout**: This is the time that AWS wait receive delete message action before enable it message to process again. If the consumer fails to process the message or delete it then the message become available to process by other consumer.
  
* **Delivery delay**: This is helpful when you need to reprocess the message, you can set up the amount of time the message will be hidden for consumers, AWS control the time and deliver the message when the time is up.

6. The next step is define who can send and receive messages to the queue, since awsmeter use AWS IAM User with programmatic access you need to enable it user to access to queue and add the policy to publish SQS Messages with the actions SendMessage and SendMessageBatch.
   

7. Create the queue. You can leave the rest fields with default values, for more details of each one go to [Creating an Amazon SQS Queue (Console)](https://docs.aws.amazon.com/AWSSimpleQueueService/latest/SQSDeveloperGuide/sqs-configure-create-queue.html).

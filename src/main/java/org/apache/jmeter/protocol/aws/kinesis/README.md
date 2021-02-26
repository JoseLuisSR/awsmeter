# Kinesis Data Stream

Kinesis Data Stream is one of the service to receive and delivery high volume of data from social networks, IoT devices, applications logs and other source of information where produce events streams and we need ingest to store and process at near real time. This service use publish and subscribre pattern where you can handle many producers publishing events stream in Kinesis and Kinesis delivery the events to consumer subscribed to stream.

Kinesis Data Stream natively integrate with other AWS services like Lambda that subscribe to Kinesis stream, receive batch events and invoke Lambda function to process.

Kinesis Data Stream and Kinesis Data Firehose are using together to receive data from many sources, collec the information and then store  in S3 buckets. This solution is for ingest information at near real time in data lake.

Kinesis is a serverless services where AWS managing the infrastructure needed to data replication in three availability zones and also scaling the resources to process gigabytes of streaming data per second. You need set up the data stream capacity specifying the avarage events size, events writen per seconds and the number of subscriber reading the events,  you need to change this configuration to scale up or down manually but you can use [Scale Amazon Kinesis Data Streams with AWS Application Auto Scaling instead](https://aws.amazon.com/blogs/big-data/scaling-amazon-kinesis-data-streams-with-aws-application-auto-scaling/).

# Setting Up

Before use `awsmeter` to produce events stream in Kinesis we need to create a stream. Stream is  a sequence of data records. A data record is the unit of data that is stored in a Kinesis data stream. Below the steps to create a stream:

1. Sig-on AWS account.


2. Go to Amazon Kinesis > Data Stream > Create data stream.


3. Enter Data stream name. Minimum length of 1. Maximum length of 128.


![Screenshot](https://raw.githubusercontent.com/JoseLuisSR/awsmeter/main/doc/img/kinesis/KinesisShardEstimator.png)

4. Data stream capacity. You need to Enter the average record size, the number of events publish per second and the number of subscribers reading the events, with this information AWS estimate the number of shards needed to receiver and delivery the events stream with good throughput. The throughput of one Shard is 1 MiB/second, 1000 Data records/second to write and 2 MiB/second to read.


5. Create data stream.


More detail [Creating and Updating Data Streams](https://docs.aws.amazon.com/streams/latest/dev/amazon-kinesis-streams.html).

# Getting Started

![Screenshot](https://raw.githubusercontent.com/JoseLuisSR/awsmeter/main/doc/img/kinesis/KinesisProducerJavaSampler.png)

After you installed `awsmeter` in JMeter you can start using it to produce data stream and publish them in Kinesis. You need fill the fields to connect AWS using IAM user with programmatic access. [awsmeter install details](https://github.com/JoseLuisSR/awsmeter).

You can find JMeter Test Plan in this repository that was configured with Kinesis Producer you just need fill the below fields to execute test.

* **kinesis_stream_name**: The name of the Stream you have created in specific aws region.


* **partition_key**: Is a value used to distribute the data records between the shards, the stream is composed of one or more shards and each shard is a sequence of data records. The partition key determine which shard a given data record belongs to. Partition keys are Unicode strings, with a maximum length limit of 256 characters for each key. 
  A [Counter](http://jmeter.apache.org/usermanual/component_reference.html#Counter) is used as partition key to distributed data records across all available shards in Stream.


* **data_record**: This the event that is going publish in Kinesis Data Stream. Data record is composed of a sequence number, a partition key, and a data blob, which is an immutable sequence of bytes. Kinesis Data Streams does not inspect, interpret, or change the data in the blob in any way. A data blob can be up to 1 MB.


# Testing

It's time to test. We are going to execute one single thread along 60 seconds for publishing data stream:

1. Open JMeter test plan (present in this repository) in JMeter go to Kinesis Thread Group > Kinesis Producer.


2. Fill the parameters to connect to AWS using **Access key id**, **Secret Access Key** and **Session Token**. If you have [credentials file](https://docs.aws.amazon.com/cli/latest/userguide/cli-configure-files.html) in you work area you just need to enter the name of the **profile** to get the access key id, secret access key and aws region from credentials and config file.


3. Enter the name of Stream that you created. Update the data record with the values you want. Remember to use the AWS region where you created the queue.


4. Execute Kinesis Thread.

![Screenshot](https://raw.githubusercontent.com/JoseLuisSR/awsmeter/main/doc/img/kinesis/awsmeter-kinesis-producer-test.png)


5. Check the response to see the shard id where data record were published and sequence number.


6. Get shard iterator through AWS CLI with the command:
```
aws kinesis get-shard-iterator --stream-name {name} --shard-id {id} --shard-iterator-type AT_SEQUENCE_NUMBER --starting-sequence-number {sequenceNumber}
```

![Screenshot](https://raw.githubusercontent.com/JoseLuisSR/awsmeter/main/doc/img/kinesis/aws-cli-kinesis-get-shard-iterator.png)

7 Get Kinesis data record using the shard iterator of the previous point and AWS CLI with the command:

```
aws kinesis get-records --shard-iterator {shardIterator}
```

![Screenshot](https://raw.githubusercontent.com/JoseLuisSR/awsmeter/main/doc/img/kinesis/aws-cli-kinesis-get-records.png)
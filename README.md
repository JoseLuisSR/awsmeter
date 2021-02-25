# AWS Meter

![Screenshot](https://raw.githubusercontent.com/JoseLuisSR/awsmeter/main/doc/img/awsmeter-kinesis-context-view.png)

It is a JMeter plugin to execute tests over AWS services like Kinesis, SQS & SNS. This plugin has a set 
of Java sampler per AWS service that are using AWS SDK to integrate with AWS and communicate with each service.

When you use AWS services you need set up those to process normal and peak of load. To make sure the service 
configuration is right you can execute load testing with `awsmeter` to see the behaviour of the system under 
variant or specific load. 

To get familiar with AWS you can use `awsmeter` to execute proof of concept (POC) over their services that 
can help you understand capabilities, boundaries and components of each one.

With `awsmeter` you can test the below aws services:

* Kinesis Data Stream
* [SQS](https://github.com/JoseLuisSR/awsmeter/tree/main/src/main/java/org/apache/jmeter/protocol/aws/sqs) 
* SNS (Working in progress)

# Install

This project is using gradle to resolve dependencies and package the main compiled classes and resources 
from `src/main/resources` into a single JAR. To build the `.jar` execute the command:

    gradle uberJar

You do not need to install gradle to build the .jar, you can use gradle wrapper feature instead, just run 
the below command:

    gradlew uberJar

Gradle builds the `awsmeter-x.y.z.jar` in `/awsmeter/build/libs` to install `awsmeter` in JMeter just put in 

    $JMETER_HOME/lib/ext

# Setting Up

Before use `awsmeter` for the first time, complete the following task:

* **AWS Account**: You need aws account to use their cloud compute services, it is free you only paid for 
  the services used. [Steps to create and active new aws account](https://aws.amazon.com/premiumsupport/knowledge-center/create-and-activate-aws-account/). 
  AWS has free tier for 12-months to use some products or services, more details [here](https://aws.amazon.com/free).
  You can use AWS products and services in different [regions](https://infrastructure.aws/) around the world, when sig-in you can choose the region you want.
  

* **IAM user**: You need create new AWS IAM user with programmatic access to connect AWS service end-point 
  using an access key ID and secret access key. Good practice is create new user instead use user root 
  because you can grant least privilege over the services for the new user. [Steps to create IAM User](https://docs.aws.amazon.com/IAM/latest/UserGuide/id_users_create.html).
  

* **IAM Group**: You need create an IAM Group to attach policies to multiple IAM Users. Groups let you specify 
  permissions over the AWS services you need to access, remember grant least privilege.
  

* **IAM Policy**: Policy define the permissions for an action over AWS service that IAM Identity (User, 
  Group or Role) can execute. We are going to specify  which policies we need to attach to IAM Group 
  to execute test for each service in the next sections.
  

* **Credential file (optional)**: You need store the access key id and secret access key in a local file named 
  credentials, in a folder named .aws in your home directory, also you can use AWS CLI to set up 
  credentials file. You can store multiple set of access key and secret access key using profiles and 
  specify the [AWS region](https://infrastructure.aws/) to connect. [Configure and credential file settings](https://docs.aws.amazon.com/cli/latest/userguide/cli-configure-files.html)


* **AWS CLI (optional)**: AWS Command Line Interface (CLI) is a tool to communicate with AWS through 
  commands that can create and configure AWS services. You can use the command `aws configure` to create 
  and initialize credentials and config files. [Installing AWS CLI](https://docs.aws.amazon.com/cli/latest/userguide/cli-chap-install.html)
  

* **JMeter**: [Download JMeter](https://jmeter.apache.org/download_jmeter.cgi) and follow the steps in Install section.
  
# Getting Started

To start using `awsmeter` is necessary use JMeter, if you don't have experience working with JMeter you can see
this [video tutorial for JMeter Beginners](https://youtube.com/playlist?list=PLhW3qG5bs-L-zox1h3eIL7CZh5zJmci4c).

This project has an example JMeter Test Plan that were configured to execute tests over Kinesis Data Stream, 
just open the file `awsmeter.jmx` in JMeter and fill the below java request parameters:

* **aws_access_key_id**: When you create IAM user with programmatic access aws assign an access key id, use it value here.
  Leave this parameter empty if you want to get the access key id from credentials file in .aws folder in your home directory.
  

* **aws_secret_access_key**: AWS gives you a secret access key when you create an IAM User with programmatic access also, 
  you only can show or download it value after finish creation of the user. Leave this parameter empty if you want to get 
  the secret access key from credentials file in .aws folder in your home directory.
  

* **aws_session_token**: Specifies an AWS session token used as part of the credentials to authenticate the IAM user. 
  A session token is required only if you manually specify temporary security credentials. Leave this parameter empty 
  if you are not using [temporary credentials](https://docs.aws.amazon.com/IAM/latest/UserGuide/id_credentials_temp_use-resources.html).
  

* **aws_region**: The aws region where aws services are. [AWS Regions and Zones](https://docs.aws.amazon.com/AWSEC2/latest/UserGuide/using-regions-availability-zones.html)
Leave this parameter empty to use the aws region defined in config file in .aws folder in yor home directory.
  

* **aws_configure_profile**: When you use profile to group a collections of settings and credentials. When you have multiple 
  aws accounts ([AWS Organization](https://aws.amazon.com/organizations/)) you can use profile to classify credentials for each AWS account. The main profile is 'default',
  you can choose other that you have in credentials and config files.
  

* **kinesis_stream_name**: The name of the Stream you have created in specific aws region. [Steps to create a Data Stream](https://docs.aws.amazon.com/streams/latest/dev/tutorial-stock-data-kplkcl-create-stream.html).


* **partition_key**: Is a value used to distribute the data records between the shards, the stream is composed of one or more 
  shards and each shard is a sequence of data records. The partition key determine which shard a given data record belongs to.
  You can choose. Partition keys are Unicode strings, with a maximum length limit of 256 characters for each key. You can choose
  the partition key you want. A [Counter](http://jmeter.apache.org/usermanual/component_reference.html#Counter)
  is used as partition key to distributed data records across all available shards in stream.
  

* **data_record**: This the information/message that is going publish in Kinesis Data Stream. Data record is composed of a 
  sequence number, a partition key, and a data blob, which is an immutable sequence of bytes. Kinesis Data Streams does not inspect, 
  interpret, or change the data in the blob in any way. A data blob can be up to 1 MB.



![Screenshot](https://raw.githubusercontent.com/JoseLuisSR/awsmeter/main/doc/img/KinesisProducesJavaRequest.png)


# Testing

In this example we are going to publish a message in Kinesis stream using `awsmeter` then we are going to consume the message using aws cli, 
the steps are:

1. Start JMeter and open `awsmeter` test plan present in this repository.


2. Fill the parameters needed for the test. In this case we are using the credentials and config files to get values of access key id, 
   secret and aws region associate with default profile. Just put the value of stream name.

![Screenshot](https://raw.githubusercontent.com/JoseLuisSR/awsmeter/main/doc/img/credentials-file.png)

![Screenshot](https://raw.githubusercontent.com/JoseLuisSR/awsmeter/main/doc/img/config-file.png)


3. Run the test plan with one thread. You can change the number of thread to execute load test but be care with the charge of Kinesis.
[Kinesis Data Stream pricing](https://aws.amazon.com/kinesis/data-streams/pricing/).


4. Check the response to see the shard id where data record were published and sequence number.

![Screenshot](https://raw.githubusercontent.com/JoseLuisSR/awsmeter/main/doc/img/awsmeter-kinesis-response.png)

5. See the CloudWatch metrics for Kinesis to get information about the incoming data count and Bytes and others metrics very helpful 
   to validate the throughput of kinesis. [Monitoring Kinesis Data Stream](https://docs.aws.amazon.com/streams/latest/dev/monitoring-with-cloudwatch.html).

![Screenshot](https://raw.githubusercontent.com/JoseLuisSR/awsmeter/main/doc/img/kinesis-metrics.png)

6. Get shard iterator through AWS CLI with the command:

`aws kinesis get-shard-iterator --stream-name {name} --shard-id {id} --shard-iterator-type AT_SEQUENCE_NUMBER --starting-sequence-number {sequenceNumber}`

![Screenshot](https://raw.githubusercontent.com/JoseLuisSR/awsmeter/main/doc/img/awscli-kinesis-get-shard-iterator.png)

7 Get Kinesis data record using the shard iterator of the previous point and AWS CLI with the command:

`aws kinesis get-records --shard-iterator {shardIterator}`

![Screenshot](https://raw.githubusercontent.com/JoseLuisSR/awsmeter/main/doc/img/awscli-kinesis-get-records.png)

8. Decoded from Base64 format the value of the field Data:

`eyAgICAgIm1lc3NhZ2UiOiAiSGVsbG8gYXdzbWV0ZXIgSk1ldGVyIHBsdWdpbiIgfQ`

`{     "message": "Hello awsmeter JMeter plugin" }`
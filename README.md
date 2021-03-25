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

* [Kinesis Data Stream](https://github.com/JoseLuisSR/awsmeter/tree/main/src/main/java/org/apache/jmeter/protocol/aws/kinesis)
* [SQS](https://github.com/JoseLuisSR/awsmeter/tree/main/src/main/java/org/apache/jmeter/protocol/aws/sqs) 
* [SNS](https://github.com/JoseLuisSR/awsmeter/tree/main/src/main/java/org/apache/jmeter/protocol/aws/sns)

# Install

This project is using gradle to resolve dependencies and package the main compiled classes and resources 
from `src/main/resources` into a single JAR. To build the `.jar` execute the command:

    gradle uberJar

You do not need to install gradle to build the .jar, you can use gradle wrapper feature instead, just run 
the below command:

    gradlew uberJar

Gradle builds the `awsmeter-x.y.z.jar` in `/awsmeter/build/libs` to install `awsmeter` in JMeter just put in 

    $JMETER_HOME/lib/ext


# JMeter Plugins

![Screenshot](https://raw.githubusercontent.com/JoseLuisSR/awsmeter/main/doc/img/jmeter-plugins-logo.png)

`awsmeter` is available in [JMeter Plugins](https://jmeter-plugins.org/), follow the below steps to install those:

1. [Install JMeter Plugins](https://jmeter-plugins.org/install/Install/). Download `plugins-manager.jar` from https://jmeter-plugins.org/install/Install/ and put it into `lib/ext` directory in your local JMeter folder.


2. Restart or open JMeter.


3. Go to **Options > Plugins Manager** then in **Available Plugins** tab search `aws`, select it and press **Apply Changes and Restart JMeter** button. `awsmeter` was build with Java 11, please execute JMeter with Java 11 to avoid issues.

![Screenshot](https://raw.githubusercontent.com/JoseLuisSR/awsmeter/main/doc/img/jmeter-plugins-awsmeter.png)

That's all, have fun using it.

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
  

* **Credential and Config files (optional)**: You need store the access key id and secret access key in a local file named 
  credentials, in a folder named .aws in your home directory, also you can use AWS CLI to set up 
  credentials file. You can store multiple set of access key and secret access key using profiles and 
  specify the [AWS region](https://infrastructure.aws/) to connect. [Configure and credential file settings](https://docs.aws.amazon.com/cli/latest/userguide/cli-configure-files.html)

![Screenshot](https://raw.githubusercontent.com/JoseLuisSR/awsmeter/main/doc/img/credentials-file.png) 
![Screenshot](https://raw.githubusercontent.com/JoseLuisSR/awsmeter/main/doc/img/config-file.png)


* **AWS CLI (optional)**: AWS Command Line Interface (CLI) is a tool to communicate with AWS through 
  commands that can create and configure AWS services. You can use the command `aws configure` to create 
  and initialize credentials and config files. [Installing AWS CLI](https://docs.aws.amazon.com/cli/latest/userguide/cli-chap-install.html)
  

* **JMeter**: [Download JMeter](https://jmeter.apache.org/download_jmeter.cgi) and follow the steps in Install section.
  
# Getting Started

To start using `awsmeter` is necessary use JMeter, if you don't have experience working with JMeter you can see
this [video tutorial for JMeter Beginners](https://youtube.com/playlist?list=PLhW3qG5bs-L-zox1h3eIL7CZh5zJmci4c).

This project has an example JMeter Test Plan that were configured to execute tests over [Kinesis Data Stream](https://github.com/JoseLuisSR/awsmeter/tree/main/src/main/java/org/apache/jmeter/protocol/aws/kinesis), 
[SQS Standard and FIFO Queue](https://github.com/JoseLuisSR/awsmeter/tree/main/src/main/java/org/apache/jmeter/protocol/aws/sqs), and more just open the file `awsmeter.jmx` in JMeter and fill the below java request parameters to connect to AWS Account:

![Screenshot](https://raw.githubusercontent.com/JoseLuisSR/awsmeter/main/doc/img/awsmeter-parameters.png)

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
  

* **aws_configure_profile**: Use profile to group a collections of settings and credentials. When you have multiple aws accounts ([AWS Organization](https://aws.amazon.com/organizations/)) 
  you can use profile to classify credentials for each AWS account. The main profile is 'default', choose other that you have in credentials and config files.


To know the parameters needed by AWS Service please go to:

* [Kinesis Data Stream](https://github.com/JoseLuisSR/awsmeter/tree/main/src/main/java/org/apache/jmeter/protocol/aws/kinesis)
* [SQS](https://github.com/JoseLuisSR/awsmeter/tree/main/src/main/java/org/apache/jmeter/protocol/aws/sqs)
* SNS (Working in progress)


# Design

`awsmeter` was designed to extend his behaviour to other AWS services, if you want to use JMeter and Java Sampler Request to do test over others AWS Services you just need to extend the class `AWSSampler.java` and overwrite the methods to create SdkClient, define parameters, run and tear down the test.

![Screenshot](https://raw.githubusercontent.com/JoseLuisSR/awsmeter/main/doc/img/awsmeter-class-diagram.png)

Below you can see the class diagram with the details of the classes created for Kinesis Data Stream `KinesisProducerSampler.java` and SQS per queue type Standard `SQSProducerStandardQueue` and FIFO `SQSProducerFifoQueue`. Each class has his own logic to connect, produce events or messages and reuse the behaviour to record the test in JMeter and define the parameter to connect AWS account.

`awsmeter` is implementing JavaSamplerClient interface to write our own implementation by AWS Service and can use JMeter to harness multiple threads, input parameter control, and data collection. Each Java Sampler defined is a protocol then we are following the JMeter package convention to define protocol per aws service:

* **org.apache.jmeter.protocol.aws.kinesis**
* **org.apache.jmeter.protocol.aws.sns**

Please use this convention to create new Java Sampler Request for other AWS Service.

# Troubleshooting


When you installed `awsmeter` using **jmeter-plugins** and executed JMeter with Java 8 or downwards, then probably you get the below exception:

![Screenshot](https://raw.githubusercontent.com/JoseLuisSR/awsmeter/main/doc/img/awsmeter-compiled-issue.png)

To fix it, please install Java 11 and execute JMeter with this version or install `awsmeter` following the steps described in the [Install](https://github.com/JoseLuisSR/awsmeter#install) section and execute JMeter with th version of Java you have (minimum 8). 
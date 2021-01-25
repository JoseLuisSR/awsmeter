# AWS Meter

It is a JMeter plugin to execute tests over AWS services like Kinesis, SQS & SNS. This plugin has a set 
of Java sampler per AWS service that are using AWS SDK to integrate with AWS and communicate with each service.

When you use AWS services you need set up those to process normal and peak of load. To make sure the service 
configuration is right you can execute load testing with `awsmeter` to see the behaviour of the system under 
variant or specific load. 

To get familiar with AWS you can use `awsmeter` to execute proof of concept (POC) over their services that 
can help you understand capabilities, boundaries and components of each one.

With `awsmeter` you can test the below aws services:

* Kinesis Data Stream
* SQS (Working in progress)
* SNS (Working in progress)

# Install

This project is using gradle to resolve dependencies and package the main compiled classes and resources 
from src/main/resources into a single JAR. To build the .jar execute the command:

    gradle uberJar

You do not need to install gradle to build the .jar, you can use gradle wrapper feature instead, just run 
the below command:

    gradlew build

Gradle builds the `awsmeter-x.y.z.jar` in `/awsmeter/build/libs` to install it in JMeter jut put in 

    $JMETER_HOME/lib/ext

# Setting up

Before use `awsmeter` for the first time, complete the following task:

* **AWS Account**: You need aws account to use his cloud compute services, it is free you only paid for 
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
  

* **Credential file**: You need store the access key id and secret access key in a local file named 
  credentials, in a folder named .aws in your home directory, also you can use AWS CLI to set up 
  credentials file. You can store multiple set of access key and secret access key using profiles and 
  specify the [AWS region](https://infrastructure.aws/) to connect. [Configure and credential file settings](https://docs.aws.amazon.com/cli/latest/userguide/cli-configure-files.html)


* **AWS CLI (optional)**: AWS Command Line Interface (CLI) is a tool to communicate with AWS through 
  commands that can create and configure AWS services. You can use the command `aws configure` to create 
  and initialize credentials and config files. [Installing AWS CLI](https://docs.aws.amazon.com/cli/latest/userguide/cli-chap-install.html)
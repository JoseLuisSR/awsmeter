# AWS Meter

It is a JMeter plugin to execute tests over AWS services like Kinesis, SQS & SNS. This plugin has a set 
of Java sampler per AWS service that are using AWS SDK to integrate with AWS and communicate with each service.

When you use AWS services you need set up those to process normal and peak of load. To make sure the service 
configuration is right you can execute load testing with awsmeter to see the behaviour of the system under 
variant or specific load. Through awsmeter you can test the below aws services:

* Kinesis Data Stream
* SQS
* SNS

# Install

This project is using gradle to resolve dependencies and package the main compiled classes and resources 
from src/main/resources into a single JAR. To build the .jar execute the command:

    gradle uberJar

You do not need to install gradle to build the .jar, you can use gradle wrapper feature instead, just run 
the below command:

    gradlew build

Gradle builds the `awsmeter-x.y.z.jar` in `/awsmeter/build/libs` to install it in JMeter jut put in 

    $JMETER_HOME/lib/ext
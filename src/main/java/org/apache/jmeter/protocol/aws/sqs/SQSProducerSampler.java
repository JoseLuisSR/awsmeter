package org.apache.jmeter.protocol.aws.sqs;

import org.apache.jmeter.config.Argument;
import org.apache.jmeter.config.Arguments;
import org.apache.jmeter.protocol.aws.AWSSampler;
import org.apache.jmeter.protocol.java.sampler.JavaSamplerContext;
import org.apache.jmeter.samplers.SampleResult;
import software.amazon.awssdk.core.SdkClient;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;
import software.amazon.awssdk.services.sqs.model.SqsException;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SQSProducerSampler extends AWSSampler {

    private static final String SQS_QUEUE_NAME = "sqs_queue_name";

    private static final String SQS_MESSAGE = "sqs_message";

    private static final List<Argument> SQS_PARAMETERS = Stream.of(
            new Argument(SQS_QUEUE_NAME, ""),
            new Argument(SQS_MESSAGE, ""))
            .collect(Collectors.toList());

    private SqsClient sqsClient;

    @Override
    public SdkClient createSdkClient(Map<String, String> credentials) {
        return SqsClient.builder()
                .region(awsRegion(credentials))
                .credentialsProvider(awsCredentialProvider(credentials))
                .build();
    }

    @Override
    public Arguments getDefaultParameters() {
        Arguments defaultParameters = new Arguments();
        defaultParameters.setArguments(Arrays.asList(AWS_PARAMETERS, SQS_PARAMETERS).stream()
                .flatMap(List::stream)
                .collect(Collectors.toList()));
        return defaultParameters;
    }

    @Override
    public void setupTest(JavaSamplerContext context) {

        getNewLogger().info("Setup SQS Producer Sampler.");
        Map<String, String> credentials = new HashMap<>();

        context.getParameterNamesIterator().forEachRemaining( k -> {
            credentials.put(k, context.getParameter(k));
            getNewLogger().info("Parameter: " + k + ", value: " + credentials.get(k));
        });

        getNewLogger().info("Create SQS Producer.");
        sqsClient = (SqsClient) createSdkClient(credentials);
    }

    @Override
    public SampleResult runTest(JavaSamplerContext context) {

        SampleResult result = newSampleResult();
        sampleResultStart(result, String.format("SQS Queue Name: %s \nSQS Message: %s",
                context.getParameter(SQS_QUEUE_NAME),
                context.getParameter(SQS_MESSAGE)));

        try{
            getNewLogger().info("Put Message on Queue");
            SendMessageResponse response = sqsClient.sendMessage(createSendMessageRequest(context));
            sampleResultSuccess(result, String.format("Message id: %s \nSequence Number: %s",
                    response.messageId(),
                    response.sequenceNumber()));
        }catch (SqsException e){
            sampleResultFail(result, e.awsErrorDetails().errorCode(), e.awsErrorDetails().errorMessage());
        }

        return result;
    }

    @Override
    public void teardownTest(JavaSamplerContext context) {

    }

    public SendMessageRequest createSendMessageRequest(JavaSamplerContext context){
        return SendMessageRequest.builder()
                .queueUrl(sqsClient.getQueueUrl(GetQueueUrlRequest.builder()
                        .queueName(context.getParameter(SQS_QUEUE_NAME))
                        .build())
                        .queueUrl())
                .messageBody(context.getParameter(SQS_MESSAGE))
                .build();
    }

}

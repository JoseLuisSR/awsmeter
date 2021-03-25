package org.apache.jmeter.protocol.aws.sqs;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.apache.jmeter.config.Argument;
import org.apache.jmeter.config.Arguments;
import org.apache.jmeter.protocol.java.sampler.JavaSamplerContext;
import org.apache.jmeter.samplers.SampleResult;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;
import software.amazon.awssdk.services.sqs.model.SqsException;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SQSProducerStandardQueue extends SQSProducerSampler{

    private static final List<Argument> SQS_PARAMETERS = Stream.of(
            new Argument(SQS_QUEUE_NAME, ""),
            new Argument(SQS_MSG_BODY, ""),
            new Argument(SQS_MSG_ATTRIBUTES, ""),
            new Argument(SQS_DELAY_SECONDS, "0"))
            .collect(Collectors.toList());

    @Override
    public Arguments getDefaultParameters() {
        Arguments defaultParameters = new Arguments();
        defaultParameters.setArguments(Stream.of(AWS_PARAMETERS, SQS_PARAMETERS)
                .flatMap(List::stream)
                .collect(Collectors.toList()));
        return defaultParameters;
    }

    @Override
    public SampleResult runTest(JavaSamplerContext context) {

        SampleResult result = newSampleResult();
        sampleResultStart(result, String.format("Queue Name: %s \nMsg Body : %s \nMsg Attribute: %s \nDelay sec: %s",
                context.getParameter(SQS_QUEUE_NAME),
                context.getParameter(SQS_MSG_BODY),
                context.getParameter(SQS_MSG_ATTRIBUTES),
                context.getIntParameter(SQS_DELAY_SECONDS)));

        getNewLogger().info("Put Message on Queue");

        try{
            SendMessageResponse msgRsp = sqsClient.sendMessage(createSendMessageRequest(context));
            sampleResultSuccess(result, String.format("Message id: %s",
                    msgRsp.messageId()));
        }catch (SqsException exc){
            sampleResultFail(result, exc.awsErrorDetails().errorCode(), exc.awsErrorDetails().errorMessage());
        } catch (JsonProcessingException exc) {
            sampleResultFail(result, FAIL_CODE, exc.getMessage());
        }

        return result;
    }

    @Override
    public SendMessageRequest createSendMessageRequest(final JavaSamplerContext context) throws JsonProcessingException {
        return SendMessageRequest.builder()
                .queueUrl(sqsClient.getQueueUrl(GetQueueUrlRequest.builder()
                        .queueName(context.getParameter(SQS_QUEUE_NAME))
                        .build())
                        .queueUrl())
                .messageBody(context.getParameter(SQS_MSG_BODY))
                .messageAttributes(buildMessageAttributes(context.getParameter(SQS_MSG_ATTRIBUTES)))
                .delaySeconds(context.getIntParameter(SQS_DELAY_SECONDS))
                .build();
    }
}

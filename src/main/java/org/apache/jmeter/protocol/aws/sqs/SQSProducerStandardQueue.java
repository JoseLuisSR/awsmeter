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

/**
 * SQS Producer Sampler class to connect and publish messages on SQS Standard queue.
 * @author JoseLuisSR
 * @since 01/27/2021
 * @see "https://github.com/JoseLuisSR/awsmeter"
 */
public class SQSProducerStandardQueue extends SQSProducerSampler{

    /**
     * List of Arguments to SQS Standard Queue.
     */
    private static final List<Argument> SQS_PARAMETERS = Stream.of(
            new Argument(SQS_QUEUE_NAME, EMPTY),
            new Argument(SQS_MSG_BODY, EMPTY),
            new Argument(SQS_MSG_ATTRIBUTES, EMPTY),
            new Argument(SQS_DELAY_SECONDS, SQS_DEFAULT_DELAY_SECONDS))
            .collect(Collectors.toList());

    /**
     * Initial values for test parameter. They are show in Java Request test sampler.
     * AWS parameters and SQS parameters.
     * @return Arguments to set as default on Java Request.
     */
    @Override
    public Arguments getDefaultParameters() {
        Arguments defaultParameters = new Arguments();
        defaultParameters.setArguments(Stream.of(AWS_PARAMETERS, SQS_PARAMETERS)
                .flatMap(List::stream)
                .collect(Collectors.toList()));
        return defaultParameters;
    }

    /**
     * Main method to execute the test on single thread. Create Message and publish it on SQS Standard Queue.
     * @param context
     *        Arguments values on Java Sampler.
     * @return SampleResult, captures data such as whether the test was successful,
     * the response code and message, any request or response data, and the test start/end times
     */
    @Override
    public SampleResult runTest(JavaSamplerContext context) {

        SampleResult result = newSampleResult();
        sampleResultStart(result, String.format("Queue Name: %s \nMsg Body : %s \nMsg Attribute: %s \nDelay sec: %s",
                context.getParameter(SQS_QUEUE_NAME),
                context.getParameter(SQS_MSG_BODY),
                context.getParameter(SQS_MSG_ATTRIBUTES),
                context.getIntParameter(SQS_DELAY_SECONDS)));

        try{
            log.info("Put Message on Queue");
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

    /**
     * Create request to publish message on SQS Standard Queue.
     * @param context
     *        Arguments values on Java Sampler.
     * @return SendMessageRequest with message elements like body, attributes and delay seconds.
     * @throws JsonProcessingException
     *         Exception when deserialize JSON to Object.
     */
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

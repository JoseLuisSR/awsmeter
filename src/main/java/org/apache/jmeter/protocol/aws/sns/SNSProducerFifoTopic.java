package org.apache.jmeter.protocol.aws.sns;

import com.amazonaws.services.sns.model.AmazonSNSException;
import com.amazonaws.services.sns.model.PublishRequest;
import com.amazonaws.services.sns.model.PublishResult;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.apache.jmeter.config.Argument;
import org.apache.jmeter.config.Arguments;
import org.apache.jmeter.protocol.java.sampler.JavaSamplerContext;
import org.apache.jmeter.samplers.SampleResult;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * SNS Producer Sampler class to connect and publish messages on SNS FIFO topic.
 * @author JoseLuisSR
 * @since 01/27/2021
 * @see "https://github.com/JoseLuisSR/awsmeter"
 */
public class SNSProducerFifoTopic extends SNSProducerSampler{

    /**
     * List of Arguments to SNS FIFO Topic.
     */
    private static final List<Argument> SNS_PARAMETERS = Stream.of(
            new Argument(SNS_TOPIC_ARN, EMPTY),
            new Argument(SNS_MSG_BODY, EMPTY),
            new Argument(SNS_MSG_ATTRIBUTES, EMPTY),
            new Argument(SNS_MSG_GROUP_ID, EMPTY),
            new Argument(SNS_MSG_DEDUPLICATION_ID, EMPTY)

    ).collect(Collectors.toList());

    /**
     * Initial values for test parameter. They are show in Java Request test sampler.
     * AWS parameters and SNS parameters.
     * @return Arguments to set as default on Java Request.
     */
    @Override
    public Arguments getDefaultParameters() {
        Arguments defaultParameters = new Arguments();
        defaultParameters.setArguments(Stream.of(AWS_PARAMETERS, SNS_PARAMETERS)
                .flatMap(List::stream)
                .collect(Collectors.toList()));
        return defaultParameters;
    }

    /**
     * Main method to execute the test on single thread. Create Message and publish it on SNS FIFO Topic.
     * @param context
     *        Arguments values on Java Sampler.
     * @return SampleResult, captures data such as whether the test was successful,
     * the response code and message, any request or response data, and the test start/end times
     */
    @Override
    public SampleResult runTest(JavaSamplerContext context) {

        SampleResult result = new SampleResult();
        sampleResultStart(result, String.format("Topic Arn: %s \nMsg Body: %s " +
                        "\nMsg Attributes: %s \nMsg Group Id: %s \nMsg Deduplication Id: %s",
                context.getParameter(SNS_TOPIC_ARN),
                context.getParameter(SNS_MSG_BODY),
                context.getParameter(SNS_MSG_ATTRIBUTES),
                context.getParameter(SNS_MSG_GROUP_ID),
                context.getParameter(SNS_MSG_DEDUPLICATION_ID)));

        try {
            log.info("Publishing Event.");
            PublishResult response = snsClient.publish(createPublishRequest(context));

            sampleResultSuccess(result, String.format("Message id: %s \nSequence number: %s",
                    response.getMessageId(),
                    response.getSequenceNumber()));

        } catch (AmazonSNSException e){
            sampleResultFail(result, e.getErrorCode(), e.getMessage());
        } catch (JsonProcessingException exc) {
            sampleResultFail(result, FAIL_CODE, exc.getMessage());
        }

        return result;
    }

    /**
     * Create request to publish message on SNS FIFO Topic.
     * @param context
     *        Arguments values on Java Sampler.
     * @return PublishRequest with message attributes like body, attributes, deduplication id, group id and more.
     * @throws JsonProcessingException
     *         Exception when deserialize JSON to Object.
     */
    @Override
    public PublishRequest createPublishRequest(final JavaSamplerContext context) throws JsonProcessingException {

        PublishRequest request = new PublishRequest();
        return request
                .withTopicArn(context.getParameter(SNS_TOPIC_ARN))
                .withMessage(context.getParameter(SNS_MSG_BODY))
                .withMessageAttributes(buildMessageAttributes(context.getParameter(SNS_MSG_ATTRIBUTES)))
                .withMessageGroupId(context.getParameter(SNS_MSG_GROUP_ID))
                .withMessageDeduplicationId(context.getParameter(SNS_MSG_DEDUPLICATION_ID));
    }
}

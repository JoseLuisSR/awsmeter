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

public class SNSProducerStandardTopic extends SNSProducerSampler {

    private static final List<Argument> SNS_PARAMETERS = Stream.of(
            new Argument(SNS_TOPIC_ARN, EMPTY),
            new Argument(SNS_MSG_BODY, EMPTY),
            new Argument(SNS_MSG_ATTRIBUTES, EMPTY)
    ).collect(Collectors.toList());

    @Override
    public Arguments getDefaultParameters() {
        Arguments defaultParameters = new Arguments();
        defaultParameters.setArguments(Stream.of(AWS_PARAMETERS, SNS_PARAMETERS)
                .flatMap(List::stream)
                .collect(Collectors.toList()));
        return defaultParameters;
    }

    @Override
    public SampleResult runTest(JavaSamplerContext context) {

        SampleResult result = new SampleResult();
        sampleResultStart(result, String.format("Topic Arn: %s \nMsg Body: %s " +
                        "\nMsg Attributes: %s",
                context.getParameter(SNS_TOPIC_ARN),
                context.getParameter(SNS_MSG_BODY),
                context.getParameter(SNS_MSG_ATTRIBUTES)));

        try {
            getNewLogger().info("Publishing Event.");
            PublishResult response = snsClient.publish(createPublishRequest(context));

            sampleResultSuccess(result, String.format("Message id: %s",
                    response.getMessageId()));

        } catch (AmazonSNSException e){
            sampleResultFail(result, e.getErrorCode(), e.getMessage());
        } catch (JsonProcessingException exc) {
            sampleResultFail(result, FAIL_CODE, exc.getMessage());
        }

        return result;
    }

    @Override
    public PublishRequest createPublishRequest(final JavaSamplerContext context) throws JsonProcessingException {

        PublishRequest request = new PublishRequest();
        return request
                .withTopicArn(context.getParameter(SNS_TOPIC_ARN))
                .withMessage(context.getParameter(SNS_MSG_BODY))
                .withMessageAttributes(buildMessageAttributes(context.getParameter(SNS_MSG_ATTRIBUTES)));
    }
}

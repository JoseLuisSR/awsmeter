package org.apache.jmeter.protocol.aws.sns;

import com.amazonaws.auth.AWSCredentialsProviderChain;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.services.sns.AmazonSNS;
import com.amazonaws.services.sns.AmazonSNSClient;
import com.amazonaws.services.sns.model.MessageAttributeValue;
import com.amazonaws.services.sns.model.PublishResult;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.jmeter.config.Argument;
import org.apache.jmeter.config.Arguments;
import org.apache.jmeter.protocol.aws.AWSSampler;
import org.apache.jmeter.protocol.aws.MessageAttribute;
import org.apache.jmeter.protocol.java.sampler.JavaSamplerContext;
import org.apache.jmeter.samplers.SampleResult;
import software.amazon.awssdk.core.SdkClient;

import com.amazonaws.services.sns.model.PublishRequest;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SNSProducerSampler extends AWSSampler {

    private static final String SNS_TOPIC_NAME = "sns_topic_name";

    private static final String SNS_TOPIC_ARN = "sns_topic_arn";

    private static final String SNS_MSG_BODY = "sns_msg_body";

    private static final String SNS_MSG_ATTRIBUTES = "sns_msg_attributes";

    private static final String MSG_ATTRIBUTE_TYPE_STR = "String";

    private static final String MSG_ATTRIBUTE_TYPE_NUM = "Number";

    private static final String MSG_ATTRIBUTE_TYPE_BIN = "Binary";

    private static final Integer MSG_ATTRIBUTES_MAX = 10;

    private static final List<Argument> SNS_PARAMETERS = Stream.of(
            new Argument(SNS_TOPIC_NAME, EMPTY),
            new Argument(SNS_MSG_BODY, EMPTY),
            new Argument(SNS_TOPIC_ARN, EMPTY),
            new Argument(SNS_MSG_ATTRIBUTES, EMPTY)
    ).collect(Collectors.toList());

    private AmazonSNS snsClient;

    @Override
    public SdkClient createSdkClient(Map<String, String> credentials) {
        return null;
    }

    @Override
    public Arguments getDefaultParameters() {
        Arguments defaultParameters = new Arguments();
        defaultParameters.setArguments(Stream.of(AWS_PARAMETERS, SNS_PARAMETERS)
                .flatMap(List::stream)
                .collect(Collectors.toList()));
        return defaultParameters;
    }

    @Override
    public void setupTest(JavaSamplerContext context) {

        getNewLogger().info("Setup SNS Publisher Sampler.");
        Map<String, String> credentials = new HashMap<>();

        context.getParameterNamesIterator().forEachRemaining( k -> {
            credentials.put(k, context.getParameter(k));
            getNewLogger().info("Parameter: " + k + ", value: " + credentials.get(k));
        });

        getNewLogger().info("Create SNS Publisher.");

        DefaultAWSCredentialsProviderChain credentialsProviderChain = new DefaultAWSCredentialsProviderChain();
        getNewLogger().info("access key  secret " + credentialsProviderChain.getCredentials().getAWSAccessKeyId() + " " + credentialsProviderChain.getCredentials().getAWSSecretKey());
        snsClient = AmazonSNSClient.builder()
                .withCredentials(new AWSCredentialsProviderChain(credentialsProviderChain))
                .withRegion(context.getParameter(AWS_REGION))
                .build();
    }

    @Override
    public SampleResult runTest(JavaSamplerContext context) {

        SampleResult result = new SampleResult();
        sampleResultStart(result, String.format("Topic Name: %s \nMsg Body: %s " +
                        "\nMsg Attributes: %s \nSNS Topic Arn: %s",
                context.getParameter(SNS_TOPIC_NAME),
                context.getParameter(SNS_MSG_BODY),
                context.getParameter(SNS_MSG_ATTRIBUTES),
                context.getParameter(SNS_TOPIC_ARN)));

        try {
            getNewLogger().info("Publishing Event.");
            PublishResult response = snsClient.publish(createPublishRequest(context));

            sampleResultSuccess(result, String.format("Message id: %s \nSequence number: %s",
                    response.getMessageId(),
                    response.getSequenceNumber()));

        } catch (JsonProcessingException exc) {
            sampleResultFail(result, FAIL_CODE, exc.getMessage());
        }

        return result;
    }

    @Override
    public void teardownTest(JavaSamplerContext context) {
        getNewLogger().info("Close SNS Publisher.");
        snsClient.shutdown();
    }

    public PublishRequest createPublishRequest(JavaSamplerContext context) throws JsonProcessingException {

        PublishRequest request = new PublishRequest();
        return request
                .withTopicArn(context.getParameter(SNS_TOPIC_ARN))
                .withMessage(context.getParameter(SNS_MSG_BODY))
                .withMessageAttributes(buildMessageAttributes(context.getParameter(SNS_MSG_ATTRIBUTES)));
    }

    public Map<String, MessageAttributeValue> buildMessageAttributes(String msgAttributes) throws JsonProcessingException {

        ObjectMapper objectMapper = new ObjectMapper();
        List<MessageAttribute> msgAttributesList = objectMapper.readValue(
                Optional.ofNullable(msgAttributes)
                        .filter(Predicate.not(String::isEmpty))
                        .orElseGet(() -> EMPTY_ARRAY),
                new TypeReference<List<MessageAttribute>>() {}).stream()
                .limit(MSG_ATTRIBUTES_MAX)
                .collect(Collectors.toList());

        return Stream.of(buildMsgAttributesStr(msgAttributesList),
                buildMsgAttributeNum(msgAttributesList),
                buildMsgAttributesBin(msgAttributesList))
                .flatMap( map -> map.entrySet().stream())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    public Map<String, MessageAttributeValue> buildMsgAttributesStr(List<MessageAttribute> msgAttributes){
        return msgAttributes.stream()
                .filter(isStringDataType)
                .collect(Collectors.toMap(MessageAttribute::getName, createStringAttribute));
    }

    Predicate<MessageAttribute> isStringDataType = msg -> msg.getType().startsWith(MSG_ATTRIBUTE_TYPE_STR);

    Function<MessageAttribute, MessageAttributeValue> createStringAttribute = msg -> {
        MessageAttributeValue attributeValue = new MessageAttributeValue();
        return attributeValue
                .withDataType(msg.getType())
                .withStringValue(msg.getValue());
    };

    public Map<String, MessageAttributeValue> buildMsgAttributeNum(List<MessageAttribute> msgAttributes){
        return msgAttributes.stream()
                .filter(isNumberDataType)
                .collect(Collectors.toMap(MessageAttribute::getName, createNumberAttribute));
    }

    Predicate<MessageAttribute> isNumberDataType = msg -> msg.getType().startsWith(MSG_ATTRIBUTE_TYPE_NUM);

    Function<MessageAttribute, MessageAttributeValue> createNumberAttribute = msg -> {
        MessageAttributeValue attributeValue = new MessageAttributeValue();
        return attributeValue
                .withDataType(msg.getType())
                .withStringValue(msg.getValue());
    };

    public Map<String, MessageAttributeValue> buildMsgAttributesBin(List<MessageAttribute> msgAttributes){
        return msgAttributes.stream()
                .filter(isBinaryDataType)
                .collect(Collectors.toMap(MessageAttribute::getName, createBinaryAttribute));
    }

    Predicate<MessageAttribute> isBinaryDataType = msg -> msg.getType().startsWith(MSG_ATTRIBUTE_TYPE_BIN);

    Function<MessageAttribute, MessageAttributeValue> createBinaryAttribute = msg -> {
        MessageAttributeValue attributeValue = new MessageAttributeValue();
        return attributeValue
                .withDataType(msg.getType())
                .withBinaryValue(ByteBuffer.wrap(msg.getValue().getBytes(StandardCharsets.UTF_8)));
    };

}

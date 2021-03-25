package org.apache.jmeter.protocol.aws.sns;

import com.amazonaws.client.builder.AwsSyncClientBuilder;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.sns.AmazonSNS;
import com.amazonaws.services.sns.AmazonSNSClient;
import com.amazonaws.services.sns.model.MessageAttributeValue;
import com.amazonaws.services.sns.model.PublishRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.apache.jmeter.protocol.aws.AWSSampler;
import org.apache.jmeter.protocol.aws.MessageAttribute;
import org.apache.jmeter.protocol.java.sampler.JavaSamplerContext;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public abstract class SNSProducerSampler extends AWSSampler {

    protected static final String SNS_TOPIC_ARN = "sns_topic_arn";

    protected static final String SNS_MSG_BODY = "sns_msg_body";

    protected static final String SNS_MSG_ATTRIBUTES = "sns_msg_attributes";

    protected static final String SNS_MSG_GROUP_ID = "sqs_msg_group_id";

    protected static final String SNS_MSG_DEDUPLICATION_ID = "sqs_msg_deduplication_id";

    private static final String MSG_ATTRIBUTE_TYPE_STR = "String";

    private static final String MSG_ATTRIBUTE_TYPE_STR_ARRAY = "String.Array";

    private static final String MSG_ATTRIBUTE_TYPE_NUM = "Number";

    private static final String MSG_ATTRIBUTE_TYPE_BIN = "Binary";

    protected AmazonSNS snsClient;

    @Override
    public AwsSyncClientBuilder createAWSClient(Map<String, String> credentials) {
        return AmazonSNSClient.builder()
                .withCredentials(getAWSCredentials(credentials))
                .withRegion(Regions.fromName(getAWSRegion(credentials)));
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
        snsClient = (AmazonSNS) createAWSClient(credentials).build();
    }

    @Override
    public void teardownTest(JavaSamplerContext context) {
        getNewLogger().info("Close SNS Publisher.");
        snsClient.shutdown();
    }

    public abstract PublishRequest createPublishRequest(final JavaSamplerContext context) throws JsonProcessingException;

    public Map<String, MessageAttributeValue> buildMessageAttributes(final String msgAttributes) throws JsonProcessingException {

        List<MessageAttribute> msgAttributesList = readMsgAttributes(msgAttributes);

        return Stream.of(buildMsgAttributesStr(msgAttributesList),
                buildMsgAttributesStrArray(msgAttributesList),
                buildMsgAttributeNum(msgAttributesList),
                buildMsgAttributesBin(msgAttributesList))
                .flatMap( map -> map.entrySet().stream())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    public Map<String, MessageAttributeValue> buildMsgAttributesStr(final List<MessageAttribute> msgAttributes){
        return msgAttributes.stream()
                .filter(isStringDataType)
                .collect(Collectors.toMap(MessageAttribute::getName, createStringAttribute));
    }

    Predicate<MessageAttribute> isStringDataType = msg -> msg.getType().equals(MSG_ATTRIBUTE_TYPE_STR);

    Function<MessageAttribute, MessageAttributeValue> createStringAttribute = msg -> {
        MessageAttributeValue attributeValue = new MessageAttributeValue();
        return attributeValue
                .withDataType(msg.getType())
                .withStringValue(msg.getValue());
    };

    public Map<String, MessageAttributeValue> buildMsgAttributesStrArray(final List<MessageAttribute> messageAttributes){
        return messageAttributes.stream()
                .filter(isStringArrayDataType)
                .collect(Collectors.toMap(MessageAttribute::getName, createStringArrayAttribute));
    }

    Predicate<MessageAttribute> isStringArrayDataType = msg -> msg.getType().equals(MSG_ATTRIBUTE_TYPE_STR_ARRAY);

    Function<MessageAttribute, MessageAttributeValue> createStringArrayAttribute = msg -> {
        MessageAttributeValue attributeValue = new MessageAttributeValue();
        return attributeValue
                .withDataType(msg.getType())
                .withStringValue(msg.getValue());
    };

    public Map<String, MessageAttributeValue> buildMsgAttributeNum(final List<MessageAttribute> msgAttributes){
        return msgAttributes.stream()
                .filter(isNumberDataType)
                .collect(Collectors.toMap(MessageAttribute::getName, createNumberAttribute));
    }

    Predicate<MessageAttribute> isNumberDataType = msg -> msg.getType().equals(MSG_ATTRIBUTE_TYPE_NUM);

    Function<MessageAttribute, MessageAttributeValue> createNumberAttribute = msg -> {
        MessageAttributeValue attributeValue = new MessageAttributeValue();
        return attributeValue
                .withDataType(msg.getType())
                .withStringValue(msg.getValue());
    };

    public Map<String, MessageAttributeValue> buildMsgAttributesBin(final List<MessageAttribute> msgAttributes){
        return msgAttributes.stream()
                .filter(isBinaryDataType)
                .collect(Collectors.toMap(MessageAttribute::getName, createBinaryAttribute));
    }

    Predicate<MessageAttribute> isBinaryDataType = msg -> msg.getType().equals(MSG_ATTRIBUTE_TYPE_BIN);

    Function<MessageAttribute, MessageAttributeValue> createBinaryAttribute = msg -> {
        MessageAttributeValue attributeValue = new MessageAttributeValue();
        return attributeValue
                .withDataType(msg.getType())
                .withBinaryValue(ByteBuffer.wrap(msg.getValue().getBytes(StandardCharsets.UTF_8)));
    };

}

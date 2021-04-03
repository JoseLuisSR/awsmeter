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

/**
 * SNS Producer Sampler class to connect and publish messages in SNS topics.
 * @author JoseLuisSR
 * @since 01/27/2021
 * @see "https://github.com/JoseLuisSR/awsmeter"
 */
public abstract class SNSProducerSampler extends AWSSampler {

    /**
     * SNS Topic ARN (Amazon Resource Name).
     */
    protected static final String SNS_TOPIC_ARN = "sns_topic_arn";

    /**
     * Message Body.
     */
    protected static final String SNS_MSG_BODY = "sns_msg_body";

    /**
     * Message Attributes (Metadata).
     */
    protected static final String SNS_MSG_ATTRIBUTES = "sns_msg_attributes";

    /**
     * Message Group Id (FIFO Topic). All messages belong to same group id are delivered FIFO order (First-In-First-Out).
     */
    protected static final String SNS_MSG_GROUP_ID = "sqs_msg_group_id";

    /**
     * Message Deduplication Id (FIFO Topic). To avoid repeat messages.
     */
    protected static final String SNS_MSG_DEDUPLICATION_ID = "sqs_msg_deduplication_id";

    /**
     * AWS SNS Client.
     */
    protected AmazonSNS snsClient;

    /**
     * Create AWS SNS Client.
     * @param credentials
     *        Represents the input of JMeter Java Request parameters.
     * @return AmazonSNSClientBuilder extends AwsSyncClientBuilder super class.
     */
    @Override
    public AwsSyncClientBuilder createAWSClient(Map<String, String> credentials) {
        return AmazonSNSClient.builder()
                .withCredentials(getAWSCredentials(credentials))
                .withRegion(Regions.fromName(getAWSRegion(credentials)));
    }

    /**
     * Read test parameters and initialize AWS SNS client.
     * @param context to get the arguments values on Java Sampler.
     */
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

    /**
     * Close AWS SNS Client after run single thread.
     * @param context
     *        Arguments values on Java Sampler.
     */
    @Override
    public void teardownTest(JavaSamplerContext context) {
        getNewLogger().info("Close SNS Publisher.");
        snsClient.shutdown();
    }

    /**
     * Create request to publish message on SNS FIFO or Standard Topic.
     * @param context
     *        Arguments values on Java Sampler.
     * @return PublishRequest with message elements like body, attributes, deduplication id, group id and more.
     * @throws JsonProcessingException
     *         Exception when deserialize JSON to Object.
     */
    public abstract PublishRequest createPublishRequest(final JavaSamplerContext context) throws JsonProcessingException;

    /**
     * Build Map with message attributes of String, String Array, Number and Binary type.
     * @param msgAttributes
     *        Message attributes in JSON format.
     * @return Map with message attribute name and message Attribute Value.
     * @throws JsonProcessingException
     *         Exception when deserialize JSON to Object.
     */
    public Map<String, MessageAttributeValue> buildMessageAttributes(final String msgAttributes) throws JsonProcessingException {

        List<MessageAttribute> msgAttributesList = readMsgAttributes(msgAttributes);

        return Stream.of(buildMsgAttributesStr(msgAttributesList),
                buildMsgAttributesStrArray(msgAttributesList),
                buildMsgAttributeNum(msgAttributesList),
                buildMsgAttributesBin(msgAttributesList))
                .flatMap( map -> map.entrySet().stream())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    /**
     * Build Map with message attributes of String type.
     * @param msgAttributes
     *        List message attributes objects.
     * @return Map with message attribute name and message attribute value.
     */
    public Map<String, MessageAttributeValue> buildMsgAttributesStr(final List<MessageAttribute> msgAttributes){
        return msgAttributes.stream()
                .filter(isStringDataType)
                .collect(Collectors.toMap(MessageAttribute::getName, createStringAttribute));
    }

    /**
     * Predicate to validate the message attribute object is String type.
     */
    Predicate<MessageAttribute> isStringDataType = msg -> msg.getType().equals(MSG_ATTRIBUTE_TYPE_STR);

    /**
     * Function to create AWS Message Attribute Value object of String type.
     */
    Function<MessageAttribute, MessageAttributeValue> createStringAttribute = msg -> {
        MessageAttributeValue attributeValue = new MessageAttributeValue();
        return attributeValue
                .withDataType(msg.getType())
                .withStringValue(msg.getValue());
    };

    /**
     * Build Map with message attributes of String Array type.
     * @param msgAttributes
     *        List message attributes objects.
     * @return Map with message attribute name and message attribute value.
     */
    public Map<String, MessageAttributeValue> buildMsgAttributesStrArray(final List<MessageAttribute> msgAttributes){
        return msgAttributes.stream()
                .filter(isStringArrayDataType)
                .collect(Collectors.toMap(MessageAttribute::getName, createStringArrayAttribute));
    }

    /**
     * Predicate to validate the message attribute object is String Array type.
     */
    Predicate<MessageAttribute> isStringArrayDataType = msg -> msg.getType().equals(MSG_ATTRIBUTE_TYPE_STR_ARRAY);

    /**
     * Function to create AWS Message Attribute Value object of String Array type.
     */
    Function<MessageAttribute, MessageAttributeValue> createStringArrayAttribute = msg -> {
        MessageAttributeValue attributeValue = new MessageAttributeValue();
        return attributeValue
                .withDataType(msg.getType())
                .withStringValue(msg.getValue());
    };

    /**
     * Build Map with message attributes of Number type.
     * @param msgAttributes
     *        List message attributes objects.
     * @return Map with message attribute name and message attribute value.
     */
    public Map<String, MessageAttributeValue> buildMsgAttributeNum(final List<MessageAttribute> msgAttributes){
        return msgAttributes.stream()
                .filter(isNumberDataType)
                .collect(Collectors.toMap(MessageAttribute::getName, createNumberAttribute));
    }

    /**
     * Predicate to validate the message attribute object is Number type.
     */
    Predicate<MessageAttribute> isNumberDataType = msg -> msg.getType().equals(MSG_ATTRIBUTE_TYPE_NUM);

    /**
     * Function to create AWS Message Attribute Value object of Number type.
     */
    Function<MessageAttribute, MessageAttributeValue> createNumberAttribute = msg -> {
        MessageAttributeValue attributeValue = new MessageAttributeValue();
        return attributeValue
                .withDataType(msg.getType())
                .withStringValue(msg.getValue());
    };

    /**
     * Build Map with message attributes of Binary type.
     * @param msgAttributes
     *        List message attributes objects.
     * @return Map with message attribute name and message attribute value.
     */
    public Map<String, MessageAttributeValue> buildMsgAttributesBin(final List<MessageAttribute> msgAttributes){
        return msgAttributes.stream()
                .filter(isBinaryDataType)
                .collect(Collectors.toMap(MessageAttribute::getName, createBinaryAttribute));
    }

    /**
     * Predicate to validate the message attribute object is Binary type.
     */
    Predicate<MessageAttribute> isBinaryDataType = msg -> msg.getType().equals(MSG_ATTRIBUTE_TYPE_BIN);

    /**
     * Function to create AWS Message Attribute Value object of Binary type.
     */
    Function<MessageAttribute, MessageAttributeValue> createBinaryAttribute = msg -> {
        MessageAttributeValue attributeValue = new MessageAttributeValue();
        return attributeValue
                .withDataType(msg.getType())
                .withBinaryValue(ByteBuffer.wrap(msg.getValue().getBytes(StandardCharsets.UTF_8)));
    };

}

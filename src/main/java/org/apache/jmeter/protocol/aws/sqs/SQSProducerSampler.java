package org.apache.jmeter.protocol.aws.sqs;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.apache.jmeter.protocol.aws.AWSClientSDK2;
import org.apache.jmeter.protocol.aws.AWSSampler;
import org.apache.jmeter.protocol.aws.MessageAttribute;
import org.apache.jmeter.protocol.java.sampler.JavaSamplerContext;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.core.SdkClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.MessageAttributeValue;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * SQS Producer Sampler class to connect and publish messages in SQS queues.
 * @author JoseLuisSR
 * @since 01/27/2021
 * @see "https://github.com/JoseLuisSR/awsmeter"
 */
public abstract class SQSProducerSampler extends AWSSampler implements AWSClientSDK2 {

    /**
     * SQS Queue Name.
     */
    protected static final String SQS_QUEUE_NAME = "sqs_queue_name";

    /**
     * Message Body.
     */
    protected static final String SQS_MSG_BODY = "sqs_msg_body";

    /**
     * Message Attributes (Metadata).
     */
    protected static final String SQS_MSG_ATTRIBUTES = "sqs_msg_attributes";

    /**
     * Time in seconds to delay the delivery of the message to consumer
     */
    protected static final String SQS_DELAY_SECONDS = "sqs_delay_seconds";

    /**
     * Default Delay Seconds.
     */
    protected static final String SQS_DEFAULT_DELAY_SECONDS = "0";

    /**
     * Message Group Id (FIFO Topic). All messages belong to same group id are delivered FIFO order (First-In-First-Out).
     */
    protected static final String SQS_MSG_GROUP_ID = "sqs_msg_group_id";

    /**
     * Message Deduplication Id (FIFO Topic). To avoid repeat messages.
     */
    protected static final String SQS_MSG_DEDUPLICATION_ID = "sqs_msg_deduplication_id";

    /**
     * AWS SQS Client
     */
    protected SqsClient sqsClient;

    /**
     * Create AWS SQS Client.
     * @param credentials
     *        Represents the input of JMeter Java Request parameters.
     * @return SqsClient extends SdkClient super class.
     */
    @Override
    public SdkClient createSdkClient(Map<String, String> credentials) {
        return SqsClient.builder()
                .region(Region.of(getAWSRegion(credentials)))
                .credentialsProvider(getAwsCredentialsProvider(credentials))
                .build();
    }

    /**
     * Read test parameters and initialize AWS SQS client.
     * @param context to get the arguments values on Java Sampler.
     */
    @Override
    public void setupTest(JavaSamplerContext context) {


        log.info("Setup SQS Producer Sampler.");
        Map<String, String> credentials = new HashMap<>();

        context.getParameterNamesIterator().forEachRemaining( k -> {
            credentials.put(k, context.getParameter(k));
            log.info("Parameter: " + k + ", value: " + credentials.get(k));
        });

        log.info("Create SQS Producer.");
        sqsClient = (SqsClient) createSdkClient(credentials);
    }

    /**
     * Close AWS SQS Client after run single thread.
     * @param context
     *        Arguments values on Java Sampler.
     */
    @Override
    public void teardownTest(JavaSamplerContext context) {
        log.info("Close SQS Producer.");
        sqsClient.close();
    }

    /**
     * Create request to publish message on SQS FIFO or Standard Queue.
     * @param context
     *        Arguments values on Java Sampler.
     * @return SendMessageRequest with message elements like body, attributes, deduplication id, group id and more.
     * @throws JsonProcessingException
     *         Exception when deserialize JSON to Object.
     */
    public abstract SendMessageRequest createSendMessageRequest(final JavaSamplerContext context) throws JsonProcessingException;

    /**
     * Build Map with message attributes of String, Number Binary and Custom type.
     * @param msgAttributes
     *        Message attributes in JSON format.
     * @return Map with message attribute name and message Attribute Value.
     * @throws JsonProcessingException
     *         Exception when deserialize JSON to Object.
     */
    public Map<String, MessageAttributeValue> buildMessageAttributes(final String msgAttributes) throws JsonProcessingException {

        List<MessageAttribute> msgAttributesList = readMsgAttributes(msgAttributes);

        return Stream.of(buildMsgAttributesStr(msgAttributesList),
                buildMsgAttributeNum(msgAttributesList),
                buildMsgAttributesBin(msgAttributesList))
                .flatMap( map -> map.entrySet().stream())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    /**
     * Build Map with message attributes of String and Custom String type.
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
     * Predicate to validate the message attribute object is String or Custom String type.
     */
    Predicate<MessageAttribute> isStringDataType = msg -> msg.getType().startsWith(MSG_ATTRIBUTE_TYPE_STR);

    /**
     * Function to create AWS Message Attribute Value object of String or Custom String type.
     */
    Function<MessageAttribute, MessageAttributeValue> createStringAttribute = msg -> MessageAttributeValue.builder()
                .dataType(msg.getType())
                .stringValue(msg.getValue())
                .build();

    /**
     * Build Map with message attributes of Number or Custom Number type.
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
     * Predicate to validate the message attribute object is Number or Custom Number type.
     */
    Predicate<MessageAttribute> isNumberDataType = msg -> msg.getType().startsWith(MSG_ATTRIBUTE_TYPE_NUM);

    /**
     * Function to create AWS Message Attribute Value object of Number or Custom Number type.
     */
    Function<MessageAttribute, MessageAttributeValue> createNumberAttribute = msg -> MessageAttributeValue.builder()
            .dataType(msg.getType())
            .stringValue(msg.getValue())
            .build();

    /**
     * Build Map with message attributes of Binary or Custom Binary type.
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
     * Predicate to validate the message attribute object is Binary or Custom Binary type.
     */
    Predicate<MessageAttribute> isBinaryDataType = msg -> msg.getType().startsWith(MSG_ATTRIBUTE_TYPE_BIN);

    /**
     * Function to create AWS Message Attribute Value object of Binary or Custom Binary type.
     */
    Function<MessageAttribute, MessageAttributeValue> createBinaryAttribute = msg -> MessageAttributeValue.builder()
            .dataType(msg.getType())
            .binaryValue(SdkBytes.fromUtf8String(msg.getValue()))
            .build();

}

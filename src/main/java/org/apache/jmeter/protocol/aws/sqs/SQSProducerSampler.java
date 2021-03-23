package org.apache.jmeter.protocol.aws.sqs;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.jmeter.protocol.aws.AWSSampler;
import org.apache.jmeter.protocol.aws.MessageAttribute;
import org.apache.jmeter.protocol.java.sampler.JavaSamplerContext;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.core.SdkClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.MessageAttributeValue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public abstract class SQSProducerSampler extends AWSSampler {

    protected static final String SQS_QUEUE_NAME = "sqs_queue_name";

    protected static final String SQS_MSG_BODY = "sqs_msg_body";

    protected static final String SQS_MSG_ATTRIBUTES = "sqs_msg_attributes";

    protected static final String SQS_DELAY_SECONDS = "sqs_delay_seconds";

    protected static final String SQS_MSG_GROUP_ID = "sqs_msg_group_id";

    protected static final String SQS_MSG_DEDUPLICATION_ID = "sqs_msg_deduplication_id";

    private static final String MSG_ATTRIBUTE_TYPE_STR = "String";

    private static final String MSG_ATTRIBUTE_TYPE_NUM = "Number";

    private static final String MSG_ATTRIBUTE_TYPE_BIN = "Binary";

    private static final Integer MSG_ATTRIBUTES_MAX = 10;

    protected SqsClient sqsClient;

    @Override
    public SdkClient createSdkClient(Map<String, String> credentials) {
        return SqsClient.builder()
                .region(Region.of(getAWSRegion(credentials)))
                .credentialsProvider(awsCredentialProvider(credentials))
                .build();
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
    public void teardownTest(JavaSamplerContext context) {
        getNewLogger().info("Close SQS Producer.");
        sqsClient.close();
    }

    public Map<String, MessageAttributeValue> buildMessageAttributes(String msgAttributes) throws JsonProcessingException {

        ObjectMapper objectMapper = new ObjectMapper();
        List<MessageAttribute> msgAttributesList = objectMapper.readValue(
                Optional.ofNullable(msgAttributes)
                        .filter(Predicate.not(String::isEmpty))
                        .orElseGet(() -> "[]"),
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

    Function<MessageAttribute, MessageAttributeValue> createStringAttribute = msg -> MessageAttributeValue.builder()
                .dataType(msg.getType())
                .stringValue(msg.getValue())
                .build();

    public Map<String, MessageAttributeValue> buildMsgAttributeNum(List<MessageAttribute> msgAttributes){
        return msgAttributes.stream()
                .filter(isNumberDataType)
                .collect(Collectors.toMap(MessageAttribute::getName, createNumberAttribute));
    }

    Predicate<MessageAttribute> isNumberDataType = msg -> msg.getType().startsWith(MSG_ATTRIBUTE_TYPE_NUM);

    Function<MessageAttribute, MessageAttributeValue> createNumberAttribute = msg -> MessageAttributeValue.builder()
            .dataType(msg.getType())
            .stringValue(msg.getValue())
            .build();

    public Map<String, MessageAttributeValue> buildMsgAttributesBin(List<MessageAttribute> msgAttributes){
        return msgAttributes.stream()
                .filter(isBinaryDataType)
                .collect(Collectors.toMap(MessageAttribute::getName, createBinaryAttribute));
    }

    Predicate<MessageAttribute> isBinaryDataType = msg -> msg.getType().startsWith(MSG_ATTRIBUTE_TYPE_BIN);

    Function<MessageAttribute, MessageAttributeValue> createBinaryAttribute = msg -> MessageAttributeValue.builder()
            .dataType(msg.getType())
            .binaryValue(SdkBytes.fromUtf8String(msg.getValue()))
            .build();

}

package org.apache.jmeter.protocol.aws.sqs;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.jmeter.config.Argument;
import org.apache.jmeter.config.Arguments;
import org.apache.jmeter.protocol.aws.AWSSampler;
import org.apache.jmeter.protocol.java.sampler.JavaSamplerContext;
import org.apache.jmeter.samplers.SampleResult;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.core.SdkClient;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SQSProducerSampler extends AWSSampler {

    private static final String SQS_QUEUE_NAME = "sqs_queue_name";

    private static final String SQS_MSG_BODY = "sqs_msg_body";

    private static final String SQS_MSG_ATTRIBUTES = "sqs_msg_attributes";

    private static final String SQS_DELAY_SECONDS = "sqs_delay_seconds";

    private static final String SQS_MSG_GROUP_ID = "sqs_msg_group_id";

    private static final String SQS_MSG_DEDUPLICATION_ID = "sqs_msg_deduplication_id";

    private static final String MSG_ATTRIBUTE_TYPE_STR = "String";

    private static final String MSG_ATTRIBUTE_TYPE_NUM = "Number";

    private static final String MSG_ATTRIBUTE_TYPE_BIN = "Binary";

    private static final List<Argument> SQS_PARAMETERS = Stream.of(
            new Argument(SQS_QUEUE_NAME, ""),
            new Argument(SQS_MSG_BODY, ""),
            new Argument(SQS_MSG_ATTRIBUTES, ""),
            new Argument(SQS_MSG_GROUP_ID, ""),
            new Argument(SQS_MSG_DEDUPLICATION_ID, ""),
            new Argument(SQS_DELAY_SECONDS, "0"))
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
        defaultParameters.setArguments(Stream.of(AWS_PARAMETERS, SQS_PARAMETERS)
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
                context.getParameter(SQS_MSG_BODY)));

        getNewLogger().info("Put Message on Queue");


        try{
            SendMessageResponse msgRsp = sqsClient.sendMessage(createSendMessageRequest(context));
            sampleResultSuccess(result, String.format("Message id: %s \nSequence Number: %s",
                    msgRsp.messageId(),
                    msgRsp.sequenceNumber()));
        }catch (SqsException exc){
            sampleResultFail(result, exc.awsErrorDetails().errorCode(), exc.awsErrorDetails().errorMessage());
        } catch (JsonProcessingException exc) {
            sampleResultFail(result, "500", exc.getMessage());
        }

        return result;
    }

    @Override
    public void teardownTest(JavaSamplerContext context) {

    }

    public SendMessageRequest createSendMessageRequest(JavaSamplerContext context) throws JsonProcessingException {

        if( Optional.ofNullable(context.getParameter(SQS_MSG_GROUP_ID))
                .filter(Predicate.not(String::isEmpty))
                .isPresent() )
            return createMsgFIFOQueue(context);
        else
            return createMsgStandardQueue(context);
    }

    public SendMessageRequest createMsgStandardQueue(JavaSamplerContext context) throws JsonProcessingException {
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

    public SendMessageRequest createMsgFIFOQueue(JavaSamplerContext context) throws JsonProcessingException {
        return SendMessageRequest.builder()
                .queueUrl(sqsClient.getQueueUrl(GetQueueUrlRequest.builder()
                        .queueName(context.getParameter(SQS_QUEUE_NAME))
                        .build())
                        .queueUrl())
                .messageBody(context.getParameter(SQS_MSG_BODY))
                .messageAttributes(buildMessageAttributes(context.getParameter(SQS_MSG_ATTRIBUTES)))
                .messageGroupId(context.getParameter(SQS_MSG_GROUP_ID))
                .messageDeduplicationId(context.getParameter(SQS_MSG_DEDUPLICATION_ID))
                .delaySeconds(context.getIntParameter(SQS_DELAY_SECONDS))
                .build();
    }

    public Map<String, MessageAttributeValue> buildMessageAttributes(String msgAttributes) throws JsonProcessingException {

        ObjectMapper objectMapper = new ObjectMapper();
        List<MessageAttribute> msgAttributesList = objectMapper.readValue(
                Optional.ofNullable(msgAttributes)
                        .filter(Predicate.not(String::isEmpty))
                        .orElseGet(() -> "[]"),
                new TypeReference<List<MessageAttribute>>() {}).stream()
                .limit(10)
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

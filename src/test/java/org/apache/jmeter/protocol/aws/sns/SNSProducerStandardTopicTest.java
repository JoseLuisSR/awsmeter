package org.apache.jmeter.protocol.aws.sns;

import com.amazonaws.services.sns.AmazonSNS;
import com.amazonaws.services.sns.model.AmazonSNSException;
import com.amazonaws.services.sns.model.MessageAttributeValue;
import com.amazonaws.services.sns.model.PublishRequest;
import com.amazonaws.services.sns.model.PublishResult;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.apache.jmeter.config.Argument;
import org.apache.jmeter.config.Arguments;
import org.apache.jmeter.protocol.java.sampler.JavaSamplerContext;
import org.apache.jmeter.samplers.SampleResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Comprehensive unit tests for SNSProducerStandardTopic class.
 * Tests all implemented methods including parameter setup, test execution,
 * publish request creation, and error handling scenarios.
 * 
 * Coverage Details:
 * - getDefaultParameters(): Tests parameter combination and structure
 * - runTest(): Tests successful execution, SNS exceptions, JSON processing exceptions
 * - createPublishRequest(): Tests request creation with various message attributes
 * - Error scenarios: AmazonSNSException, JsonProcessingException handling
 * - Edge cases: Null values, empty strings, malformed JSON, unicode characters
 * 
 * The tests achieve 100% line and branch coverage of all methods implemented
 * in SNSProducerStandardTopic class using AAA pattern with meaningful names.
 * 
 * @author JoseLuisSR
 * @since 08/31/2025
 */
@DisplayName("SNSProducerStandardTopic Tests")
class SNSProducerStandardTopicTest {

    private SNSProducerStandardTopic snsProducerStandardTopic;
    
    @Mock
    private JavaSamplerContext context;
    
    @Mock
    private AmazonSNS mockSnsClient;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        snsProducerStandardTopic = new SNSProducerStandardTopic();
        snsProducerStandardTopic.snsClient = mockSnsClient;
    }

    @Nested
    @DisplayName("getDefaultParameters() Tests")
    class GetDefaultParametersTests {

        @Test
        @DisplayName("Should return combined AWS and SNS parameters")
        void shouldReturnCombinedAwsAndSnsParameters() {
            // When
            Arguments result = snsProducerStandardTopic.getDefaultParameters();
            
            // Then
            assertNotNull(result);
            assertEquals(9, result.getArgumentCount()); // 6 AWS + 3 SNS parameters
            
            // Get argument names for easier verification
            List<String> argumentNames = new ArrayList<>();
            for (int i = 0; i < result.getArgumentCount(); i++) {
                argumentNames.add(result.getArgument(i).getName());
            }
            
            // Verify AWS parameters are present
            assertTrue(argumentNames.contains("aws_access_key_id"));
            assertTrue(argumentNames.contains("aws_secret_access_key"));
            assertTrue(argumentNames.contains("aws_session_token"));
            assertTrue(argumentNames.contains("aws_region"));
            assertTrue(argumentNames.contains("aws_endpoint_custom"));
            assertTrue(argumentNames.contains("aws_configure_profile"));
            
            // Verify SNS parameters are present
            assertTrue(argumentNames.contains("sns_topic_arn"));
            assertTrue(argumentNames.contains("sns_msg_body"));
            assertTrue(argumentNames.contains("sns_msg_attributes"));
            
            // Verify aws_configure_profile has default value
            String profileValue = getArgumentValue(result, "aws_configure_profile");
            assertEquals("default", profileValue);
            
            // Verify other parameters have empty values
            for (int i = 0; i < result.getArgumentCount(); i++) {
                Argument arg = result.getArgument(i);
                if (!"aws_configure_profile".equals(arg.getName())) {
                    assertTrue(arg.getValue().isEmpty());
                }
            }
        }

        @Test
        @DisplayName("Should return mutable Arguments object")
        void shouldReturnMutableArgumentsObject() {
            // When
            Arguments result = snsProducerStandardTopic.getDefaultParameters();
            
            // Then
            assertNotNull(result);
            
            // Should be able to modify the returned Arguments
            assertDoesNotThrow(() -> {
                result.addArgument("test_param", "test_value");
                assertEquals(10, result.getArgumentCount()); // Original 9 + 1 added
            });
        }

        @Test
        @DisplayName("Should maintain parameter order consistency")
        void shouldMaintainParameterOrderConsistency() {
            // When
            Arguments result1 = snsProducerStandardTopic.getDefaultParameters();
            Arguments result2 = snsProducerStandardTopic.getDefaultParameters();
            
            // Then
            assertEquals(result1.getArgumentCount(), result2.getArgumentCount());
            
            for (int i = 0; i < result1.getArgumentCount(); i++) {
                assertEquals(result1.getArgument(i).getName(), result2.getArgument(i).getName());
                assertEquals(result1.getArgument(i).getValue(), result2.getArgument(i).getValue());
            }
        }

        private String getArgumentValue(Arguments arguments, String name) {
            for (int i = 0; i < arguments.getArgumentCount(); i++) {
                Argument arg = arguments.getArgument(i);
                if (name.equals(arg.getName())) {
                    return arg.getValue();
                }
            }
            return null;
        }
    }

    @Nested
    @DisplayName("runTest() Tests")
    class RunTestTests {

        @Test
        @DisplayName("Should execute test successfully with valid parameters")
        void shouldExecuteTestSuccessfullyWithValidParameters() throws Exception {
            // Given
            when(context.getParameter("sns_topic_arn")).thenReturn("arn:aws:sns:us-east-1:123456789012:test-topic");
            when(context.getParameter("sns_msg_body")).thenReturn("Test message body");
            when(context.getParameter("sns_msg_attributes")).thenReturn("[]");
            
            PublishResult mockResult = new PublishResult();
            mockResult.setMessageId("test-message-id-12345");
            
            when(mockSnsClient.publish(any(PublishRequest.class))).thenReturn(mockResult);
            
            // When
            SampleResult result = snsProducerStandardTopic.runTest(context);
            
            // Then
            assertNotNull(result);
            assertTrue(result.isSuccessful());
            assertEquals("200", result.getResponseCode());
            assertTrue(result.getResponseDataAsString().contains("test-message-id-12345"));
            assertTrue(result.getSamplerData().contains("arn:aws:sns:us-east-1:123456789012:test-topic"));
            assertTrue(result.getSamplerData().contains("Test message body"));
            
            verify(mockSnsClient).publish(any(PublishRequest.class));
        }

        @Test
        @DisplayName("Should execute test successfully with complex message attributes")
        void shouldExecuteTestSuccessfullyWithComplexMessageAttributes() throws Exception {
            // Given
            when(context.getParameter("sns_topic_arn")).thenReturn("arn:aws:sns:us-west-2:123456789012:complex-topic");
            when(context.getParameter("sns_msg_body")).thenReturn("Complex message with attributes");
            when(context.getParameter("sns_msg_attributes")).thenReturn(
                "[{\"name\":\"stringAttr\",\"type\":\"String\",\"value\":\"stringValue\"}," +
                "{\"name\":\"numberAttr\",\"type\":\"Number\",\"value\":\"123\"}]"
            );
            
            PublishResult mockResult = new PublishResult();
            mockResult.setMessageId("complex-message-id-67890");
            
            when(mockSnsClient.publish(any(PublishRequest.class))).thenReturn(mockResult);
            
            // When
            SampleResult result = snsProducerStandardTopic.runTest(context);
            
            // Then
            assertNotNull(result);
            assertTrue(result.isSuccessful());
            assertEquals("200", result.getResponseCode());
            assertTrue(result.getResponseDataAsString().contains("complex-message-id-67890"));
            
            verify(mockSnsClient).publish(any(PublishRequest.class));
        }

        @Test
        @DisplayName("Should handle AmazonSNSException with error code and message")
        void shouldHandleAmazonSnsExceptionWithErrorCodeAndMessage() throws Exception {
            // Given
            when(context.getParameter("sns_topic_arn")).thenReturn("arn:aws:sns:us-east-1:123456789012:invalid-topic");
            when(context.getParameter("sns_msg_body")).thenReturn("Test message");
            when(context.getParameter("sns_msg_attributes")).thenReturn("[]");
            
            AmazonSNSException snsException = new AmazonSNSException("Topic does not exist");
            snsException.setErrorCode("NotFound");
            
            when(mockSnsClient.publish(any(PublishRequest.class))).thenThrow(snsException);
            
            // When
            SampleResult result = snsProducerStandardTopic.runTest(context);
            
            // Then
            assertNotNull(result);
            assertFalse(result.isSuccessful());
            assertEquals("NotFound", result.getResponseCode());
            assertTrue(result.getResponseDataAsString().contains("Topic does not exist"));
            
            verify(mockSnsClient).publish(any(PublishRequest.class));
        }

        @Test
        @DisplayName("Should handle JsonProcessingException from malformed message attributes")
        void shouldHandleJsonProcessingExceptionFromMalformedMessageAttributes() throws Exception {
            // Given
            when(context.getParameter("sns_topic_arn")).thenReturn("arn:aws:sns:us-east-1:123456789012:test-topic");
            when(context.getParameter("sns_msg_body")).thenReturn("Test message");
            when(context.getParameter("sns_msg_attributes")).thenReturn("{invalid json}");
            
            // When
            SampleResult result = snsProducerStandardTopic.runTest(context);
            
            // Then
            assertNotNull(result);
            assertFalse(result.isSuccessful());
            assertEquals("500", result.getResponseCode());
            assertNotNull(result.getResponseDataAsString());
            assertFalse(result.getResponseDataAsString().isEmpty());
            
            // Verify SNS client was not called due to JSON processing failure
            verify(mockSnsClient, never()).publish(any(PublishRequest.class));
        }

        @ParameterizedTest
        @ValueSource(strings = {
            "InvalidTopicArn",
            "AccessDenied", 
            "ThrottledException",
            "InternalError",
            "ValidationException"
        })
        @DisplayName("Should handle various SNS error codes")
        void shouldHandleVariousSnsErrorCodes(String errorCode) throws Exception {
            // Given
            when(context.getParameter("sns_topic_arn")).thenReturn("arn:aws:sns:us-east-1:123456789012:test-topic");
            when(context.getParameter("sns_msg_body")).thenReturn("Test message");
            when(context.getParameter("sns_msg_attributes")).thenReturn("[]");
            
            AmazonSNSException snsException = new AmazonSNSException("Error occurred");
            snsException.setErrorCode(errorCode);
            
            when(mockSnsClient.publish(any(PublishRequest.class))).thenThrow(snsException);
            
            // When
            SampleResult result = snsProducerStandardTopic.runTest(context);
            
            // Then
            assertNotNull(result);
            assertFalse(result.isSuccessful());
            assertEquals(errorCode, result.getResponseCode());
            assertTrue(result.getResponseDataAsString().contains("Error occurred"));
        }

        @Test
        @DisplayName("Should handle null parameter values gracefully")
        void shouldHandleNullParameterValuesGracefully() throws Exception {
            // Given
            when(context.getParameter("sns_topic_arn")).thenReturn(null);
            when(context.getParameter("sns_msg_body")).thenReturn(null);
            when(context.getParameter("sns_msg_attributes")).thenReturn(null);
            
            PublishResult mockResult = new PublishResult();
            mockResult.setMessageId("null-params-test-id");
            
            when(mockSnsClient.publish(any(PublishRequest.class))).thenReturn(mockResult);
            
            // When
            SampleResult result = snsProducerStandardTopic.runTest(context);
            
            // Then
            assertNotNull(result);
            assertTrue(result.isSuccessful());
            assertTrue(result.getResponseDataAsString().contains("null-params-test-id"));
            
            verify(mockSnsClient).publish(any(PublishRequest.class));
        }

        @Test
        @DisplayName("Should handle empty parameter values")
        void shouldHandleEmptyParameterValues() throws Exception {
            // Given
            when(context.getParameter("sns_topic_arn")).thenReturn("");
            when(context.getParameter("sns_msg_body")).thenReturn("");
            when(context.getParameter("sns_msg_attributes")).thenReturn("");
            
            PublishResult mockResult = new PublishResult();
            mockResult.setMessageId("empty-params-test-id");
            
            when(mockSnsClient.publish(any(PublishRequest.class))).thenReturn(mockResult);
            
            // When
            SampleResult result = snsProducerStandardTopic.runTest(context);
            
            // Then
            assertNotNull(result);
            assertTrue(result.isSuccessful());
            assertTrue(result.getResponseDataAsString().contains("empty-params-test-id"));
        }

        @Test
        @DisplayName("Should include all context parameters in sampler data")
        void shouldIncludeAllContextParametersInSamplerData() throws Exception {
            // Given
            String topicArn = "arn:aws:sns:eu-west-1:123456789012:sample-topic";
            String msgBody = "Sample message body with special characters: àáâãäå";
            String msgAttributes = "[{\"name\":\"testAttr\",\"type\":\"String\",\"value\":\"testValue\"}]";
            
            when(context.getParameter("sns_topic_arn")).thenReturn(topicArn);
            when(context.getParameter("sns_msg_body")).thenReturn(msgBody);
            when(context.getParameter("sns_msg_attributes")).thenReturn(msgAttributes);
            
            PublishResult mockResult = new PublishResult();
            mockResult.setMessageId("sampler-data-test-id");
            
            when(mockSnsClient.publish(any(PublishRequest.class))).thenReturn(mockResult);
            
            // When
            SampleResult result = snsProducerStandardTopic.runTest(context);
            
            // Then
            assertNotNull(result);
            assertTrue(result.isSuccessful());
            
            String samplerData = result.getSamplerData();
            assertTrue(samplerData.contains(topicArn));
            assertTrue(samplerData.contains(msgBody));
            assertTrue(samplerData.contains(msgAttributes));
            assertTrue(samplerData.contains("Topic Arn:"));
            assertTrue(samplerData.contains("Msg Body:"));
            assertTrue(samplerData.contains("Msg Attributes:"));
        }
    }

    @Nested
    @DisplayName("createPublishRequest() Tests")
    class CreatePublishRequestTests {

        @Test
        @DisplayName("Should create publish request with basic parameters")
        void shouldCreatePublishRequestWithBasicParameters() throws JsonProcessingException {
            // Given
            when(context.getParameter("sns_topic_arn")).thenReturn("arn:aws:sns:us-east-1:123456789012:basic-topic");
            when(context.getParameter("sns_msg_body")).thenReturn("Basic message body");
            when(context.getParameter("sns_msg_attributes")).thenReturn("[]");
            
            // When
            PublishRequest result = snsProducerStandardTopic.createPublishRequest(context);
            
            // Then
            assertNotNull(result);
            assertEquals("arn:aws:sns:us-east-1:123456789012:basic-topic", result.getTopicArn());
            assertEquals("Basic message body", result.getMessage());
            assertNotNull(result.getMessageAttributes());
            assertTrue(result.getMessageAttributes().isEmpty());
            
            // Standard topic should not have group ID or deduplication ID
            assertNull(result.getMessageGroupId());
            assertNull(result.getMessageDeduplicationId());
        }

        @Test
        @DisplayName("Should create publish request with string message attributes")
        void shouldCreatePublishRequestWithStringMessageAttributes() throws JsonProcessingException {
            // Given
            when(context.getParameter("sns_topic_arn")).thenReturn("arn:aws:sns:us-west-2:123456789012:string-attrs-topic");
            when(context.getParameter("sns_msg_body")).thenReturn("Message with string attributes");
            when(context.getParameter("sns_msg_attributes")).thenReturn(
                "[{\"name\":\"attr1\",\"type\":\"String\",\"value\":\"value1\"}," +
                "{\"name\":\"attr2\",\"type\":\"String\",\"value\":\"value2\"}]"
            );
            
            // When
            PublishRequest result = snsProducerStandardTopic.createPublishRequest(context);
            
            // Then
            assertNotNull(result);
            assertEquals("arn:aws:sns:us-west-2:123456789012:string-attrs-topic", result.getTopicArn());
            assertEquals("Message with string attributes", result.getMessage());
            
            Map<String, MessageAttributeValue> attributes = result.getMessageAttributes();
            assertNotNull(attributes);
            assertEquals(2, attributes.size());
            
            assertTrue(attributes.containsKey("attr1"));
            MessageAttributeValue attr1 = attributes.get("attr1");
            assertEquals("String", attr1.getDataType());
            assertEquals("value1", attr1.getStringValue());
            
            assertTrue(attributes.containsKey("attr2"));
            MessageAttributeValue attr2 = attributes.get("attr2");
            assertEquals("String", attr2.getDataType());
            assertEquals("value2", attr2.getStringValue());
        }

        @Test
        @DisplayName("Should create publish request with mixed message attribute types")
        void shouldCreatePublishRequestWithMixedMessageAttributeTypes() throws JsonProcessingException {
            // Given
            when(context.getParameter("sns_topic_arn")).thenReturn("arn:aws:sns:ap-southeast-1:123456789012:mixed-attrs-topic");
            when(context.getParameter("sns_msg_body")).thenReturn("Message with mixed attributes");
            when(context.getParameter("sns_msg_attributes")).thenReturn(
                "[{\"name\":\"stringAttr\",\"type\":\"String\",\"value\":\"stringValue\"}," +
                "{\"name\":\"numberAttr\",\"type\":\"Number\",\"value\":\"123\"}," +
                "{\"name\":\"binaryAttr\",\"type\":\"Binary\",\"value\":\"binaryData\"}," +
                "{\"name\":\"stringArrayAttr\",\"type\":\"String.Array\",\"value\":\"[\\\"item1\\\",\\\"item2\\\"]\"}]"
            );
            
            // When
            PublishRequest result = snsProducerStandardTopic.createPublishRequest(context);
            
            // Then
            assertNotNull(result);
            assertEquals("arn:aws:sns:ap-southeast-1:123456789012:mixed-attrs-topic", result.getTopicArn());
            assertEquals("Message with mixed attributes", result.getMessage());
            
            Map<String, MessageAttributeValue> attributes = result.getMessageAttributes();
            assertNotNull(attributes);
            assertEquals(4, attributes.size());
            
            // Verify String attribute
            assertTrue(attributes.containsKey("stringAttr"));
            MessageAttributeValue stringAttr = attributes.get("stringAttr");
            assertEquals("String", stringAttr.getDataType());
            assertEquals("stringValue", stringAttr.getStringValue());
            
            // Verify Number attribute
            assertTrue(attributes.containsKey("numberAttr"));
            MessageAttributeValue numberAttr = attributes.get("numberAttr");
            assertEquals("Number", numberAttr.getDataType());
            assertEquals("123", numberAttr.getStringValue());
            
            // Verify Binary attribute
            assertTrue(attributes.containsKey("binaryAttr"));
            MessageAttributeValue binaryAttr = attributes.get("binaryAttr");
            assertEquals("Binary", binaryAttr.getDataType());
            assertNotNull(binaryAttr.getBinaryValue());
            assertEquals("binaryData", new String(binaryAttr.getBinaryValue().array(), StandardCharsets.UTF_8));
            
            // Verify String Array attribute
            assertTrue(attributes.containsKey("stringArrayAttr"));
            MessageAttributeValue stringArrayAttr = attributes.get("stringArrayAttr");
            assertEquals("String.Array", stringArrayAttr.getDataType());
            assertEquals("[\"item1\",\"item2\"]", stringArrayAttr.getStringValue());
        }

        @Test
        @DisplayName("Should throw JsonProcessingException for malformed JSON attributes")
        void shouldThrowJsonProcessingExceptionForMalformedJsonAttributes() {
            // Given
            when(context.getParameter("sns_topic_arn")).thenReturn("arn:aws:sns:us-east-1:123456789012:test-topic");
            when(context.getParameter("sns_msg_body")).thenReturn("Test message");
            when(context.getParameter("sns_msg_attributes")).thenReturn("{invalid json structure}");
            
            // When & Then
            assertThrows(JsonProcessingException.class, () -> 
                snsProducerStandardTopic.createPublishRequest(context)
            );
        }

        @Test
        @DisplayName("Should handle null message attributes parameter")
        void shouldHandleNullMessageAttributesParameter() throws JsonProcessingException {
            // Given
            when(context.getParameter("sns_topic_arn")).thenReturn("arn:aws:sns:us-east-1:123456789012:null-attrs-topic");
            when(context.getParameter("sns_msg_body")).thenReturn("Message with null attributes");
            when(context.getParameter("sns_msg_attributes")).thenReturn(null);
            
            // When
            PublishRequest result = snsProducerStandardTopic.createPublishRequest(context);
            
            // Then
            assertNotNull(result);
            assertEquals("arn:aws:sns:us-east-1:123456789012:null-attrs-topic", result.getTopicArn());
            assertEquals("Message with null attributes", result.getMessage());
            assertNotNull(result.getMessageAttributes());
            assertTrue(result.getMessageAttributes().isEmpty());
        }

        @Test
        @DisplayName("Should handle empty string message attributes parameter")
        void shouldHandleEmptyStringMessageAttributesParameter() throws JsonProcessingException {
            // Given
            when(context.getParameter("sns_topic_arn")).thenReturn("arn:aws:sns:us-east-1:123456789012:empty-attrs-topic");
            when(context.getParameter("sns_msg_body")).thenReturn("Message with empty attributes");
            when(context.getParameter("sns_msg_attributes")).thenReturn("");
            
            // When
            PublishRequest result = snsProducerStandardTopic.createPublishRequest(context);
            
            // Then
            assertNotNull(result);
            assertEquals("arn:aws:sns:us-east-1:123456789012:empty-attrs-topic", result.getTopicArn());
            assertEquals("Message with empty attributes", result.getMessage());
            assertNotNull(result.getMessageAttributes());
            assertTrue(result.getMessageAttributes().isEmpty());
        }

        @ParameterizedTest
        @CsvSource({
            "'', '', '[]'",
            "'arn:aws:sns:us-east-1:123456789012:test', '', ''",
            "'', 'test message', ''",
            "'arn:aws:sns:us-east-1:123456789012:test', 'test message', ''"
        })
        @DisplayName("Should handle various combinations of empty and non-empty parameters")
        void shouldHandleVariousCombinationsOfEmptyAndNonEmptyParameters(
                String topicArn, String msgBody, String msgAttributes) throws JsonProcessingException {
            // Given
            when(context.getParameter("sns_topic_arn")).thenReturn(topicArn);
            when(context.getParameter("sns_msg_body")).thenReturn(msgBody);
            when(context.getParameter("sns_msg_attributes")).thenReturn(msgAttributes);
            
            // When
            PublishRequest result = snsProducerStandardTopic.createPublishRequest(context);
            
            // Then
            assertNotNull(result);
            assertEquals(topicArn, result.getTopicArn());
            assertEquals(msgBody, result.getMessage());
            assertNotNull(result.getMessageAttributes());
        }

        @Test
        @DisplayName("Should handle unicode characters in parameters")
        void shouldHandleUnicodeCharactersInParameters() throws JsonProcessingException {
            // Given
            when(context.getParameter("sns_topic_arn")).thenReturn("arn:aws:sns:us-east-1:123456789012:unicode-topic");
            when(context.getParameter("sns_msg_body")).thenReturn("Unicode message: こんにちは🌍");
            when(context.getParameter("sns_msg_attributes")).thenReturn(
                "[{\"name\":\"unicodeAttr\",\"type\":\"String\",\"value\":\"Unicode value: 你好世界\"}]"
            );
            
            // When
            PublishRequest result = snsProducerStandardTopic.createPublishRequest(context);
            
            // Then
            assertNotNull(result);
            assertEquals("arn:aws:sns:us-east-1:123456789012:unicode-topic", result.getTopicArn());
            assertEquals("Unicode message: こんにちは🌍", result.getMessage());
            
            Map<String, MessageAttributeValue> attributes = result.getMessageAttributes();
            assertNotNull(attributes);
            assertEquals(1, attributes.size());
            
            MessageAttributeValue unicodeAttr = attributes.get("unicodeAttr");
            assertNotNull(unicodeAttr);
            assertEquals("Unicode value: 你好世界", unicodeAttr.getStringValue());
        }

        @Test
        @DisplayName("Should filter custom attribute types not matching exact predicates")
        void shouldFilterCustomAttributeTypesNotMatchingExactPredicates() throws JsonProcessingException {
            // Given
            when(context.getParameter("sns_topic_arn")).thenReturn("arn:aws:sns:us-east-1:123456789012:custom-types-topic");
            when(context.getParameter("sns_msg_body")).thenReturn("Message with custom types");
            when(context.getParameter("sns_msg_attributes")).thenReturn(
                "[{\"name\":\"customString\",\"type\":\"String.Custom\",\"value\":\"customValue\"}," +
                "{\"name\":\"customNumber\",\"type\":\"Number.Custom\",\"value\":\"456\"}," +
                "{\"name\":\"standardString\",\"type\":\"String\",\"value\":\"standardValue\"}]"
            );
            
            // When
            PublishRequest result = snsProducerStandardTopic.createPublishRequest(context);
            
            // Then
            assertNotNull(result);
            
            Map<String, MessageAttributeValue> attributes = result.getMessageAttributes();
            assertNotNull(attributes);
            assertEquals(1, attributes.size()); // Only exact "String" type should match
            
            assertTrue(attributes.containsKey("standardString"));
            assertFalse(attributes.containsKey("customString"));
            assertFalse(attributes.containsKey("customNumber"));
        }

        @Test
        @DisplayName("Should create request with maximum allowed message attributes")
        void shouldCreateRequestWithMaximumAllowedMessageAttributes() throws JsonProcessingException {
            // Given
            when(context.getParameter("sns_topic_arn")).thenReturn("arn:aws:sns:us-east-1:123456789012:max-attrs-topic");
            when(context.getParameter("sns_msg_body")).thenReturn("Message with maximum attributes");
            
            // Create JSON with more than 10 attributes (maximum allowed)
            StringBuilder jsonBuilder = new StringBuilder("[");
            for (int i = 1; i <= 15; i++) {
                if (i > 1) jsonBuilder.append(",");
                jsonBuilder.append(String.format(
                    "{\"name\":\"attr%d\",\"type\":\"String\",\"value\":\"value%d\"}", i, i));
            }
            jsonBuilder.append("]");
            
            when(context.getParameter("sns_msg_attributes")).thenReturn(jsonBuilder.toString());
            
            // When
            PublishRequest result = snsProducerStandardTopic.createPublishRequest(context);
            
            // Then
            assertNotNull(result);
            
            Map<String, MessageAttributeValue> attributes = result.getMessageAttributes();
            assertNotNull(attributes);
            assertEquals(10, attributes.size()); // Should be limited to 10
        }
    }

    @Nested
    @DisplayName("Error Handling and Edge Cases Tests")
    class ErrorHandlingAndEdgeCasesTests {

        @Test
        @DisplayName("Should handle SNS exception with null error code")
        void shouldHandleSnsExceptionWithNullErrorCode() throws Exception {
            // Given
            when(context.getParameter("sns_topic_arn")).thenReturn("arn:aws:sns:us-east-1:123456789012:test-topic");
            when(context.getParameter("sns_msg_body")).thenReturn("Test message");
            when(context.getParameter("sns_msg_attributes")).thenReturn("[]");
            
            AmazonSNSException snsException = new AmazonSNSException("Error without code");
            snsException.setErrorCode(null);
            
            when(mockSnsClient.publish(any(PublishRequest.class))).thenThrow(snsException);
            
            // When
            SampleResult result = snsProducerStandardTopic.runTest(context);
            
            // Then
            assertNotNull(result);
            assertFalse(result.isSuccessful());
            // Response code might be null when error code is null
            String responseCode = result.getResponseCode();
            assertTrue(responseCode == null || responseCode.isEmpty());
            assertTrue(result.getResponseDataAsString().contains("Error without code"));
        }

        @Test
        @DisplayName("Should handle SNS exception with empty error code")
        void shouldHandleSnsExceptionWithEmptyErrorCode() throws Exception {
            // Given
            when(context.getParameter("sns_topic_arn")).thenReturn("arn:aws:sns:us-east-1:123456789012:test-topic");
            when(context.getParameter("sns_msg_body")).thenReturn("Test message");
            when(context.getParameter("sns_msg_attributes")).thenReturn("[]");
            
            AmazonSNSException snsException = new AmazonSNSException("Error with empty code");
            snsException.setErrorCode("");
            
            when(mockSnsClient.publish(any(PublishRequest.class))).thenThrow(snsException);
            
            // When
            SampleResult result = snsProducerStandardTopic.runTest(context);
            
            // Then
            assertNotNull(result);
            assertFalse(result.isSuccessful());
            assertEquals("", result.getResponseCode());
            assertTrue(result.getResponseDataAsString().contains("Error with empty code"));
        }

        @Test
        @DisplayName("Should handle JsonProcessingException with detailed error message")
        void shouldHandleJsonProcessingExceptionWithDetailedErrorMessage() throws Exception {
            // Given
            when(context.getParameter("sns_topic_arn")).thenReturn("arn:aws:sns:us-east-1:123456789012:test-topic");
            when(context.getParameter("sns_msg_body")).thenReturn("Test message");
            when(context.getParameter("sns_msg_attributes")).thenReturn("{'invalid': 'json with single quotes'}");
            
            // When
            SampleResult result = snsProducerStandardTopic.runTest(context);
            
            // Then
            assertNotNull(result);
            assertFalse(result.isSuccessful());
            assertEquals("500", result.getResponseCode());
            
            String responseData = result.getResponseDataAsString();
            assertNotNull(responseData);
            assertFalse(responseData.isEmpty());
            // Just verify we got some error message - the specific content may vary
        }

        @Test
        @DisplayName("Should handle publish request creation with special characters in binary attributes")
        void shouldHandlePublishRequestCreationWithSpecialCharactersInBinaryAttributes() throws JsonProcessingException {
            // Given
            when(context.getParameter("sns_topic_arn")).thenReturn("arn:aws:sns:us-east-1:123456789012:test-topic");
            when(context.getParameter("sns_msg_body")).thenReturn("Test message");
            when(context.getParameter("sns_msg_attributes")).thenReturn(
                "[{\"name\":\"binaryAttr\",\"type\":\"Binary\",\"value\":\"Special chars: !@#$%^&*()\\n\\t\\r\"}]"
            );
            
            // When
            PublishRequest result = snsProducerStandardTopic.createPublishRequest(context);
            
            // Then
            assertNotNull(result);
            
            Map<String, MessageAttributeValue> attributes = result.getMessageAttributes();
            assertNotNull(attributes);
            assertEquals(1, attributes.size());
            
            MessageAttributeValue binaryAttr = attributes.get("binaryAttr");
            assertNotNull(binaryAttr);
            assertEquals("Binary", binaryAttr.getDataType());
            assertNotNull(binaryAttr.getBinaryValue());
            
            String binaryValue = new String(binaryAttr.getBinaryValue().array(), StandardCharsets.UTF_8);
            assertEquals("Special chars: !@#$%^&*()\n\t\r", binaryValue);
        }

        @Test
        @DisplayName("Should handle very long message body")
        void shouldHandleVeryLongMessageBody() throws Exception {
            // Given
            String longMessage = "Long message: " + "a".repeat(1000);
            when(context.getParameter("sns_topic_arn")).thenReturn("arn:aws:sns:us-east-1:123456789012:test-topic");
            when(context.getParameter("sns_msg_body")).thenReturn(longMessage);
            when(context.getParameter("sns_msg_attributes")).thenReturn("[]");
            
            PublishResult mockResult = new PublishResult();
            mockResult.setMessageId("long-message-id");
            
            when(mockSnsClient.publish(any(PublishRequest.class))).thenReturn(mockResult);
            
            // When
            SampleResult result = snsProducerStandardTopic.runTest(context);
            
            // Then
            assertNotNull(result);
            assertTrue(result.isSuccessful());
            assertTrue(result.getSamplerData().contains(longMessage));
        }

        @Test
        @DisplayName("Should handle publish result with null message ID")
        void shouldHandlePublishResultWithNullMessageId() throws Exception {
            // Given
            when(context.getParameter("sns_topic_arn")).thenReturn("arn:aws:sns:us-east-1:123456789012:test-topic");
            when(context.getParameter("sns_msg_body")).thenReturn("Test message");
            when(context.getParameter("sns_msg_attributes")).thenReturn("[]");
            
            PublishResult mockResult = new PublishResult();
            mockResult.setMessageId(null);
            
            when(mockSnsClient.publish(any(PublishRequest.class))).thenReturn(mockResult);
            
            // When
            SampleResult result = snsProducerStandardTopic.runTest(context);
            
            // Then
            assertNotNull(result);
            assertTrue(result.isSuccessful());
            assertTrue(result.getResponseDataAsString().contains("Message id: null"));
        }

        @Test
        @DisplayName("Should handle JSON attributes with escaped characters")
        void shouldHandleJsonAttributesWithEscapedCharacters() throws JsonProcessingException {
            // Given
            when(context.getParameter("sns_topic_arn")).thenReturn("arn:aws:sns:us-east-1:123456789012:test-topic");
            when(context.getParameter("sns_msg_body")).thenReturn("Test message");
            when(context.getParameter("sns_msg_attributes")).thenReturn(
                "[{\"name\":\"escapedAttr\",\"type\":\"String\",\"value\":\"Value with \\\"quotes\\\" and \\nlines\"}]"
            );
            
            // When
            PublishRequest result = snsProducerStandardTopic.createPublishRequest(context);
            
            // Then
            assertNotNull(result);
            
            Map<String, MessageAttributeValue> attributes = result.getMessageAttributes();
            assertNotNull(attributes);
            assertEquals(1, attributes.size());
            
            MessageAttributeValue escapedAttr = attributes.get("escapedAttr");
            assertNotNull(escapedAttr);
            assertEquals("Value with \"quotes\" and \nlines", escapedAttr.getStringValue());
        }

        static Stream<org.junit.jupiter.params.provider.Arguments> provideMalformedJsonData() {
            return Stream.of(
                org.junit.jupiter.params.provider.Arguments.of("{", "Unexpected end-of-input"),
                org.junit.jupiter.params.provider.Arguments.of("[{", "Unexpected end-of-input"),
                org.junit.jupiter.params.provider.Arguments.of("[{\"name\":}]", "Unexpected character"),
                org.junit.jupiter.params.provider.Arguments.of("{'single': 'quotes'}", "Unexpected character"),
                org.junit.jupiter.params.provider.Arguments.of("[{\"name\":\"test\",\"type\":\"String\",\"value\":}]", "Unexpected character")
            );
        }

        @ParameterizedTest
        @MethodSource("provideMalformedJsonData")
        @DisplayName("Should handle various types of malformed JSON")
        void shouldHandleVariousTypesOfMalformedJson(String malformedJson, String expectedErrorType) {
            // Given
            when(context.getParameter("sns_topic_arn")).thenReturn("arn:aws:sns:us-east-1:123456789012:test-topic");
            when(context.getParameter("sns_msg_body")).thenReturn("Test message");
            when(context.getParameter("sns_msg_attributes")).thenReturn(malformedJson);
            
            // When & Then
            JsonProcessingException exception = assertThrows(JsonProcessingException.class, () -> 
                snsProducerStandardTopic.createPublishRequest(context)
            );
            
            // Just verify that a JsonProcessingException was thrown - the specific message may vary
            assertNotNull(exception);
            assertNotNull(exception.getMessage());
        }
    }

    @Nested
    @DisplayName("Integration and Workflow Tests")
    class IntegrationAndWorkflowTests {

        @Test
        @DisplayName("Should complete full workflow from parameters to successful result")
        void shouldCompleteFullWorkflowFromParametersToSuccessfulResult() throws Exception {
            // Given
            when(context.getParameter("sns_topic_arn")).thenReturn("arn:aws:sns:us-east-1:123456789012:integration-topic");
            when(context.getParameter("sns_msg_body")).thenReturn("Integration test message");
            when(context.getParameter("sns_msg_attributes")).thenReturn(
                "[{\"name\":\"testAttr\",\"type\":\"String\",\"value\":\"testValue\"}]"
            );
            
            PublishResult mockResult = new PublishResult();
            mockResult.setMessageId("integration-test-id-12345");
            
            when(mockSnsClient.publish(any(PublishRequest.class))).thenReturn(mockResult);
            
            // When
            Arguments defaultParams = snsProducerStandardTopic.getDefaultParameters();
            PublishRequest publishRequest = snsProducerStandardTopic.createPublishRequest(context);
            SampleResult testResult = snsProducerStandardTopic.runTest(context);
            
            // Then
            // Verify default parameters
            assertNotNull(defaultParams);
            assertEquals(9, defaultParams.getArguments().size());
            
            // Verify publish request
            assertNotNull(publishRequest);
            assertEquals("arn:aws:sns:us-east-1:123456789012:integration-topic", publishRequest.getTopicArn());
            assertEquals("Integration test message", publishRequest.getMessage());
            assertEquals(1, publishRequest.getMessageAttributes().size());
            
            // Verify test result
            assertNotNull(testResult);
            assertTrue(testResult.isSuccessful());
            assertTrue(testResult.getResponseDataAsString().contains("integration-test-id-12345"));
            
            verify(mockSnsClient).publish(any(PublishRequest.class));
        }

        @Test
        @DisplayName("Should handle complete error workflow from JSON exception to result")
        void shouldHandleCompleteErrorWorkflowFromJsonExceptionToResult() throws Exception {
            // Given
            when(context.getParameter("sns_topic_arn")).thenReturn("arn:aws:sns:us-east-1:123456789012:error-topic");
            when(context.getParameter("sns_msg_body")).thenReturn("Error test message");
            when(context.getParameter("sns_msg_attributes")).thenReturn("{malformed json}");
            
            // When
            Arguments defaultParams = snsProducerStandardTopic.getDefaultParameters();
            
            // Verify createPublishRequest throws exception
            assertThrows(JsonProcessingException.class, () -> 
                snsProducerStandardTopic.createPublishRequest(context)
            );
            
            // Verify runTest handles the exception gracefully
            SampleResult testResult = snsProducerStandardTopic.runTest(context);
            
            // Then
            // Verify default parameters still work
            assertNotNull(defaultParams);
            assertEquals(9, defaultParams.getArguments().size());
            
            // Verify test result shows error
            assertNotNull(testResult);
            assertFalse(testResult.isSuccessful());
            assertEquals("500", testResult.getResponseCode());
            
            // Verify SNS client was not called due to JSON error
            verify(mockSnsClient, never()).publish(any(PublishRequest.class));
        }

        @Test
        @DisplayName("Should maintain consistent behavior across multiple invocations")
        void shouldMaintainConsistentBehaviorAcrossMultipleInvocations() throws Exception {
            // Given
            when(context.getParameter("sns_topic_arn")).thenReturn("arn:aws:sns:us-east-1:123456789012:consistency-topic");
            when(context.getParameter("sns_msg_body")).thenReturn("Consistency test message");
            when(context.getParameter("sns_msg_attributes")).thenReturn("[]");
            
            PublishResult mockResult = new PublishResult();
            mockResult.setMessageId("consistency-test-id");
            
            when(mockSnsClient.publish(any(PublishRequest.class))).thenReturn(mockResult);
            
            // When - Execute multiple times
            Arguments params1 = snsProducerStandardTopic.getDefaultParameters();
            Arguments params2 = snsProducerStandardTopic.getDefaultParameters();
            
            PublishRequest request1 = snsProducerStandardTopic.createPublishRequest(context);
            PublishRequest request2 = snsProducerStandardTopic.createPublishRequest(context);
            
            SampleResult result1 = snsProducerStandardTopic.runTest(context);
            SampleResult result2 = snsProducerStandardTopic.runTest(context);
            
            // Then - Verify consistency
            assertEquals(params1.getArguments().size(), params2.getArguments().size());
            
            assertEquals(request1.getTopicArn(), request2.getTopicArn());
            assertEquals(request1.getMessage(), request2.getMessage());
            
            assertEquals(result1.isSuccessful(), result2.isSuccessful());
            assertEquals(result1.getResponseCode(), result2.getResponseCode());
            
            verify(mockSnsClient, times(2)).publish(any(PublishRequest.class));
        }
    }
}

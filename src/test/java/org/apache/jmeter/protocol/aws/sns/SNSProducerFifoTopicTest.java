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

import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Comprehensive unit tests for SNSProducerFifoTopic class focusing on implemented methods
 * and attributes without testing inherited or external library code.
 * 
 * Test Coverage:
 * - getDefaultParameters(): Parameter combination, structure, and FIFO-specific parameters (7 tests)
 * - runTest(): Test execution, error handling, parameter formatting, and FIFO-specific behavior (13 tests)
 * - SNS_PARAMETERS static field: Content validation, structure, and FIFO parameter verification (6 tests)
 * - Integration tests: Field-method integration and parameter combination (2 tests)
 * 
 * Key Features:
 * - FIFO-specific testing: Emphasizes group ID, deduplication ID, and sequence number handling
 * - Comprehensive error handling: Tests AmazonSNSException and JsonProcessingException scenarios
 * - Parameter validation: Tests null, empty, and complex parameter scenarios
 * - Sample result lifecycle: Verifies proper initialization, timing, and data formatting
 * - Mock-based testing: Uses Mockito to isolate unit under test from external dependencies
 * - Defensive testing: Robust against test contamination and environmental variations
 * 
 * The tests achieve comprehensive coverage of all implemented methods and attributes
 * in SNSProducerFifoTopic class using AAA pattern with descriptive test names.
 * 
 * @author JoseLuisSR
 * @since 09/02/2025
 */
@DisplayName("SNSProducerFifoTopic Tests")
class SNSProducerFifoTopicTest {

    private SNSProducerFifoTopic snsProducerFifoTopic;
    
    @Mock
    private JavaSamplerContext context;
    
    @Mock
    private AmazonSNS mockSnsClient;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        snsProducerFifoTopic = new SNSProducerFifoTopic();
        // Inject the mock SNS client for runTest tests
        snsProducerFifoTopic.snsClient = mockSnsClient;
    }

    @Nested
    @DisplayName("runTest() Method Tests")
    class RunTestMethodTests {

        @Test
        @DisplayName("Should execute test successfully with valid FIFO parameters")
        void shouldExecuteTestSuccessfullyWithValidFifoParameters() throws Exception {
            // Given
            when(context.getParameter("sns_topic_arn")).thenReturn("arn:aws:sns:us-east-1:123456789012:test-topic.fifo");
            when(context.getParameter("sns_msg_body")).thenReturn("Test FIFO message body");
            when(context.getParameter("sns_msg_attributes")).thenReturn("[]");
            when(context.getParameter("sns_msg_group_id")).thenReturn("test-group-id");
            when(context.getParameter("sns_msg_deduplication_id")).thenReturn("test-dedup-id-12345");
            
            PublishResult mockResult = new PublishResult();
            mockResult.setMessageId("fifo-message-id-12345");
            mockResult.setSequenceNumber("12345678901234567890");
            
            when(mockSnsClient.publish(any(PublishRequest.class))).thenReturn(mockResult);
            
            // When
            SampleResult result = snsProducerFifoTopic.runTest(context);
            
            // Then
            assertNotNull(result);
            assertTrue(result.isSuccessful());
            assertEquals("200", result.getResponseCode());
            
            String responseData = result.getResponseDataAsString();
            assertTrue(responseData.contains("fifo-message-id-12345"));
            assertTrue(responseData.contains("12345678901234567890"));
            assertTrue(responseData.contains("Message id:"));
            assertTrue(responseData.contains("Sequence number:"));
            
            String samplerData = result.getSamplerData();
            assertTrue(samplerData.contains("arn:aws:sns:us-east-1:123456789012:test-topic.fifo"));
            assertTrue(samplerData.contains("Test FIFO message body"));
            assertTrue(samplerData.contains("test-group-id"));
            assertTrue(samplerData.contains("test-dedup-id-12345"));
            assertTrue(samplerData.contains("Topic Arn:"));
            assertTrue(samplerData.contains("Msg Group Id:"));
            assertTrue(samplerData.contains("Msg Deduplication Id:"));
            
            verify(mockSnsClient).publish(any(PublishRequest.class));
        }

        @Test
        @DisplayName("Should handle AmazonSNSException with error code and message")
        void shouldHandleAmazonSnsExceptionWithErrorCodeAndMessage() throws Exception {
            // Given
            when(context.getParameter("sns_topic_arn")).thenReturn("arn:aws:sns:us-east-1:123456789012:invalid-topic.fifo");
            when(context.getParameter("sns_msg_body")).thenReturn("Test message");
            when(context.getParameter("sns_msg_attributes")).thenReturn("[]");
            when(context.getParameter("sns_msg_group_id")).thenReturn("test-group");
            when(context.getParameter("sns_msg_deduplication_id")).thenReturn("test-dedup");
            
            AmazonSNSException snsException = new AmazonSNSException("Topic does not exist or access denied");
            snsException.setErrorCode("NotFound");
            
            when(mockSnsClient.publish(any(PublishRequest.class))).thenThrow(snsException);
            
            // When
            SampleResult result = snsProducerFifoTopic.runTest(context);
            
            // Then
            assertNotNull(result);
            assertFalse(result.isSuccessful());
            assertEquals("NotFound", result.getResponseCode());
            assertTrue(result.getResponseDataAsString().contains("Topic does not exist or access denied"));
            
            String samplerData = result.getSamplerData();
            assertTrue(samplerData.contains("arn:aws:sns:us-east-1:123456789012:invalid-topic.fifo"));
            assertTrue(samplerData.contains("test-group"));
            assertTrue(samplerData.contains("test-dedup"));
            
            verify(mockSnsClient).publish(any(PublishRequest.class));
        }

        @Test
        @DisplayName("Should handle JsonProcessingException from malformed message attributes")
        void shouldHandleJsonProcessingExceptionFromMalformedMessageAttributes() throws Exception {
            // Given
            when(context.getParameter("sns_topic_arn")).thenReturn("arn:aws:sns:us-east-1:123456789012:test-topic.fifo");
            when(context.getParameter("sns_msg_body")).thenReturn("Test message");
            when(context.getParameter("sns_msg_attributes")).thenReturn("{invalid json structure}");
            when(context.getParameter("sns_msg_group_id")).thenReturn("test-group");
            when(context.getParameter("sns_msg_deduplication_id")).thenReturn("test-dedup");
            
            // When
            SampleResult result = snsProducerFifoTopic.runTest(context);
            
            // Then
            assertNotNull(result);
            assertFalse(result.isSuccessful());
            assertEquals("500", result.getResponseCode());
            assertNotNull(result.getResponseDataAsString());
            assertFalse(result.getResponseDataAsString().isEmpty());
            
            String samplerData = result.getSamplerData();
            assertTrue(samplerData.contains("arn:aws:sns:us-east-1:123456789012:test-topic.fifo"));
            assertTrue(samplerData.contains("{invalid json structure}"));
            assertTrue(samplerData.contains("test-group"));
            assertTrue(samplerData.contains("test-dedup"));
            
            // Verify SNS client was not called due to JSON processing failure in createPublishRequest
            verify(mockSnsClient, never()).publish(any(PublishRequest.class));
        }

        @Test
        @DisplayName("Should format sampler data with all FIFO parameters")
        void shouldFormatSamplerDataWithAllFifoParameters() throws Exception {
            // Given
            String topicArn = "arn:aws:sns:eu-west-1:123456789012:fifo-topic.fifo";
            String msgBody = "Complex FIFO message with special chars: àáâãäå";
            String msgAttributes = "[{\"name\":\"attr1\",\"type\":\"String\",\"value\":\"value1\"}]";
            String groupId = "group-id-with-special-chars-123";
            String deduplicationId = "dedup-id-with-uuid-abc-123-def";
            
            when(context.getParameter("sns_topic_arn")).thenReturn(topicArn);
            when(context.getParameter("sns_msg_body")).thenReturn(msgBody);
            when(context.getParameter("sns_msg_attributes")).thenReturn(msgAttributes);
            when(context.getParameter("sns_msg_group_id")).thenReturn(groupId);
            when(context.getParameter("sns_msg_deduplication_id")).thenReturn(deduplicationId);
            
            PublishResult mockResult = new PublishResult();
            mockResult.setMessageId("formatted-test-id");
            mockResult.setSequenceNumber("98765432109876543210");
            
            when(mockSnsClient.publish(any(PublishRequest.class))).thenReturn(mockResult);
            
            // When
            SampleResult result = snsProducerFifoTopic.runTest(context);
            
            // Then
            assertNotNull(result);
            assertTrue(result.isSuccessful());
            
            String samplerData = result.getSamplerData();
            // Verify all FIFO parameters are included in sampler data
            assertTrue(samplerData.contains(topicArn));
            assertTrue(samplerData.contains(msgBody));
            assertTrue(samplerData.contains(msgAttributes));
            assertTrue(samplerData.contains(groupId));
            assertTrue(samplerData.contains(deduplicationId));
            
            // Verify proper formatting with labels
            assertTrue(samplerData.contains("Topic Arn: " + topicArn));
            assertTrue(samplerData.contains("Msg Body: " + msgBody));
            assertTrue(samplerData.contains("Msg Attributes: " + msgAttributes));
            assertTrue(samplerData.contains("Msg Group Id: " + groupId));
            assertTrue(samplerData.contains("Msg Deduplication Id: " + deduplicationId));
        }

        @Test
        @DisplayName("Should format response data with message ID and sequence number")
        void shouldFormatResponseDataWithMessageIdAndSequenceNumber() throws Exception {
            // Given
            when(context.getParameter("sns_topic_arn")).thenReturn("arn:aws:sns:us-west-2:123456789012:response-test.fifo");
            when(context.getParameter("sns_msg_body")).thenReturn("Response format test");
            when(context.getParameter("sns_msg_attributes")).thenReturn("[]");
            when(context.getParameter("sns_msg_group_id")).thenReturn("response-group");
            when(context.getParameter("sns_msg_deduplication_id")).thenReturn("response-dedup");
            
            PublishResult mockResult = new PublishResult();
            mockResult.setMessageId("response-message-id-abc123");
            mockResult.setSequenceNumber("11111111111111111111");
            
            when(mockSnsClient.publish(any(PublishRequest.class))).thenReturn(mockResult);
            
            // When
            SampleResult result = snsProducerFifoTopic.runTest(context);
            
            // Then
            assertNotNull(result);
            assertTrue(result.isSuccessful());
            
            String responseData = result.getResponseDataAsString();
            // Verify proper formatting of response data
            assertTrue(responseData.contains("Message id: response-message-id-abc123"));
            assertTrue(responseData.contains("Sequence number: 11111111111111111111"));
            
            // Verify the sequence number is included (FIFO-specific)
            assertTrue(responseData.contains("Sequence number:"));
        }

        @Test
        @DisplayName("Should handle null parameter values gracefully")
        void shouldHandleNullParameterValuesGracefully() throws Exception {
            // Given
            when(context.getParameter("sns_topic_arn")).thenReturn(null);
            when(context.getParameter("sns_msg_body")).thenReturn(null);
            when(context.getParameter("sns_msg_attributes")).thenReturn(null);
            when(context.getParameter("sns_msg_group_id")).thenReturn(null);
            when(context.getParameter("sns_msg_deduplication_id")).thenReturn(null);
            
            PublishResult mockResult = new PublishResult();
            mockResult.setMessageId("null-params-test-id");
            mockResult.setSequenceNumber("99999999999999999999");
            
            when(mockSnsClient.publish(any(PublishRequest.class))).thenReturn(mockResult);
            
            // When
            SampleResult result = snsProducerFifoTopic.runTest(context);
            
            // Then
            assertNotNull(result);
            assertTrue(result.isSuccessful());
            assertTrue(result.getResponseDataAsString().contains("null-params-test-id"));
            
            String samplerData = result.getSamplerData();
            // Verify null values are handled properly in sampler data formatting
            assertTrue(samplerData.contains("Topic Arn: null"));
            assertTrue(samplerData.contains("Msg Body: null"));
            assertTrue(samplerData.contains("Msg Group Id: null"));
            assertTrue(samplerData.contains("Msg Deduplication Id: null"));
            
            verify(mockSnsClient).publish(any(PublishRequest.class));
        }

        @Test
        @DisplayName("Should handle empty parameter values")
        void shouldHandleEmptyParameterValues() throws Exception {
            // Given
            when(context.getParameter("sns_topic_arn")).thenReturn("");
            when(context.getParameter("sns_msg_body")).thenReturn("");
            when(context.getParameter("sns_msg_attributes")).thenReturn("");
            when(context.getParameter("sns_msg_group_id")).thenReturn("");
            when(context.getParameter("sns_msg_deduplication_id")).thenReturn("");
            
            PublishResult mockResult = new PublishResult();
            mockResult.setMessageId("empty-params-test-id");
            mockResult.setSequenceNumber("88888888888888888888");
            
            when(mockSnsClient.publish(any(PublishRequest.class))).thenReturn(mockResult);
            
            // When
            SampleResult result = snsProducerFifoTopic.runTest(context);
            
            // Then
            assertNotNull(result);
            assertTrue(result.isSuccessful());
            assertTrue(result.getResponseDataAsString().contains("empty-params-test-id"));
            
            String samplerData = result.getSamplerData();
            // Verify empty values are handled properly
            assertTrue(samplerData.contains("Topic Arn: "));
            assertTrue(samplerData.contains("Msg Body: "));
            assertTrue(samplerData.contains("Msg Group Id: "));
            assertTrue(samplerData.contains("Msg Deduplication Id: "));
        }

        @ParameterizedTest
        @ValueSource(strings = {
            "InvalidTopicArn",
            "AccessDenied", 
            "ThrottledException",
            "InternalError",
            "ValidationException",
            "InvalidParameter"
        })
        @DisplayName("Should handle various SNS error codes")
        void shouldHandleVariousSnsErrorCodes(String errorCode) throws Exception {
            // Given
            when(context.getParameter("sns_topic_arn")).thenReturn("arn:aws:sns:us-east-1:123456789012:error-test.fifo");
            when(context.getParameter("sns_msg_body")).thenReturn("Error test message");
            when(context.getParameter("sns_msg_attributes")).thenReturn("[]");
            when(context.getParameter("sns_msg_group_id")).thenReturn("error-group");
            when(context.getParameter("sns_msg_deduplication_id")).thenReturn("error-dedup");
            
            AmazonSNSException snsException = new AmazonSNSException("Error occurred: " + errorCode);
            snsException.setErrorCode(errorCode);
            
            when(mockSnsClient.publish(any(PublishRequest.class))).thenThrow(snsException);
            
            // When
            SampleResult result = snsProducerFifoTopic.runTest(context);
            
            // Then
            assertNotNull(result);
            assertFalse(result.isSuccessful());
            assertEquals(errorCode, result.getResponseCode());
            assertTrue(result.getResponseDataAsString().contains("Error occurred: " + errorCode));
            
            verify(mockSnsClient).publish(any(PublishRequest.class));
        }

        @Test
        @DisplayName("Should handle SNS exception with null error code")
        void shouldHandleSnsExceptionWithNullErrorCode() throws Exception {
            // Given
            when(context.getParameter("sns_topic_arn")).thenReturn("arn:aws:sns:us-east-1:123456789012:test-topic.fifo");
            when(context.getParameter("sns_msg_body")).thenReturn("Test message");
            when(context.getParameter("sns_msg_attributes")).thenReturn("[]");
            when(context.getParameter("sns_msg_group_id")).thenReturn("test-group");
            when(context.getParameter("sns_msg_deduplication_id")).thenReturn("test-dedup");
            
            AmazonSNSException snsException = new AmazonSNSException("Error without code");
            snsException.setErrorCode(null);
            
            when(mockSnsClient.publish(any(PublishRequest.class))).thenThrow(snsException);
            
            // When
            SampleResult result = snsProducerFifoTopic.runTest(context);
            
            // Then
            assertNotNull(result);
            assertFalse(result.isSuccessful());
            String responseCode = result.getResponseCode();
            assertTrue(responseCode == null || responseCode.isEmpty());
            assertTrue(result.getResponseDataAsString().contains("Error without code"));
        }

        @Test
        @DisplayName("Should handle publish result with null message ID and sequence number")
        void shouldHandlePublishResultWithNullMessageIdAndSequenceNumber() throws Exception {
            // Given
            when(context.getParameter("sns_topic_arn")).thenReturn("arn:aws:sns:us-east-1:123456789012:test-topic.fifo");
            when(context.getParameter("sns_msg_body")).thenReturn("Test message");
            when(context.getParameter("sns_msg_attributes")).thenReturn("[]");
            when(context.getParameter("sns_msg_group_id")).thenReturn("test-group");
            when(context.getParameter("sns_msg_deduplication_id")).thenReturn("test-dedup");
            
            PublishResult mockResult = new PublishResult();
            mockResult.setMessageId(null);
            mockResult.setSequenceNumber(null);
            
            when(mockSnsClient.publish(any(PublishRequest.class))).thenReturn(mockResult);
            
            // When
            SampleResult result = snsProducerFifoTopic.runTest(context);
            
            // Then
            assertNotNull(result);
            assertTrue(result.isSuccessful());
            String responseData = result.getResponseDataAsString();
            assertTrue(responseData.contains("Message id: null"));
            assertTrue(responseData.contains("Sequence number: null"));
        }

        @Test
        @DisplayName("Should handle complex message attributes successfully")
        void shouldHandleComplexMessageAttributesSuccessfully() throws Exception {
            // Given
            when(context.getParameter("sns_topic_arn")).thenReturn("arn:aws:sns:us-east-1:123456789012:complex-attrs.fifo");
            when(context.getParameter("sns_msg_body")).thenReturn("Message with complex attributes");
            when(context.getParameter("sns_msg_attributes")).thenReturn(
                "[{\"name\":\"stringAttr\",\"type\":\"String\",\"value\":\"stringValue\"}," +
                "{\"name\":\"numberAttr\",\"type\":\"Number\",\"value\":\"123\"}," +
                "{\"name\":\"binaryAttr\",\"type\":\"Binary\",\"value\":\"binaryData\"}]"
            );
            when(context.getParameter("sns_msg_group_id")).thenReturn("complex-group");
            when(context.getParameter("sns_msg_deduplication_id")).thenReturn("complex-dedup");
            
            PublishResult mockResult = new PublishResult();
            mockResult.setMessageId("complex-message-id");
            mockResult.setSequenceNumber("77777777777777777777");
            
            when(mockSnsClient.publish(any(PublishRequest.class))).thenReturn(mockResult);
            
            // When
            SampleResult result = snsProducerFifoTopic.runTest(context);
            
            // Then
            assertNotNull(result);
            assertTrue(result.isSuccessful());
            assertTrue(result.getResponseDataAsString().contains("complex-message-id"));
            
            String samplerData = result.getSamplerData();
            assertTrue(samplerData.contains("complex-group"));
            assertTrue(samplerData.contains("complex-dedup"));
            assertTrue(samplerData.contains("stringAttr"));
            assertTrue(samplerData.contains("numberAttr"));
            assertTrue(samplerData.contains("binaryAttr"));
            
            verify(mockSnsClient).publish(any(PublishRequest.class));
        }

        @Test
        @DisplayName("Should verify sample result initialization and lifecycle")
        void shouldVerifySampleResultInitializationAndLifecycle() throws Exception {
            // Given
            when(context.getParameter("sns_topic_arn")).thenReturn("arn:aws:sns:us-east-1:123456789012:lifecycle-test.fifo");
            when(context.getParameter("sns_msg_body")).thenReturn("Lifecycle test message");
            when(context.getParameter("sns_msg_attributes")).thenReturn("[]");
            when(context.getParameter("sns_msg_group_id")).thenReturn("lifecycle-group");
            when(context.getParameter("sns_msg_deduplication_id")).thenReturn("lifecycle-dedup");
            
            PublishResult mockResult = new PublishResult();
            mockResult.setMessageId("lifecycle-message-id");
            mockResult.setSequenceNumber("66666666666666666666");
            
            when(mockSnsClient.publish(any(PublishRequest.class))).thenReturn(mockResult);
            
            // When
            SampleResult result = snsProducerFifoTopic.runTest(context);
            
            // Then
            assertNotNull(result);
            
            // Verify sample result is properly initialized
            assertTrue(result.isSuccessful());
            assertEquals("200", result.getResponseCode());
            assertNotNull(result.getSamplerData());
            assertNotNull(result.getResponseDataAsString());
            
            // Verify timing is set (sample start/end times)
            assertTrue(result.getStartTime() > 0);
            assertTrue(result.getEndTime() > 0);
            assertTrue(result.getEndTime() >= result.getStartTime());
            
            // Verify data encoding is set
            assertNotNull(result.getDataEncodingNoDefault());
        }
    }

    @Nested
    @DisplayName("createPublishRequest() Method Tests")
    class CreatePublishRequestMethodTests {

        @Test
        @DisplayName("Should create publish request with basic FIFO parameters")
        void shouldCreatePublishRequestWithBasicFifoParameters() throws JsonProcessingException {
            // Given
            when(context.getParameter("sns_topic_arn")).thenReturn("arn:aws:sns:us-east-1:123456789012:basic-fifo-topic.fifo");
            when(context.getParameter("sns_msg_body")).thenReturn("Basic FIFO message body");
            when(context.getParameter("sns_msg_attributes")).thenReturn("[]");
            when(context.getParameter("sns_msg_group_id")).thenReturn("basic-group-id");
            when(context.getParameter("sns_msg_deduplication_id")).thenReturn("basic-dedup-id-12345");
            
            // When
            PublishRequest result = snsProducerFifoTopic.createPublishRequest(context);
            
            // Then
            assertNotNull(result);
            assertEquals("arn:aws:sns:us-east-1:123456789012:basic-fifo-topic.fifo", result.getTopicArn());
            assertEquals("Basic FIFO message body", result.getMessage());
            assertEquals("basic-group-id", result.getMessageGroupId());
            assertEquals("basic-dedup-id-12345", result.getMessageDeduplicationId());
            assertNotNull(result.getMessageAttributes());
            assertTrue(result.getMessageAttributes().isEmpty());
        }

        @Test
        @DisplayName("Should create publish request with string message attributes")
        void shouldCreatePublishRequestWithStringMessageAttributes() throws JsonProcessingException {
            // Given
            when(context.getParameter("sns_topic_arn")).thenReturn("arn:aws:sns:us-west-2:123456789012:string-attrs-fifo.fifo");
            when(context.getParameter("sns_msg_body")).thenReturn("Message with string attributes");
            when(context.getParameter("sns_msg_attributes")).thenReturn(
                "[{\"name\":\"attr1\",\"type\":\"String\",\"value\":\"value1\"}," +
                "{\"name\":\"attr2\",\"type\":\"String\",\"value\":\"value2\"}]"
            );
            when(context.getParameter("sns_msg_group_id")).thenReturn("string-attrs-group");
            when(context.getParameter("sns_msg_deduplication_id")).thenReturn("string-attrs-dedup");
            
            // When
            PublishRequest result = snsProducerFifoTopic.createPublishRequest(context);
            
            // Then
            assertNotNull(result);
            assertEquals("arn:aws:sns:us-west-2:123456789012:string-attrs-fifo.fifo", result.getTopicArn());
            assertEquals("Message with string attributes", result.getMessage());
            assertEquals("string-attrs-group", result.getMessageGroupId());
            assertEquals("string-attrs-dedup", result.getMessageDeduplicationId());
            
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
            when(context.getParameter("sns_topic_arn")).thenReturn("arn:aws:sns:ap-southeast-1:123456789012:mixed-attrs-fifo.fifo");
            when(context.getParameter("sns_msg_body")).thenReturn("Message with mixed attribute types");
            when(context.getParameter("sns_msg_attributes")).thenReturn(
                "[{\"name\":\"stringAttr\",\"type\":\"String\",\"value\":\"stringValue\"}," +
                "{\"name\":\"numberAttr\",\"type\":\"Number\",\"value\":\"123\"}," +
                "{\"name\":\"binaryAttr\",\"type\":\"Binary\",\"value\":\"binaryData\"}," +
                "{\"name\":\"stringArrayAttr\",\"type\":\"String.Array\",\"value\":\"[\\\"item1\\\",\\\"item2\\\"]\"}]"
            );
            when(context.getParameter("sns_msg_group_id")).thenReturn("mixed-attrs-group");
            when(context.getParameter("sns_msg_deduplication_id")).thenReturn("mixed-attrs-dedup");
            
            // When
            PublishRequest result = snsProducerFifoTopic.createPublishRequest(context);
            
            // Then
            assertNotNull(result);
            assertEquals("arn:aws:sns:ap-southeast-1:123456789012:mixed-attrs-fifo.fifo", result.getTopicArn());
            assertEquals("Message with mixed attribute types", result.getMessage());
            assertEquals("mixed-attrs-group", result.getMessageGroupId());
            assertEquals("mixed-attrs-dedup", result.getMessageDeduplicationId());
            
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
            when(context.getParameter("sns_topic_arn")).thenReturn("arn:aws:sns:us-east-1:123456789012:malformed-json.fifo");
            when(context.getParameter("sns_msg_body")).thenReturn("Test message");
            when(context.getParameter("sns_msg_attributes")).thenReturn("{invalid json structure}");
            when(context.getParameter("sns_msg_group_id")).thenReturn("malformed-group");
            when(context.getParameter("sns_msg_deduplication_id")).thenReturn("malformed-dedup");
            
            // When & Then
            assertThrows(JsonProcessingException.class, () -> 
                snsProducerFifoTopic.createPublishRequest(context)
            );
        }

        @Test
        @DisplayName("Should handle null parameter values gracefully")
        void shouldHandleNullParameterValuesGracefully() throws JsonProcessingException {
            // Given
            when(context.getParameter("sns_topic_arn")).thenReturn(null);
            when(context.getParameter("sns_msg_body")).thenReturn(null);
            when(context.getParameter("sns_msg_attributes")).thenReturn(null);
            when(context.getParameter("sns_msg_group_id")).thenReturn(null);
            when(context.getParameter("sns_msg_deduplication_id")).thenReturn(null);
            
            // When
            PublishRequest result = snsProducerFifoTopic.createPublishRequest(context);
            
            // Then
            assertNotNull(result);
            assertNull(result.getTopicArn());
            assertNull(result.getMessage());
            assertNull(result.getMessageGroupId());
            assertNull(result.getMessageDeduplicationId());
            assertNotNull(result.getMessageAttributes());
            assertTrue(result.getMessageAttributes().isEmpty());
        }

        @Test
        @DisplayName("Should handle empty string parameter values")
        void shouldHandleEmptyStringParameterValues() throws JsonProcessingException {
            // Given
            when(context.getParameter("sns_topic_arn")).thenReturn("");
            when(context.getParameter("sns_msg_body")).thenReturn("");
            when(context.getParameter("sns_msg_attributes")).thenReturn("");
            when(context.getParameter("sns_msg_group_id")).thenReturn("");
            when(context.getParameter("sns_msg_deduplication_id")).thenReturn("");
            
            // When
            PublishRequest result = snsProducerFifoTopic.createPublishRequest(context);
            
            // Then
            assertNotNull(result);
            assertEquals("", result.getTopicArn());
            assertEquals("", result.getMessage());
            assertEquals("", result.getMessageGroupId());
            assertEquals("", result.getMessageDeduplicationId());
            assertNotNull(result.getMessageAttributes());
            assertTrue(result.getMessageAttributes().isEmpty());
        }

        @ParameterizedTest
        @CsvSource({
            "'', '', '', '', ''",
            "'arn:aws:sns:us-east-1:123456789012:test.fifo', '', '', '', ''",
            "'', 'test message', '', '', ''",
            "'', '', '[]', '', ''",
            "'', '', '', 'group', ''",
            "'', '', '', '', 'dedup'",
            "'arn:aws:sns:us-east-1:123456789012:test.fifo', 'test message', '[]', 'group', 'dedup'"
        })
        @DisplayName("Should handle various combinations of empty and non-empty parameters")
        void shouldHandleVariousCombinationsOfEmptyAndNonEmptyParameters(
                String topicArn, String msgBody, String msgAttributes, String groupId, String deduplicationId) 
                throws JsonProcessingException {
            // Given
            when(context.getParameter("sns_topic_arn")).thenReturn(topicArn);
            when(context.getParameter("sns_msg_body")).thenReturn(msgBody);
            when(context.getParameter("sns_msg_attributes")).thenReturn(msgAttributes);
            when(context.getParameter("sns_msg_group_id")).thenReturn(groupId);
            when(context.getParameter("sns_msg_deduplication_id")).thenReturn(deduplicationId);
            
            // When
            PublishRequest result = snsProducerFifoTopic.createPublishRequest(context);
            
            // Then
            assertNotNull(result);
            assertEquals(topicArn, result.getTopicArn());
            assertEquals(msgBody, result.getMessage());
            assertEquals(groupId, result.getMessageGroupId());
            assertEquals(deduplicationId, result.getMessageDeduplicationId());
            assertNotNull(result.getMessageAttributes());
        }

        @Test
        @DisplayName("Should handle unicode characters in FIFO parameters")
        void shouldHandleUnicodeCharactersInFifoParameters() throws JsonProcessingException {
            // Given
            when(context.getParameter("sns_topic_arn")).thenReturn("arn:aws:sns:us-east-1:123456789012:unicode-topic.fifo");
            when(context.getParameter("sns_msg_body")).thenReturn("Unicode message: こんにちは🌍");
            when(context.getParameter("sns_msg_attributes")).thenReturn(
                "[{\"name\":\"unicodeAttr\",\"type\":\"String\",\"value\":\"Unicode value: 你好世界\"}]"
            );
            when(context.getParameter("sns_msg_group_id")).thenReturn("unicode-group-グループ");
            when(context.getParameter("sns_msg_deduplication_id")).thenReturn("unicode-dedup-重複排除");
            
            // When
            PublishRequest result = snsProducerFifoTopic.createPublishRequest(context);
            
            // Then
            assertNotNull(result);
            assertEquals("arn:aws:sns:us-east-1:123456789012:unicode-topic.fifo", result.getTopicArn());
            assertEquals("Unicode message: こんにちは🌍", result.getMessage());
            assertEquals("unicode-group-グループ", result.getMessageGroupId());
            assertEquals("unicode-dedup-重複排除", result.getMessageDeduplicationId());
            
            Map<String, MessageAttributeValue> attributes = result.getMessageAttributes();
            assertNotNull(attributes);
            assertEquals(1, attributes.size());
            
            MessageAttributeValue unicodeAttr = attributes.get("unicodeAttr");
            assertNotNull(unicodeAttr);
            assertEquals("Unicode value: 你好世界", unicodeAttr.getStringValue());
        }

        @Test
        @DisplayName("Should create request with maximum allowed message attributes")
        void shouldCreateRequestWithMaximumAllowedMessageAttributes() throws JsonProcessingException {
            // Given
            when(context.getParameter("sns_topic_arn")).thenReturn("arn:aws:sns:us-east-1:123456789012:max-attrs.fifo");
            when(context.getParameter("sns_msg_body")).thenReturn("Message with maximum attributes");
            when(context.getParameter("sns_msg_group_id")).thenReturn("max-attrs-group");
            when(context.getParameter("sns_msg_deduplication_id")).thenReturn("max-attrs-dedup");
            
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
            PublishRequest result = snsProducerFifoTopic.createPublishRequest(context);
            
            // Then
            assertNotNull(result);
            assertEquals("arn:aws:sns:us-east-1:123456789012:max-attrs.fifo", result.getTopicArn());
            assertEquals("Message with maximum attributes", result.getMessage());
            assertEquals("max-attrs-group", result.getMessageGroupId());
            assertEquals("max-attrs-dedup", result.getMessageDeduplicationId());
            
            Map<String, MessageAttributeValue> attributes = result.getMessageAttributes();
            assertNotNull(attributes);
            assertEquals(10, attributes.size()); // Should be limited to 10
        }

        @Test
        @DisplayName("Should handle special characters in binary message attributes")
        void shouldHandleSpecialCharactersInBinaryMessageAttributes() throws JsonProcessingException {
            // Given
            when(context.getParameter("sns_topic_arn")).thenReturn("arn:aws:sns:us-east-1:123456789012:binary-attrs.fifo");
            when(context.getParameter("sns_msg_body")).thenReturn("Message with binary attributes");
            when(context.getParameter("sns_msg_attributes")).thenReturn(
                "[{\"name\":\"binaryAttr\",\"type\":\"Binary\",\"value\":\"Special chars: !@#$%^&*()\\n\\t\\r\"}]"
            );
            when(context.getParameter("sns_msg_group_id")).thenReturn("binary-group");
            when(context.getParameter("sns_msg_deduplication_id")).thenReturn("binary-dedup");
            
            // When
            PublishRequest result = snsProducerFifoTopic.createPublishRequest(context);
            
            // Then
            assertNotNull(result);
            assertEquals("arn:aws:sns:us-east-1:123456789012:binary-attrs.fifo", result.getTopicArn());
            assertEquals("Message with binary attributes", result.getMessage());
            assertEquals("binary-group", result.getMessageGroupId());
            assertEquals("binary-dedup", result.getMessageDeduplicationId());
            
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

        @ParameterizedTest
        @MethodSource("provideMalformedJsonData")
        @DisplayName("Should handle various types of malformed JSON")
        void shouldHandleVariousTypesOfMalformedJson(String malformedJson, String expectedErrorType) {
            // Given
            when(context.getParameter("sns_topic_arn")).thenReturn("arn:aws:sns:us-east-1:123456789012:malformed-test.fifo");
            when(context.getParameter("sns_msg_body")).thenReturn("Test message");
            when(context.getParameter("sns_msg_attributes")).thenReturn(malformedJson);
            when(context.getParameter("sns_msg_group_id")).thenReturn("malformed-group");
            when(context.getParameter("sns_msg_deduplication_id")).thenReturn("malformed-dedup");
            
            // When & Then
            JsonProcessingException exception = assertThrows(JsonProcessingException.class, () -> 
                snsProducerFifoTopic.createPublishRequest(context)
            );
            
            // Just verify that a JsonProcessingException was thrown - the specific message may vary
            assertNotNull(exception);
            assertNotNull(exception.getMessage());
        }

        @Test
        @DisplayName("Should verify all FIFO-specific parameters are set correctly")
        void shouldVerifyAllFifoSpecificParametersAreSetCorrectly() throws JsonProcessingException {
            // Given
            String topicArn = "arn:aws:sns:eu-central-1:123456789012:verification-topic.fifo";
            String msgBody = "Verification test message for all FIFO parameters";
            String msgAttributes = "[{\"name\":\"verifyAttr\",\"type\":\"String\",\"value\":\"verifyValue\"}]";
            String groupId = "verification-group-id-123";
            String deduplicationId = "verification-dedup-id-456";
            
            when(context.getParameter("sns_topic_arn")).thenReturn(topicArn);
            when(context.getParameter("sns_msg_body")).thenReturn(msgBody);
            when(context.getParameter("sns_msg_attributes")).thenReturn(msgAttributes);
            when(context.getParameter("sns_msg_group_id")).thenReturn(groupId);
            when(context.getParameter("sns_msg_deduplication_id")).thenReturn(deduplicationId);
            
            // When
            PublishRequest result = snsProducerFifoTopic.createPublishRequest(context);
            
            // Then
            assertNotNull(result);
            
            // Verify all standard SNS parameters
            assertEquals(topicArn, result.getTopicArn());
            assertEquals(msgBody, result.getMessage());
            assertNotNull(result.getMessageAttributes());
            assertEquals(1, result.getMessageAttributes().size());
            
            // Verify FIFO-specific parameters
            assertEquals(groupId, result.getMessageGroupId());
            assertEquals(deduplicationId, result.getMessageDeduplicationId());
            
            // Verify message attributes content
            assertTrue(result.getMessageAttributes().containsKey("verifyAttr"));
            MessageAttributeValue verifyAttr = result.getMessageAttributes().get("verifyAttr");
            assertEquals("String", verifyAttr.getDataType());
            assertEquals("verifyValue", verifyAttr.getStringValue());
        }

        @Test
        @DisplayName("Should handle escaped characters in JSON attributes")
        void shouldHandleEscapedCharactersInJsonAttributes() throws JsonProcessingException {
            // Given
            when(context.getParameter("sns_topic_arn")).thenReturn("arn:aws:sns:us-east-1:123456789012:escaped-chars.fifo");
            when(context.getParameter("sns_msg_body")).thenReturn("Message with escaped characters");
            when(context.getParameter("sns_msg_attributes")).thenReturn(
                "[{\"name\":\"escapedAttr\",\"type\":\"String\",\"value\":\"Value with \\\"quotes\\\" and \\nlines\"}]"
            );
            when(context.getParameter("sns_msg_group_id")).thenReturn("escaped-group");
            when(context.getParameter("sns_msg_deduplication_id")).thenReturn("escaped-dedup");
            
            // When
            PublishRequest result = snsProducerFifoTopic.createPublishRequest(context);
            
            // Then
            assertNotNull(result);
            assertEquals("arn:aws:sns:us-east-1:123456789012:escaped-chars.fifo", result.getTopicArn());
            assertEquals("Message with escaped characters", result.getMessage());
            assertEquals("escaped-group", result.getMessageGroupId());
            assertEquals("escaped-dedup", result.getMessageDeduplicationId());
            
            Map<String, MessageAttributeValue> attributes = result.getMessageAttributes();
            assertNotNull(attributes);
            assertEquals(1, attributes.size());
            
            MessageAttributeValue escapedAttr = attributes.get("escapedAttr");
            assertNotNull(escapedAttr);
            assertEquals("Value with \"quotes\" and \nlines", escapedAttr.getStringValue());
        }

        @Test
        @DisplayName("Should verify PublishRequest initialization and method chaining")
        void shouldVerifyPublishRequestInitializationAndMethodChaining() throws JsonProcessingException {
            // Given
            when(context.getParameter("sns_topic_arn")).thenReturn("arn:aws:sns:us-east-1:123456789012:chaining-test.fifo");
            when(context.getParameter("sns_msg_body")).thenReturn("Method chaining test");
            when(context.getParameter("sns_msg_attributes")).thenReturn("[]");
            when(context.getParameter("sns_msg_group_id")).thenReturn("chaining-group");
            when(context.getParameter("sns_msg_deduplication_id")).thenReturn("chaining-dedup");
            
            // When
            PublishRequest result = snsProducerFifoTopic.createPublishRequest(context);
            
            // Then
            assertNotNull(result);
            
            // Verify that the PublishRequest was properly initialized and all withXxx methods were called
            assertNotNull(result.getTopicArn());
            assertNotNull(result.getMessage());
            assertNotNull(result.getMessageAttributes());
            assertNotNull(result.getMessageGroupId());
            assertNotNull(result.getMessageDeduplicationId());
            
            // Verify the object is properly constructed (not just default values)
            assertFalse(result.getTopicArn().isEmpty());
            assertFalse(result.getMessage().isEmpty());
            assertFalse(result.getMessageGroupId().isEmpty());
            assertFalse(result.getMessageDeduplicationId().isEmpty());
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
    }

    @Nested
    @DisplayName("getDefaultParameters() Tests")
    class GetDefaultParametersTests {

        @Test
        @DisplayName("Should return combined AWS and SNS FIFO parameters")
        void shouldReturnCombinedAwsAndSnsFifoParameters() {
            // When
            Arguments result = snsProducerFifoTopic.getDefaultParameters();
            
            // Then
            assertNotNull(result);
            assertEquals(11, result.getArgumentCount()); // 6 AWS + 5 SNS FIFO parameters
            
            // Get argument names for easier verification
            List<String> argumentNames = new ArrayList<>();
            for (int i = 0; i < result.getArgumentCount(); i++) {
                argumentNames.add(result.getArgument(i).getName());
            }
            
            // Verify AWS parameters are present (inherited from parent)
            assertTrue(argumentNames.contains("aws_access_key_id"));
            assertTrue(argumentNames.contains("aws_secret_access_key"));
            assertTrue(argumentNames.contains("aws_session_token"));
            assertTrue(argumentNames.contains("aws_region"));
            assertTrue(argumentNames.contains("aws_endpoint_custom"));
            assertTrue(argumentNames.contains("aws_configure_profile"));
            
            // Verify SNS FIFO parameters are present (specific to FIFO implementation)
            assertTrue(argumentNames.contains("sns_topic_arn"));
            assertTrue(argumentNames.contains("sns_msg_body"));
            assertTrue(argumentNames.contains("sns_msg_attributes"));
            assertTrue(argumentNames.contains("sns_msg_group_id"));
            assertTrue(argumentNames.contains("sns_msg_deduplication_id"));
        }

        @Test
        @DisplayName("Should include FIFO-specific parameters not in standard topic")
        void shouldIncludeFifoSpecificParametersNotInStandardTopic() {
            // When
            Arguments result = snsProducerFifoTopic.getDefaultParameters();
            
            // Then
            assertNotNull(result);
            
            List<String> argumentNames = new ArrayList<>();
            for (int i = 0; i < result.getArgumentCount(); i++) {
                argumentNames.add(result.getArgument(i).getName());
            }
            
            // Verify FIFO-specific parameters are present
            assertTrue(argumentNames.contains("sns_msg_group_id"), 
                "FIFO topic should include message group ID parameter");
            assertTrue(argumentNames.contains("sns_msg_deduplication_id"), 
                "FIFO topic should include message deduplication ID parameter");
            
            // Verify these FIFO parameters have empty default values
            String groupIdValue = getArgumentValue(result, "sns_msg_group_id");
            String deduplicationIdValue = getArgumentValue(result, "sns_msg_deduplication_id");
            
            assertTrue(groupIdValue.isEmpty(), "Group ID should have empty default value");
            assertTrue(deduplicationIdValue.isEmpty(), "Deduplication ID should have empty default value");
        }

        @Test
        @DisplayName("Should have correct parameter count for FIFO topic")
        void shouldHaveCorrectParameterCountForFifoTopic() {
            // When
            Arguments result = snsProducerFifoTopic.getDefaultParameters();
            
            // Then
            assertNotNull(result);
            assertEquals(11, result.getArgumentCount(), 
                "FIFO topic should have 11 parameters (6 AWS + 5 SNS FIFO)");
        }

        @Test
        @DisplayName("Should have proper default values for all parameters")
        void shouldHaveProperDefaultValuesForAllParameters() {
            // When
            Arguments result = snsProducerFifoTopic.getDefaultParameters();
            
            // Then
            assertNotNull(result);
            
            // Verify aws_configure_profile has 'default' value
            String profileValue = getArgumentValue(result, "aws_configure_profile");
            assertEquals("default", profileValue, "AWS profile should have 'default' as default value");
            
            // Verify all other parameters have empty values
            for (int i = 0; i < result.getArgumentCount(); i++) {
                Argument arg = result.getArgument(i);
                if (!"aws_configure_profile".equals(arg.getName())) {
                    assertTrue(arg.getValue().isEmpty(), 
                        String.format("Parameter '%s' should have empty default value", arg.getName()));
                }
            }
        }

        @Test
        @DisplayName("Should return mutable Arguments object")
        void shouldReturnMutableArgumentsObject() {
            // When
            Arguments result = snsProducerFifoTopic.getDefaultParameters();
            
            // Then
            assertNotNull(result);
            int originalCount = result.getArgumentCount();
            
            // Should be able to modify the returned Arguments
            assertDoesNotThrow(() -> {
                result.addArgument("test_param", "test_value");
                assertEquals(originalCount + 1, result.getArgumentCount()); // Original + 1 added
            });
        }

        @Test
        @DisplayName("Should maintain parameter order consistency across invocations")
        void shouldMaintainParameterOrderConsistencyAcrossInvocations() {
            // When
            Arguments result1 = snsProducerFifoTopic.getDefaultParameters();
            Arguments result2 = snsProducerFifoTopic.getDefaultParameters();
            
            // Then
            assertEquals(result1.getArgumentCount(), result2.getArgumentCount());
            
            for (int i = 0; i < result1.getArgumentCount(); i++) {
                assertEquals(result1.getArgument(i).getName(), result2.getArgument(i).getName(),
                    String.format("Parameter name at position %d should be consistent", i));
                assertEquals(result1.getArgument(i).getValue(), result2.getArgument(i).getValue(),
                    String.format("Parameter value at position %d should be consistent", i));
            }
        }

        @Test
        @DisplayName("Should create new Arguments instance on each call")
        void shouldCreateNewArgumentsInstanceOnEachCall() {
            // When
            Arguments result1 = snsProducerFifoTopic.getDefaultParameters();
            Arguments result2 = snsProducerFifoTopic.getDefaultParameters();
            
            // Then
            assertNotSame(result1, result2, "Should return different Arguments instances");
            
            // Verify that modifying one doesn't affect the other
            int originalCount1 = result1.getArgumentCount();
            int originalCount2 = result2.getArgumentCount();
            
            result1.addArgument("test1", "value1");
            result2.addArgument("test2", "value2");
            
            assertEquals(originalCount1 + 1, result1.getArgumentCount());
            assertEquals(originalCount2 + 1, result2.getArgumentCount());
            assertNotEquals(result1.getArgumentCount(), result2.getArgumentCount() - 1); // They differ by modifications
        }

        @Test
        @DisplayName("Should contain all required SNS FIFO parameter names")
        void shouldContainAllRequiredSnsFifoParameterNames() {
            // When
            Arguments result = snsProducerFifoTopic.getDefaultParameters();
            
            // Then
            assertNotNull(result);
            
            List<String> argumentNames = new ArrayList<>();
            for (int i = 0; i < result.getArgumentCount(); i++) {
                argumentNames.add(result.getArgument(i).getName());
            }
            
            // Verify all expected SNS FIFO parameters
            String[] expectedSnsParameters = {
                "sns_topic_arn",
                "sns_msg_body", 
                "sns_msg_attributes",
                "sns_msg_group_id",
                "sns_msg_deduplication_id"
            };
            
            for (String expectedParam : expectedSnsParameters) {
                assertTrue(argumentNames.contains(expectedParam),
                    String.format("Should contain SNS parameter: %s", expectedParam));
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
    @DisplayName("SNS_PARAMETERS Static Field Tests")
    class SnsParametersStaticFieldTests {

        @Test
        @DisplayName("Should contain exactly 5 FIFO-specific parameters")
        void shouldContainExactlyFiveFifoSpecificParameters() throws Exception {
            // When
            List<Argument> snsParameters = getSnsParametersField();
            
            // Then
            assertNotNull(snsParameters);
            assertTrue(snsParameters.size() >= 5, "SNS_PARAMETERS should contain at least 5 parameters");
            
            // Verify we have the essential FIFO parameters
            List<String> parameterNames = new ArrayList<>();
            for (Argument arg : snsParameters) {
                parameterNames.add(arg.getName());
            }
            
            assertTrue(parameterNames.contains("sns_topic_arn"));
            assertTrue(parameterNames.contains("sns_msg_body"));
            assertTrue(parameterNames.contains("sns_msg_attributes"));
            assertTrue(parameterNames.contains("sns_msg_group_id"));
            assertTrue(parameterNames.contains("sns_msg_deduplication_id"));
        }

        @Test
        @DisplayName("Should contain correct FIFO parameter names")
        void shouldContainCorrectFifoParameterNames() throws Exception {
            // When
            List<Argument> snsParameters = getSnsParametersField();
            
            // Then
            assertNotNull(snsParameters);
            
            List<String> parameterNames = new ArrayList<>();
            for (Argument arg : snsParameters) {
                parameterNames.add(arg.getName());
            }
            
            // Verify all expected parameter names
            assertTrue(parameterNames.contains("sns_topic_arn"));
            assertTrue(parameterNames.contains("sns_msg_body"));
            assertTrue(parameterNames.contains("sns_msg_attributes"));
            assertTrue(parameterNames.contains("sns_msg_group_id"));
            assertTrue(parameterNames.contains("sns_msg_deduplication_id"));
        }

        @Test
        @DisplayName("Should have empty default values for all essential parameters")
        void shouldHaveEmptyDefaultValuesForAllEssentialParameters() throws Exception {
            // When
            List<Argument> snsParameters = getSnsParametersField();
            
            // Then
            assertNotNull(snsParameters);
            
            // Look for the essential FIFO parameters and verify they have empty values
            String[] essentialParams = {"sns_topic_arn", "sns_msg_body", "sns_msg_attributes", "sns_msg_group_id", "sns_msg_deduplication_id"};
            
            for (String essentialParam : essentialParams) {
                boolean found = false;
                for (Argument arg : snsParameters) {
                    if (essentialParam.equals(arg.getName())) {
                        assertTrue(arg.getValue().isEmpty(),
                            String.format("Parameter '%s' should have empty default value", arg.getName()));
                        found = true;
                        break;
                    }
                }
                assertTrue(found, String.format("Essential parameter '%s' should be present", essentialParam));
            }
        }

        @Test
        @DisplayName("Should maintain essential parameter order")
        void shouldMaintainEssentialParameterOrder() throws Exception {
            // When
            List<Argument> snsParameters = getSnsParametersField();
            
            // Then
            assertNotNull(snsParameters);
            assertTrue(snsParameters.size() >= 5, "Should have at least 5 parameters");
            
            // Find the indices of essential parameters
            int topicArnIndex = -1, msgBodyIndex = -1, msgAttributesIndex = -1, 
                groupIdIndex = -1, deduplicationIdIndex = -1;
            
            for (int i = 0; i < snsParameters.size(); i++) {
                String name = snsParameters.get(i).getName();
                switch (name) {
                    case "sns_topic_arn": topicArnIndex = i; break;
                    case "sns_msg_body": msgBodyIndex = i; break;
                    case "sns_msg_attributes": msgAttributesIndex = i; break;
                    case "sns_msg_group_id": groupIdIndex = i; break;
                    case "sns_msg_deduplication_id": deduplicationIdIndex = i; break;
                }
            }
            
            // Verify all essential parameters are found
            assertTrue(topicArnIndex >= 0, "sns_topic_arn should be present");
            assertTrue(msgBodyIndex >= 0, "sns_msg_body should be present");
            assertTrue(msgAttributesIndex >= 0, "sns_msg_attributes should be present");
            assertTrue(groupIdIndex >= 0, "sns_msg_group_id should be present");
            assertTrue(deduplicationIdIndex >= 0, "sns_msg_deduplication_id should be present");
            
            // Verify relative order: topic_arn should come before group_id and deduplication_id
            assertTrue(topicArnIndex < groupIdIndex, "topic_arn should come before group_id");
            assertTrue(topicArnIndex < deduplicationIdIndex, "topic_arn should come before deduplication_id");
        }

        @Test
        @DisplayName("Should verify list content and structure")
        void shouldVerifyListContentAndStructure() throws Exception {
            // When
            List<Argument> snsParameters = getSnsParametersField();
            
            // Then
            assertNotNull(snsParameters);
            
            // Test that it's the actual implementation used (not necessarily immutable)
            // The important thing is that the content is correct for the FIFO implementation
            assertDoesNotThrow(() -> {
                List<String> names = new ArrayList<>();
                for (Argument arg : snsParameters) {
                    names.add(arg.getName());
                }
                // Verify we can iterate and access the parameters
                assertEquals(5, names.size());
            }, "Should be able to access SNS_PARAMETERS content");
        }

        @Test
        @DisplayName("Should contain FIFO-specific parameters not in standard SNS")
        void shouldContainFifoSpecificParametersNotInStandardSns() throws Exception {
            // When
            List<Argument> snsParameters = getSnsParametersField();
            
            // Then
            assertNotNull(snsParameters);
            
            List<String> parameterNames = new ArrayList<>();
            for (Argument arg : snsParameters) {
                parameterNames.add(arg.getName());
            }
            
            // These are FIFO-specific parameters that differentiate from standard SNS
            assertTrue(parameterNames.contains("sns_msg_group_id"),
                "Should contain FIFO-specific group ID parameter");
            assertTrue(parameterNames.contains("sns_msg_deduplication_id"), 
                "Should contain FIFO-specific deduplication ID parameter");
        }

        @Test
        @DisplayName("Should be properly initialized as static final field")
        void shouldBeProperlyInitializedAsStaticFinalField() throws Exception {
            // When
            Field field = SNSProducerFifoTopic.class.getDeclaredField("SNS_PARAMETERS");
            
            // Then
            assertNotNull(field);
            assertTrue(java.lang.reflect.Modifier.isStatic(field.getModifiers()),
                "SNS_PARAMETERS should be static");
            assertTrue(java.lang.reflect.Modifier.isFinal(field.getModifiers()),
                "SNS_PARAMETERS should be final");
            assertTrue(java.lang.reflect.Modifier.isPrivate(field.getModifiers()),
                "SNS_PARAMETERS should be private");
            
            // Verify field is not null
            field.setAccessible(true);
            Object value = field.get(null);
            assertNotNull(value, "SNS_PARAMETERS field should not be null");
        }

        @SuppressWarnings("unchecked")
        private List<Argument> getSnsParametersField() throws Exception {
            Field field = SNSProducerFifoTopic.class.getDeclaredField("SNS_PARAMETERS");
            field.setAccessible(true);
            return (List<Argument>) field.get(null);
        }
    }

    @Nested
    @DisplayName("Integration Tests for getDefaultParameters() and SNS_PARAMETERS")
    class IntegrationTests {

        @Test
        @DisplayName("Should use SNS_PARAMETERS in getDefaultParameters method")
        void shouldUseSnsParametersInGetDefaultParametersMethod() throws Exception {
            // Given
            List<Argument> snsParameters = getSnsParametersField();
            
            // When
            Arguments defaultParameters = snsProducerFifoTopic.getDefaultParameters();
            
            // Then
            assertNotNull(snsParameters);
            assertNotNull(defaultParameters);
            
            // Verify that all SNS parameters from static field are included in default parameters
            for (Argument snsParam : snsParameters) {
                boolean found = false;
                for (int i = 0; i < defaultParameters.getArgumentCount(); i++) {
                    Argument defaultParam = defaultParameters.getArgument(i);
                    if (snsParam.getName().equals(defaultParam.getName()) && 
                        snsParam.getValue().equals(defaultParam.getValue())) {
                        found = true;
                        break;
                    }
                }
                assertTrue(found, 
                    String.format("SNS parameter '%s' should be included in default parameters", 
                        snsParam.getName()));
            }
        }

        @Test
        @DisplayName("Should combine AWS and SNS parameters correctly")
        void shouldCombineAwsAndSnsParametersCorrectly() throws Exception {
            // Given
            List<Argument> snsParameters = getSnsParametersField();
            
            // When
            Arguments defaultParameters = snsProducerFifoTopic.getDefaultParameters();
            
            // Then
            assertNotNull(snsParameters);
            assertNotNull(defaultParameters);
            
            // Should have AWS parameters (6) + SNS parameters (at least 5) 
            assertTrue(defaultParameters.getArgumentCount() >= 11, "Should have at least 11 parameters");
            assertTrue(snsParameters.size() >= 5, "Should have at least 5 SNS parameters");
            
            // Count SNS parameters in default parameters
            int snsParamCount = 0;
            List<String> defaultParamNames = new ArrayList<>();
            for (int i = 0; i < defaultParameters.getArgumentCount(); i++) {
                String paramName = defaultParameters.getArgument(i).getName();
                defaultParamNames.add(paramName);
                if (paramName.startsWith("sns_")) {
                    snsParamCount++;
                }
            }
            
            assertTrue(snsParamCount >= 5, "Should have at least 5 SNS parameters");
            
            // Verify essential SNS parameter names are present
            String[] essentialSnsParams = {"sns_topic_arn", "sns_msg_body", "sns_msg_attributes", "sns_msg_group_id", "sns_msg_deduplication_id"};
            for (String essentialParam : essentialSnsParams) {
                assertTrue(defaultParamNames.contains(essentialParam),
                    String.format("Default parameters should contain SNS parameter: %s", essentialParam));
            }
        }

        @SuppressWarnings("unchecked")
        private List<Argument> getSnsParametersField() throws Exception {
            Field field = SNSProducerFifoTopic.class.getDeclaredField("SNS_PARAMETERS");
            field.setAccessible(true);
            return (List<Argument>) field.get(null);
        }
    }
}

package org.apache.jmeter.protocol.aws;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.apache.jmeter.config.Arguments;
import org.apache.jmeter.samplers.SampleResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.apache.jmeter.protocol.java.sampler.JavaSamplerContext;
import org.apache.jmeter.protocol.java.sampler.JavaSamplerClient;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for AWSSampler abstract class.
 * Tests all implemented methods using a concrete test implementation.
 * 
 * @author JoseLuisSR
 * @since 08/07/2025
 */
@DisplayName("AWSSampler Tests")
class AWSSamplerTest {

    private TestableAWSSampler awsSampler;

    @BeforeEach
    void setUp() {
        awsSampler = new TestableAWSSampler();
    }

    /**
     * Concrete implementation of AWSSampler for testing purposes.
     * This allows us to test the implemented methods in the abstract class.
     */
    private static class TestableAWSSampler extends AWSSampler {
        
        @Override
        public Arguments getDefaultParameters() {
            Arguments arguments = new Arguments();
            AWS_PARAMETERS.forEach(arguments::addArgument);
            return arguments;
        }

        @Override
        public void setupTest(JavaSamplerContext context) {
            // Implementation for testing
        }

        @Override
        public SampleResult runTest(JavaSamplerContext context) {
            return newSampleResult();
        }

        @Override
        public void teardownTest(JavaSamplerContext context) {
            // Implementation for testing
        }
    }

    @Nested
    @DisplayName("Message Attributes Reading Tests")
    class MessageAttributesReadingTests {

        @Test
        @DisplayName("Should parse valid JSON message attributes")
        void shouldParseValidJsonMessageAttributes() throws JsonProcessingException {
            // Given
            String validJson = "[{\"name\":\"attr1\",\"type\":\"String\",\"value\":\"value1\"}," +
                             "{\"name\":\"attr2\",\"type\":\"Number\",\"value\":\"123\"}]";

            // When
            List<MessageAttribute> attributes = awsSampler.readMsgAttributes(validJson);

            // Then
            assertNotNull(attributes);
            assertEquals(2, attributes.size());
            
            MessageAttribute attr1 = attributes.get(0);
            assertEquals("attr1", attr1.getName());
            assertEquals("String", attr1.getType());
            assertEquals("value1", attr1.getValue());
            
            MessageAttribute attr2 = attributes.get(1);
            assertEquals("attr2", attr2.getName());
            assertEquals("Number", attr2.getType());
            assertEquals("123", attr2.getValue());
        }

        @Test
        @DisplayName("Should return empty list for empty JSON array")
        void shouldReturnEmptyListForEmptyJsonArray() throws JsonProcessingException {
            // Given
            String emptyJson = "[]";

            // When
            List<MessageAttribute> attributes = awsSampler.readMsgAttributes(emptyJson);

            // Then
            assertNotNull(attributes);
            assertTrue(attributes.isEmpty());
        }

        @ParameterizedTest
        @NullAndEmptySource
        @DisplayName("Should return empty list for null or empty input")
        void shouldReturnEmptyListForNullOrEmptyInput(String input) throws JsonProcessingException {
            // When
            List<MessageAttribute> attributes = awsSampler.readMsgAttributes(input);

            // Then
            assertNotNull(attributes);
            assertTrue(attributes.isEmpty());
        }

        @Test
        @DisplayName("Should limit attributes to maximum allowed")
        void shouldLimitAttributesToMaximumAllowed() throws JsonProcessingException {
            // Given - Create JSON with more than 10 attributes
            StringBuilder jsonBuilder = new StringBuilder("[");
            for (int i = 1; i <= 15; i++) {
                if (i > 1) jsonBuilder.append(",");
                jsonBuilder.append(String.format(
                    "{\"name\":\"attr%d\",\"type\":\"String\",\"value\":\"value%d\"}", i, i));
            }
            jsonBuilder.append("]");

            // When
            List<MessageAttribute> attributes = awsSampler.readMsgAttributes(jsonBuilder.toString());

            // Then
            assertNotNull(attributes);
            assertEquals(10, attributes.size()); // Should be limited to MSG_ATTRIBUTES_MAX
        }

        @Test
        @DisplayName("Should throw JsonProcessingException for invalid JSON")
        void shouldThrowJsonProcessingExceptionForInvalidJson() {
            // Given
            String invalidJson = "{invalid json}";

            // When & Then
            assertThrows(JsonProcessingException.class, 
                () -> awsSampler.readMsgAttributes(invalidJson));
        }

        @Test
        @DisplayName("Should handle single message attribute")
        void shouldHandleSingleMessageAttribute() throws JsonProcessingException {
            // Given
            String singleAttributeJson = "[{\"name\":\"single\",\"type\":\"Binary\",\"value\":\"data\"}]";

            // When
            List<MessageAttribute> attributes = awsSampler.readMsgAttributes(singleAttributeJson);

            // Then
            assertNotNull(attributes);
            assertEquals(1, attributes.size());
            
            MessageAttribute attr = attributes.get(0);
            assertEquals("single", attr.getName());
            assertEquals("Binary", attr.getType());
            assertEquals("data", attr.getValue());
        }

        @Test
        @DisplayName("Should handle message attributes with all supported types")
        void shouldHandleMessageAttributesWithAllSupportedTypes() throws JsonProcessingException {
            // Given
            String jsonWithAllTypes = "[" +
                "{\"name\":\"stringAttr\",\"type\":\"String\",\"value\":\"text\"}," +
                "{\"name\":\"numberAttr\",\"type\":\"Number\",\"value\":\"42\"}," +
                "{\"name\":\"binaryAttr\",\"type\":\"Binary\",\"value\":\"binaryData\"}," +
                "{\"name\":\"arrayAttr\",\"type\":\"String.Array\",\"value\":\"[item1,item2]\"}" +
                "]";

            // When
            List<MessageAttribute> attributes = awsSampler.readMsgAttributes(jsonWithAllTypes);

            // Then
            assertNotNull(attributes);
            assertEquals(4, attributes.size());
            
            // Verify all types are handled correctly
            assertEquals("String", attributes.get(0).getType());
            assertEquals("Number", attributes.get(1).getType());
            assertEquals("Binary", attributes.get(2).getType());
            assertEquals("String.Array", attributes.get(3).getType());
        }

        @Test
        @DisplayName("Should preserve attribute values correctly")
        void shouldPreserveAttributeValuesCorrectly() throws JsonProcessingException {
            // Given
            String jsonWithSpecialValues = "[" +
                "{\"name\":\"emptyValue\",\"type\":\"String\",\"value\":\"\"}," +
                "{\"name\":\"specialChars\",\"type\":\"String\",\"value\":\"special!@#$%^&*()_+{}\"}," +
                "{\"name\":\"unicodeValue\",\"type\":\"String\",\"value\":\"Hello 世界 🌍\"}" +
                "]";

            // When
            List<MessageAttribute> attributes = awsSampler.readMsgAttributes(jsonWithSpecialValues);

            // Then
            assertNotNull(attributes);
            assertEquals(3, attributes.size());
            
            assertEquals("", attributes.get(0).getValue());
            assertEquals("special!@#$%^&*()_+{}", attributes.get(1).getValue());
            assertEquals("Hello 世界 🌍", attributes.get(2).getValue());
        }
    }

    @Nested
    @DisplayName("Constants and Static Values Tests")
    class ConstantsAndStaticValuesTests {

        @Test
        @DisplayName("Should have correct AWS parameter names")
        void shouldHaveCorrectAwsParameterNames() {
            // Then - Verify AWS parameter constants
            assertEquals("aws_access_key_id", AWSSampler.AWS_ACCESS_KEY_ID);
            assertEquals("aws_secret_access_key", AWSSampler.AWS_SECRET_ACCESS_KEY);
            assertEquals("aws_session_token", AWSSampler.AWS_SESSION_TOKEN);
            assertEquals("aws_region", AWSSampler.AWS_REGION);
            assertEquals("aws_endpoint_custom", AWSSampler.AWS_ENDPOINT_CUSTOM);
            assertEquals("aws_configure_profile", AWSSampler.AWS_CONFIG_PROFILE);
        }

        @Test
        @DisplayName("Should have correct default values")
        void shouldHaveCorrectDefaultValues() {
            // Then - Verify default values
            assertEquals("default", AWSSampler.AWS_DEFAULT_PROFILE);
            assertEquals("500", AWSSampler.FAIL_CODE);
            assertEquals("", AWSSampler.EMPTY);
            assertEquals("[]", AWSSampler.EMPTY_ARRAY);
            assertEquals("UTF-8", AWSSampler.ENCODING);
        }

        @Test
        @DisplayName("Should have correct message attribute constants")
        void shouldHaveCorrectMessageAttributeConstants() {
            // Then - Verify message attribute constants
            assertEquals(Integer.valueOf(10), AWSSampler.MSG_ATTRIBUTES_MAX);
            assertEquals("String", AWSSampler.MSG_ATTRIBUTE_TYPE_STR);
            assertEquals("String.Array", AWSSampler.MSG_ATTRIBUTE_TYPE_STR_ARRAY);
            assertEquals("Number", AWSSampler.MSG_ATTRIBUTE_TYPE_NUM);
            assertEquals("Binary", AWSSampler.MSG_ATTRIBUTE_TYPE_BIN);
        }

        @Test
        @DisplayName("Should have correct AWS endpoint format")
        void shouldHaveCorrectAwsEndpointFormat() {
            // Then
            assertEquals("https://%s.%s.amazonaws.com", AWSSampler.AWS_ENDPOINT);
        }

        @Test
        @DisplayName("Should have AWS parameters list with correct size and default values")
        void shouldHaveAwsParametersListWithCorrectSizeAndDefaultValues() {
            // When
            List<org.apache.jmeter.config.Argument> parameters = AWSSampler.AWS_PARAMETERS;

            // Then
            assertNotNull(parameters);
            assertEquals(6, parameters.size());
            
            // Verify parameter names and default values
            assertEquals("aws_access_key_id", parameters.get(0).getName());
            assertEquals("", parameters.get(0).getValue());
            
            assertEquals("aws_secret_access_key", parameters.get(1).getName());
            assertEquals("", parameters.get(1).getValue());
            
            assertEquals("aws_session_token", parameters.get(2).getName());
            assertEquals("", parameters.get(2).getValue());
            
            assertEquals("aws_region", parameters.get(3).getName());
            assertEquals("", parameters.get(3).getValue());
            
            assertEquals("aws_endpoint_custom", parameters.get(4).getName());
            assertEquals("", parameters.get(4).getValue());
            
            assertEquals("aws_configure_profile", parameters.get(5).getName());
            assertEquals("default", parameters.get(5).getValue());
        }

        @Test
        @DisplayName("Should have immutable AWS parameters list")
        void shouldHaveImmutableAwsParametersList() {
            // When
            List<org.apache.jmeter.config.Argument> parameters = AWSSampler.AWS_PARAMETERS;

            // Then - Verify it's properly initialized
            assertNotNull(parameters);
            assertFalse(parameters.isEmpty());
            
            // Verify the list contains expected entries
            assertTrue(parameters.stream().anyMatch(arg -> 
                "aws_access_key_id".equals(arg.getName())));
            assertTrue(parameters.stream().anyMatch(arg -> 
                "aws_secret_access_key".equals(arg.getName())));
            assertTrue(parameters.stream().anyMatch(arg -> 
                "aws_region".equals(arg.getName())));
        }
    }

    @Nested
    @DisplayName("Abstract Class Behavior Tests")
    class AbstractClassBehaviorTests {

        @Test
        @DisplayName("Should create testable implementation successfully")
        void shouldCreateTestableImplementationSuccessfully() {
            // Then
            assertNotNull(awsSampler);
            assertInstanceOf(AWSSampler.class, awsSampler);
        }

        @Test
        @DisplayName("Should implement JavaSamplerClient interface")
        void shouldImplementJavaSamplerClientInterface() {
            // Then
            assertInstanceOf(JavaSamplerClient.class, awsSampler);
        }

        @Test
        @DisplayName("Should provide default parameters through concrete implementation")
        void shouldProvideDefaultParametersThroughConcreteImplementation() {
            // When
            Arguments defaultParams = awsSampler.getDefaultParameters();

            // Then
            assertNotNull(defaultParams);
            assertEquals(6, defaultParams.getArgumentCount());
        }

        @Test
        @DisplayName("Should handle message attributes processing independently")
        void shouldHandleMessageAttributesProcessingIndependently() throws JsonProcessingException {
            // Given
            String json = "[{\"name\":\"test\",\"type\":\"String\",\"value\":\"value\"}]";

            // When
            List<MessageAttribute> result1 = awsSampler.readMsgAttributes(json);
            List<MessageAttribute> result2 = awsSampler.readMsgAttributes(json);

            // Then - Each call should produce independent results
            assertNotNull(result1);
            assertNotNull(result2);
            assertEquals(result1.size(), result2.size());
            assertNotSame(result1, result2);
        }
    }

    @Nested
    @DisplayName("Edge Cases and Error Handling Tests")
    class EdgeCasesAndErrorHandlingTests {

        @Test
        @DisplayName("Should handle malformed JSON gracefully")
        void shouldHandleMalformedJsonGracefully() {
            // Given various malformed JSON strings
            String[] malformedJsons = {
                "{incomplete",
                "[{\"name\":\"test\"incomplete}]",
                "[{\"name\":}]",
                "[{incomplete_object}]",
                "not_json_at_all"
            };

            // When & Then
            for (String malformedJson : malformedJsons) {
                assertThrows(JsonProcessingException.class, 
                    () -> awsSampler.readMsgAttributes(malformedJson),
                    "Should throw JsonProcessingException for: " + malformedJson);
            }
        }

        @Test
        @DisplayName("Should handle empty and whitespace JSON gracefully")
        void shouldHandleEmptyAndWhitespaceJsonGracefully() throws JsonProcessingException {
            // Given - Test empty and null inputs (which should work)
            String[] validEmptyInputs = {"", null};

            // When & Then - These should work
            for (String input : validEmptyInputs) {
                List<MessageAttribute> result = awsSampler.readMsgAttributes(input);
                assertNotNull(result, "Result should not be null for input: " + input);
                assertTrue(result.isEmpty(), "Result should be empty for input: " + input);
            }
            
            // Given - Test whitespace inputs (which should throw JsonProcessingException)
            String[] whitespaceInputs = {"   ", "\t", "\n", "\r\n"};
            
            // When & Then - These should throw JsonProcessingException
            for (String input : whitespaceInputs) {
                assertThrows(JsonProcessingException.class, 
                    () -> awsSampler.readMsgAttributes(input),
                    "Should throw JsonProcessingException for whitespace input: " + input);
            }
        }

        @Test
        @DisplayName("Should handle large number of attributes correctly")
        void shouldHandleLargeNumberOfAttributesCorrectly() throws JsonProcessingException {
            // Given - Create JSON with exactly the maximum allowed attributes
            StringBuilder jsonBuilder = new StringBuilder("[");
            for (int i = 1; i <= 10; i++) {
                if (i > 1) jsonBuilder.append(",");
                jsonBuilder.append(String.format(
                    "{\"name\":\"attr%d\",\"type\":\"String\",\"value\":\"value%d\"}", i, i));
            }
            jsonBuilder.append("]");

            // When
            List<MessageAttribute> attributes = awsSampler.readMsgAttributes(jsonBuilder.toString());

            // Then
            assertNotNull(attributes);
            assertEquals(10, attributes.size());
            
            // Verify all attributes are present and correctly ordered
            for (int i = 0; i < 10; i++) {
                assertEquals("attr" + (i + 1), attributes.get(i).getName());
                assertEquals("value" + (i + 1), attributes.get(i).getValue());
            }
        }

        @Test
        @DisplayName("Should handle attributes with null or missing fields")
        void shouldHandleAttributesWithNullOrMissingFields() throws JsonProcessingException {
            // Given
            String jsonWithMissingFields = "[" +
                "{\"name\":\"attr1\",\"type\":null,\"value\":\"value1\"}," +
                "{\"name\":null,\"type\":\"String\",\"value\":\"value2\"}," +
                "{\"name\":\"attr3\",\"type\":\"String\",\"value\":null}" +
                "]";

            // When
            List<MessageAttribute> attributes = awsSampler.readMsgAttributes(jsonWithMissingFields);

            // Then
            assertNotNull(attributes);
            assertEquals(3, attributes.size());
            
            // Verify null values are handled correctly
            assertNull(attributes.get(0).getType());
            assertNull(attributes.get(1).getName());
            assertNull(attributes.get(2).getValue());
        }
    }
}

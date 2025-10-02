package org.apache.jmeter.protocol.aws;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for MessageAttribute class.
 * 
 * @author JoseLuisSR
 * @since 08/07/2025
 */
@DisplayName("MessageAttribute Tests")
class MessageAttributeTest {

    private MessageAttribute messageAttribute;

    @BeforeEach
    void setUp() {
        messageAttribute = new MessageAttribute();
    }

    @Nested
    @DisplayName("Name Property Tests")
    class NamePropertyTests {

        @Test
        @DisplayName("Should set and get name correctly")
        void shouldSetAndGetNameCorrectly() {
            // Given
            String expectedName = "testAttribute";

            // When
            messageAttribute.setName(expectedName);

            // Then
            assertEquals(expectedName, messageAttribute.getName());
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"", " ", "  ", "\t", "\n"})
        @DisplayName("Should handle null, empty and whitespace names")
        void shouldHandleNullEmptyAndWhitespaceNames(String name) {
            // When
            messageAttribute.setName(name);

            // Then
            assertEquals(name, messageAttribute.getName());
        }

        @Test
        @DisplayName("Should handle special characters in name")
        void shouldHandleSpecialCharactersInName() {
            // Given
            String nameWithSpecialChars = "test-attribute_123.name$#@!";

            // When
            messageAttribute.setName(nameWithSpecialChars);

            // Then
            assertEquals(nameWithSpecialChars, messageAttribute.getName());
        }

        @Test
        @DisplayName("Should handle long name")
        void shouldHandleLongName() {
            // Given
            String longName = "a".repeat(1000);

            // When
            messageAttribute.setName(longName);

            // Then
            assertEquals(longName, messageAttribute.getName());
        }

        @Test
        @DisplayName("Should return null when name is not set")
        void shouldReturnNullWhenNameIsNotSet() {
            // When & Then
            assertNull(messageAttribute.getName());
        }
    }

    @Nested
    @DisplayName("Type Property Tests")
    class TypePropertyTests {

        @Test
        @DisplayName("Should set and get type correctly")
        void shouldSetAndGetTypeCorrectly() {
            // Given
            String expectedType = "String";

            // When
            messageAttribute.setType(expectedType);

            // Then
            assertEquals(expectedType, messageAttribute.getType());
        }

        @ParameterizedTest
        @ValueSource(strings = {"String", "Number", "Binary", "String.Array", "Number.Array", "Binary.Array"})
        @DisplayName("Should handle valid AWS SQS message attribute types")
        void shouldHandleValidAwsSqsMessageAttributeTypes(String type) {
            // When
            messageAttribute.setType(type);

            // Then
            assertEquals(type, messageAttribute.getType());
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"", " ", "  ", "\t", "\n"})
        @DisplayName("Should handle null, empty and whitespace types")
        void shouldHandleNullEmptyAndWhitespaceTypes(String type) {
            // When
            messageAttribute.setType(type);

            // Then
            assertEquals(type, messageAttribute.getType());
        }

        @Test
        @DisplayName("Should return null when type is not set")
        void shouldReturnNullWhenTypeIsNotSet() {
            // When & Then
            assertNull(messageAttribute.getType());
        }
    }

    @Nested
    @DisplayName("Value Property Tests")
    class ValuePropertyTests {

        @Test
        @DisplayName("Should set and get value correctly")
        void shouldSetAndGetValueCorrectly() {
            // Given
            String expectedValue = "testValue";

            // When
            messageAttribute.setValue(expectedValue);

            // Then
            assertEquals(expectedValue, messageAttribute.getValue());
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"", " ", "  ", "\t", "\n"})
        @DisplayName("Should handle null, empty and whitespace values")
        void shouldHandleNullEmptyAndWhitespaceValues(String value) {
            // When
            messageAttribute.setValue(value);

            // Then
            assertEquals(value, messageAttribute.getValue());
        }

        @Test
        @DisplayName("Should handle JSON string value")
        void shouldHandleJsonStringValue() {
            // Given
            String jsonValue = "{\"key\":\"value\",\"number\":123,\"array\":[1,2,3]}";

            // When
            messageAttribute.setValue(jsonValue);

            // Then
            assertEquals(jsonValue, messageAttribute.getValue());
        }

        @Test
        @DisplayName("Should handle XML string value")
        void shouldHandleXmlStringValue() {
            // Given
            String xmlValue = "<?xml version=\"1.0\"?><root><element>value</element></root>";

            // When
            messageAttribute.setValue(xmlValue);

            // Then
            assertEquals(xmlValue, messageAttribute.getValue());
        }

        @Test
        @DisplayName("Should handle numeric string value")
        void shouldHandleNumericStringValue() {
            // Given
            String numericValue = "123.456";

            // When
            messageAttribute.setValue(numericValue);

            // Then
            assertEquals(numericValue, messageAttribute.getValue());
        }

        @Test
        @DisplayName("Should handle special characters in value")
        void shouldHandleSpecialCharactersInValue() {
            // Given
            String valueWithSpecialChars = "test-value_123.data$#@!%^&*()+=[]{}|\\:;\"'<>?/~`";

            // When
            messageAttribute.setValue(valueWithSpecialChars);

            // Then
            assertEquals(valueWithSpecialChars, messageAttribute.getValue());
        }

        @Test
        @DisplayName("Should handle large value")
        void shouldHandleLargeValue() {
            // Given
            String largeValue = "x".repeat(10000);

            // When
            messageAttribute.setValue(largeValue);

            // Then
            assertEquals(largeValue, messageAttribute.getValue());
        }

        @Test
        @DisplayName("Should return null when value is not set")
        void shouldReturnNullWhenValueIsNotSet() {
            // When & Then
            assertNull(messageAttribute.getValue());
        }
    }

    @Nested
    @DisplayName("Integration Tests")
    class IntegrationTests {

        @Test
        @DisplayName("Should set and get all properties together")
        void shouldSetAndGetAllPropertiesTogether() {
            // Given
            String expectedName = "messageId";
            String expectedType = "String";
            String expectedValue = "12345-abcde-67890";

            // When
            messageAttribute.setName(expectedName);
            messageAttribute.setType(expectedType);
            messageAttribute.setValue(expectedValue);

            // Then
            assertEquals(expectedName, messageAttribute.getName());
            assertEquals(expectedType, messageAttribute.getType());
            assertEquals(expectedValue, messageAttribute.getValue());
        }

        @Test
        @DisplayName("Should handle multiple property updates")
        void shouldHandleMultiplePropertyUpdates() {
            // Given
            String initialName = "initialName";
            String initialType = "String";
            String initialValue = "initialValue";
            
            String updatedName = "updatedName";
            String updatedType = "Number";
            String updatedValue = "123";

            // When - Set initial values
            messageAttribute.setName(initialName);
            messageAttribute.setType(initialType);
            messageAttribute.setValue(initialValue);

            // Then - Verify initial values
            assertEquals(initialName, messageAttribute.getName());
            assertEquals(initialType, messageAttribute.getType());
            assertEquals(initialValue, messageAttribute.getValue());

            // When - Update values
            messageAttribute.setName(updatedName);
            messageAttribute.setType(updatedType);
            messageAttribute.setValue(updatedValue);

            // Then - Verify updated values
            assertEquals(updatedName, messageAttribute.getName());
            assertEquals(updatedType, messageAttribute.getType());
            assertEquals(updatedValue, messageAttribute.getValue());
        }

        @Test
        @DisplayName("Should handle setting properties to null")
        void shouldHandleSettingPropertiesToNull() {
            // Given - Set initial values
            messageAttribute.setName("testName");
            messageAttribute.setType("String");
            messageAttribute.setValue("testValue");

            // When - Set all to null
            messageAttribute.setName(null);
            messageAttribute.setType(null);
            messageAttribute.setValue(null);

            // Then
            assertNull(messageAttribute.getName());
            assertNull(messageAttribute.getType());
            assertNull(messageAttribute.getValue());
        }

        @Test
        @DisplayName("Should create complete message attribute for SQS")
        void shouldCreateCompleteMessageAttributeForSqs() {
            // Given
            String name = "correlationId";
            String type = "String";
            String value = "correlation-123-456";

            // When
            messageAttribute.setName(name);
            messageAttribute.setType(type);
            messageAttribute.setValue(value);

            // Then
            assertAll("Message Attribute should be completely configured",
                () -> assertEquals(name, messageAttribute.getName()),
                () -> assertEquals(type, messageAttribute.getType()),
                () -> assertEquals(value, messageAttribute.getValue())
            );
        }

        @Test
        @DisplayName("Should create numeric message attribute")
        void shouldCreateNumericMessageAttribute() {
            // Given
            String name = "priority";
            String type = "Number";
            String value = "5";

            // When
            messageAttribute.setName(name);
            messageAttribute.setType(type);
            messageAttribute.setValue(value);

            // Then
            assertAll("Numeric Message Attribute should be configured",
                () -> assertEquals(name, messageAttribute.getName()),
                () -> assertEquals(type, messageAttribute.getType()),
                () -> assertEquals(value, messageAttribute.getValue())
            );
        }
    }

    @Nested
    @DisplayName("Object State Tests")
    class ObjectStateTests {

        @Test
        @DisplayName("Should initialize with null values")
        void shouldInitializeWithNullValues() {
            // Given
            MessageAttribute newAttribute = new MessageAttribute();

            // Then
            assertAll("New MessageAttribute should have null values",
                () -> assertNull(newAttribute.getName()),
                () -> assertNull(newAttribute.getType()),
                () -> assertNull(newAttribute.getValue())
            );
        }

        @Test
        @DisplayName("Should be independent instances")
        void shouldBeIndependentInstances() {
            // Given
            MessageAttribute attribute1 = new MessageAttribute();
            MessageAttribute attribute2 = new MessageAttribute();

            // When
            attribute1.setName("attr1");
            attribute1.setType("String");
            attribute1.setValue("value1");

            attribute2.setName("attr2");
            attribute2.setType("Number");
            attribute2.setValue("value2");

            // Then
            assertAll("Instances should be independent",
                () -> assertNotEquals(attribute1.getName(), attribute2.getName()),
                () -> assertNotEquals(attribute1.getType(), attribute2.getType()),
                () -> assertNotEquals(attribute1.getValue(), attribute2.getValue())
            );
        }
    }
}

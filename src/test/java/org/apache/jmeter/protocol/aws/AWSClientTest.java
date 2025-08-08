package org.apache.jmeter.protocol.aws;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for AWSClient interface default methods.
 * 
 * @author JoseLuisSR
 * @since 08/07/2025
 */
@DisplayName("AWSClient Tests")
class AWSClientTest {

    private AWSClient awsClient;
    private Map<String, String> credentials;

    /**
     * Test implementation of AWSClient interface for testing purposes.
     */
    private static class TestAWSClient implements AWSClient {
        // No additional implementation needed for testing default methods
    }

    @BeforeEach
    void setUp() {
        awsClient = new TestAWSClient();
        credentials = new HashMap<>();
    }

    @Nested
    @DisplayName("AWS Region Tests")
    class AWSRegionTests {

        @Test
        @DisplayName("Should return region from credentials when provided")
        void shouldReturnRegionFromCredentialsWhenProvided() {
            // Given
            credentials.put(AWSSampler.AWS_REGION, "us-west-2");

            // When
            String result = awsClient.getAWSRegion(credentials);

            // Then
            assertEquals("us-west-2", result);
        }
    }

    @Nested
    @DisplayName("Profile Tests")
    class ProfileTests {

        @ParameterizedTest
        @ValueSource(strings = {"", "default"})
        @DisplayName("Should return false for default or empty profile")
        void shouldReturnFalseForDefaultOrEmptyProfile(String profileValue) {
            // Given
            credentials.put(AWSSampler.AWS_CONFIG_PROFILE, profileValue);

            // When
            boolean result = awsClient.hasSpecificProfile(credentials);

            // Then
            assertFalse(result);
        }

        @ParameterizedTest
        @ValueSource(strings = {"production", "staging", "development", "custom-profile"})
        @DisplayName("Should return true for specific profile names")
        void shouldReturnTrueForSpecificProfileNames(String profileValue) {
            // Given
            credentials.put(AWSSampler.AWS_CONFIG_PROFILE, profileValue);

            // When
            boolean result = awsClient.hasSpecificProfile(credentials);

            // Then
            assertTrue(result);
        }

        @Test
        @DisplayName("Should return false when profile is null")
        void shouldReturnFalseWhenProfileIsNull() {
            // Given - no profile in credentials

            // When
            boolean result = awsClient.hasSpecificProfile(credentials);

            // Then
            assertFalse(result);
        }
    }

    @Nested
    @DisplayName("Explicit Credentials Tests")
    class ExplicitCredentialsTests {

        @Test
        @DisplayName("Should return true when both access key and secret key are present")
        void shouldReturnTrueWhenBothKeysArePresent() {
            // Given
            credentials.put(AWSSampler.AWS_ACCESS_KEY_ID, "AKIAIOSFODNN7EXAMPLE");
            credentials.put(AWSSampler.AWS_SECRET_ACCESS_KEY, "wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY");

            // When
            boolean result = awsClient.hasExplicitCredentials(credentials);

            // Then
            assertTrue(result);
        }

        @ParameterizedTest
        @MethodSource("provideIncompleteCredentials")
        @DisplayName("Should return false when credentials are incomplete")
        void shouldReturnFalseWhenCredentialsAreIncomplete(String accessKey, String secretKey) {
            // Given
            if (accessKey != null) {
                credentials.put(AWSSampler.AWS_ACCESS_KEY_ID, accessKey);
            }
            if (secretKey != null) {
                credentials.put(AWSSampler.AWS_SECRET_ACCESS_KEY, secretKey);
            }

            // When
            boolean result = awsClient.hasExplicitCredentials(credentials);

            // Then
            assertFalse(result);
        }

        private static Stream<Arguments> provideIncompleteCredentials() {
            return Stream.of(
                Arguments.of(null, null),
                Arguments.of("", ""),
                Arguments.of("AKIAIOSFODNN7EXAMPLE", ""),
                Arguments.of("", "wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY"),
                Arguments.of("AKIAIOSFODNN7EXAMPLE", null),
                Arguments.of(null, "wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY")
            );
        }
    }

    @Nested
    @DisplayName("Parameter Presence Tests")
    class ParameterPresenceTests {

        @Test
        @DisplayName("Should return true when parameter is present and not empty")
        void shouldReturnTrueWhenParameterIsPresentAndNotEmpty() {
            // Given
            credentials.put("test-key", "test-value");

            // When
            boolean result = awsClient.isParameterPresent(credentials, "test-key");

            // Then
            assertTrue(result);
        }

        @Test
        @DisplayName("Should return false when parameter is empty string")
        void shouldReturnFalseWhenParameterIsEmptyString() {
            // Given
            credentials.put("test-key", "");

            // When
            boolean result = awsClient.isParameterPresent(credentials, "test-key");

            // Then
            assertFalse(result);
        }

        @Test
        @DisplayName("Should return false when parameter is null")
        void shouldReturnFalseWhenParameterIsNull() {
            // Given
            credentials.put("test-key", null);

            // When
            boolean result = awsClient.isParameterPresent(credentials, "test-key");

            // Then
            assertFalse(result);
        }

        @Test
        @DisplayName("Should return false when parameter key doesn't exist")
        void shouldReturnFalseWhenParameterKeyDoesntExist() {
            // Given - empty credentials

            // When
            boolean result = awsClient.isParameterPresent(credentials, "non-existent-key");

            // Then
            assertFalse(result);
        }

        @Test
        @DisplayName("Should handle whitespace in parameter values correctly")
        void shouldHandleWhitespaceInParameterValuesCorrectly() {
            // Given
            credentials.put("whitespace-key", "   ");

            // When
            boolean result = awsClient.isParameterPresent(credentials, "whitespace-key");

            // Then
            // The method uses Predicate.not(String::isEmpty) which only checks for empty string, not whitespace
            assertTrue(result, "Parameter with whitespace should be considered present");
        }
    }

    @Nested
    @DisplayName("AWS Endpoint Tests")
    class AWSEndpointTests {

        @Test
        @DisplayName("Should return custom endpoint when provided")
        void shouldReturnCustomEndpointWhenProvided() {
            // Given
            credentials.put(AWSSampler.AWS_ENDPOINT_CUSTOM, "https://custom-endpoint.example.com");

            // When
            String result = awsClient.getAWSEndpoint(credentials, "s3", "us-east-1");

            // Then
            assertEquals("https://custom-endpoint.example.com", result);
        }

        @Test
        @DisplayName("Should return default endpoint when custom endpoint is not provided")
        void shouldReturnDefaultEndpointWhenCustomEndpointIsNotProvided() {
            // Given - no custom endpoint

            // When
            String result = awsClient.getAWSEndpoint(credentials, "s3", "us-east-1");

            // Then
            assertEquals("https://s3.us-east-1.amazonaws.com", result);
        }

        @Test
        @DisplayName("Should return default endpoint when custom endpoint is empty")
        void shouldReturnDefaultEndpointWhenCustomEndpointIsEmpty() {
            // Given
            credentials.put(AWSSampler.AWS_ENDPOINT_CUSTOM, "");

            // When
            String result = awsClient.getAWSEndpoint(credentials, "dynamodb", "eu-west-1");

            // Then
            assertEquals("https://dynamodb.eu-west-1.amazonaws.com", result);
        }

        @ParameterizedTest
        @MethodSource("provideServiceAndRegionCombinations")
        @DisplayName("Should build correct default endpoint for different services and regions")
        void shouldBuildCorrectDefaultEndpointForDifferentServicesAndRegions(String service, String region, String expected) {
            // Given - no custom endpoint

            // When
            String result = awsClient.getAWSEndpoint(credentials, service, region);

            // Then
            assertEquals(expected, result);
        }

        private static Stream<Arguments> provideServiceAndRegionCombinations() {
            return Stream.of(
                Arguments.of("s3", "us-east-1", "https://s3.us-east-1.amazonaws.com"),
                Arguments.of("dynamodb", "eu-west-1", "https://dynamodb.eu-west-1.amazonaws.com"),
                Arguments.of("sqs", "ap-southeast-1", "https://sqs.ap-southeast-1.amazonaws.com"),
                Arguments.of("sns", "ca-central-1", "https://sns.ca-central-1.amazonaws.com")
            );
        }
    }

    @Nested
    @DisplayName("Edge Cases Tests")
    class EdgeCasesTests {

        @Test
        @DisplayName("Should handle null credentials map gracefully")
        void shouldHandleNullCredentialsMapGracefully() {
            // Given
            Map<String, String> nullCredentials = null;

            // When & Then
            assertThrows(NullPointerException.class, () -> awsClient.getAWSRegion(nullCredentials));
        }

        @Test
        @DisplayName("Should handle null values in credentials map gracefully")
        void shouldHandleNullValuesInCredentialsMapGracefully() {
            // Given
            credentials.put(AWSSampler.AWS_REGION, null);
            credentials.put(AWSSampler.AWS_CONFIG_PROFILE, null);

            // When & Then
            assertDoesNotThrow(() -> {
                awsClient.hasSpecificProfile(credentials);
                awsClient.hasExplicitCredentials(credentials);
                awsClient.isParameterPresent(credentials, AWSSampler.AWS_REGION);
            });
        }

        @Test
        @DisplayName("Should handle special characters in endpoint construction")
        void shouldHandleSpecialCharactersInEndpointConstruction() {
            // Given
            String serviceWithDash = "application-autoscaling";
            String regionWithNumber = "us-gov-east-1";

            // When
            String result = awsClient.getAWSEndpoint(credentials, serviceWithDash, regionWithNumber);

            // Then
            assertEquals("https://application-autoscaling.us-gov-east-1.amazonaws.com", result);
        }
    }

    @Nested
    @DisplayName("Integration Tests")
    class IntegrationTests {

        @Test
        @DisplayName("Should work with complete AWS configuration")
        void shouldWorkWithCompleteAWSConfiguration() {
            // Given
            credentials.put(AWSSampler.AWS_ACCESS_KEY_ID, "AKIAIOSFODNN7EXAMPLE");
            credentials.put(AWSSampler.AWS_SECRET_ACCESS_KEY, "wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY");
            credentials.put(AWSSampler.AWS_REGION, "us-west-2");
            credentials.put(AWSSampler.AWS_CONFIG_PROFILE, "production");
            credentials.put(AWSSampler.AWS_ENDPOINT_CUSTOM, "https://my-custom-endpoint.com");

            // When & Then
            assertTrue(awsClient.hasExplicitCredentials(credentials));
            assertTrue(awsClient.hasSpecificProfile(credentials));
            assertEquals("us-west-2", awsClient.getAWSRegion(credentials));
            assertEquals("https://my-custom-endpoint.com", awsClient.getAWSEndpoint(credentials, "s3", "us-west-2"));
        }

        @Test
        @DisplayName("Should work with minimal AWS configuration")
        void shouldWorkWithMinimalAWSConfiguration() {
            // Given - minimal configuration (just region)
            credentials.put(AWSSampler.AWS_REGION, "eu-central-1");

            // When & Then
            assertFalse(awsClient.hasExplicitCredentials(credentials));
            assertFalse(awsClient.hasSpecificProfile(credentials));
            assertEquals("eu-central-1", awsClient.getAWSRegion(credentials));
            assertEquals("https://s3.eu-central-1.amazonaws.com", awsClient.getAWSEndpoint(credentials, "s3", "eu-central-1"));
        }
    }
}

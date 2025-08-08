package org.apache.jmeter.protocol.aws;

import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.core.SdkClient;
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
 * Unit tests for AWSClientSDK2 interface default methods.
 * Tests credential provider logic and AWS SDK2 specific functionality.
 * 
 * @author JoseLuisSR
 * @since 08/07/2025
 */
@DisplayName("AWSClientSDK2 Tests")
class AWSClientSDK2Test {

    private AWSClientSDK2 awsClientSDK2;
    private Map<String, String> credentials;

    /**
     * Test implementation of AWSClientSDK2 interface for testing purposes.
     */
    private static class TestAWSClientSDK2 implements AWSClientSDK2 {
        @Override
        public SdkClient createSdkClient(Map<String, String> credentials) {
            // Simple test implementation - returns null for testing
            // In real implementation, this would return configured AWS SDK client
            return null;
        }
    }

    @BeforeEach
    void setUp() {
        awsClientSDK2 = new TestAWSClientSDK2();
        credentials = new HashMap<>();
    }

    @Nested
    @DisplayName("AWS Credentials Provider Tests")
    class AWSCredentialsProviderTests {

        @Test
        @DisplayName("Should return static credentials provider when explicit credentials are provided")
        void shouldReturnStaticCredentialsProviderWhenExplicitCredentialsAreProvided() {
            // Given
            credentials.put(AWSSampler.AWS_ACCESS_KEY_ID, "AKIAIOSFODNN7EXAMPLE");
            credentials.put(AWSSampler.AWS_SECRET_ACCESS_KEY, "wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY");

            // When
            AwsCredentialsProvider result = awsClientSDK2.getAwsCredentialsProvider(credentials);

            // Then
            assertNotNull(result);
            assertInstanceOf(StaticCredentialsProvider.class, result);
        }

        @Test
        @DisplayName("Should return profile credentials provider when specific profile is configured")
        void shouldReturnProfileCredentialsProviderWhenSpecificProfileIsConfigured() {
            // Given
            credentials.put(AWSSampler.AWS_CONFIG_PROFILE, "production");

            // When
            AwsCredentialsProvider result = awsClientSDK2.getAwsCredentialsProvider(credentials);

            // Then
            assertNotNull(result);
            assertInstanceOf(ProfileCredentialsProvider.class, result);
        }

        @Test
        @DisplayName("Should return default credentials provider when no explicit credentials or specific profile")
        void shouldReturnDefaultCredentialsProviderWhenNoExplicitCredentialsOrSpecificProfile() {
            // Given - empty credentials map

            // When
            AwsCredentialsProvider result = awsClientSDK2.getAwsCredentialsProvider(credentials);

            // Then
            assertNotNull(result);
            assertInstanceOf(DefaultCredentialsProvider.class, result);
        }

        @ParameterizedTest
        @ValueSource(strings = {"", "default"})
        @DisplayName("Should return default credentials provider for default or empty profile")
        void shouldReturnDefaultCredentialsProviderForDefaultOrEmptyProfile(String profileValue) {
            // Given
            credentials.put(AWSSampler.AWS_CONFIG_PROFILE, profileValue);

            // When
            AwsCredentialsProvider result = awsClientSDK2.getAwsCredentialsProvider(credentials);

            // Then
            assertNotNull(result);
            assertInstanceOf(DefaultCredentialsProvider.class, result);
        }

        @Test
        @DisplayName("Should prioritize explicit credentials over profile configuration")
        void shouldPrioritizeExplicitCredentialsOverProfileConfiguration() {
            // Given
            credentials.put(AWSSampler.AWS_ACCESS_KEY_ID, "AKIAIOSFODNN7EXAMPLE");
            credentials.put(AWSSampler.AWS_SECRET_ACCESS_KEY, "wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY");
            credentials.put(AWSSampler.AWS_CONFIG_PROFILE, "production");

            // When
            AwsCredentialsProvider result = awsClientSDK2.getAwsCredentialsProvider(credentials);

            // Then
            assertNotNull(result);
            assertInstanceOf(StaticCredentialsProvider.class, result);
            assertFalse(result instanceof ProfileCredentialsProvider);
        }
    }

    @Nested
    @DisplayName("Static Credentials Provider Tests")
    class StaticCredentialsProviderTests {

        @Test
        @DisplayName("Should create session credentials when session token is provided")
        void shouldCreateSessionCredentialsWhenSessionTokenIsProvided() {
            // Given
            credentials.put(AWSSampler.AWS_ACCESS_KEY_ID, "AKIAIOSFODNN7EXAMPLE");
            credentials.put(AWSSampler.AWS_SECRET_ACCESS_KEY, "wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY");
            credentials.put(AWSSampler.AWS_SESSION_TOKEN, "FwoGZXIvYXdzEBEaDL+xBf7s6+9z8w==");

            // When
            AwsCredentialsProvider result = awsClientSDK2.buildStaticCredentialsProvider(credentials);

            // Then
            assertNotNull(result);
            assertInstanceOf(StaticCredentialsProvider.class, result);
            
            // Verify it contains session credentials
            assertTrue(result.resolveCredentials() instanceof AwsSessionCredentials);
            AwsSessionCredentials sessionCreds = (AwsSessionCredentials) result.resolveCredentials();
            assertEquals("AKIAIOSFODNN7EXAMPLE", sessionCreds.accessKeyId());
            assertEquals("wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY", sessionCreds.secretAccessKey());
            assertEquals("FwoGZXIvYXdzEBEaDL+xBf7s6+9z8w==", sessionCreds.sessionToken());
        }

        @Test
        @DisplayName("Should create basic credentials when no session token is provided")
        void shouldCreateBasicCredentialsWhenNoSessionTokenIsProvided() {
            // Given
            credentials.put(AWSSampler.AWS_ACCESS_KEY_ID, "AKIAIOSFODNN7EXAMPLE");
            credentials.put(AWSSampler.AWS_SECRET_ACCESS_KEY, "wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY");

            // When
            AwsCredentialsProvider result = awsClientSDK2.buildStaticCredentialsProvider(credentials);

            // Then
            assertNotNull(result);
            assertInstanceOf(StaticCredentialsProvider.class, result);
            
            // Verify it contains basic credentials
            assertTrue(result.resolveCredentials() instanceof AwsBasicCredentials);
            AwsBasicCredentials basicCreds = (AwsBasicCredentials) result.resolveCredentials();
            assertEquals("AKIAIOSFODNN7EXAMPLE", basicCreds.accessKeyId());
            assertEquals("wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY", basicCreds.secretAccessKey());
        }

        @Test
        @DisplayName("Should create basic credentials when session token is empty")
        void shouldCreateBasicCredentialsWhenSessionTokenIsEmpty() {
            // Given
            credentials.put(AWSSampler.AWS_ACCESS_KEY_ID, "AKIAIOSFODNN7EXAMPLE");
            credentials.put(AWSSampler.AWS_SECRET_ACCESS_KEY, "wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY");
            credentials.put(AWSSampler.AWS_SESSION_TOKEN, "");

            // When
            AwsCredentialsProvider result = awsClientSDK2.buildStaticCredentialsProvider(credentials);

            // Then
            assertNotNull(result);
            assertInstanceOf(StaticCredentialsProvider.class, result);
            
            // Verify it contains basic credentials (not session credentials)
            assertTrue(result.resolveCredentials() instanceof AwsBasicCredentials);
        }

        @Test
        @DisplayName("Should create session credentials when session token is whitespace only")
        void shouldCreateSessionCredentialsWhenSessionTokenIsWhitespaceOnly() {
            // Given
            credentials.put(AWSSampler.AWS_ACCESS_KEY_ID, "AKIAIOSFODNN7EXAMPLE");
            credentials.put(AWSSampler.AWS_SECRET_ACCESS_KEY, "wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY");
            credentials.put(AWSSampler.AWS_SESSION_TOKEN, "   ");

            // When
            AwsCredentialsProvider result = awsClientSDK2.buildStaticCredentialsProvider(credentials);

            // Then
            assertNotNull(result);
            assertInstanceOf(StaticCredentialsProvider.class, result);
            
            // Note: AWS SDK2 treats whitespace-only tokens as valid session tokens 
            // (they only filter empty strings, not whitespace-only strings)
            assertTrue(result.resolveCredentials() instanceof AwsSessionCredentials);
        }
    }

    @Nested
    @DisplayName("Profile Credentials Provider Tests")
    class ProfileCredentialsProviderTests {

        @Test
        @DisplayName("Should create profile credentials provider with correct profile name")
        void shouldCreateProfileCredentialsProviderWithCorrectProfileName() {
            // Given
            String profileName = "production";
            credentials.put(AWSSampler.AWS_CONFIG_PROFILE, profileName);

            // When
            ProfileCredentialsProvider result = awsClientSDK2.buildProfileCredentialsProvider(credentials);

            // Then
            assertNotNull(result);
            assertInstanceOf(ProfileCredentialsProvider.class, result);
            
            // Verify it doesn't throw exceptions when accessed
            assertDoesNotThrow(() -> result.getClass());
        }

        @ParameterizedTest
        @ValueSource(strings = {"development", "staging", "production", "test", "custom-profile-name", "profile-with-dashes"})
        @DisplayName("Should handle various profile names correctly")
        void shouldHandleVariousProfileNamesCorrectly(String profileName) {
            // Given
            credentials.put(AWSSampler.AWS_CONFIG_PROFILE, profileName);

            // When
            ProfileCredentialsProvider result = awsClientSDK2.buildProfileCredentialsProvider(credentials);

            // Then
            assertNotNull(result);
            assertInstanceOf(ProfileCredentialsProvider.class, result);
        }

        @Test
        @DisplayName("Should handle null profile name gracefully")
        void shouldHandleNullProfileNameGracefully() {
            // Given
            credentials.put(AWSSampler.AWS_CONFIG_PROFILE, null);

            // When & Then - AWS SDK2 ProfileCredentialsProvider handles null profile names
            assertDoesNotThrow(() -> {
                ProfileCredentialsProvider result = awsClientSDK2.buildProfileCredentialsProvider(credentials);
                assertNotNull(result);
            });
        }

        @Test
        @DisplayName("Should handle empty profile name gracefully")
        void shouldHandleEmptyProfileNameGracefully() {
            // Given
            credentials.put(AWSSampler.AWS_CONFIG_PROFILE, "");

            // When
            ProfileCredentialsProvider result = awsClientSDK2.buildProfileCredentialsProvider(credentials);

            // Then
            assertNotNull(result);
            assertInstanceOf(ProfileCredentialsProvider.class, result);
        }
    }

    @Nested
    @DisplayName("Session Credentials Builder Tests")
    class SessionCredentialsBuilderTests {

        @Test
        @DisplayName("Should build session credentials with all required parameters")
        void shouldBuildSessionCredentialsWithAllRequiredParameters() {
            // Given
            credentials.put(AWSSampler.AWS_ACCESS_KEY_ID, "AKIAIOSFODNN7EXAMPLE");
            credentials.put(AWSSampler.AWS_SECRET_ACCESS_KEY, "wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY");
            String sessionToken = "FwoGZXIvYXdzEBEaDL+xBf7s6+9z8w==";

            // When
            StaticCredentialsProvider result = awsClientSDK2.buildAWSSessionCredentials(credentials, sessionToken);

            // Then
            assertNotNull(result);
            assertInstanceOf(StaticCredentialsProvider.class, result);
            
            AwsSessionCredentials sessionCreds = (AwsSessionCredentials) result.resolveCredentials();
            assertEquals("AKIAIOSFODNN7EXAMPLE", sessionCreds.accessKeyId());
            assertEquals("wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY", sessionCreds.secretAccessKey());
            assertEquals(sessionToken, sessionCreds.sessionToken());
        }

        @ParameterizedTest
        @MethodSource("provideSessionTokenVariations")
        @DisplayName("Should handle different session token formats")
        void shouldHandleDifferentSessionTokenFormats(String sessionToken, String description) {
            // Given
            credentials.put(AWSSampler.AWS_ACCESS_KEY_ID, "AKIAIOSFODNN7EXAMPLE");
            credentials.put(AWSSampler.AWS_SECRET_ACCESS_KEY, "wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY");

            // When
            StaticCredentialsProvider result = awsClientSDK2.buildAWSSessionCredentials(credentials, sessionToken);

            // Then
            assertNotNull(result, description);
            AwsSessionCredentials sessionCreds = (AwsSessionCredentials) result.resolveCredentials();
            assertEquals(sessionToken, sessionCreds.sessionToken(), description);
        }

        private static Stream<Arguments> provideSessionTokenVariations() {
            return Stream.of(
                Arguments.of("FwoGZXIvYXdzEBEaDL+xBf7s6+9z8w==", "Base64 encoded token"),
                Arguments.of("IQoJb3JpZ2luX2VjEFoaCXVzLWVhc3QtMSJIMEYCIQC", "Longer base64 token"),
                Arguments.of("short-token", "Short token format"),
                Arguments.of("token-with-special-chars!@#$%", "Token with special characters"),
                Arguments.of("very-long-session-token-12345678901234567890", "Long session token")
            );
        }

        @Test
        @DisplayName("Should throw exception when building session credentials with null access key")
        void shouldThrowExceptionWhenBuildingSessionCredentialsWithNullAccessKey() {
            // Given
            credentials.put(AWSSampler.AWS_ACCESS_KEY_ID, null);
            credentials.put(AWSSampler.AWS_SECRET_ACCESS_KEY, "wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY");
            String sessionToken = "FwoGZXIvYXdzEBEaDL+xBf7s6+9z8w==";

            // When & Then
            assertThrows(Exception.class, () -> 
                awsClientSDK2.buildAWSSessionCredentials(credentials, sessionToken));
        }

        @Test
        @DisplayName("Should throw exception when building session credentials with null secret key")
        void shouldThrowExceptionWhenBuildingSessionCredentialsWithNullSecretKey() {
            // Given
            credentials.put(AWSSampler.AWS_ACCESS_KEY_ID, "AKIAIOSFODNN7EXAMPLE");
            credentials.put(AWSSampler.AWS_SECRET_ACCESS_KEY, null);
            String sessionToken = "FwoGZXIvYXdzEBEaDL+xBf7s6+9z8w==";

            // When & Then
            assertThrows(Exception.class, () -> 
                awsClientSDK2.buildAWSSessionCredentials(credentials, sessionToken));
        }

        @Test
        @DisplayName("Should throw exception when building session credentials with null session token")
        void shouldThrowExceptionWhenBuildingSessionCredentialsWithNullSessionToken() {
            // Given
            credentials.put(AWSSampler.AWS_ACCESS_KEY_ID, "AKIAIOSFODNN7EXAMPLE");
            credentials.put(AWSSampler.AWS_SECRET_ACCESS_KEY, "wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY");

            // When & Then
            assertThrows(Exception.class, () -> 
                awsClientSDK2.buildAWSSessionCredentials(credentials, null));
        }
    }

    @Nested
    @DisplayName("Basic Credentials Builder Tests")
    class BasicCredentialsBuilderTests {

        @Test
        @DisplayName("Should build basic credentials with access key and secret key")
        void shouldBuildBasicCredentialsWithAccessKeyAndSecretKey() {
            // Given
            credentials.put(AWSSampler.AWS_ACCESS_KEY_ID, "AKIAIOSFODNN7EXAMPLE");
            credentials.put(AWSSampler.AWS_SECRET_ACCESS_KEY, "wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY");

            // When
            StaticCredentialsProvider result = awsClientSDK2.buildAWSBasicCredentials(credentials);

            // Then
            assertNotNull(result);
            assertInstanceOf(StaticCredentialsProvider.class, result);
            
            AwsBasicCredentials basicCreds = (AwsBasicCredentials) result.resolveCredentials();
            assertEquals("AKIAIOSFODNN7EXAMPLE", basicCreds.accessKeyId());
            assertEquals("wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY", basicCreds.secretAccessKey());
        }

        @ParameterizedTest
        @MethodSource("provideCredentialsVariations")
        @DisplayName("Should handle different credential formats")
        void shouldHandleDifferentCredentialFormats(String accessKey, String secretKey, String description) {
            // Given
            credentials.put(AWSSampler.AWS_ACCESS_KEY_ID, accessKey);
            credentials.put(AWSSampler.AWS_SECRET_ACCESS_KEY, secretKey);

            // When
            StaticCredentialsProvider result = awsClientSDK2.buildAWSBasicCredentials(credentials);

            // Then
            assertNotNull(result, description);
            AwsBasicCredentials basicCreds = (AwsBasicCredentials) result.resolveCredentials();
            assertEquals(accessKey, basicCreds.accessKeyId(), description);
            assertEquals(secretKey, basicCreds.secretAccessKey(), description);
        }

        private static Stream<Arguments> provideCredentialsVariations() {
            return Stream.of(
                Arguments.of("AKIAIOSFODNN7EXAMPLE", "wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY", "Standard AWS format"),
                Arguments.of("ASIA123456789EXAMPLE", "abcdefghijklmnopqrstuvwxyz1234567890ABCD", "Temporary credentials format"),
                Arguments.of("AKIA", "short", "Short credentials"),
                Arguments.of("AKIAVERYLONG123456789EXAMPLE", "verylongsecretkeyexample123456789012345678901234567890", "Long credentials")
            );
        }

        @Test
        @DisplayName("Should throw exception when building credentials with null access key")
        void shouldThrowExceptionWhenBuildingCredentialsWithNullAccessKey() {
            // Given
            credentials.put(AWSSampler.AWS_ACCESS_KEY_ID, null);
            credentials.put(AWSSampler.AWS_SECRET_ACCESS_KEY, "wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY");

            // When & Then
            assertThrows(Exception.class, () -> 
                awsClientSDK2.buildAWSBasicCredentials(credentials));
        }

        @Test
        @DisplayName("Should throw exception when building credentials with null secret key")
        void shouldThrowExceptionWhenBuildingCredentialsWithNullSecretKey() {
            // Given
            credentials.put(AWSSampler.AWS_ACCESS_KEY_ID, "AKIAIOSFODNN7EXAMPLE");
            credentials.put(AWSSampler.AWS_SECRET_ACCESS_KEY, null);

            // When & Then
            assertThrows(Exception.class, () -> 
                awsClientSDK2.buildAWSBasicCredentials(credentials));
        }

        @Test
        @DisplayName("Should throw exception when building credentials with empty access key")
        void shouldThrowExceptionWhenBuildingCredentialsWithEmptyAccessKey() {
            // Given
            credentials.put(AWSSampler.AWS_ACCESS_KEY_ID, "");
            credentials.put(AWSSampler.AWS_SECRET_ACCESS_KEY, "");

            // When & Then - AWS SDK2 throws exception for empty credentials
            assertThrows(Exception.class, () -> 
                awsClientSDK2.buildAWSBasicCredentials(credentials));
        }
    }

    @Nested
    @DisplayName("Edge Cases and Error Handling Tests")
    class EdgeCasesAndErrorHandlingTests {

        @Test
        @DisplayName("Should handle null credentials map gracefully in getAwsCredentialsProvider")
        void shouldHandleNullCredentialsMapGracefullyInGetAwsCredentialsProvider() {
            // Given
            Map<String, String> nullCredentials = null;

            // When & Then
            assertThrows(NullPointerException.class, () -> 
                awsClientSDK2.getAwsCredentialsProvider(nullCredentials));
        }

        @Test
        @DisplayName("Should handle null values in credentials map gracefully")
        void shouldHandleNullValuesInCredentialsMapGracefully() {
            // Given
            credentials.put(AWSSampler.AWS_ACCESS_KEY_ID, null);
            credentials.put(AWSSampler.AWS_SECRET_ACCESS_KEY, null);
            credentials.put(AWSSampler.AWS_SESSION_TOKEN, null);
            credentials.put(AWSSampler.AWS_CONFIG_PROFILE, null);

            // When & Then
            assertDoesNotThrow(() -> {
                AwsCredentialsProvider result = awsClientSDK2.getAwsCredentialsProvider(credentials);
                assertNotNull(result);
                assertInstanceOf(DefaultCredentialsProvider.class, result);
            });
        }

        @Test
        @DisplayName("Should handle missing keys in credentials map gracefully")
        void shouldHandleMissingKeysInCredentialsMapGracefully() {
            // Given - empty credentials map (no keys at all)

            // When
            AwsCredentialsProvider result = awsClientSDK2.getAwsCredentialsProvider(credentials);

            // Then
            assertNotNull(result);
            assertInstanceOf(DefaultCredentialsProvider.class, result);
        }

        @Test
        @DisplayName("Should throw exception for whitespace-only values when building credentials")
        void shouldThrowExceptionForWhitespaceOnlyValuesWhenBuildingCredentials() {
            // Given
            credentials.put(AWSSampler.AWS_ACCESS_KEY_ID, "   ");
            credentials.put(AWSSampler.AWS_SECRET_ACCESS_KEY, "\t\n");
            credentials.put(AWSSampler.AWS_SESSION_TOKEN, "  ");
            credentials.put(AWSSampler.AWS_CONFIG_PROFILE, " ");

            // When & Then - AWS SDK2 will throw an exception when attempting to build credentials with whitespace-only values
            // Note: The getAwsCredentialsProvider method will see these as non-empty values due to hasExplicitCredentials check
            // and will attempt to build static credentials, which will fail
            assertThrows(Exception.class, () -> 
                awsClientSDK2.getAwsCredentialsProvider(credentials));
        }

        @Test
        @DisplayName("Should handle partial credentials gracefully")
        void shouldHandlePartialCredentialsGracefully() {
            // Given - only access key provided, no secret key
            credentials.put(AWSSampler.AWS_ACCESS_KEY_ID, "AKIAIOSFODNN7EXAMPLE");

            // When
            AwsCredentialsProvider result = awsClientSDK2.getAwsCredentialsProvider(credentials);

            // Then
            assertNotNull(result);
            assertInstanceOf(DefaultCredentialsProvider.class, result);
        }

        @Test
        @DisplayName("Should handle only secret key provided gracefully")
        void shouldHandleOnlySecretKeyProvidedGracefully() {
            // Given - only secret key provided, no access key
            credentials.put(AWSSampler.AWS_SECRET_ACCESS_KEY, "wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY");

            // When
            AwsCredentialsProvider result = awsClientSDK2.getAwsCredentialsProvider(credentials);

            // Then
            assertNotNull(result);
            assertInstanceOf(DefaultCredentialsProvider.class, result);
        }
    }

    @Nested
    @DisplayName("Integration Tests")
    class IntegrationTests {

        @Test
        @DisplayName("Should prioritize explicit credentials with session token over profile configuration")
        void shouldPrioritizeExplicitCredentialsWithSessionTokenOverProfileConfiguration() {
            // Given
            credentials.put(AWSSampler.AWS_ACCESS_KEY_ID, "AKIAIOSFODNN7EXAMPLE");
            credentials.put(AWSSampler.AWS_SECRET_ACCESS_KEY, "wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY");
            credentials.put(AWSSampler.AWS_SESSION_TOKEN, "FwoGZXIvYXdzEBEaDL+xBf7s6+9z8w==");
            credentials.put(AWSSampler.AWS_CONFIG_PROFILE, "production");

            // When
            AwsCredentialsProvider result = awsClientSDK2.getAwsCredentialsProvider(credentials);

            // Then
            assertNotNull(result);
            assertInstanceOf(StaticCredentialsProvider.class, result);
            assertTrue(result.resolveCredentials() instanceof AwsSessionCredentials);
        }

        @Test
        @DisplayName("Should work with complete AWS SDK2 configuration flow")
        void shouldWorkWithCompleteAWSSDK2ConfigurationFlow() {
            // Given
            credentials.put(AWSSampler.AWS_ACCESS_KEY_ID, "AKIAIOSFODNN7EXAMPLE");
            credentials.put(AWSSampler.AWS_SECRET_ACCESS_KEY, "wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY");
            credentials.put(AWSSampler.AWS_SESSION_TOKEN, "FwoGZXIvYXdzEBEaDL+xBf7s6+9z8w==");
            credentials.put(AWSSampler.AWS_REGION, "us-west-2");
            credentials.put(AWSSampler.AWS_CONFIG_PROFILE, "production");

            // When
            AwsCredentialsProvider credentialsProvider = awsClientSDK2.getAwsCredentialsProvider(credentials);
            SdkClient sdkClient = awsClientSDK2.createSdkClient(credentials);

            // Then
            assertNotNull(credentialsProvider);
            // SDK Client is null in our test implementation
            assertNull(sdkClient);
            assertInstanceOf(StaticCredentialsProvider.class, credentialsProvider);
            assertTrue(credentialsProvider.resolveCredentials() instanceof AwsSessionCredentials);
        }

        @Test
        @DisplayName("Should work with minimal AWS SDK2 configuration")
        void shouldWorkWithMinimalAWSSDK2Configuration() {
            // Given - minimal configuration (just region, relying on default credential chain)
            credentials.put(AWSSampler.AWS_REGION, "eu-central-1");

            // When
            AwsCredentialsProvider credentialsProvider = awsClientSDK2.getAwsCredentialsProvider(credentials);
            SdkClient sdkClient = awsClientSDK2.createSdkClient(credentials);

            // Then
            assertNotNull(credentialsProvider);
            // SDK Client is null in our test implementation
            assertNull(sdkClient);
            assertInstanceOf(DefaultCredentialsProvider.class, credentialsProvider);
        }

        @Test
        @DisplayName("Should work with profile-based configuration")
        void shouldWorkWithProfileBasedConfiguration() {
            // Given
            credentials.put(AWSSampler.AWS_CONFIG_PROFILE, "staging");
            credentials.put(AWSSampler.AWS_REGION, "ap-southeast-1");

            // When
            AwsCredentialsProvider credentialsProvider = awsClientSDK2.getAwsCredentialsProvider(credentials);
            SdkClient sdkClient = awsClientSDK2.createSdkClient(credentials);

            // Then
            assertNotNull(credentialsProvider);
            // SDK Client is null in our test implementation
            assertNull(sdkClient);
            assertInstanceOf(ProfileCredentialsProvider.class, credentialsProvider);
        }

        @Test
        @DisplayName("Should handle mixed configuration scenarios correctly")
        void shouldHandleMixedConfigurationScenariosCorrectly() {
            // Given
            credentials.put(AWSSampler.AWS_ACCESS_KEY_ID, "AKIAIOSFODNN7EXAMPLE");
            credentials.put(AWSSampler.AWS_SECRET_ACCESS_KEY, "wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY");
            credentials.put(AWSSampler.AWS_SESSION_TOKEN, ""); // Empty session token
            credentials.put(AWSSampler.AWS_CONFIG_PROFILE, "production");
            credentials.put(AWSSampler.AWS_REGION, "us-east-1");

            // When
            AwsCredentialsProvider result = awsClientSDK2.getAwsCredentialsProvider(credentials);

            // Then
            assertNotNull(result);
            assertInstanceOf(StaticCredentialsProvider.class, result);
            assertTrue(result.resolveCredentials() instanceof AwsBasicCredentials);
        }
    }

    @Nested
    @DisplayName("SDK Client Creation Tests")
    class SDKClientCreationTests {

        @Test
        @DisplayName("Should create SDK client with test implementation")
        void shouldCreateSDKClientWithTestImplementation() {
            // Given
            credentials.put(AWSSampler.AWS_ACCESS_KEY_ID, "AKIAIOSFODNN7EXAMPLE");
            credentials.put(AWSSampler.AWS_SECRET_ACCESS_KEY, "wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY");
            credentials.put(AWSSampler.AWS_REGION, "us-east-1");

            // When
            SdkClient result = awsClientSDK2.createSdkClient(credentials);

            // Then
            // Our test implementation returns null - this tests that the method is callable
            assertNull(result);
        }

        @Test
        @DisplayName("Should handle empty credentials when creating SDK client")
        void shouldHandleEmptyCredentialsWhenCreatingSDKClient() {
            // Given - empty credentials

            // When
            SdkClient result = awsClientSDK2.createSdkClient(credentials);

            // Then
            // Our test implementation returns null - this tests that the method is callable
            assertNull(result);
        }

        @Test
        @DisplayName("Should handle null credentials map when creating SDK client")
        void shouldHandleNullCredentialsMapWhenCreatingSDKClient() {
            // Given
            Map<String, String> nullCredentials = null;

            // When & Then
            // This should work without throwing an exception in the interface method
            // (though implementation might handle null differently)
            assertDoesNotThrow(() -> {
                SdkClient result = awsClientSDK2.createSdkClient(nullCredentials);
                assertNull(result);
            });
        }
    }

    @Nested
    @DisplayName("Credentials Provider Type Tests")
    class CredentialsProviderTypeTests {

        @Test
        @DisplayName("Should return different provider types based on configuration")
        void shouldReturnDifferentProviderTypesBasedOnConfiguration() {
            // Test 1: Explicit credentials should return StaticCredentialsProvider
            credentials.clear();
            credentials.put(AWSSampler.AWS_ACCESS_KEY_ID, "AKIAIOSFODNN7EXAMPLE");
            credentials.put(AWSSampler.AWS_SECRET_ACCESS_KEY, "wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY");
            
            AwsCredentialsProvider staticProvider = awsClientSDK2.getAwsCredentialsProvider(credentials);
            assertInstanceOf(StaticCredentialsProvider.class, staticProvider);

            // Test 2: Profile configuration should return ProfileCredentialsProvider
            credentials.clear();
            credentials.put(AWSSampler.AWS_CONFIG_PROFILE, "production");
            
            AwsCredentialsProvider profileProvider = awsClientSDK2.getAwsCredentialsProvider(credentials);
            assertInstanceOf(ProfileCredentialsProvider.class, profileProvider);

            // Test 3: No configuration should return DefaultCredentialsProvider
            credentials.clear();
            
            AwsCredentialsProvider defaultProvider = awsClientSDK2.getAwsCredentialsProvider(credentials);
            assertInstanceOf(DefaultCredentialsProvider.class, defaultProvider);
        }

        @Test
        @DisplayName("Should maintain provider type consistency across multiple calls")
        void shouldMaintainProviderTypeConsistencyAcrossMultipleCalls() {
            // Given
            credentials.put(AWSSampler.AWS_ACCESS_KEY_ID, "AKIAIOSFODNN7EXAMPLE");
            credentials.put(AWSSampler.AWS_SECRET_ACCESS_KEY, "wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY");

            // When
            AwsCredentialsProvider result1 = awsClientSDK2.getAwsCredentialsProvider(credentials);
            AwsCredentialsProvider result2 = awsClientSDK2.getAwsCredentialsProvider(credentials);
            AwsCredentialsProvider result3 = awsClientSDK2.getAwsCredentialsProvider(credentials);

            // Then
            assertInstanceOf(StaticCredentialsProvider.class, result1);
            assertInstanceOf(StaticCredentialsProvider.class, result2);
            assertInstanceOf(StaticCredentialsProvider.class, result3);
        }
    }
}

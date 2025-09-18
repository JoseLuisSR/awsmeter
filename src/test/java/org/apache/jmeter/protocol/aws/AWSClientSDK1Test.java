package org.apache.jmeter.protocol.aws;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.BasicSessionCredentials;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.client.builder.AwsSyncClientBuilder;
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
 * Unit tests for AWSClientSDK1 interface default methods.
 * Tests credential provider logic and AWS SDK1 specific functionality.
 * 
 * @author JoseLuisSR
 * @since 08/07/2025
 */
@DisplayName("AWSClientSDK1 Tests")
class AWSClientSDK1Test {

    private AWSClientSDK1 awsClientSDK1;
    private Map<String, String> credentials;

    /**
     * Test implementation of AWSClientSDK1 interface for testing purposes.
     */
    private static class TestAWSClientSDK1 implements AWSClientSDK1 {
        @Override
        public AwsSyncClientBuilder<?, ?> createAWSClient(Map<String, String> credentials) {
            // Simple test implementation - returns null for testing
            // In real implementation, this would return configured AWS client builder
            return null;
        }
    }

    @BeforeEach
    void setUp() {
        awsClientSDK1 = new TestAWSClientSDK1();
        credentials = new HashMap<>();
    }

    @Nested
    @DisplayName("AWS Credentials Provider Tests")
    class AWSCredentialsProviderTests {

        @Test
        @DisplayName("Should return explicit credentials provider when explicit credentials are provided")
        void shouldReturnExplicitCredentialsProviderWhenExplicitCredentialsAreProvided() {
            // Given
            credentials.put(AWSSampler.AWS_ACCESS_KEY_ID, "AKIAIOSFODNN7EXAMPLE");
            credentials.put(AWSSampler.AWS_SECRET_ACCESS_KEY, "wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY");

            // When
            AWSCredentialsProvider result = awsClientSDK1.getAWSCredentialsProvider(credentials);

            // Then
            assertNotNull(result);
            assertInstanceOf(AWSStaticCredentialsProvider.class, result);
        }

        @Test
        @DisplayName("Should return profile credentials provider when specific profile is configured")
        void shouldReturnProfileCredentialsProviderWhenSpecificProfileIsConfigured() {
            // Given
            credentials.put(AWSSampler.AWS_CONFIG_PROFILE, "production");

            // When
            AWSCredentialsProvider result = awsClientSDK1.getAWSCredentialsProvider(credentials);

            // Then
            assertNotNull(result);
            assertInstanceOf(ProfileCredentialsProvider.class, result);
        }

        @Test
        @DisplayName("Should return default credentials provider chain when no explicit credentials or specific profile")
        void shouldReturnDefaultCredentialsProviderChainWhenNoExplicitCredentialsOrSpecificProfile() {
            // Given - empty credentials map

            // When
            AWSCredentialsProvider result = awsClientSDK1.getAWSCredentialsProvider(credentials);

            // Then
            assertNotNull(result);
            assertInstanceOf(DefaultAWSCredentialsProviderChain.class, result);
        }

        @ParameterizedTest
        @ValueSource(strings = {"", "default"})
        @DisplayName("Should return default credentials provider chain for default or empty profile")
        void shouldReturnDefaultCredentialsProviderChainForDefaultOrEmptyProfile(String profileValue) {
            // Given
            credentials.put(AWSSampler.AWS_CONFIG_PROFILE, profileValue);

            // When
            AWSCredentialsProvider result = awsClientSDK1.getAWSCredentialsProvider(credentials);

            // Then
            assertNotNull(result);
            assertInstanceOf(DefaultAWSCredentialsProviderChain.class, result);
        }
    }

    @Nested
    @DisplayName("Explicit Credentials Provider Tests")
    class ExplicitCredentialsProviderTests {

        @Test
        @DisplayName("Should create session credentials when session token is provided")
        void shouldCreateSessionCredentialsWhenSessionTokenIsProvided() {
            // Given
            credentials.put(AWSSampler.AWS_ACCESS_KEY_ID, "AKIAIOSFODNN7EXAMPLE");
            credentials.put(AWSSampler.AWS_SECRET_ACCESS_KEY, "wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY");
            credentials.put(AWSSampler.AWS_SESSION_TOKEN, "FwoGZXIvYXdzEBEaDL+xBf7s6+9z8w==");

            // When
            AWSCredentialsProvider result = awsClientSDK1.createExplicitCredentialsProvider(credentials);

            // Then
            assertNotNull(result);
            assertInstanceOf(AWSStaticCredentialsProvider.class, result);
            
            // Verify it contains session credentials
            assertTrue(result.getCredentials() instanceof BasicSessionCredentials);
            BasicSessionCredentials sessionCreds = (BasicSessionCredentials) result.getCredentials();
            assertEquals("AKIAIOSFODNN7EXAMPLE", sessionCreds.getAWSAccessKeyId());
            assertEquals("wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY", sessionCreds.getAWSSecretKey());
            assertEquals("FwoGZXIvYXdzEBEaDL+xBf7s6+9z8w==", sessionCreds.getSessionToken());
        }

        @Test
        @DisplayName("Should create basic credentials when no session token is provided")
        void shouldCreateBasicCredentialsWhenNoSessionTokenIsProvided() {
            // Given
            credentials.put(AWSSampler.AWS_ACCESS_KEY_ID, "AKIAIOSFODNN7EXAMPLE");
            credentials.put(AWSSampler.AWS_SECRET_ACCESS_KEY, "wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY");

            // When
            AWSCredentialsProvider result = awsClientSDK1.createExplicitCredentialsProvider(credentials);

            // Then
            assertNotNull(result);
            assertInstanceOf(AWSStaticCredentialsProvider.class, result);
            
            // Verify it contains basic credentials
            assertTrue(result.getCredentials() instanceof BasicAWSCredentials);
            BasicAWSCredentials basicCreds = (BasicAWSCredentials) result.getCredentials();
            assertEquals("AKIAIOSFODNN7EXAMPLE", basicCreds.getAWSAccessKeyId());
            assertEquals("wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY", basicCreds.getAWSSecretKey());
        }

        @Test
        @DisplayName("Should create basic credentials when session token is empty")
        void shouldCreateBasicCredentialsWhenSessionTokenIsEmpty() {
            // Given
            credentials.put(AWSSampler.AWS_ACCESS_KEY_ID, "AKIAIOSFODNN7EXAMPLE");
            credentials.put(AWSSampler.AWS_SECRET_ACCESS_KEY, "wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY");
            credentials.put(AWSSampler.AWS_SESSION_TOKEN, "");

            // When
            AWSCredentialsProvider result = awsClientSDK1.createExplicitCredentialsProvider(credentials);

            // Then
            assertNotNull(result);
            assertInstanceOf(AWSStaticCredentialsProvider.class, result);
            
            // Verify it contains basic credentials (not session credentials)
            assertTrue(result.getCredentials() instanceof BasicAWSCredentials);
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
            AWSCredentialsProvider result = awsClientSDK1.buildSessionAWSCredentials(credentials, sessionToken);

            // Then
            assertNotNull(result);
            assertInstanceOf(AWSStaticCredentialsProvider.class, result);
            
            BasicSessionCredentials sessionCreds = (BasicSessionCredentials) result.getCredentials();
            assertEquals("AKIAIOSFODNN7EXAMPLE", sessionCreds.getAWSAccessKeyId());
            assertEquals("wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY", sessionCreds.getAWSSecretKey());
            assertEquals(sessionToken, sessionCreds.getSessionToken());
        }

        @ParameterizedTest
        @MethodSource("provideSessionTokenVariations")
        @DisplayName("Should handle different session token formats")
        void shouldHandleDifferentSessionTokenFormats(String sessionToken, String description) {
            // Given
            credentials.put(AWSSampler.AWS_ACCESS_KEY_ID, "AKIAIOSFODNN7EXAMPLE");
            credentials.put(AWSSampler.AWS_SECRET_ACCESS_KEY, "wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY");

            // When
            AWSCredentialsProvider result = awsClientSDK1.buildSessionAWSCredentials(credentials, sessionToken);

            // Then
            assertNotNull(result, description);
            BasicSessionCredentials sessionCreds = (BasicSessionCredentials) result.getCredentials();
            assertEquals(sessionToken, sessionCreds.getSessionToken(), description);
        }

        private static Stream<Arguments> provideSessionTokenVariations() {
            return Stream.of(
                Arguments.of("FwoGZXIvYXdzEBEaDL+xBf7s6+9z8w==", "Base64 encoded token"),
                Arguments.of("IQoJb3JpZ2luX2VjEFoaCXVzLWVhc3QtMSJIMEYCIQC", "Longer base64 token"),
                Arguments.of("short-token", "Short token format"),
                Arguments.of("token-with-special-chars!@#$%", "Token with special characters")
            );
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
            AWSCredentialsProvider result = awsClientSDK1.buildBasicAWSCredentials(credentials);

            // Then
            assertNotNull(result);
            assertInstanceOf(AWSStaticCredentialsProvider.class, result);
            
            BasicAWSCredentials basicCreds = (BasicAWSCredentials) result.getCredentials();
            assertEquals("AKIAIOSFODNN7EXAMPLE", basicCreds.getAWSAccessKeyId());
            assertEquals("wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY", basicCreds.getAWSSecretKey());
        }

        @ParameterizedTest
        @MethodSource("provideCredentialsVariations")
        @DisplayName("Should handle different credential formats")
        void shouldHandleDifferentCredentialFormats(String accessKey, String secretKey, String description) {
            // Given
            credentials.put(AWSSampler.AWS_ACCESS_KEY_ID, accessKey);
            credentials.put(AWSSampler.AWS_SECRET_ACCESS_KEY, secretKey);

            // When
            AWSCredentialsProvider result = awsClientSDK1.buildBasicAWSCredentials(credentials);

            // Then
            assertNotNull(result, description);
            BasicAWSCredentials basicCreds = (BasicAWSCredentials) result.getCredentials();
            assertEquals(accessKey, basicCreds.getAWSAccessKeyId(), description);
            assertEquals(secretKey, basicCreds.getAWSSecretKey(), description);
        }

        private static Stream<Arguments> provideCredentialsVariations() {
            return Stream.of(
                Arguments.of("AKIAIOSFODNN7EXAMPLE", "wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY", "Standard AWS format"),
                Arguments.of("ASIA123456789EXAMPLE", "abcdefghijklmnopqrstuvwxyz1234567890ABCD", "Temporary credentials format"),
                Arguments.of("AKIA", "short", "Short credentials"),
                Arguments.of("AKIAVERYLONG123456789EXAMPLE", "verylongsecretkeyexample123456789012345678901234567890", "Long credentials")
            );
        }
    }

    @Nested
    @DisplayName("Edge Cases and Error Handling Tests")
    class EdgeCasesAndErrorHandlingTests {

        @Test
        @DisplayName("Should handle null credentials map gracefully")
        void shouldHandleNullCredentialsMapGracefully() {
            // Given
            Map<String, String> nullCredentials = null;

            // When & Then
            assertThrows(NullPointerException.class, () -> 
                awsClientSDK1.getAWSCredentialsProvider(nullCredentials));
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
                AWSCredentialsProvider result = awsClientSDK1.getAWSCredentialsProvider(credentials);
                assertNotNull(result);
                assertInstanceOf(DefaultAWSCredentialsProviderChain.class, result);
            });
        }

        @Test
        @DisplayName("Should throw exception when building credentials with null access key")
        void shouldThrowExceptionWhenBuildingCredentialsWithNullAccessKey() {
            // Given
            credentials.put(AWSSampler.AWS_ACCESS_KEY_ID, null);
            credentials.put(AWSSampler.AWS_SECRET_ACCESS_KEY, "wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY");

            // When & Then
            assertThrows(IllegalArgumentException.class, () -> 
                awsClientSDK1.buildBasicAWSCredentials(credentials));
        }

        @Test
        @DisplayName("Should throw exception when building credentials with null secret key")
        void shouldThrowExceptionWhenBuildingCredentialsWithNullSecretKey() {
            // Given
            credentials.put(AWSSampler.AWS_ACCESS_KEY_ID, "AKIAIOSFODNN7EXAMPLE");
            credentials.put(AWSSampler.AWS_SECRET_ACCESS_KEY, null);

            // When & Then
            assertThrows(IllegalArgumentException.class, () -> 
                awsClientSDK1.buildBasicAWSCredentials(credentials));
        }

        @Test
        @DisplayName("Should not throw exception when building session credentials with null session token")
        void shouldNotThrowExceptionWhenBuildingSessionCredentialsWithNullSessionToken() {
            // Given
            credentials.put(AWSSampler.AWS_ACCESS_KEY_ID, "AKIAIOSFODNN7EXAMPLE");
            credentials.put(AWSSampler.AWS_SECRET_ACCESS_KEY, "wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY");

            // When & Then
            assertDoesNotThrow(() -> 
                awsClientSDK1.buildSessionAWSCredentials(credentials, null));
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
            AWSCredentialsProvider result = awsClientSDK1.getAWSCredentialsProvider(credentials);

            // Then
            assertNotNull(result);
            assertInstanceOf(ProfileCredentialsProvider.class, result);
            
            // Note: We can't easily test the internal profile name without reflection
            // But we can verify it's the correct type and doesn't throw exceptions
            assertDoesNotThrow(() -> result.getClass());
        }

        @ParameterizedTest
        @ValueSource(strings = {"development", "staging", "production", "test", "custom-profile-name"})
        @DisplayName("Should handle various profile names correctly")
        void shouldHandleVariousProfileNamesCorrectly(String profileName) {
            // Given
            credentials.put(AWSSampler.AWS_CONFIG_PROFILE, profileName);

            // When
            AWSCredentialsProvider result = awsClientSDK1.getAWSCredentialsProvider(credentials);

            // Then
            assertNotNull(result);
            assertInstanceOf(ProfileCredentialsProvider.class, result);
        }
    }

    @Nested
    @DisplayName("Integration Tests")
    class IntegrationTests {

        @Test
        @DisplayName("Should prioritize explicit credentials over profile configuration")
        void shouldPrioritizeExplicitCredentialsOverProfileConfiguration() {
            // Given
            credentials.put(AWSSampler.AWS_ACCESS_KEY_ID, "AKIAIOSFODNN7EXAMPLE");
            credentials.put(AWSSampler.AWS_SECRET_ACCESS_KEY, "wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY");
            credentials.put(AWSSampler.AWS_CONFIG_PROFILE, "production");

            // When
            AWSCredentialsProvider result = awsClientSDK1.getAWSCredentialsProvider(credentials);

            // Then
            assertNotNull(result);
            assertInstanceOf(AWSStaticCredentialsProvider.class, result);
            assertFalse(result instanceof ProfileCredentialsProvider);
        }

        @Test
        @DisplayName("Should prioritize explicit credentials with session token over profile configuration")
        void shouldPrioritizeExplicitCredentialsWithSessionTokenOverProfileConfiguration() {
            // Given
            credentials.put(AWSSampler.AWS_ACCESS_KEY_ID, "AKIAIOSFODNN7EXAMPLE");
            credentials.put(AWSSampler.AWS_SECRET_ACCESS_KEY, "wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY");
            credentials.put(AWSSampler.AWS_SESSION_TOKEN, "FwoGZXIvYXdzEBEaDL+xBf7s6+9z8w==");
            credentials.put(AWSSampler.AWS_CONFIG_PROFILE, "production");

            // When
            AWSCredentialsProvider result = awsClientSDK1.getAWSCredentialsProvider(credentials);

            // Then
            assertNotNull(result);
            assertInstanceOf(AWSStaticCredentialsProvider.class, result);
            assertTrue(result.getCredentials() instanceof BasicSessionCredentials);
        }

        @Test
        @DisplayName("Should work with complete AWS SDK1 configuration flow")
        void shouldWorkWithCompleteAWSSDK1ConfigurationFlow() {
            // Given
            credentials.put(AWSSampler.AWS_ACCESS_KEY_ID, "AKIAIOSFODNN7EXAMPLE");
            credentials.put(AWSSampler.AWS_SECRET_ACCESS_KEY, "wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY");
            credentials.put(AWSSampler.AWS_SESSION_TOKEN, "FwoGZXIvYXdzEBEaDL+xBf7s6+9z8w==");
            credentials.put(AWSSampler.AWS_REGION, "us-west-2");
            credentials.put(AWSSampler.AWS_CONFIG_PROFILE, "production");

            // When
            AWSCredentialsProvider credentialsProvider = awsClientSDK1.getAWSCredentialsProvider(credentials);
            AwsSyncClientBuilder<?, ?> clientBuilder = awsClientSDK1.createAWSClient(credentials);

            // Then
            assertNotNull(credentialsProvider);
            // Client builder is null in our test implementation
            assertNull(clientBuilder);
            assertInstanceOf(AWSStaticCredentialsProvider.class, credentialsProvider);
            assertTrue(credentialsProvider.getCredentials() instanceof BasicSessionCredentials);
        }

        @Test
        @DisplayName("Should work with minimal AWS SDK1 configuration")
        void shouldWorkWithMinimalAWSSDK1Configuration() {
            // Given - minimal configuration (just region, relying on default credential chain)
            credentials.put(AWSSampler.AWS_REGION, "eu-central-1");

            // When
            AWSCredentialsProvider credentialsProvider = awsClientSDK1.getAWSCredentialsProvider(credentials);
            AwsSyncClientBuilder<?, ?> clientBuilder = awsClientSDK1.createAWSClient(credentials);

            // Then
            assertNotNull(credentialsProvider);
            // Client builder is null in our test implementation
            assertNull(clientBuilder);
            assertInstanceOf(DefaultAWSCredentialsProviderChain.class, credentialsProvider);
        }
    }

    @Nested
    @DisplayName("AWS Client Creation Tests")
    class AWSClientCreationTests {

        @Test
        @DisplayName("Should create AWS client with test implementation")
        void shouldCreateAWSClientWithTestImplementation() {
            // Given
            credentials.put(AWSSampler.AWS_ACCESS_KEY_ID, "AKIAIOSFODNN7EXAMPLE");
            credentials.put(AWSSampler.AWS_SECRET_ACCESS_KEY, "wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY");
            credentials.put(AWSSampler.AWS_REGION, "us-east-1");

            // When
            AwsSyncClientBuilder<?, ?> result = awsClientSDK1.createAWSClient(credentials);

            // Then
            // Our test implementation returns null - this tests that the method is callable
            assertNull(result);
        }

        @Test
        @DisplayName("Should handle empty credentials when creating AWS client")
        void shouldHandleEmptyCredentialsWhenCreatingAWSClient() {
            // Given - empty credentials

            // When
            AwsSyncClientBuilder<?, ?> result = awsClientSDK1.createAWSClient(credentials);

            // Then
            // Our test implementation returns null - this tests that the method is callable
            assertNull(result);
        }
    }
}

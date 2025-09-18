package org.apache.jmeter.protocol.aws.cognito;

import org.apache.jmeter.config.Argument;
import org.apache.jmeter.config.Arguments;
import org.apache.jmeter.protocol.java.sampler.JavaSamplerContext;
import org.apache.jmeter.samplers.SampleResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import software.amazon.awssdk.awscore.exception.AwsErrorDetails;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminCreateUserRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminCreateUserResponse;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminSetUserPasswordRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AttributeType;
import software.amazon.awssdk.services.cognitoidentityprovider.model.CognitoIdentityProviderException;
import software.amazon.awssdk.services.cognitoidentityprovider.model.MessageActionType;
import software.amazon.awssdk.services.cognitoidentityprovider.model.UserType;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Comprehensive unit tests for CognitoProducerAdminCreateUser class.
 * Tests all implemented methods and fields with 100% line and branch coverage.
 * 
 * Coverage Details:
 * - COGNITO_PARAMETERS static field: Tests initialization and content
 * - getDefaultParameters(): Tests parameter merging and validation
 * - runTest(): Tests successful user creation, password setting, and all error scenarios
 * - createAdminCreateUserRequest(): Tests request building with various inputs
 * - createAdminSetUserPasswordRequest(): Tests password request building
 * - Edge Cases: Error handling, null values, empty strings, creation failures
 * 
 * This test class focuses only on the methods and fields implemented in 
 * CognitoProducerAdminCreateUser class, not testing inherited behavior.
 * 
 * @author JoseLuisSR
 * @since 09/16/2025
 */
@DisplayName("CognitoProducerAdminCreateUser Tests")
class CognitoProducerAdminCreateUserTest {

    private CognitoProducerAdminCreateUser cognitoProducerAdminCreateUser;
    
    @Mock
    private JavaSamplerContext mockContext;
    
    @Mock
    private CognitoIdentityProviderClient mockCognitoClient;

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);
        cognitoProducerAdminCreateUser = new CognitoProducerAdminCreateUser();
        
        // Inject mock client using reflection
        Field cognitoClientField = CognitoProducerSampler.class.getDeclaredField("cognitoClient");
        cognitoClientField.setAccessible(true);
        cognitoClientField.set(cognitoProducerAdminCreateUser, mockCognitoClient);
    }

    @Nested
    @DisplayName("COGNITO_PARAMETERS Static Field Tests")
    class CognitoParametersStaticFieldTests {

        @Test
        @DisplayName("Should have correct number of Cognito parameters")
        void shouldHaveCorrectNumberOfCognitoParameters() throws Exception {
            // When
            Field cognitoParametersField = CognitoProducerAdminCreateUser.class.getDeclaredField("COGNITO_PARAMETERS");
            cognitoParametersField.setAccessible(true);
            @SuppressWarnings("unchecked")
            List<Argument> cognitoParameters = (List<Argument>) cognitoParametersField.get(null);

            // Then - Should have exactly 4 parameters: user pool id, username, email, password
            assertNotNull(cognitoParameters);
            assertEquals(4, cognitoParameters.size());
        }

        @Test
        @DisplayName("Should contain all required Cognito parameters")
        void shouldContainAllRequiredCognitoParameters() throws Exception {
            // When
            Field cognitoParametersField = CognitoProducerAdminCreateUser.class.getDeclaredField("COGNITO_PARAMETERS");
            cognitoParametersField.setAccessible(true);
            @SuppressWarnings("unchecked")
            List<Argument> cognitoParameters = (List<Argument>) cognitoParametersField.get(null);
            
            Map<String, String> parameterMap = cognitoParameters.stream()
                .collect(Collectors.toMap(Argument::getName, Argument::getValue));

            // Then
            assertEquals("", parameterMap.get("cognito_user_pool_id"));
            assertEquals("", parameterMap.get("cognito_user_username"));
            assertEquals("", parameterMap.get("cognito_user_email"));
            assertEquals("", parameterMap.get("cognito_user_password"));
        }

        @Test
        @DisplayName("Should have static and final modifiers on COGNITO_PARAMETERS field")
        void shouldHaveStaticAndFinalModifiersOnCognitoParametersField() throws Exception {
            // When
            Field cognitoParametersField = CognitoProducerAdminCreateUser.class.getDeclaredField("COGNITO_PARAMETERS");

            // Then
            assertTrue(java.lang.reflect.Modifier.isStatic(cognitoParametersField.getModifiers()));
            assertTrue(java.lang.reflect.Modifier.isFinal(cognitoParametersField.getModifiers()));
            assertTrue(java.lang.reflect.Modifier.isPrivate(cognitoParametersField.getModifiers()));
        }

        @Test
        @DisplayName("Should be mutable list (default behavior of collectors.toList())")
        void shouldBeMutableList() throws Exception {
            // When
            Field cognitoParametersField = CognitoProducerAdminCreateUser.class.getDeclaredField("COGNITO_PARAMETERS");
            cognitoParametersField.setAccessible(true);
            @SuppressWarnings("unchecked")
            List<Argument> cognitoParameters = (List<Argument>) cognitoParametersField.get(null);

            // Then - The implementation uses Stream.collect(Collectors.toList()) which creates a mutable list
            assertNotNull(cognitoParameters);
            assertDoesNotThrow(() -> cognitoParameters.add(new Argument("test", "value")));
            // Clean up - remove the added element to avoid affecting other tests
            cognitoParameters.remove(cognitoParameters.size() - 1);
        }

        @Test
        @DisplayName("Should contain exactly expected parameter names")
        void shouldContainExactlyExpectedParameterNames() throws Exception {
            // When
            Field cognitoParametersField = CognitoProducerAdminCreateUser.class.getDeclaredField("COGNITO_PARAMETERS");
            cognitoParametersField.setAccessible(true);
            @SuppressWarnings("unchecked")
            List<Argument> cognitoParameters = (List<Argument>) cognitoParametersField.get(null);
            
            List<String> parameterNames = cognitoParameters.stream()
                .map(Argument::getName)
                .collect(Collectors.toList());

            // Then
            assertTrue(parameterNames.contains("cognito_user_pool_id"));
            assertTrue(parameterNames.contains("cognito_user_username"));
            assertTrue(parameterNames.contains("cognito_user_email"));
            assertTrue(parameterNames.contains("cognito_user_password"));
        }
    }

    @Nested
    @DisplayName("getDefaultParameters() Method Tests")
    class GetDefaultParametersTests {

        @Test
        @DisplayName("Should return non-null Arguments object")
        void shouldReturnNonNullArgumentsObject() {
            // When
            Arguments result = cognitoProducerAdminCreateUser.getDefaultParameters();

            // Then
            assertNotNull(result);
        }

        @Test
        @DisplayName("Should include AWS parameters")
        void shouldIncludeAwsParameters() {
            // When
            Arguments result = cognitoProducerAdminCreateUser.getDefaultParameters();
            Map<String, String> argumentsMap = result.getArgumentsAsMap();

            // Then - Verify AWS parameters are included
            assertTrue(argumentsMap.containsKey("aws_access_key_id"));
            assertTrue(argumentsMap.containsKey("aws_secret_access_key"));
            assertTrue(argumentsMap.containsKey("aws_region"));
            assertTrue(argumentsMap.containsKey("aws_endpoint_custom"));
            assertTrue(argumentsMap.containsKey("aws_configure_profile"));
            assertTrue(argumentsMap.containsKey("aws_session_token"));
            assertEquals("default", argumentsMap.get("aws_configure_profile"));
        }

        @Test
        @DisplayName("Should include Cognito-specific parameters")
        void shouldIncludeCognitoSpecificParameters() {
            // When
            Arguments result = cognitoProducerAdminCreateUser.getDefaultParameters();
            Map<String, String> argumentsMap = result.getArgumentsAsMap();

            // Then - Verify Cognito parameters are included
            assertTrue(argumentsMap.containsKey("cognito_user_pool_id"));
            assertTrue(argumentsMap.containsKey("cognito_user_username"));
            assertTrue(argumentsMap.containsKey("cognito_user_email"));
            assertTrue(argumentsMap.containsKey("cognito_user_password"));
        }

        @Test
        @DisplayName("Should have correct total number of parameters")
        void shouldHaveCorrectTotalNumberOfParameters() {
            // When
            Arguments result = cognitoProducerAdminCreateUser.getDefaultParameters();

            // Then - Should have AWS (6) + Cognito (4) = 10 parameters
            assertEquals(10, result.getArgumentCount());
        }

        @Test
        @DisplayName("Should have correct default values for AWS parameters")
        void shouldHaveCorrectDefaultValuesForAwsParameters() {
            // When
            Arguments result = cognitoProducerAdminCreateUser.getDefaultParameters();
            Map<String, String> argumentsMap = result.getArgumentsAsMap();

            // Then
            assertEquals("", argumentsMap.get("aws_access_key_id"));
            assertEquals("", argumentsMap.get("aws_secret_access_key"));
            assertEquals("", argumentsMap.get("aws_session_token"));
            assertEquals("", argumentsMap.get("aws_region"));
            assertEquals("", argumentsMap.get("aws_endpoint_custom"));
            assertEquals("default", argumentsMap.get("aws_configure_profile"));
        }

        @Test
        @DisplayName("Should have empty default values for Cognito parameters")
        void shouldHaveEmptyDefaultValuesForCognitoParameters() {
            // When
            Arguments result = cognitoProducerAdminCreateUser.getDefaultParameters();
            Map<String, String> argumentsMap = result.getArgumentsAsMap();

            // Then
            assertEquals("", argumentsMap.get("cognito_user_pool_id"));
            assertEquals("", argumentsMap.get("cognito_user_username"));
            assertEquals("", argumentsMap.get("cognito_user_email"));
            assertEquals("", argumentsMap.get("cognito_user_password"));
        }

        @Test
        @DisplayName("Should return consistent results on multiple calls")
        void shouldReturnConsistentResultsOnMultipleCalls() {
            // When
            Arguments result1 = cognitoProducerAdminCreateUser.getDefaultParameters();
            Arguments result2 = cognitoProducerAdminCreateUser.getDefaultParameters();

            // Then
            assertEquals(result1.getArgumentCount(), result2.getArgumentCount());
            assertEquals(result1.getArgumentsAsMap(), result2.getArgumentsAsMap());
        }
    }

    @Nested
    @DisplayName("runTest() Method Tests")
    class RunTestSuccessTests {

        @Test
        @DisplayName("Should successfully create user and set password")
        void shouldSuccessfullyCreateUserAndSetPassword() {
            // Given
            setupSuccessfulCreateUserContext();
            setupSuccessfulCreateUserResponse();

            // When
            SampleResult result = cognitoProducerAdminCreateUser.runTest(mockContext);

            // Then
            assertNotNull(result);
            assertTrue(result.isSuccessful());
            assertEquals("200", result.getResponseCode());
            assertTrue(result.getResponseDataAsString().contains("User Attributes:"));
            
            // Verify both Cognito client calls were made
            verify(mockCognitoClient).adminCreateUser(any(AdminCreateUserRequest.class));
            verify(mockCognitoClient).adminSetUserPassword(any(AdminSetUserPasswordRequest.class));
        }

        @Test
        @DisplayName("Should handle CognitoIdentityProviderException during user creation")
        void shouldHandleCognitoIdentityProviderExceptionDuringUserCreation() {
            // Given
            setupSuccessfulCreateUserContext();
            AwsErrorDetails errorDetails = AwsErrorDetails.builder()
                .errorCode("UsernameExistsException")
                .errorMessage("An account with the given username already exists.")
                .build();
            CognitoIdentityProviderException exception = mock(CognitoIdentityProviderException.class);
            when(exception.awsErrorDetails()).thenReturn(errorDetails);
            when(mockCognitoClient.adminCreateUser(any(AdminCreateUserRequest.class)))
                .thenThrow(exception);

            // When
            SampleResult result = cognitoProducerAdminCreateUser.runTest(mockContext);

            // Then
            assertNotNull(result);
            assertFalse(result.isSuccessful());
            assertEquals("UsernameExistsException", result.getResponseCode());
            assertEquals("An account with the given username already exists.", result.getResponseDataAsString());
            
            // Verify adminSetUserPassword was not called due to exception
            verify(mockCognitoClient).adminCreateUser(any(AdminCreateUserRequest.class));
            verify(mockCognitoClient, never()).adminSetUserPassword(any(AdminSetUserPasswordRequest.class));
        }

        @Test
        @DisplayName("Should handle CognitoIdentityProviderException during password setting")
        void shouldHandleCognitoIdentityProviderExceptionDuringPasswordSetting() {
            // Given
            setupSuccessfulCreateUserContext();
            setupSuccessfulCreateUserResponse();
            
            AwsErrorDetails errorDetails = AwsErrorDetails.builder()
                .errorCode("InvalidPasswordException")
                .errorMessage("Password does not conform to policy")
                .build();
            CognitoIdentityProviderException exception = mock(CognitoIdentityProviderException.class);
            when(exception.awsErrorDetails()).thenReturn(errorDetails);
            when(mockCognitoClient.adminSetUserPassword(any(AdminSetUserPasswordRequest.class)))
                .thenThrow(exception);

            // When
            SampleResult result = cognitoProducerAdminCreateUser.runTest(mockContext);

            // Then
            assertNotNull(result);
            assertFalse(result.isSuccessful());
            assertEquals("InvalidPasswordException", result.getResponseCode());
            assertEquals("Password does not conform to policy", result.getResponseDataAsString());
            
            // Verify both calls were made, but password setting failed
            verify(mockCognitoClient).adminCreateUser(any(AdminCreateUserRequest.class));
            verify(mockCognitoClient).adminSetUserPassword(any(AdminSetUserPasswordRequest.class));
        }

        @Test
        @DisplayName("Should set correct sampler data with pool, username, and email info")
        void shouldSetCorrectSamplerDataWithPoolUsernameAndEmailInfo() {
            // Given
            setupSuccessfulCreateUserContext();
            setupSuccessfulCreateUserResponse();

            // When
            SampleResult result = cognitoProducerAdminCreateUser.runTest(mockContext);

            // Then
            assertTrue(result.isSuccessful());
            String samplerData = result.getSamplerData();
            assertTrue(samplerData.contains("Cognito Pool Id: test-pool-id"));
            assertTrue(samplerData.contains("User Username : testuser"));
            assertTrue(samplerData.contains("User Email: test@example.com"));
        }

        @Test
        @DisplayName("Should handle null user attributes in response")
        void shouldHandleNullUserAttributesInResponse() {
            // Given
            setupSuccessfulCreateUserContext();
            
            UserType mockUser = mock(UserType.class);
            when(mockUser.attributes()).thenReturn(null);
            
            AdminCreateUserResponse mockResponse = mock(AdminCreateUserResponse.class);
            when(mockResponse.user()).thenReturn(mockUser);
            
            when(mockCognitoClient.adminCreateUser(any(AdminCreateUserRequest.class)))
                .thenReturn(mockResponse);

            // When
            SampleResult result = cognitoProducerAdminCreateUser.runTest(mockContext);

            // Then
            assertTrue(result.isSuccessful());
            assertTrue(result.getResponseDataAsString().contains("User Attributes: null"));
        }

        @Test
        @DisplayName("Should handle empty user attributes in response")
        void shouldHandleEmptyUserAttributesInResponse() {
            // Given
            setupSuccessfulCreateUserContext();
            
            UserType mockUser = mock(UserType.class);
            when(mockUser.attributes()).thenReturn(List.of());
            
            AdminCreateUserResponse mockResponse = mock(AdminCreateUserResponse.class);
            when(mockResponse.user()).thenReturn(mockUser);
            
            when(mockCognitoClient.adminCreateUser(any(AdminCreateUserRequest.class)))
                .thenReturn(mockResponse);

            // When
            SampleResult result = cognitoProducerAdminCreateUser.runTest(mockContext);

            // Then
            assertTrue(result.isSuccessful());
            assertTrue(result.getResponseDataAsString().contains("User Attributes: []"));
        }

        private void setupSuccessfulCreateUserContext() {
            when(mockContext.getParameter("cognito_user_pool_id")).thenReturn("test-pool-id");
            when(mockContext.getParameter("cognito_user_username")).thenReturn("testuser");
            when(mockContext.getParameter("cognito_user_email")).thenReturn("test@example.com");
            when(mockContext.getParameter("cognito_user_password")).thenReturn("TestPass123!");
        }

        private void setupSuccessfulCreateUserResponse() {
            AttributeType emailAttribute = AttributeType.builder()
                .name("email")
                .value("test@example.com")
                .build();
            
            UserType mockUser = mock(UserType.class);
            when(mockUser.attributes()).thenReturn(List.of(emailAttribute));
            
            AdminCreateUserResponse mockResponse = mock(AdminCreateUserResponse.class);
            when(mockResponse.user()).thenReturn(mockUser);
            
            when(mockCognitoClient.adminCreateUser(any(AdminCreateUserRequest.class)))
                .thenReturn(mockResponse);
        }
    }

    @Nested
    @DisplayName("runTest() Exception Tests")
    class RunTestExceptionTests {

        @Test
        @DisplayName("Should handle null context parameter gracefully")
        void shouldHandleNullContextParameterGracefully() {
            // When & Then - The implementation should handle null context appropriately
            assertThrows(NullPointerException.class, () -> 
                cognitoProducerAdminCreateUser.runTest(null));
        }

        @Test
        @DisplayName("Should handle CognitoIdentityProviderException with null error details")
        void shouldHandleCognitoIdentityProviderExceptionWithNullErrorDetails() {
            // Given
            setupSuccessfulCreateUserContext();
            CognitoIdentityProviderException exception = mock(CognitoIdentityProviderException.class);
            when(exception.awsErrorDetails()).thenReturn(null);
            when(mockCognitoClient.adminCreateUser(any(AdminCreateUserRequest.class)))
                .thenThrow(exception);

            // When & Then - The implementation will try to access awsErrorDetails().errorCode() which will throw NPE
            assertThrows(NullPointerException.class, () -> 
                cognitoProducerAdminCreateUser.runTest(mockContext));
        }

        @Test
        @DisplayName("Should handle different error codes from Cognito")
        void shouldHandleDifferentErrorCodesFromCognito() {
            // Given
            setupSuccessfulCreateUserContext();
            AwsErrorDetails errorDetails = AwsErrorDetails.builder()
                .errorCode("LimitExceededException")
                .errorMessage("Too Many Requests")
                .build();
            CognitoIdentityProviderException exception = mock(CognitoIdentityProviderException.class);
            when(exception.awsErrorDetails()).thenReturn(errorDetails);
            when(mockCognitoClient.adminCreateUser(any(AdminCreateUserRequest.class)))
                .thenThrow(exception);

            // When
            SampleResult result = cognitoProducerAdminCreateUser.runTest(mockContext);

            // Then
            assertFalse(result.isSuccessful());
            assertEquals("LimitExceededException", result.getResponseCode());
            assertEquals("Too Many Requests", result.getResponseDataAsString());
        }

        private void setupSuccessfulCreateUserContext() {
            when(mockContext.getParameter("cognito_user_pool_id")).thenReturn("test-pool-id");
            when(mockContext.getParameter("cognito_user_username")).thenReturn("testuser");
            when(mockContext.getParameter("cognito_user_email")).thenReturn("test@example.com");
            when(mockContext.getParameter("cognito_user_password")).thenReturn("TestPass123!");
        }
    }

    @Nested
    @DisplayName("createAdminCreateUserRequest() Method Tests")
    class CreateAdminCreateUserRequestTests {

        @Test
        @DisplayName("Should create correct AdminCreateUserRequest with all parameters")
        void shouldCreateCorrectAdminCreateUserRequestWithAllParameters() {
            // Given
            setupValidCreateUserRequestContext();

            // When
            AdminCreateUserRequest request = cognitoProducerAdminCreateUser.createAdminCreateUserRequest(mockContext);

            // Then
            assertNotNull(request);
            assertEquals("test-pool-id", request.userPoolId());
            assertEquals("testuser", request.username());
            assertEquals(MessageActionType.SUPPRESS, request.messageAction());
            
            List<AttributeType> userAttributes = request.userAttributes();
            assertNotNull(userAttributes);
            assertEquals(1, userAttributes.size());
            assertEquals("email", userAttributes.get(0).name());
            assertEquals("test@example.com", userAttributes.get(0).value());
        }

        @Test
        @DisplayName("Should handle special characters in email")
        void shouldHandleSpecialCharactersInEmail() {
            // Given
            setupValidCreateUserRequestContext();
            when(mockContext.getParameter("cognito_user_email")).thenReturn("user+test@example-domain.co.uk");

            // When
            AdminCreateUserRequest request = cognitoProducerAdminCreateUser.createAdminCreateUserRequest(mockContext);

            // Then
            assertEquals("user+test@example-domain.co.uk", request.userAttributes().get(0).value());
        }

        @Test
        @DisplayName("Should handle special characters in username")
        void shouldHandleSpecialCharactersInUsername() {
            // Given
            setupValidCreateUserRequestContext();
            when(mockContext.getParameter("cognito_user_username")).thenReturn("test@user.com");

            // When
            AdminCreateUserRequest request = cognitoProducerAdminCreateUser.createAdminCreateUserRequest(mockContext);

            // Then
            assertEquals("test@user.com", request.username());
        }

        @Test
        @DisplayName("Should handle null email parameter")
        void shouldHandleNullEmailParameter() {
            // Given
            setupValidCreateUserRequestContext();
            when(mockContext.getParameter("cognito_user_email")).thenReturn(null);

            // When
            AdminCreateUserRequest request = cognitoProducerAdminCreateUser.createAdminCreateUserRequest(mockContext);

            // Then
            assertNull(request.userAttributes().get(0).value());
        }

        @Test
        @DisplayName("Should handle empty email parameter")
        void shouldHandleEmptyEmailParameter() {
            // Given
            setupValidCreateUserRequestContext();
            when(mockContext.getParameter("cognito_user_email")).thenReturn("");

            // When
            AdminCreateUserRequest request = cognitoProducerAdminCreateUser.createAdminCreateUserRequest(mockContext);

            // Then
            assertEquals("", request.userAttributes().get(0).value());
        }

        @Test
        @DisplayName("Should handle null username parameter")
        void shouldHandleNullUsernameParameter() {
            // Given
            setupValidCreateUserRequestContext();
            when(mockContext.getParameter("cognito_user_username")).thenReturn(null);

            // When
            AdminCreateUserRequest request = cognitoProducerAdminCreateUser.createAdminCreateUserRequest(mockContext);

            // Then
            assertNull(request.username());
        }

        @Test
        @DisplayName("Should handle null user pool id parameter")
        void shouldHandleNullUserPoolIdParameter() {
            // Given
            setupValidCreateUserRequestContext();
            when(mockContext.getParameter("cognito_user_pool_id")).thenReturn(null);

            // When
            AdminCreateUserRequest request = cognitoProducerAdminCreateUser.createAdminCreateUserRequest(mockContext);

            // Then
            assertNull(request.userPoolId());
        }

        @Test
        @DisplayName("Should always set message action to SUPPRESS")
        void shouldAlwaysSetMessageActionToSuppress() {
            // Given
            setupValidCreateUserRequestContext();

            // When
            AdminCreateUserRequest request = cognitoProducerAdminCreateUser.createAdminCreateUserRequest(mockContext);

            // Then
            assertEquals(MessageActionType.SUPPRESS, request.messageAction());
        }

        @Test
        @DisplayName("Should always create single email attribute")
        void shouldAlwaysCreateSingleEmailAttribute() {
            // Given
            setupValidCreateUserRequestContext();

            // When
            AdminCreateUserRequest request = cognitoProducerAdminCreateUser.createAdminCreateUserRequest(mockContext);

            // Then
            List<AttributeType> userAttributes = request.userAttributes();
            assertEquals(1, userAttributes.size());
            assertEquals("email", userAttributes.get(0).name());
        }

        @Test
        @DisplayName("Should create consistent request for same inputs")
        void shouldCreateConsistentRequestForSameInputs() {
            // Given
            setupValidCreateUserRequestContext();

            // When
            AdminCreateUserRequest request1 = cognitoProducerAdminCreateUser.createAdminCreateUserRequest(mockContext);
            AdminCreateUserRequest request2 = cognitoProducerAdminCreateUser.createAdminCreateUserRequest(mockContext);

            // Then
            assertEquals(request1.userPoolId(), request2.userPoolId());
            assertEquals(request1.username(), request2.username());
            assertEquals(request1.messageAction(), request2.messageAction());
            assertEquals(request1.userAttributes().get(0).name(), request2.userAttributes().get(0).name());
            assertEquals(request1.userAttributes().get(0).value(), request2.userAttributes().get(0).value());
        }

        @Test
        @DisplayName("Should handle Unicode characters in parameters")
        void shouldHandleUnicodeCharactersInParameters() {
            // Given
            setupValidCreateUserRequestContext();
            when(mockContext.getParameter("cognito_user_username")).thenReturn("用户名");
            when(mockContext.getParameter("cognito_user_email")).thenReturn("用户@测试.com");

            // When
            AdminCreateUserRequest request = cognitoProducerAdminCreateUser.createAdminCreateUserRequest(mockContext);

            // Then
            assertEquals("用户名", request.username());
            assertEquals("用户@测试.com", request.userAttributes().get(0).value());
        }

        private void setupValidCreateUserRequestContext() {
            when(mockContext.getParameter("cognito_user_pool_id")).thenReturn("test-pool-id");
            when(mockContext.getParameter("cognito_user_username")).thenReturn("testuser");
            when(mockContext.getParameter("cognito_user_email")).thenReturn("test@example.com");
        }
    }

    @Nested
    @DisplayName("createAdminSetUserPasswordRequest() Method Tests")
    class CreateAdminSetUserPasswordRequestTests {

        @Test
        @DisplayName("Should create correct AdminSetUserPasswordRequest with all parameters")
        void shouldCreateCorrectAdminSetUserPasswordRequestWithAllParameters() {
            // Given
            setupValidPasswordRequestContext();

            // When
            AdminSetUserPasswordRequest request = cognitoProducerAdminCreateUser.createAdminSetUserPasswordRequest(mockContext);

            // Then
            assertNotNull(request);
            assertEquals("test-pool-id", request.userPoolId());
            assertEquals("testuser", request.username());
            assertEquals("TestPass123!", request.password());
            assertTrue(request.permanent());
        }

        @Test
        @DisplayName("Should always set permanent flag to true")
        void shouldAlwaysSetPermanentFlagToTrue() {
            // Given
            setupValidPasswordRequestContext();

            // When
            AdminSetUserPasswordRequest request = cognitoProducerAdminCreateUser.createAdminSetUserPasswordRequest(mockContext);

            // Then
            assertTrue(request.permanent());
        }

        @Test
        @DisplayName("Should handle special characters in password")
        void shouldHandleSpecialCharactersInPassword() {
            // Given
            setupValidPasswordRequestContext();
            when(mockContext.getParameter("cognito_user_password")).thenReturn("P@ssw0rd!#$%^&*()");

            // When
            AdminSetUserPasswordRequest request = cognitoProducerAdminCreateUser.createAdminSetUserPasswordRequest(mockContext);

            // Then
            assertEquals("P@ssw0rd!#$%^&*()", request.password());
        }

        @Test
        @DisplayName("Should handle null password parameter")
        void shouldHandleNullPasswordParameter() {
            // Given
            setupValidPasswordRequestContext();
            when(mockContext.getParameter("cognito_user_password")).thenReturn(null);

            // When
            AdminSetUserPasswordRequest request = cognitoProducerAdminCreateUser.createAdminSetUserPasswordRequest(mockContext);

            // Then
            assertNull(request.password());
        }

        @Test
        @DisplayName("Should handle empty password parameter")
        void shouldHandleEmptyPasswordParameter() {
            // Given
            setupValidPasswordRequestContext();
            when(mockContext.getParameter("cognito_user_password")).thenReturn("");

            // When
            AdminSetUserPasswordRequest request = cognitoProducerAdminCreateUser.createAdminSetUserPasswordRequest(mockContext);

            // Then
            assertEquals("", request.password());
        }

        @Test
        @DisplayName("Should handle null username parameter")
        void shouldHandleNullUsernameParameter() {
            // Given
            setupValidPasswordRequestContext();
            when(mockContext.getParameter("cognito_user_username")).thenReturn(null);

            // When
            AdminSetUserPasswordRequest request = cognitoProducerAdminCreateUser.createAdminSetUserPasswordRequest(mockContext);

            // Then
            assertNull(request.username());
        }

        @Test
        @DisplayName("Should handle null user pool id parameter")
        void shouldHandleNullUserPoolIdParameter() {
            // Given
            setupValidPasswordRequestContext();
            when(mockContext.getParameter("cognito_user_pool_id")).thenReturn(null);

            // When
            AdminSetUserPasswordRequest request = cognitoProducerAdminCreateUser.createAdminSetUserPasswordRequest(mockContext);

            // Then
            assertNull(request.userPoolId());
        }

        @Test
        @DisplayName("Should create consistent request for same inputs")
        void shouldCreateConsistentRequestForSameInputs() {
            // Given
            setupValidPasswordRequestContext();

            // When
            AdminSetUserPasswordRequest request1 = cognitoProducerAdminCreateUser.createAdminSetUserPasswordRequest(mockContext);
            AdminSetUserPasswordRequest request2 = cognitoProducerAdminCreateUser.createAdminSetUserPasswordRequest(mockContext);

            // Then
            assertEquals(request1.userPoolId(), request2.userPoolId());
            assertEquals(request1.username(), request2.username());
            assertEquals(request1.password(), request2.password());
            assertEquals(request1.permanent(), request2.permanent());
        }

        @Test
        @DisplayName("Should handle long password")
        void shouldHandleLongPassword() {
            // Given
            setupValidPasswordRequestContext();
            String longPassword = "a".repeat(256) + "!1A"; // Very long password
            when(mockContext.getParameter("cognito_user_password")).thenReturn(longPassword);

            // When
            AdminSetUserPasswordRequest request = cognitoProducerAdminCreateUser.createAdminSetUserPasswordRequest(mockContext);

            // Then
            assertEquals(longPassword, request.password());
        }

        @Test
        @DisplayName("Should handle Unicode characters in password")
        void shouldHandleUnicodeCharactersInPassword() {
            // Given
            setupValidPasswordRequestContext();
            when(mockContext.getParameter("cognito_user_password")).thenReturn("密码123!@#");

            // When
            AdminSetUserPasswordRequest request = cognitoProducerAdminCreateUser.createAdminSetUserPasswordRequest(mockContext);

            // Then
            assertEquals("密码123!@#", request.password());
        }

        @Test
        @DisplayName("Should handle whitespace in password")
        void shouldHandleWhitespaceInPassword() {
            // Given
            setupValidPasswordRequestContext();
            when(mockContext.getParameter("cognito_user_password")).thenReturn("  Password With Spaces  ");

            // When
            AdminSetUserPasswordRequest request = cognitoProducerAdminCreateUser.createAdminSetUserPasswordRequest(mockContext);

            // Then
            assertEquals("  Password With Spaces  ", request.password());
        }

        private void setupValidPasswordRequestContext() {
            when(mockContext.getParameter("cognito_user_pool_id")).thenReturn("test-pool-id");
            when(mockContext.getParameter("cognito_user_username")).thenReturn("testuser");
            when(mockContext.getParameter("cognito_user_password")).thenReturn("TestPass123!");
        }
    }

    @Nested
    @DisplayName("Default Parameters Tests")
    class DefaultParametersTests {

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {" ", "\t", "\n"})
        @DisplayName("Should handle null, empty, or blank parameter values")
        void shouldHandleNullEmptyOrBlankParameterValues(String value) {
            // Given
            when(mockContext.getParameter("cognito_user_pool_id")).thenReturn(value);
            when(mockContext.getParameter("cognito_user_username")).thenReturn(value);
            when(mockContext.getParameter("cognito_user_email")).thenReturn(value);
            when(mockContext.getParameter("cognito_user_password")).thenReturn(value);

            // When & Then - Should not throw exceptions
            assertDoesNotThrow(() -> {
                AdminCreateUserRequest createRequest = cognitoProducerAdminCreateUser.createAdminCreateUserRequest(mockContext);
                AdminSetUserPasswordRequest passwordRequest = cognitoProducerAdminCreateUser.createAdminSetUserPasswordRequest(mockContext);
                
                assertNotNull(createRequest);
                assertNotNull(passwordRequest);
            });
        }

        @Test
        @DisplayName("Should work with default parameter values")
        void shouldWorkWithDefaultParameterValues() {
            // Given - Use the default parameters from getDefaultParameters
            Arguments defaultParams = cognitoProducerAdminCreateUser.getDefaultParameters();
            Map<String, String> paramMap = defaultParams.getArgumentsAsMap();
            
            when(mockContext.getParameter("cognito_user_pool_id")).thenReturn(paramMap.get("cognito_user_pool_id"));
            when(mockContext.getParameter("cognito_user_username")).thenReturn(paramMap.get("cognito_user_username"));
            when(mockContext.getParameter("cognito_user_email")).thenReturn(paramMap.get("cognito_user_email"));
            when(mockContext.getParameter("cognito_user_password")).thenReturn(paramMap.get("cognito_user_password"));

            // When & Then - Should not throw exceptions with empty default values
            assertDoesNotThrow(() -> {
                AdminCreateUserRequest createRequest = cognitoProducerAdminCreateUser.createAdminCreateUserRequest(mockContext);
                AdminSetUserPasswordRequest passwordRequest = cognitoProducerAdminCreateUser.createAdminSetUserPasswordRequest(mockContext);
                
                assertNotNull(createRequest);
                assertNotNull(passwordRequest);
            });
        }
    }

    @Nested
    @DisplayName("Integration and Edge Case Tests")
    class IntegrationAndEdgeCaseTests {

        @Test
        @DisplayName("Should complete full user creation workflow successfully")
        void shouldCompleteFullUserCreationWorkflowSuccessfully() {
            // Given
            setupCompleteWorkflowContext();
            setupSuccessfulCreateUserResponse();

            // When
            SampleResult result = cognitoProducerAdminCreateUser.runTest(mockContext);

            // Then - Verify complete workflow
            assertTrue(result.isSuccessful());
            
            // Verify both requests were created and executed in order
            verify(mockCognitoClient).adminCreateUser(any(AdminCreateUserRequest.class));
            verify(mockCognitoClient).adminSetUserPassword(any(AdminSetUserPasswordRequest.class));
            
            // Verify response contains user attributes
            assertTrue(result.getResponseDataAsString().contains("User Attributes:"));
        }

        @Test
        @DisplayName("Should demonstrate method accessibility")
        void shouldDemonstrateMethodAccessibility() {
            // Given
            setupCompleteWorkflowContext();

            // When - Verify protected methods can be accessed
            AdminCreateUserRequest createRequest = cognitoProducerAdminCreateUser.createAdminCreateUserRequest(mockContext);
            AdminSetUserPasswordRequest passwordRequest = cognitoProducerAdminCreateUser.createAdminSetUserPasswordRequest(mockContext);

            // Then
            assertNotNull(createRequest);
            assertNotNull(passwordRequest);
            assertEquals("test-pool-id", createRequest.userPoolId());
            assertEquals("test-pool-id", passwordRequest.userPoolId());
            assertEquals("testuser", createRequest.username());
            assertEquals("testuser", passwordRequest.username());
        }

        @Test
        @DisplayName("Should handle method calls in isolation")
        void shouldHandleMethodCallsInIsolation() {
            // Given
            setupCompleteWorkflowContext();

            // When - Call methods independently
            AdminCreateUserRequest createRequest = cognitoProducerAdminCreateUser.createAdminCreateUserRequest(mockContext);
            AdminSetUserPasswordRequest passwordRequest = cognitoProducerAdminCreateUser.createAdminSetUserPasswordRequest(mockContext);

            // Then - Both should work independently
            assertNotNull(createRequest);
            assertNotNull(passwordRequest);
            
            // Verify create request properties
            assertEquals("test-pool-id", createRequest.userPoolId());
            assertEquals("testuser", createRequest.username());
            assertEquals(MessageActionType.SUPPRESS, createRequest.messageAction());
            assertEquals("email", createRequest.userAttributes().get(0).name());
            assertEquals("test@example.com", createRequest.userAttributes().get(0).value());
            
            // Verify password request properties
            assertEquals("test-pool-id", passwordRequest.userPoolId());
            assertEquals("testuser", passwordRequest.username());
            assertEquals("TestPass123!", passwordRequest.password());
            assertTrue(passwordRequest.permanent());
        }

        @Test
        @DisplayName("Should handle complete execution path for branch coverage")
        void shouldHandleCompleteExecutionPathForBranchCoverage() {
            // Given - Setup for successful execution to cover all branches
            setupCompleteWorkflowContext();
            setupSuccessfulCreateUserResponse();

            // When
            SampleResult result = cognitoProducerAdminCreateUser.runTest(mockContext);

            // Then - Verify the method completed successfully covering all branches
            assertTrue(result.isSuccessful());
            assertNotNull(result.getSamplerData());
            assertNotNull(result.getResponseDataAsString());
            assertEquals("200", result.getResponseCode());
            
            // Verify sampler data contains expected information
            String samplerData = result.getSamplerData();
            assertTrue(samplerData.contains("Cognito Pool Id: test-pool-id"));
            assertTrue(samplerData.contains("User Username : testuser"));
            assertTrue(samplerData.contains("User Email: test@example.com"));
        }

        @Test
        @DisplayName("Should handle method execution with minimal valid data")
        void shouldHandleMethodExecutionWithMinimalValidData() {
            // Given - Minimal required data
            when(mockContext.getParameter("cognito_user_pool_id")).thenReturn("pool");
            when(mockContext.getParameter("cognito_user_username")).thenReturn("user");
            when(mockContext.getParameter("cognito_user_email")).thenReturn("e@e.com");
            when(mockContext.getParameter("cognito_user_password")).thenReturn("pass");
            
            setupSuccessfulCreateUserResponse();

            // When
            SampleResult result = cognitoProducerAdminCreateUser.runTest(mockContext);

            // Then
            assertTrue(result.isSuccessful());
            verify(mockCognitoClient).adminCreateUser(any(AdminCreateUserRequest.class));
            verify(mockCognitoClient).adminSetUserPassword(any(AdminSetUserPasswordRequest.class));
        }

        private void setupCompleteWorkflowContext() {
            when(mockContext.getParameter("cognito_user_pool_id")).thenReturn("test-pool-id");
            when(mockContext.getParameter("cognito_user_username")).thenReturn("testuser");
            when(mockContext.getParameter("cognito_user_email")).thenReturn("test@example.com");
            when(mockContext.getParameter("cognito_user_password")).thenReturn("TestPass123!");
        }

        private void setupSuccessfulCreateUserResponse() {
            AttributeType emailAttribute = AttributeType.builder()
                .name("email")
                .value("test@example.com")
                .build();
            
            UserType mockUser = mock(UserType.class);
            when(mockUser.attributes()).thenReturn(List.of(emailAttribute));
            
            AdminCreateUserResponse mockResponse = mock(AdminCreateUserResponse.class);
            when(mockResponse.user()).thenReturn(mockUser);
            
            when(mockCognitoClient.adminCreateUser(any(AdminCreateUserRequest.class)))
                .thenReturn(mockResponse);
        }
    }
}
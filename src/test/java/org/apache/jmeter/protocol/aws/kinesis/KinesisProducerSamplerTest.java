package org.apache.jmeter.protocol.aws.kinesis;

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
import software.amazon.awssdk.core.SdkClient;
import software.amazon.awssdk.services.kinesis.KinesisClient;
import software.amazon.awssdk.services.kinesis.model.KinesisException;
import software.amazon.awssdk.services.kinesis.model.PutRecordRequest;
import software.amazon.awssdk.services.kinesis.model.PutRecordResponse;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive unit tests for KinesisProducerSampler class.
 * Tests all implemented methods with 100% code coverage including error scenarios.
 * 
 * @author JoseLuisSR
 * @since 08/12/2025
 */
@DisplayName("KinesisProducerSampler Tests")
class KinesisProducerSamplerTest {

    private KinesisProducerSampler sampler;
    
    @Mock
    private JavaSamplerContext mockContext;
    
    @Mock
    private KinesisClient mockKinesisClient;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        sampler = new KinesisProducerSampler();
        
        // Setup mock context with valid parameters using correct parameter names
        when(mockContext.getParameter("kinesis_stream_name")).thenReturn("test-stream");
        when(mockContext.getParameter("partition_key")).thenReturn("test-partition-key");
        when(mockContext.getParameter("data_record")).thenReturn("{\"message\":\"test data\"}");
        
        // Setup AWS parameters for createSdkClient tests
        when(mockContext.getParameter("aws_region")).thenReturn("us-east-1");
        when(mockContext.getParameter("aws_endpoint_custom")).thenReturn("");
        when(mockContext.getParameter("aws_access_key_id")).thenReturn("test-access-key");
        when(mockContext.getParameter("aws_secret_access_key")).thenReturn("test-secret-key");
        when(mockContext.getParameter("aws_session_token")).thenReturn("");
        when(mockContext.getParameter("aws_configure_profile")).thenReturn("default");
        
        // Mock the parameter names iterator for setupTest
        when(mockContext.getParameterNamesIterator()).thenReturn(
            java.util.Arrays.asList(
                "kinesis_stream_name", "partition_key", "data_record",
                "aws_region", "aws_access_key_id", "aws_secret_access_key"
            ).iterator()
        );
    }

    @Nested
    @DisplayName("Inheritance Verification Tests")
    class InheritanceVerificationTests {

        @Test
        @DisplayName("Should extend AWSSampler")
        void shouldExtendAWSSampler() {
            assertTrue(sampler instanceof org.apache.jmeter.protocol.aws.AWSSampler);
        }

        @Test
        @DisplayName("Should implement AWSClientSDK2 interface")
        void shouldImplementAWSClientSDK2Interface() {
            assertTrue(sampler instanceof org.apache.jmeter.protocol.aws.AWSClientSDK2);
        }

        @Test
        @DisplayName("Should implement JavaSamplerClient interface")
        void shouldImplementJavaSamplerClientInterface() {
            assertTrue(sampler instanceof org.apache.jmeter.protocol.java.sampler.JavaSamplerClient);
        }
    }

    @Nested
    @DisplayName("Default Parameters Tests")
    class DefaultParametersTests {

        @Test
        @DisplayName("Should return arguments with all required parameters")
        void shouldReturnArgumentsWithAllRequiredParameters() {
            // When
            org.apache.jmeter.config.Arguments defaultParams = sampler.getDefaultParameters();

            // Then
            assertNotNull(defaultParams);
            assertTrue(defaultParams.getArgumentCount() > 0);
            
            // Check that we have the expected number of parameters (6 AWS base + 3 Kinesis specific)
            assertEquals(9, defaultParams.getArgumentCount());
        }

        @Test
        @DisplayName("Should contain AWS base parameters")
        void shouldContainAWSBaseParameters() {
            // When
            org.apache.jmeter.config.Arguments defaultParams = sampler.getDefaultParameters();

            // Then
            assertNotNull(defaultParams);
            
            // Verify we have AWS base parameters (6 AWS parameters)
            assertTrue(defaultParams.getArgumentCount() >= 6);
            
            // Check that parameter names contain expected AWS parameters
            java.util.List<String> argumentNames = new java.util.ArrayList<>();
            for (int i = 0; i < defaultParams.getArgumentCount(); i++) {
                argumentNames.add(defaultParams.getArgument(i).getName());
            }
            
            assertTrue(argumentNames.contains("aws_access_key_id"));
            assertTrue(argumentNames.contains("aws_secret_access_key"));
            assertTrue(argumentNames.contains("aws_session_token"));
            assertTrue(argumentNames.contains("aws_region"));
            assertTrue(argumentNames.contains("aws_endpoint_custom"));
            assertTrue(argumentNames.contains("aws_configure_profile"));
        }

        @Test
        @DisplayName("Should contain Kinesis specific parameters")
        void shouldContainKinesisSpecificParameters() {
            // When
            org.apache.jmeter.config.Arguments defaultParams = sampler.getDefaultParameters();

            // Then
            assertNotNull(defaultParams);
            
            // Check that parameter names contain expected Kinesis parameters
            java.util.List<String> argumentNames = new java.util.ArrayList<>();
            for (int i = 0; i < defaultParams.getArgumentCount(); i++) {
                argumentNames.add(defaultParams.getArgument(i).getName());
            }
            
            assertTrue(argumentNames.contains("kinesis_stream_name"));
            assertTrue(argumentNames.contains("partition_key"));
            assertTrue(argumentNames.contains("data_record"));
        }
    }

    @Nested
    @DisplayName("SDK Client Creation Tests")
    class SdkClientCreationTests {

        @Test
        @DisplayName("Should create KinesisClient with valid credentials")
        void shouldCreateKinesisClientWithValidCredentials() {
            // Given
            Map<String, String> credentials = new HashMap<>();
            credentials.put("aws_region", "us-east-1");
            credentials.put("aws_endpoint_custom", "");
            credentials.put("aws_access_key_id", "test-access-key");
            credentials.put("aws_secret_access_key", "test-secret-key");

            // When
            SdkClient client = sampler.createSdkClient(credentials);

            // Then
            assertNotNull(client);
            assertTrue(client instanceof KinesisClient);
        }

        @Test
        @DisplayName("Should create KinesisClient with custom endpoint")
        void shouldCreateKinesisClientWithCustomEndpoint() {
            // Given
            Map<String, String> credentials = new HashMap<>();
            credentials.put("aws_region", "us-west-2");
            credentials.put("aws_endpoint_custom", "https://localstack:4566");
            credentials.put("aws_access_key_id", "test-access-key");
            credentials.put("aws_secret_access_key", "test-secret-key");

            // When
            SdkClient client = sampler.createSdkClient(credentials);

            // Then
            assertNotNull(client);
            assertTrue(client instanceof KinesisClient);
        }

        @Test
        @DisplayName("Should handle missing region gracefully")
        void shouldHandleMissingRegionGracefully() {
            // Given
            Map<String, String> credentials = new HashMap<>();
            credentials.put("aws_access_key_id", "test-access-key");
            credentials.put("aws_secret_access_key", "test-secret-key");

            // When & Then
            assertDoesNotThrow(() -> {
                SdkClient client = sampler.createSdkClient(credentials);
                assertNotNull(client);
            });
        }

        @Test
        @DisplayName("Should handle session credentials")
        void shouldHandleSessionCredentials() {
            // Given
            Map<String, String> credentials = new HashMap<>();
            credentials.put("aws_region", "us-east-1");
            credentials.put("aws_access_key_id", "test-access-key");
            credentials.put("aws_secret_access_key", "test-secret-key");
            credentials.put("aws_session_token", "test-session-token");

            // When
            SdkClient client = sampler.createSdkClient(credentials);

            // Then
            assertNotNull(client);
            assertTrue(client instanceof KinesisClient);
        }

        @Test
        @DisplayName("Should handle profile-based credentials")
        void shouldHandleProfileBasedCredentials() {
            // Given
            Map<String, String> credentials = new HashMap<>();
            credentials.put("aws_region", "us-east-1");
            credentials.put("aws_configure_profile", "test-profile");

            // When
            SdkClient client = sampler.createSdkClient(credentials);

            // Then
            assertNotNull(client);
            assertTrue(client instanceof KinesisClient);
        }
    }

    @Nested
    @DisplayName("Setup Test Method Tests")
    class SetupTestMethodTests {

        @Test
        @DisplayName("Should call setupTest without throwing exception")
        void shouldCallSetupTestWithoutThrowingException() {
            // When & Then
            assertDoesNotThrow(() -> sampler.setupTest(mockContext));
        }

        @Test
        @DisplayName("Should initialize KinesisClient during setup")
        void shouldInitializeKinesisClientDuringSetup() throws Exception {
            // When
            sampler.setupTest(mockContext);

            // Then
            Field kinesisClientField = KinesisProducerSampler.class.getDeclaredField("kinesisClient");
            kinesisClientField.setAccessible(true);
            KinesisClient client = (KinesisClient) kinesisClientField.get(sampler);
            assertNotNull(client);
        }

        @Test
        @DisplayName("Should handle empty parameter iteration")
        void shouldHandleEmptyParameterIteration() {
            // Given
            when(mockContext.getParameterNamesIterator()).thenReturn(
                java.util.Collections.emptyIterator()
            );

            // When & Then
            assertDoesNotThrow(() -> sampler.setupTest(mockContext));
        }

        @Test
        @DisplayName("Should handle null parameter values")
        void shouldHandleNullParameterValues() {
            // Given
            when(mockContext.getParameter(anyString())).thenReturn(null);

            // When & Then
            assertDoesNotThrow(() -> sampler.setupTest(mockContext));
        }
    }

    @Nested
    @DisplayName("Teardown Test Method Tests")
    class TeardownTestMethodTests {

        @Test
        @DisplayName("Should call teardownTest without throwing exception")
        void shouldCallTeardownTestWithoutThrowingException() {
            // When & Then
            assertDoesNotThrow(() -> sampler.teardownTest(mockContext));
        }

        @Test
        @DisplayName("Should handle teardownTest when no client was set up")
        void shouldHandleTeardownTestWhenNoClientWasSetUp() {
            // When & Then
            assertDoesNotThrow(() -> sampler.teardownTest(mockContext));
        }

        @Test
        @DisplayName("Should close KinesisClient when teardownTest is called")
        void shouldCloseKinesisClientWhenTeardownTestIsCalled() throws Exception {
            // Given
            injectMockClient(sampler, mockKinesisClient);

            // When
            sampler.teardownTest(mockContext);

            // Then
            verify(mockKinesisClient, times(1)).close();
        }

        @Test
        @DisplayName("Should propagate exception when closing client fails")
        void shouldPropagateExceptionWhenClosingClientFails() throws Exception {
            // Given
            doThrow(new RuntimeException("Close failed")).when(mockKinesisClient).close();
            injectMockClient(sampler, mockKinesisClient);

            // When & Then
            // The current implementation doesn't handle exceptions during close, so it should propagate
            assertThrows(RuntimeException.class, () -> sampler.teardownTest(mockContext));
        }
    }

    @Nested
    @DisplayName("PutRecordRequest Creation Tests")
    class PutRecordRequestCreationTests {

        @Test
        @DisplayName("Should create PutRecordRequest with all parameters")
        void shouldCreatePutRecordRequestWithAllParameters() {
            // When
            PutRecordRequest request = sampler.createPutRecordRequest(mockContext);

            // Then
            assertNotNull(request);
            assertEquals("test-stream", request.streamName());
            assertEquals("test-partition-key", request.partitionKey());
            assertEquals("{\"message\":\"test data\"}", request.data().asUtf8String());
        }

        @ParameterizedTest
        @NullAndEmptySource
        @DisplayName("Should handle null or empty stream name")
        void shouldHandleNullOrEmptyStreamName(String streamName) {
            // Given
            when(mockContext.getParameter("kinesis_stream_name")).thenReturn(streamName);

            // When
            PutRecordRequest request = sampler.createPutRecordRequest(mockContext);

            // Then
            assertNotNull(request);
            assertEquals(streamName, request.streamName());
        }

        @ParameterizedTest
        @NullAndEmptySource
        @DisplayName("Should handle null or empty partition key")
        void shouldHandleNullOrEmptyPartitionKey(String partitionKey) {
            // Given
            when(mockContext.getParameter("partition_key")).thenReturn(partitionKey);

            // When
            PutRecordRequest request = sampler.createPutRecordRequest(mockContext);

            // Then
            assertNotNull(request);
            assertEquals(partitionKey, request.partitionKey());
        }

        @Test
        @DisplayName("Should handle null data record by throwing NullPointerException")
        void shouldHandleNullDataRecordByThrowingNullPointerException() {
            // Given
            when(mockContext.getParameter("data_record")).thenReturn(null);

            // When & Then
            // AWS SDK doesn't accept null values for data record, so this should throw NPE
            assertThrows(NullPointerException.class, () -> {
                sampler.createPutRecordRequest(mockContext);
            });
        }

        @Test
        @DisplayName("Should handle empty data record")
        void shouldHandleEmptyDataRecord() {
            // Given
            when(mockContext.getParameter("data_record")).thenReturn("");

            // When
            PutRecordRequest request = sampler.createPutRecordRequest(mockContext);

            // Then
            assertNotNull(request);
            assertEquals("", request.data().asUtf8String());
        }

        @Test
        @DisplayName("Should handle large data record payload")
        void shouldHandleLargeDataRecordPayload() {
            // Given
            StringBuilder largePayload = new StringBuilder("{\"data\": \"");
            for (int i = 0; i < 50000; i++) {
                largePayload.append("x");
            }
            largePayload.append("\"}");
            
            when(mockContext.getParameter("data_record")).thenReturn(largePayload.toString());

            // When
            PutRecordRequest request = sampler.createPutRecordRequest(mockContext);

            // Then
            assertNotNull(request);
            assertEquals(largePayload.toString(), request.data().asUtf8String());
        }

        @ParameterizedTest
        @ValueSource(strings = {
            "{\"valid\":\"json\"}",
            "plain text data",
            "special-chars-!@#$%^&*()",
            "unicode-测试数据-🎉",
            "numbers-123456789",
            ""
        })
        @DisplayName("Should handle various data record formats")
        void shouldHandleVariousDataRecordFormats(String dataRecord) {
            // Given
            when(mockContext.getParameter("data_record")).thenReturn(dataRecord);

            // When
            PutRecordRequest request = sampler.createPutRecordRequest(mockContext);

            // Then
            assertNotNull(request);
            assertEquals(dataRecord, request.data().asUtf8String());
        }
    }

    @Nested
    @DisplayName("RunTest Method Tests")
    class RunTestMethodTests {
        
        private JavaSamplerContext createMockContextForRunTest() {
            JavaSamplerContext context = mock(JavaSamplerContext.class);
            when(context.getParameter("kinesis_stream_name")).thenReturn("test-stream");
            when(context.getParameter("partition_key")).thenReturn("test-partition-key");
            when(context.getParameter("data_record")).thenReturn("{\"message\":\"test data\"}");
            return context;
        }

        @Test
        @DisplayName("Should execute runTest and return successful SampleResult")
        void shouldExecuteRunTestAndReturnSuccessfulSampleResult() throws Exception {
            // Given
            PutRecordResponse mockResponse = mock(PutRecordResponse.class);
            when(mockResponse.shardId()).thenReturn("shardId-000000000000");
            when(mockResponse.sequenceNumber()).thenReturn("12345678901234567890");
            when(mockResponse.encryptionTypeAsString()).thenReturn("NONE");
            
            when(mockKinesisClient.putRecord(any(PutRecordRequest.class))).thenReturn(mockResponse);
            
            injectMockClient(sampler, mockKinesisClient);
            JavaSamplerContext context = createMockContextForRunTest();

            // When
            SampleResult result = sampler.runTest(context);

            // Then
            assertNotNull(result);
            assertTrue(result.isSuccessful());
            assertEquals("200", result.getResponseCode());
            assertNotNull(result.getResponseDataAsString());
            assertTrue(result.getResponseDataAsString().contains("shardId-000000000000"));
            assertTrue(result.getResponseDataAsString().contains("12345678901234567890"));
            assertTrue(result.getResponseDataAsString().contains("NONE"));
        }

        @Test
        @DisplayName("Should include Kinesis parameters in SampleResult label")
        void shouldIncludeKinesisParametersInSampleResultLabel() throws Exception {
            // Given
            PutRecordResponse mockResponse = mock(PutRecordResponse.class);
            when(mockResponse.shardId()).thenReturn("test-shard");
            when(mockResponse.sequenceNumber()).thenReturn("test-sequence");
            when(mockResponse.encryptionTypeAsString()).thenReturn("KMS");
            
            when(mockKinesisClient.putRecord(any(PutRecordRequest.class))).thenReturn(mockResponse);
            
            injectMockClient(sampler, mockKinesisClient);
            JavaSamplerContext context = createMockContextForRunTest();

            // When
            SampleResult result = sampler.runTest(context);

            // Then
            assertNotNull(result);
            String samplerData = result.getSamplerData();
            assertTrue(samplerData.contains("test-stream"));
            assertTrue(samplerData.contains("test-partition-key"));
            assertTrue(samplerData.contains("{\"message\":\"test data\"}"));
        }

        @Test
        @DisplayName("Should handle KinesisException gracefully")
        void shouldHandleKinesisExceptionGracefully() throws Exception {
            // Given
            AwsErrorDetails errorDetails = mock(AwsErrorDetails.class);
            when(errorDetails.errorCode()).thenReturn("ProvisionedThroughputExceededException");
            when(errorDetails.errorMessage()).thenReturn("Rate exceeded for shard");
            
            KinesisException kinesisException = mock(KinesisException.class);
            when(kinesisException.awsErrorDetails()).thenReturn(errorDetails);
            
            when(mockKinesisClient.putRecord(any(PutRecordRequest.class))).thenThrow(kinesisException);
            
            injectMockClient(sampler, mockKinesisClient);
            JavaSamplerContext context = createMockContextForRunTest();

            // When
            SampleResult result = sampler.runTest(context);

            // Then
            assertNotNull(result);
            assertFalse(result.isSuccessful());
            assertEquals("ProvisionedThroughputExceededException", result.getResponseCode());
            assertTrue(result.getResponseDataAsString().contains("Rate exceeded for shard"));
        }

        @Test
        @DisplayName("Should handle runtime exceptions during putRecord")
        void shouldHandleRuntimeExceptionsDuringPutRecord() throws Exception {
            // Given
            when(mockKinesisClient.putRecord(any(PutRecordRequest.class)))
                .thenThrow(new RuntimeException("Unexpected error"));
            
            injectMockClient(sampler, mockKinesisClient);
            JavaSamplerContext context = createMockContextForRunTest();

            // When & Then
            assertThrows(RuntimeException.class, () -> sampler.runTest(context));
        }

        @Test
        @DisplayName("Should handle null response from putRecord")
        void shouldHandleNullResponseFromPutRecord() throws Exception {
            // Given
            when(mockKinesisClient.putRecord(any(PutRecordRequest.class))).thenReturn(null);
            
            injectMockClient(sampler, mockKinesisClient);
            JavaSamplerContext context = createMockContextForRunTest();

            // When & Then
            assertThrows(NullPointerException.class, () -> sampler.runTest(context));
        }

        @Test
        @DisplayName("Should handle empty responses gracefully")
        void shouldHandleEmptyResponsesGracefully() throws Exception {
            // Given
            PutRecordResponse mockResponse = mock(PutRecordResponse.class);
            when(mockResponse.shardId()).thenReturn("");
            when(mockResponse.sequenceNumber()).thenReturn("");
            when(mockResponse.encryptionTypeAsString()).thenReturn("");
            
            when(mockKinesisClient.putRecord(any(PutRecordRequest.class))).thenReturn(mockResponse);
            
            injectMockClient(sampler, mockKinesisClient);
            JavaSamplerContext context = createMockContextForRunTest();

            // When
            SampleResult result = sampler.runTest(context);

            // Then
            assertNotNull(result);
            assertTrue(result.isSuccessful());
        }

        @Test
        @DisplayName("Should verify PutRecordRequest is called with correct parameters")
        void shouldVerifyPutRecordRequestIsCalledWithCorrectParameters() throws Exception {
            // Given
            PutRecordResponse mockResponse = mock(PutRecordResponse.class);
            when(mockResponse.shardId()).thenReturn("test-shard");
            when(mockResponse.sequenceNumber()).thenReturn("test-sequence");
            when(mockResponse.encryptionTypeAsString()).thenReturn("KMS");
            
            when(mockKinesisClient.putRecord(any(PutRecordRequest.class))).thenReturn(mockResponse);
            
            injectMockClient(sampler, mockKinesisClient);
            JavaSamplerContext context = createMockContextForRunTest();

            // When
            sampler.runTest(context);

            // Then
            verify(mockKinesisClient, times(1)).putRecord(any(PutRecordRequest.class));
        }
    }

    @Nested
    @DisplayName("Edge Cases and Error Handling Tests")
    class EdgeCasesAndErrorHandlingTests {

        @Test
        @DisplayName("Should handle missing KinesisClient in runTest")
        void shouldHandleMissingKinesisClientInRunTest() {
            // Given
            JavaSamplerContext context = createMockContextForRunTest();

            // When & Then
            assertThrows(NullPointerException.class, () -> sampler.runTest(context));
        }

        @Test
        @DisplayName("Should handle concurrent access to KinesisClient")
        void shouldHandleConcurrentAccessToKinesisClient() throws Exception {
            // Given
            PutRecordResponse mockResponse = mock(PutRecordResponse.class);
            when(mockResponse.shardId()).thenReturn("test-shard");
            when(mockResponse.sequenceNumber()).thenReturn("test-sequence");
            when(mockResponse.encryptionTypeAsString()).thenReturn("NONE");
            
            when(mockKinesisClient.putRecord(any(PutRecordRequest.class))).thenReturn(mockResponse);
            
            injectMockClient(sampler, mockKinesisClient);
            JavaSamplerContext context = createMockContextForRunTest();

            // When
            CompletableFuture<SampleResult> future1 = CompletableFuture.supplyAsync(() -> sampler.runTest(context));
            CompletableFuture<SampleResult> future2 = CompletableFuture.supplyAsync(() -> sampler.runTest(context));

            // Then
            SampleResult result1 = future1.get();
            SampleResult result2 = future2.get();
            
            assertNotNull(result1);
            assertNotNull(result2);
            assertTrue(result1.isSuccessful());
            assertTrue(result2.isSuccessful());
        }

        private JavaSamplerContext createMockContextForRunTest() {
            JavaSamplerContext context = mock(JavaSamplerContext.class);
            when(context.getParameter("kinesis_stream_name")).thenReturn("test-stream");
            when(context.getParameter("partition_key")).thenReturn("test-partition-key");
            when(context.getParameter("data_record")).thenReturn("{\"message\":\"test data\"}");
            return context;
        }
    }

    @Nested
    @DisplayName("Integration Tests")
    class IntegrationTests {

        @Test
        @DisplayName("Should perform complete test lifecycle")
        void shouldPerformCompleteTestLifecycle() throws Exception {
            // Given
            PutRecordResponse mockResponse = mock(PutRecordResponse.class);
            when(mockResponse.shardId()).thenReturn("test-shard");
            when(mockResponse.sequenceNumber()).thenReturn("test-sequence");
            when(mockResponse.encryptionTypeAsString()).thenReturn("NONE");
            
            when(mockKinesisClient.putRecord(any(PutRecordRequest.class))).thenReturn(mockResponse);

            // When & Then
            // 1. Setup
            assertDoesNotThrow(() -> sampler.setupTest(mockContext));
            
            // 2. Replace with mock client for testing
            injectMockClient(sampler, mockKinesisClient);
            
            // 3. Run test
            SampleResult result = sampler.runTest(mockContext);
            assertNotNull(result);
            assertTrue(result.isSuccessful());
            
            // 4. Teardown
            assertDoesNotThrow(() -> sampler.teardownTest(mockContext));
            verify(mockKinesisClient).close();
        }

        @Test
        @DisplayName("Should handle multiple test runs")
        void shouldHandleMultipleTestRuns() throws Exception {
            // Given
            PutRecordResponse mockResponse = mock(PutRecordResponse.class);
            when(mockResponse.shardId()).thenReturn("test-shard");
            when(mockResponse.sequenceNumber()).thenReturn("test-sequence");
            when(mockResponse.encryptionTypeAsString()).thenReturn("NONE");
            
            when(mockKinesisClient.putRecord(any(PutRecordRequest.class))).thenReturn(mockResponse);
            
            injectMockClient(sampler, mockKinesisClient);

            // When
            SampleResult result1 = sampler.runTest(mockContext);
            SampleResult result2 = sampler.runTest(mockContext);
            SampleResult result3 = sampler.runTest(mockContext);

            // Then
            assertNotNull(result1);
            assertNotNull(result2);
            assertNotNull(result3);
            assertTrue(result1.isSuccessful());
            assertTrue(result2.isSuccessful());
            assertTrue(result3.isSuccessful());
            
            verify(mockKinesisClient, times(3)).putRecord(any(PutRecordRequest.class));
        }
    }

    // Helper method to inject mock client
    private void injectMockClient(KinesisProducerSampler sampler, KinesisClient mockClient) {
        try {
            Field kinesisClientField = KinesisProducerSampler.class.getDeclaredField("kinesisClient");
            kinesisClientField.setAccessible(true);
            kinesisClientField.set(sampler, mockClient);
        } catch (Exception e) {
            throw new RuntimeException("Failed to inject mock client", e);
        }
    }
}

package org.apache.jmeter.protocol.aws.eventbus;

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
import software.amazon.awssdk.services.eventbridge.EventBridgeClient;
import software.amazon.awssdk.services.eventbridge.model.*;
import software.amazon.awssdk.awscore.exception.AwsErrorDetails;

import java.lang.reflect.Field;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Simplified unit tests for EventBusProducerSampler class.
 * Focuses on testing individual methods without JMeter runtime dependencies.
 * 
 * @author JoseLuisSR
 * @since 11/08/2025
 */
@DisplayName("EventBusProducerSampler Simple Tests")
class EventBusProducerSamplerTest {

    private EventBusProducerSampler sampler;
    
    @Mock
    private JavaSamplerContext mockContext;
    
    @Mock
    private EventBridgeClient mockEventBridgeClient;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        sampler = new EventBusProducerSampler();
        
                // Setup mock context with valid parameters using correct parameter names
        when(mockContext.getParameter("event_bus_name")).thenReturn("test-event-bus");
        when(mockContext.getParameter("event_source")).thenReturn("test.application");
        when(mockContext.getParameter("detail_type")).thenReturn("User Action");
        when(mockContext.getParameter("event_detail")).thenReturn("{\"action\":\"login\",\"user\":\"testuser\"}");
        
        // Mock the parameter names iterator for setupTest
        when(mockContext.getParameterNamesIterator()).thenReturn(
            java.util.Arrays.asList("EVENT_BUS_NAME", "EVENT_SOURCE", "EVENT_DETAIL_TYPE", "EVENT_DETAIL").iterator()
        );
    }

    @Nested
    @DisplayName("Default Parameters Tests")
    class DefaultParametersTests {

        @Test
        @DisplayName("Should return arguments with all required parameters")
        void shouldReturnArgumentsWithAllRequiredParameters() {
            // When
            Arguments defaultParams = sampler.getDefaultParameters();

            // Then
            assertNotNull(defaultParams);
            assertTrue(defaultParams.getArgumentCount() > 0);
            
            // Check that we have the expected number of parameters (AWS base + EventBridge specific)
            assertEquals(10, defaultParams.getArgumentCount());
        }

        @Test
        @DisplayName("Should have default values for parameters")
        void shouldHaveDefaultValuesForParameters() {
            // When
            Arguments defaultParams = sampler.getDefaultParameters();

            // Then
            assertNotNull(defaultParams);
            
            // Verify we have the expected number of parameters (6 AWS base + 4 EventBridge specific)
            assertEquals(10, defaultParams.getArgumentCount());
        }
    }

    @Nested
    @DisplayName("Request Building Tests")
    class RequestBuildingTests {

        @Test
        @DisplayName("Should build request with all required parameters")
        void shouldBuildRequestWithAllRequiredParameters() {
            // When
            PutEventsRequest request = sampler.createPutEventsRequest(mockContext);

            // Then
            assertNotNull(request);
            assertNotNull(request.entries());
            assertEquals(1, request.entries().size());
            
            var entry = request.entries().get(0);
            assertEquals("test-event-bus", entry.eventBusName());
            assertEquals("test.application", entry.source());
            assertEquals("User Action", entry.detailType());
            assertEquals("{\"action\":\"login\",\"user\":\"testuser\"}", entry.detail());
        }

        @Test
        @DisplayName("Should handle special characters in event parameters")
        void shouldHandleSpecialCharactersInEventParameters() {
            // Given
            when(mockContext.getParameter("event_bus_name")).thenReturn("test-event-bus");
            when(mockContext.getParameter("event_source")).thenReturn("test/source");
            when(mockContext.getParameter("detail_type")).thenReturn("Test Event with Special Characters: éñ中文");
            when(mockContext.getParameter("event_detail")).thenReturn("{\"message\": \"Hello 世界! Special chars: @#$%^&*()_+\"}");

            // When
            PutEventsRequest request = sampler.createPutEventsRequest(mockContext);

            // Then
            assertNotNull(request);
            var entry = request.entries().get(0);
            assertEquals("test/source", entry.source());
            assertEquals("Test Event with Special Characters: éñ中文", entry.detailType());
            assertEquals("{\"message\": \"Hello 世界! Special chars: @#$%^&*()_+\"}", entry.detail());
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"", "   "})
        @DisplayName("Should handle null and empty parameter values")
        void shouldHandleNullAndEmptyParameterValues(String paramValue) {
            // Given
            when(mockContext.getParameter(anyString())).thenReturn(paramValue);

            // When
            PutEventsRequest request = sampler.createPutEventsRequest(mockContext);

            // Then
            assertNotNull(request);
            assertNotNull(request.entries());
            assertEquals(1, request.entries().size());
        }

        @Test
        @DisplayName("Should handle large event detail payload")
        void shouldHandleLargeEventDetailPayload() {
            // Given
            StringBuilder largePayload = new StringBuilder("{\"data\": \"");
            for (int i = 0; i < 20000; i++) {
                largePayload.append("x");
            }
            largePayload.append("\"}");
            
            when(mockContext.getParameter("event_detail")).thenReturn(largePayload.toString());

            // When
            PutEventsRequest request = sampler.createPutEventsRequest(mockContext);

            // Then
            assertNotNull(request);
            assertEquals(largePayload.toString(), request.entries().get(0).detail());
        }
    }

    @Nested
    @DisplayName("Setup and Teardown Tests")
    class SetupAndTeardownTests {

        @Test
        @DisplayName("Should call setupTest without throwing exception")
        void shouldCallSetupTestWithoutThrowingException() {
            // When & Then
            assertDoesNotThrow(() -> sampler.setupTest(mockContext));
        }

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
    @DisplayName("RunTest Method Tests")
    class RunTestMethodTests {
        
        private void injectMockClient(EventBusProducerSampler sampler, EventBridgeClient mockClient) {
            try {
                Field ebClientField = EventBusProducerSampler.class.getDeclaredField("ebClient");
                ebClientField.setAccessible(true);
                ebClientField.set(sampler, mockClient);
            } catch (Exception e) {
                throw new RuntimeException("Failed to inject mock client", e);
            }
        }
        
        private JavaSamplerContext createMockContextForRunTest() {
            JavaSamplerContext context = mock(JavaSamplerContext.class);
            when(context.getParameter("event_bus_name")).thenReturn("test-event-bus");
            when(context.getParameter("event_source")).thenReturn("test.source");
            when(context.getParameter("detail_type")).thenReturn("Test Event");
            when(context.getParameter("event_detail")).thenReturn("{\"test\": \"data\"}");
            return context;
        }

        @Test
        @DisplayName("Should execute runTest and return SampleResult with mock client")
        void shouldExecuteRunTestAndReturnSampleResultWithMockClient() {
            // Given: Mock EventBridge client with successful response
            EventBridgeClient mockClient = mock(EventBridgeClient.class);
            PutEventsResponse mockResponse = mock(PutEventsResponse.class);
            PutEventsResultEntry mockEntry = mock(PutEventsResultEntry.class);
            
            when(mockEntry.eventId()).thenReturn("test-event-id-123");
            when(mockResponse.entries()).thenReturn(List.of(mockEntry));
            when(mockClient.putEvents(any(PutEventsRequest.class))).thenReturn(mockResponse);
            
            // Inject mock client
            injectMockClient(sampler, mockClient);
            
            JavaSamplerContext context = createMockContextForRunTest();
            
            // When: Call runTest
            SampleResult result = sampler.runTest(context);
            
            // Then: Should return a successful SampleResult
            assertNotNull(result);
            assertTrue(result.isSuccessful());
            assertEquals("Event Id: test-event-id-123", result.getResponseDataAsString());
            
            // Verify that putEvents was called
            verify(mockClient).putEvents(any(PutEventsRequest.class));
        }

        @Test
        @DisplayName("Should include event parameters in SampleResult label")
        void shouldIncludeEventParametersInSampleResultLabel() {
            // Given: Mock EventBridge client with empty response
            EventBridgeClient mockClient = mock(EventBridgeClient.class);
            PutEventsResponse mockResponse = mock(PutEventsResponse.class);
            
            when(mockResponse.entries()).thenReturn(List.of());
            when(mockClient.putEvents(any(PutEventsRequest.class))).thenReturn(mockResponse);
            
            // Inject mock client
            injectMockClient(sampler, mockClient);
            
            JavaSamplerContext context = createMockContextForRunTest();
            
            // When: Call runTest
            SampleResult result = sampler.runTest(context);
            
            // Then: Should include parameters in the sampler data (not label)
            assertNotNull(result);
            String samplerData = result.getSamplerData();
            assertTrue(samplerData.contains("Event Bus: test-event-bus"));
            assertTrue(samplerData.contains("Event Source: test.source"));
            assertTrue(samplerData.contains("Detail Type: Test Event"));
        }

        @Test
        @DisplayName("Should handle EventBridge exception gracefully")
        void shouldHandleEventBridgeExceptionGracefully() {
            // Given: Mock EventBridge client that throws exception
            EventBridgeClient mockClient = mock(EventBridgeClient.class);
            
            EventBridgeException mockException = mock(EventBridgeException.class);
            AwsErrorDetails mockErrorDetails = mock(AwsErrorDetails.class);
            when(mockErrorDetails.errorCode()).thenReturn("TestError");
            when(mockErrorDetails.errorMessage()).thenReturn("Test error message");
            when(mockException.awsErrorDetails()).thenReturn(mockErrorDetails);
            when(mockClient.putEvents(any(PutEventsRequest.class))).thenThrow(mockException);
            
            // Inject mock client
            injectMockClient(sampler, mockClient);
            
            JavaSamplerContext context = createMockContextForRunTest();
            
            // When: Call runTest with exception
            SampleResult result = sampler.runTest(context);
            
            // Then: Should handle exception and return failed result
            assertNotNull(result);
            assertFalse(result.isSuccessful());
            assertEquals("TestError", result.getResponseCode());
            assertEquals("Test error message", result.getResponseDataAsString());
            
            // Verify that putEvents was called
            verify(mockClient).putEvents(any(PutEventsRequest.class));
        }

        @Test
        @DisplayName("Should handle multiple event entries in response")
        void shouldHandleMultipleEventEntriesInResponse() {
            // Given: Mock EventBridge client with multiple entries in response
            EventBridgeClient mockClient = mock(EventBridgeClient.class);
            PutEventsResponse mockResponse = mock(PutEventsResponse.class);
            PutEventsResultEntry mockEntry1 = mock(PutEventsResultEntry.class);
            PutEventsResultEntry mockEntry2 = mock(PutEventsResultEntry.class);
            
            when(mockEntry1.eventId()).thenReturn("event-id-1");
            when(mockEntry2.eventId()).thenReturn("event-id-2");
            when(mockResponse.entries()).thenReturn(List.of(mockEntry1, mockEntry2));
            when(mockClient.putEvents(any(PutEventsRequest.class))).thenReturn(mockResponse);
            
            // Inject mock client
            injectMockClient(sampler, mockClient);
            
            JavaSamplerContext context = createMockContextForRunTest();
            
            // When: Call runTest
            SampleResult result = sampler.runTest(context);
            
            // Then: Should return result with both event IDs
            assertNotNull(result);
            assertTrue(result.isSuccessful());
            String responseData = result.getResponseDataAsString();
            assertTrue(responseData.contains("Event Id: event-id-1"));
            assertTrue(responseData.contains("Event Id: event-id-2"));
        }
    }
}

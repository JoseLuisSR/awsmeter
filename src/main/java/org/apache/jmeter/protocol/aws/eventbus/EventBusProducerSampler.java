package org.apache.jmeter.protocol.aws.eventbus;

import org.apache.jmeter.config.Argument;
import org.apache.jmeter.protocol.aws.AWSClientSDK2;
import org.apache.jmeter.protocol.aws.AWSSampler;
import org.apache.jmeter.protocol.java.sampler.JavaSamplerContext;
import org.apache.jmeter.config.Arguments;
import org.apache.jmeter.samplers.SampleResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.core.SdkClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.eventbridge.EventBridgeClient;
import software.amazon.awssdk.services.eventbridge.model.PutEventsRequest;
import software.amazon.awssdk.services.eventbridge.model.PutEventsRequestEntry;
import software.amazon.awssdk.services.eventbridge.model.PutEventsResponse;
import software.amazon.awssdk.services.kinesis.model.KinesisException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Event Bus Producer Sampler class to connect and publish events in Event Bridge.
 * @author kosala-almeda
 * @since 05/29/2024
 * @see "https://github.com/kosala-almeda/awsmeter"
 */
public class EventBusProducerSampler extends AWSSampler implements AWSClientSDK2 {

    /**
     * Log attribute.
     */
    protected static Logger log = LoggerFactory.getLogger(EventBusProducerSampler.class);

    /**
     * Event bus
     */
    private static final String EVENT_BUS_NAME = "event_bus_name";

    /**
     * Event source
     */
    private static final String EVENT_SOURCE = "event_source";

    /**
     * Detail type
     */
    private static final String EVENT_DETAIL_TYPE = "detail_type";

    /**
     * Event detail
     */
    private static final String EVENT_DETAIL = "event_detail";

    /**
     * Set Event Bridge Bus event
     */
    private static final List<Argument> EVENT_BUS_PARAMETERS = Stream.of(
            new Argument(EVENT_BUS_NAME, EMPTY),
            new Argument(EVENT_SOURCE, EMPTY),
            new Argument(EVENT_DETAIL_TYPE, EMPTY),
            new Argument(EVENT_DETAIL, EMPTY))
            .collect(Collectors.toList());

    /**
     * AWS Kinesis Data Stream Client.
     */
    private EventBridgeClient ebClient;

    /**
     * Create AWS Kinesis Data Stream Client.
     * @param credentials
     *        Represents the input of JMeter Java Request parameters.
     * @return KinesisClient extends SdkClient super class.
     */
    @Override
    public SdkClient createSdkClient(Map<String, String> credentials) {
        return EventBridgeClient.builder()
                .region(Region.of(getAWSRegion(credentials)))
                .credentialsProvider(getAwsCredentialsProvider(credentials))
                .build();
    }

    /**
     * Initial values for test parameter. They are show in Java Request test sampler.
     * AWS parameters and Event Bridge parameters.
     * @return Arguments to set as default on Java Request.
     */
    @Override
    public Arguments getDefaultParameters() {
        Arguments defaultParameters = new Arguments();
        defaultParameters.setArguments(Stream.of(AWS_PARAMETERS, EVENT_BUS_PARAMETERS)
                .flatMap(List::stream)
                .collect(Collectors.toList()));
        return defaultParameters;
    }

    /**
     * Read test parameters and initialize AWS Event Bridge client.
     * @param context to get the arguments values on Java Sampler.
     */
    @Override
    public void setupTest(JavaSamplerContext context) {

        log.info("Setup Kinesis Producer Sampler.");
        Map<String, String> credentials = new HashMap<>();

        context.getParameterNamesIterator()
                .forEachRemaining( k -> {
                    credentials.put(k, context.getParameter(k));
                    log.info("Parameter: " + k + ", value: " + credentials.get(k));
                });

        log.info("Create Kinesis Producer.");
        ebClient = (EventBridgeClient) createSdkClient(credentials);
    }

    /**
     * Main method to execute the test on single thread. Create Event Request and publish it in Event bus.
     * @param context
     *        Arguments values on Java Sampler.
     * @return SampleResult, captures data such as whether the test was successful,
     * the response code and message, any request or response data, and the test start/end times
     */
    @Override
    public SampleResult runTest(JavaSamplerContext context) {

        SampleResult result = newSampleResult();
        sampleResultStart(result, String.format(
            "Event Bus: %s \nEvent Source: %s \nDetail Type: %s  \nEvent Detail: %s",
                context.getParameter(EVENT_BUS_NAME),
                context.getParameter(EVENT_SOURCE),
                context.getParameter(EVENT_DETAIL_TYPE),
                context.getParameter(EVENT_DETAIL)));

        try {
            log.info("Publishing Event.");
            PutEventsResponse response = ebClient.putEvents(createPutEventsRequest(context));
            sampleResultSuccess(result, 
                response.entries().stream().map(
                    entry -> String.format("Event Id: %s", entry.eventId())).collect(Collectors.joining (",")));
        }catch (KinesisException e){
            sampleResultFail(result, e.awsErrorDetails().errorCode(), e.awsErrorDetails().errorMessage());
        }

        return result;
    }

    /**
     * Close AWS Event Bridge Client after run single thread.
     * @param context
     *        Arguments values on Java Sampler.
     */
    @Override
    public void teardownTest(JavaSamplerContext context) {
        log.info("Close Kinesis Producer.");
        Optional.ofNullable(ebClient)
                .ifPresent(client -> client.close());
    }

    /**
     * Create PutEventsRequest with a PutEventsRequestEntry object
     *        (event bus name, detail type, event source and event detail)
     * @param context
     *        Arguments values on Java Sampler.
     * @return PutEventsRequest
     */
    public PutEventsRequest createPutEventsRequest(JavaSamplerContext context){
        return PutEventsRequest.builder()
                .entries(PutEventsRequestEntry.builder()
                        .eventBusName(context.getParameter(EVENT_BUS_NAME))
                        .detailType(context.getParameter(EVENT_DETAIL_TYPE))
                        .source(context.getParameter(EVENT_SOURCE))
                        .detail(context.getParameter(EVENT_DETAIL))
                        .build())
                .build();
    }

}

package org.apache.jmeter.protocol.aws.kinesis;

import org.apache.jmeter.config.Argument;
import org.apache.jmeter.config.Arguments;
import org.apache.jmeter.protocol.aws.AWSClientSDK2;
import org.apache.jmeter.protocol.aws.AWSSampler;
import org.apache.jmeter.protocol.java.sampler.JavaSamplerContext;
import org.apache.jmeter.samplers.SampleResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.core.SdkClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.kinesis.KinesisClient;
import software.amazon.awssdk.services.kinesis.model.*;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Kinesis Producer Sampler class to connect and publish data records in Kinesis streams.
 * @author samar ranjan
 * @since 01/18/2021
 * @see "https://github.com/JoseLuisSR/awsmeter"
 */
public class KinesisRecordProducerSampler extends AWSSampler implements AWSClientSDK2 {

    /**
     * Log attribute.
     */
    protected static Logger log = LoggerFactory.getLogger(KinesisRecordProducerSampler.class);

    /**
     * Kinesis Stream name.
     */
    private static final String KINESIS_STREAM_NAME = "kinesis_stream_name";

    /**
     * Kinesis Stream Partition key.
     */
    private static final String KINESIS_PARTITION_KEY = "partition_key";

    /**
     * Kinesis Stream Data Record.
     */
    private static final String KINESIS_DATA_RECORD = "data_record";

    /**
     * Set Kinesis Data Stream.
     */
    private static final List<Argument> KINESIS_PARAMETERS = Stream.of(
            new Argument(KINESIS_STREAM_NAME, EMPTY),
            new Argument(KINESIS_PARTITION_KEY, EMPTY),
            new Argument(KINESIS_DATA_RECORD, EMPTY))
            .collect(Collectors.toList());

    /**
     * AWS Kinesis Data Stream Client.
     */
    private KinesisClient kinesisClient;

    /**
     * Create AWS Kinesis Data Stream Client.
     * @param credentials
     *        Represents the input of JMeter Java Request parameters.
     * @return KinesisClient extends SdkClient super class.
     */
    @Override
    public SdkClient createSdkClient(Map<String, String> credentials) {
        return KinesisClient.builder()
                .region(Region.of(getAWSRegion(credentials)))
                .credentialsProvider(getAwsCredentialsProvider(credentials))
                .build();
    }

    /**
     * Initial values for test parameter. They are show in Java Request test sampler.
     * AWS parameters and Kinesis Data Stream parameters.
     * @return Arguments to set as default on Java Request.
     */
    @Override
    public Arguments getDefaultParameters() {
        Arguments defaultParameters = new Arguments();
        defaultParameters.setArguments(Stream.of(AWS_PARAMETERS, KINESIS_PARAMETERS)
                .flatMap(List::stream)
                .collect(Collectors.toList()));
        return defaultParameters;
    }

    /**
     * Read test parameters and initialize AWS Kinesis Data Stream client.
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
        kinesisClient = (KinesisClient) createSdkClient(credentials);
    }

    /**
     * Main method to execute the test on single thread. Create Data Records and publish it in Kinesis stream.
     * @param context
     *        Arguments values on Java Sampler.
     * @return SampleResult, captures data such as whether the test was successful,
     * the response code and message, any request or response data, and the test start/end times
     */
    @Override
    public SampleResult runTest(JavaSamplerContext context) {

        SampleResult result = newSampleResult();
        sampleResultStart(result, String.format("Stream Name: %s \nPartition Key: %s \nData Record: %s",
                context.getParameter(KINESIS_STREAM_NAME),
                context.getParameter(KINESIS_PARTITION_KEY),
                context.getParameter(KINESIS_DATA_RECORD)));

        try {
            log.info("Publishing Data Records.");
            PutRecordsResponse response = kinesisClient.putRecords(createPutRecordRequest(context));
            sampleResultSuccess(result,String.format("Response details: %s \nEncryption Type: %s",
                    response.toString(),
                    response.encryptionTypeAsString()));
        }catch (KinesisException e){
            sampleResultFail(result, e.awsErrorDetails().errorCode(), e.awsErrorDetails().errorMessage());
        }

        return result;
    }

    /**
     * Close AWS Kinesis Data Stream Client after run single thread.
     * @param context
     *        Arguments values on Java Sampler.
     */
    @Override
    public void teardownTest(JavaSamplerContext context) {
        log.info("Close Kinesis Producer.");
        kinesisClient.close();
    }

    /**
     * Create PutRecordsRequest with stream name, partition key and data.
     * @param context
     *        Arguments values on Java Sampler.
     * @return PutRecordRequest
     */
    public PutRecordsRequest createPutRecordRequest(JavaSamplerContext context) {
        String tempPayload = context.getParameter(KINESIS_DATA_RECORD);
        JSONArray jsonArray = new JSONArray(tempPayload);
        List<PutRecordsRequestEntry> putRecordsRequestEntryList = new ArrayList<>();
        PutRecordsRequest putRecordsRequest = null;
        for (int i = 0; i < jsonArray.length(); i++) {
            try {
                log.info("json node:"+i);
                log.info(jsonArray.get(i).toString());
                putRecordsRequestEntryList.add(PutRecordsRequestEntry.builder()
                        .data(SdkBytes.fromByteArray(String.valueOf(jsonArray.get(i)).getBytes(StandardCharsets.UTF_8)))
                        .partitionKey(context.getParameter(KINESIS_PARTITION_KEY) + "-" + i).build());


            } catch (JSONException e) {
                e.printStackTrace();
            }
            putRecordsRequest = PutRecordsRequest.builder()
                    .streamName(context.getParameter(KINESIS_STREAM_NAME))
                    .records(putRecordsRequestEntryList).build();
        }
        return putRecordsRequest;
    }
    }



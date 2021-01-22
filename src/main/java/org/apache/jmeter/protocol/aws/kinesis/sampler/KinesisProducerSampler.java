package org.apache.jmeter.protocol.aws.kinesis.sampler;

import org.apache.jmeter.config.Argument;
import org.apache.jmeter.protocol.aws.AWSClient;
import org.apache.jmeter.protocol.aws.AWSSampler;
import org.apache.jmeter.protocol.java.sampler.JavaSamplerClient;
import org.apache.jmeter.protocol.java.sampler.JavaSamplerContext;
import org.apache.jmeter.config.Arguments;
import org.apache.jmeter.samplers.SampleResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.core.SdkClient;
import software.amazon.awssdk.services.kinesis.KinesisClient;
import software.amazon.awssdk.services.kinesis.model.KinesisException;
import software.amazon.awssdk.services.kinesis.model.PutRecordRequest;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class KinesisProducerSampler extends AWSSampler implements JavaSamplerClient, AWSClient {

    private static final Logger log = LoggerFactory.getLogger(KinesisProducerSampler.class);

    private static final String KINESIS_STREAM_NAME = "kinesis_stream_name";

    private static final String KINESIS_PARTITION_KEY = "partition_key";

    private static final String KINESIS_DATA_RECORD = "data_record";

    private static final List<Argument> KINESIS_PARAMETERS = Stream.of(
            new Argument(AWS_ACCESS_KEY_ID, ""),
            new Argument(AWS_SECRET_ACCESS_KEY, ""),
            new Argument(AWS_SESSION_TOKEN, ""),
            new Argument(AWS_REGION, ""),
            new Argument(AWS_CONFIG_PROFILE, AWS_DEFAULT_PROFILE),
            new Argument(KINESIS_STREAM_NAME, ""),
            new Argument(KINESIS_PARTITION_KEY, ""),
            new Argument(KINESIS_DATA_RECORD, ""))
            .collect(Collectors.toList());

    private KinesisClient kinesisClient;

    @Override
    public SdkClient createSdkClient(Map<String, String> credentials) {
        return KinesisClient.builder()
                .region(awsRegion(credentials))
                .credentialsProvider(awsCredentialProvider(credentials))
                .build();
    }

    @Override
    public Arguments getDefaultParameters() {
        Arguments defaultParameters = new Arguments();
        defaultParameters.setArguments(KINESIS_PARAMETERS);
        return defaultParameters;
    }

    @Override
    public void setupTest(JavaSamplerContext context) {

        log.info("Setup Kinesis Producer Sampler.");
        Map<String, String> credentials = new HashMap<>();

        context.getParameterNamesIterator()
                .forEachRemaining( k -> {
                    credentials.put(k, context.getParameter(k));
                    log.info("Parameter: " + k + ", value: " + credentials.get(k));
                });

        log.info("Create Kinesis Producer");
        kinesisClient = (KinesisClient) createSdkClient(credentials);
    }

    @Override
    public SampleResult runTest(JavaSamplerContext context) {

        SampleResult result = newSampleResult();
        sampleResultStart(result, context.getParameter(KINESIS_DATA_RECORD));

        try {
            log.info("Publishing Data Record.");
            String sequenceNum = kinesisClient.putRecord(createPutRecordRequest(context)).sequenceNumber();
            sampleResultSuccess(result,sequenceNum);
        }catch (KinesisException e){
            sampleResultFail(result, e.awsErrorDetails().errorCode(), e.awsErrorDetails().errorMessage());
        }

        return result;
    }

    @Override
    public void teardownTest(JavaSamplerContext context) {

    }

    public PutRecordRequest createPutRecordRequest(JavaSamplerContext context){
        return PutRecordRequest.builder()
                .streamName(context.getParameter(KINESIS_STREAM_NAME))
                .partitionKey(context.getParameter(KINESIS_PARTITION_KEY))
                .data(SdkBytes.fromUtf8String(context.getParameter(KINESIS_DATA_RECORD)))
                .build();
    }

}

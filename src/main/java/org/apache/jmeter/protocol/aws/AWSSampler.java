package org.apache.jmeter.protocol.aws;

import org.apache.jmeter.config.Argument;
import org.apache.jmeter.protocol.java.sampler.JavaSamplerClient;
import org.apache.jmeter.samplers.SampleResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public abstract class AWSSampler implements JavaSamplerClient, AWSClient {

    private static final Logger log = LoggerFactory.getLogger(AWSSampler.class);

    public static final String AWS_ACCESS_KEY_ID = "aws_access_key_id";

    public static final String AWS_SECRET_ACCESS_KEY = "aws_secret_access_key";

    public static final String AWS_SESSION_TOKEN = "aws_session_token";

    public static final String AWS_REGION = "aws_region";

    public static final String AWS_CONFIG_PROFILE = "aws_configure_profile";

    public static final String AWS_DEFAULT_PROFILE = "default";

    private static final String ENCODING = "UTF-8";

    public static final List<Argument> AWS_PARAMETERS = Stream.of(
            new Argument(AWS_ACCESS_KEY_ID, ""),
            new Argument(AWS_SECRET_ACCESS_KEY, ""),
            new Argument(AWS_SESSION_TOKEN, ""),
            new Argument(AWS_REGION, ""),
            new Argument(AWS_CONFIG_PROFILE, AWS_DEFAULT_PROFILE))
            .collect(Collectors.toList());

    protected SampleResult newSampleResult(){
        SampleResult result = new SampleResult();
        result.setDataEncoding(ENCODING);
        result.setDataType(SampleResult.TEXT);
        return result;
    }

    protected void sampleResultStart(SampleResult result, String data){
        result.setSamplerData(data);
        result.sampleStart();
    }

    protected void sampleResultSuccess(SampleResult result, String response){
        result.sampleEnd();
        result.setSuccessful(true);
        result.setResponseCodeOK();
        result.setResponseData(response, ENCODING);
    }

    protected void sampleResultFail(SampleResult result, String code, String response) {
        result.sampleEnd();
        result.setSuccessful(false);
        result.setResponseCode(code);
        result.setResponseData(response, ENCODING);
    }

    protected Logger getNewLogger() {
        return log;
    }

}

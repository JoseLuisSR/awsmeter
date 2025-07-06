package org.apache.jmeter.protocol.aws;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.jmeter.config.Argument;
import org.apache.jmeter.protocol.java.sampler.JavaSamplerClient;
import org.apache.jmeter.samplers.SampleResult;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * AWS Sampler class that implements JavaSamplerClient class to create custom Java Request Sampler per AWS Service.
 * @author JoseLuisSR
 * @since 01/27/2021
 * @see "https://github.com/JoseLuisSR/awsmeter"
 */
public abstract class AWSSampler implements JavaSamplerClient{


    /**
     * IAM user with programmatic access, access key id.
     */
    protected static final String AWS_ACCESS_KEY_ID = "aws_access_key_id";

    /**
     * IAM user with programmatic access, secret access key.
     */
    protected static final String AWS_SECRET_ACCESS_KEY = "aws_secret_access_key";

    /**
     * IAM user with programmatic access, session token.
     */
    protected static final String AWS_SESSION_TOKEN = "aws_session_token";

    /**
     * AWS region, https://docs.aws.amazon.com/AWSEC2/latest/UserGuide/using-regions-availability-zones.html#concepts-available-regions
     */
    protected static final String AWS_REGION = "aws_region";

    /**
     * AWS CLI profile. A named profile is a collection of settings and credentials store in your machine.
     */
    protected static final String AWS_CONFIG_PROFILE = "aws_configure_profile";

    /**
     * Default AWS CLI profile.
     */
    protected static final String AWS_DEFAULT_PROFILE = "default";

    /**
     * AWS endpoint
     */
    protected static final String AWS_ENDPOINT = "https://%s.%s.amazonaws.com";

    /**
     * Custom AWS endpoint.
     */
    protected static final String AWS_ENDPOINT_CUSTOM = "aws_endpoint_custom";

    /**
     * Fail code.
     */
    protected static final String FAIL_CODE = "500";

    /**
     * Empty String.
     */
    protected static final String EMPTY = "";

    /**
     * Empty array.
     */
    protected static final String EMPTY_ARRAY = "[]";

    /**
     * Encoding.
     */
    protected static final String ENCODING = "UTF-8";

    /**
     * SQS and SNS maximum message attributes.
     */
    protected static final Integer MSG_ATTRIBUTES_MAX = 10;

    /**
     * Message Attribute String type.
     */
    protected static final String MSG_ATTRIBUTE_TYPE_STR = "String";

    /**
     * Message Attribute String Array type.
     */
    protected static final String MSG_ATTRIBUTE_TYPE_STR_ARRAY = "String.Array";

    /**
     * Message Attribute Number type.
     */
    protected static final String MSG_ATTRIBUTE_TYPE_NUM = "Number";

    /**
     * Message Attribute Binary type.
     */
    protected static final String MSG_ATTRIBUTE_TYPE_BIN = "Binary";

    /**
     * Set AWS Parameters needed to access API.
     */
    protected static final List<Argument> AWS_PARAMETERS = Stream.of(
            new Argument(AWS_ACCESS_KEY_ID, EMPTY),
            new Argument(AWS_SECRET_ACCESS_KEY, EMPTY),
            new Argument(AWS_SESSION_TOKEN, EMPTY),
            new Argument(AWS_REGION, EMPTY),
            new Argument(AWS_ENDPOINT_CUSTOM, EMPTY),
            new Argument(AWS_CONFIG_PROFILE, AWS_DEFAULT_PROFILE))
            .collect(Collectors.toList());

    /**
     * Create new SampleResult.
     * @return SampleResult, captures data such as whether the test was successful,
     * the response code and message, any request or response data and the test start/end times
     */
    protected SampleResult newSampleResult(){
        SampleResult result = new SampleResult();
        result.setDataEncoding(ENCODING);
        result.setDataType(SampleResult.TEXT);
        return result;
    }

    /**
     * Start the sample request and set the <code>samplerData</code> to the requestData.
     * @param result
     *        SampleResult mutable object to update status.
     * @param data
     *        The request to set as <code>samplerData</code>.
     */
    protected void sampleResultStart(SampleResult result, String data){
        result.setSamplerData(data);
        result.sampleStart();
    }

    /**
     * Set the sample result as <code>sampleEnd()</code>,
     * <code>setSuccessful(true)</code>, <code>setResponseCode("OK")</code> and if
     * the response is not <code>null</code> then
     * <code>setResponseData(response.toString(), ENCODING)</code> otherwise it is
     * marked as not requiring a response.
     *
     * @param result
     *        SampleResult mutable object to change.
     * @param response
     *        The successful result message, may be null.
     */
    protected void sampleResultSuccess(SampleResult result, String response){
        result.sampleEnd();
        result.setSuccessful(true);
        result.setResponseCodeOK();
        result.setResponseData(response, ENCODING);
    }

    /**
     * Mark the sample result as <code>sampleEnd</code>,
     * <code>setSuccessful(false)</code> and the <code>setResponseCode</code> to
     * reason.
     *
     * @param result
     *        SampleResult mutable object to change.
     * @param code
     *        The failure code.
     * @param response
     *        The failure reason.
     */
    protected void sampleResultFail(SampleResult result, String code, String response) {
        result.sampleEnd();
        result.setSuccessful(false);
        result.setResponseCode(code);
        result.setResponseData(response, ENCODING);
    }

    /**
     * Read message attributes and deserialize from JSON to Objects.
     * @param msgAttributes
     *        Messages attributes in JSON format.
     * @return Message attributes list.
     * @throws JsonProcessingException
     *         Exception when deserialize JSON to Object.
     */
    protected List<MessageAttribute> readMsgAttributes(final String msgAttributes) throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.readValue(Optional.ofNullable(msgAttributes)
                        .filter(Predicate.not(String::isEmpty))
                        .orElseGet(() -> EMPTY_ARRAY),
                new TypeReference<List<MessageAttribute>>() {}).stream()
                .limit(MSG_ATTRIBUTES_MAX)
                .collect(Collectors.toList());
    }

}

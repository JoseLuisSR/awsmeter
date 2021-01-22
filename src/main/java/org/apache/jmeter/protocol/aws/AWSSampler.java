package org.apache.jmeter.protocol.aws;

import org.apache.jmeter.samplers.SampleResult;

public class AWSSampler {

    private static final String ENCODING = "UTF-8";

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
}

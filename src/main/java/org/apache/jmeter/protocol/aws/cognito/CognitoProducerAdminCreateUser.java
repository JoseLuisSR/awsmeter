package org.apache.jmeter.protocol.aws.cognito;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.jmeter.config.Argument;
import org.apache.jmeter.config.Arguments;
import org.apache.jmeter.protocol.java.sampler.JavaSamplerContext;
import org.apache.jmeter.samplers.SampleResult;

import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminCreateUserRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminCreateUserResponse;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminSetUserPasswordRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AttributeType;
import software.amazon.awssdk.services.cognitoidentityprovider.model.CognitoIdentityProviderException;

/**
 * Cognito Producer Sampler class to create a new user.
 * This requires Cognito admin permission. 
 * @author J Rao
 * @since 04/01/2023
 * @see "https://github.com/JoseLuisSR/awsmeter"
 */
public class CognitoProducerAdminCreateUser extends CognitoProducerSampler {

    /**
     * List of Arguments to create user sampler.
     */
    private static final List<Argument> COGNITO_PARAMETERS = Stream.of(
            new Argument(COGNITO_USER_POOL_ID, EMPTY),
            new Argument(COGNITO_USER_USERNAME, EMPTY),
            new Argument(COGNITO_USER_EMAIL, EMPTY),
            new Argument(COGNITO_USER_PASSWORD, EMPTY))
            .collect(Collectors.toList());

    /**
     * Initial values for test parameter. They are show in Java Request test sampler.
     * AWS parameters and Cognito parameters.
     * @return Arguments to set as default on Java Request.
     */
    @Override
    public Arguments getDefaultParameters() {
        Arguments defaultParameters = new Arguments();
        defaultParameters.setArguments(Stream.of(AWS_PARAMETERS, COGNITO_PARAMETERS)
                .flatMap(List::stream)
                .collect(Collectors.toList()));
        return defaultParameters;
    }

    /**
     * Main method to execute the test on single thread. Create new user in Cognito.
     * @param context
     *        Arguments values on Java Sampler.
     * @return SampleResult, captures data such as whether the test was successful,
     * the response code and message, any request or response data, and the test start/end times
     */
    @Override
    public SampleResult runTest(JavaSamplerContext context) {

        SampleResult result = newSampleResult();
        sampleResultStart(result, String.format("Cognito Pool Id: %s " + 
                        "\nUser Username : %s \nUser Email: %s",
                context.getParameter(COGNITO_USER_POOL_ID),
                context.getParameter(COGNITO_USER_USERNAME),
                context.getParameter(COGNITO_USER_EMAIL)));

        try{
            log.info("Create user in Cognito");
            AdminCreateUserResponse createUserRsp = cognitoClient.adminCreateUser(createAdminCreateUserRequest(context));
            cognitoClient.adminSetUserPassword(createAdminSetUserPasswordRequest(context)); // this will also set user status to confirmed
            
            sampleResultSuccess(result, String.format("User Attributes: %s", createUserRsp.user().attributes()));            
        } catch (CognitoIdentityProviderException exc){
            log.error("Unable to create user " + context.getParameter(COGNITO_USER_USERNAME), exc);
            sampleResultFail(result, exc.awsErrorDetails().errorCode(), exc.awsErrorDetails().errorMessage());
        }

        return result;
    }
    
    protected AdminCreateUserRequest createAdminCreateUserRequest(JavaSamplerContext context) {
        AttributeType attributeType = AttributeType.builder()
                .name("email")
                .value(context.getParameter(COGNITO_USER_EMAIL))
                .build();      
        
        return AdminCreateUserRequest.builder()
                .userPoolId(context.getParameter(COGNITO_USER_POOL_ID))
                .username(context.getParameter(COGNITO_USER_USERNAME))
                .userAttributes(attributeType)
                .messageAction("SUPPRESS")
                .build();
    }
    
    protected AdminSetUserPasswordRequest createAdminSetUserPasswordRequest(JavaSamplerContext context) {
        return AdminSetUserPasswordRequest.builder()
                .userPoolId(context.getParameter(COGNITO_USER_POOL_ID))
                .username(context.getParameter(COGNITO_USER_USERNAME))
                .password(context.getParameter(COGNITO_USER_PASSWORD))
                .permanent(true) // Set to 'true' for a permanent password, 'false' for a temporary one
                .build();
    }
}

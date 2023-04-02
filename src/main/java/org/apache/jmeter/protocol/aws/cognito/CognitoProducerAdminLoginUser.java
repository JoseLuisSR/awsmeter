package org.apache.jmeter.protocol.aws.cognito;

import static java.util.function.Predicate.not;

import java.security.GeneralSecurityException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.jmeter.config.Argument;
import org.apache.jmeter.config.Arguments;
import org.apache.jmeter.protocol.java.sampler.JavaSamplerContext;
import org.apache.jmeter.samplers.SampleResult;

import com.fasterxml.jackson.core.JsonProcessingException;

import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminInitiateAuthRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminInitiateAuthResponse;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AuthFlowType;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AuthenticationResultType;
import software.amazon.awssdk.services.cognitoidentityprovider.model.CognitoIdentityProviderException;

/**
 * Cognito Producer Sampler class to login a user.
 * This requires Cognito admin permission, and ALLOW_ADMIN_USER_PASSWORD_AUTH
 * needs to be enabled in the app client of the user pool.
 * @author J Rao
 * @since 04/01/2023
 * @see "https://github.com/JoseLuisSR/awsmeter"
 */
public class CognitoProducerAdminLoginUser extends CognitoProducerSampler {

    /**
     * List of Arguments to create user sampler.
     */
    private static final List<Argument> COGNITO_PARAMETERS = Stream.of(
            new Argument(COGNITO_CLIENT_ID, EMPTY),
            new Argument(COGNITO_CLIENT_SECRET_KEY, EMPTY),
            new Argument(COGNITO_USER_POOL_ID, EMPTY),
            new Argument(COGNITO_USER_USERNAME, EMPTY),
            new Argument(COGNITO_USER_PASSWORD, EMPTY),
            new Argument(COGNITO_USER_ACCESS_TOKEN_VAR_NAME, "COGNITO_USER_ACCESS_TOKEN"),
            new Argument(COGNITO_USER_ID_TOKEN_VAR_NAME, "COGNITO_USER_ID_TOKEN"),
            new Argument(COGNITO_USER_REFRESH_TOKEN_VAR_NAME, "COGNITO_USER_REFRESH_TOKEN")
            ).collect(Collectors.toList());

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
     * Main method to execute the test on single thread. Login user in Cognito.
     * @param context
     *        Arguments values on Java Sampler.
     * @return SampleResult, captures data such as whether the test was successful,
     * the response code and message, any request or response data, and the test start/end times
     */
    @Override
    public SampleResult runTest(JavaSamplerContext context) {

        SampleResult result = newSampleResult();
        sampleResultStart(result, String.format("Cognito Client Id: %s \nPool Id: %s " + 
                        "\nUser Username : %s",
                context.getParameter(COGNITO_CLIENT_ID),
                context.getParameter(COGNITO_USER_POOL_ID),
                context.getParameter(COGNITO_USER_USERNAME)));

        try{
            log.info("Login user in Cognito");
            AdminInitiateAuthResponse loginUserRsp = cognitoClient.adminInitiateAuth(createAdminInitiateAuthRequest(context));
            
            AuthenticationResultType authResult = loginUserRsp.authenticationResult();
            Optional.ofNullable(context.getParameter(COGNITO_USER_ACCESS_TOKEN_VAR_NAME))
                .filter(not(String::isBlank))
                .ifPresent(varName -> context.getJMeterVariables().put(varName, authResult.accessToken()));
            Optional.ofNullable(context.getParameter(COGNITO_USER_ID_TOKEN_VAR_NAME))
                .filter(not(String::isBlank))
                .ifPresent(varName -> context.getJMeterVariables().put(varName, authResult.idToken()));
            Optional.ofNullable(context.getParameter(COGNITO_USER_REFRESH_TOKEN_VAR_NAME))
                .filter(not(String::isBlank))
                .ifPresent(varName -> context.getJMeterVariables().put(varName, authResult.refreshToken()));
            
            sampleResultSuccess(result, String.format("Auth Result: %s", loginUserRsp.authenticationResult()));            
        } catch (CognitoIdentityProviderException exc){
            log.error("Unable to login user " + context.getParameter(COGNITO_USER_USERNAME), exc);
            sampleResultFail(result, exc.awsErrorDetails().errorCode(), exc.awsErrorDetails().errorMessage());
        } catch (JsonProcessingException exc) {
            log.error("Unable to login user " + context.getParameter(COGNITO_USER_USERNAME), exc);
            sampleResultFail(result, FAIL_CODE, exc.getMessage());
        } catch (GeneralSecurityException exc) {
            log.error("Unable to login user " + context.getParameter(COGNITO_USER_USERNAME), exc);
            sampleResultFail(result, FAIL_CODE, exc.getMessage());
        }

        return result;
    }
    
    protected AdminInitiateAuthRequest createAdminInitiateAuthRequest(JavaSamplerContext context) 
            throws JsonProcessingException, GeneralSecurityException {
        Map<String,String> authParameters = new HashMap<>();
        authParameters.put("USERNAME", context.getParameter(COGNITO_USER_USERNAME));
        authParameters.put("PASSWORD", context.getParameter(COGNITO_USER_PASSWORD));
        authParameters.put("SECRET_HASH", calculateSecretHash(context));
        
        return AdminInitiateAuthRequest.builder()
                .clientId(context.getParameter(COGNITO_CLIENT_ID))
                .userPoolId(context.getParameter(COGNITO_USER_POOL_ID))
                .authParameters(authParameters)
                .authFlow(AuthFlowType.ADMIN_USER_PASSWORD_AUTH)
                .build();
    }
}

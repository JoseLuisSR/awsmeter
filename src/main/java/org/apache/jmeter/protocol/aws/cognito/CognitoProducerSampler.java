package org.apache.jmeter.protocol.aws.cognito;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.apache.jmeter.protocol.aws.AWSClientSDK2;
import org.apache.jmeter.protocol.aws.AWSSampler;
import org.apache.jmeter.protocol.java.sampler.JavaSamplerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import software.amazon.awssdk.core.SdkClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;

/**
 * Cognito Producer Sampler class to sign up and authenticate users.
 * @author J Rao
 * @since 04/01/2023
 * @see "https://github.com/JoseLuisSR/awsmeter"
 */
public abstract class CognitoProducerSampler extends AWSSampler implements AWSClientSDK2 {

    /**
     * Log attribute.
     */
    protected static Logger log = LoggerFactory.getLogger(CognitoProducerSampler.class);

    /**
     * Cognito Client Id.
     */
    protected static final String COGNITO_CLIENT_ID = "cognito_client_id";

    /**
     * Cognito Client Secret Key.
     */
    protected static final String COGNITO_CLIENT_SECRET_KEY = "cognito_client_secret_key";

    /**
     * Cognito User Pool Id.
     */
    protected static final String COGNITO_USER_POOL_ID = "cognito_user_pool_id";
    
    /**
     * Cognito User's Username.
     */
    protected static final String COGNITO_USER_USERNAME = "cognito_user_username";

    /**
     * Cognito User's Email.
     */
    protected static final String COGNITO_USER_EMAIL = "cognito_user_email";

    /**
     * Cognito User's Password.
     */
    protected static final String COGNITO_USER_PASSWORD = "cognito_user_password";

    /**
     * Name of the JMeter Variable to store Cognito User's Access Token.
     */
    protected static final String COGNITO_USER_ACCESS_TOKEN_VAR_NAME = "cognito_user_access_token_var_name";

    /**
     * Name of the JMeter Variable to store Cognito User's Id Token.
     */
    protected static final String COGNITO_USER_ID_TOKEN_VAR_NAME = "cognito_user_id_token_var_name";

    /**
     * Name of the JMeter Variable to store Cognito User's Refresh Token.
     */
    protected static final String COGNITO_USER_REFRESH_TOKEN_VAR_NAME = "cognito_user_refresh_token_var_name";
    
    /**
     * AWS Cognito Identity Provider Client
     */
    protected CognitoIdentityProviderClient cognitoClient;

    /**
     * Create AWS Identity Provider Client.
     * @param credentials
     *        Represents the input of JMeter Java Request parameters.
     * @return CognitoIdentityProviderClient extends SdkClient super class.
     */
    @Override
    public SdkClient createSdkClient(Map<String, String> credentials) {

        String region = getAWSRegion(credentials);
        return CognitoIdentityProviderClient.builder()
                .endpointOverride(URI.create(getAWSEndpoint(credentials, CognitoIdentityProviderClient.SERVICE_NAME, region)))
                .region(Region.of(region))
                .credentialsProvider(getAwsCredentialsProvider(credentials))
                .build();
    }

    /**
     * Read test parameters and initialize AWS Identity Provider Client.
     * @param context to get the arguments values on Java Sampler.
     */
    @Override
    public void setupTest(JavaSamplerContext context) {
        log.info("Setup Cognito Producer Sampler.");
        Map<String, String> credentials = new HashMap<>();

        context.getParameterNamesIterator().forEachRemaining( k -> {
            credentials.put(k, context.getParameter(k));
            log.info("Parameter: " + k + ", value: " + credentials.get(k));
        });

        log.info("Create Cognito Producer.");
        cognitoClient = (CognitoIdentityProviderClient) createSdkClient(credentials);
    }

    /**
     * Close AWS Identity Provider Client after run single thread.
     * @param context
     *        Arguments values on Java Sampler.
     */
    @Override
    public void teardownTest(JavaSamplerContext context) {
        log.info("Close Cognito Producer.");
        Optional.ofNullable(cognitoClient)
                .ifPresent(client -> client.close());
    }

    public static String calculateSecretHash(JavaSamplerContext context) 
            throws NoSuchAlgorithmException, InvalidKeyException {
        final String HMAC_SHA256_ALGORITHM = "HmacSHA256";
        
        String userPoolClientId = context.getParameter(COGNITO_CLIENT_ID);
        String userPoolClientSecret = context.getParameter(COGNITO_CLIENT_SECRET_KEY);
        String userName = context.getParameter(COGNITO_USER_USERNAME);

        SecretKeySpec signingKey = new SecretKeySpec(
            userPoolClientSecret.getBytes(StandardCharsets.UTF_8),
            HMAC_SHA256_ALGORITHM);

        Mac mac = Mac.getInstance(HMAC_SHA256_ALGORITHM);
        mac.init(signingKey);
        mac.update(userName.getBytes(StandardCharsets.UTF_8));
        byte[] rawHmac = mac.doFinal(userPoolClientId.getBytes(StandardCharsets.UTF_8));
        return java.util.Base64.getEncoder().encodeToString(rawHmac);
    }
}

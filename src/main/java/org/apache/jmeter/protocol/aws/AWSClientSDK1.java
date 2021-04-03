package org.apache.jmeter.protocol.aws;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicSessionCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.STSSessionCredentialsProvider;
import com.amazonaws.client.builder.AwsSyncClientBuilder;

import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * Interface segregation with implementation of AWS SDK1.
 * @author JoseLuisSR
 * @since 01/27/2021
 * @see "https://github.com/JoseLuisSR/awsmeter"
 */
public interface AWSClientSDK1 extends AWSClient{

    /**
     * Create AWS Client by service to perform operations.
     * @param credentials
     *        Represents the input of JMeter Java Request parameters.
     * @return AwsSyncClientBuilder by AWS Service.
     */
    AwsSyncClientBuilder createAWSClient(Map<String, String> credentials);

    /**
     * Build AWS Credential Provider with Session Credential or Static Credential.
     * @param credentials
     *        Represents the input of JMeter Java Request parameters.
     * @return AWSCredentialsProvider result of create Basic Session Credentials or Basic Credentials.
     */
    default AWSCredentialsProvider getAWSCredentials(Map<String, String> credentials){
        return Optional.ofNullable(credentials.get(AWSSampler.AWS_SESSION_TOKEN))
                .filter(Predicate.not(String::isEmpty))
                .map( sessionToken -> buildSessionAWSCredentials(credentials, sessionToken))
                .orElseGet( () -> buildBasicAWSCredentials(credentials));
    }

    /**
     * Build AWS Session Credential Provider object with access key, secret access key and session token.
     * @param credentials
     *        Represents the input of JMeter Java Request parameters.
     * @param sessionToken
     *        Security token when use MFA to protect programmatic calls.
     * @return AWSCredentialsProvider object created with access key, secret access key and session token.
     */
    default AWSCredentialsProvider buildSessionAWSCredentials(Map<String, String> credentials, String sessionToken){
        return new STSSessionCredentialsProvider(new BasicSessionCredentials(getAWSAccessKeyId(credentials),
                getAWSSecretAccessKey(credentials),
                sessionToken));
    }

    /**
     * Build AWS Static Credential Provider object with access key and secret access key.
     * @param credentials
     *        Represents the input of JMeter Java Request parameters.
     * @return AWSCredentialsProvider object created with access key, secret access key
     */
    default AWSCredentialsProvider buildBasicAWSCredentials(Map<String, String> credentials){
        return new AWSStaticCredentialsProvider(new BasicAWSCredentials(getAWSAccessKeyId(credentials),
                getAWSSecretAccessKey(credentials)));
    }

}

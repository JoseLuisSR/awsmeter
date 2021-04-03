package org.apache.jmeter.protocol.aws;

import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.core.SdkClient;

import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * Interface segregation with implementation of AWS SDK2.
 * @author JoseLuisSR
 * @since 01/27/2021
 * @see "https://github.com/JoseLuisSR/awsmeter"
 */
public interface AWSClientSDK2 extends AWSClient{

    /**
     * Create AWS Client by service to perform operations.
     * @param credentials
     *        Represents the input of JMeter Java Request parameters.
     * @return AwsSyncClientBuilder by AWS Service.
     */
    SdkClient createSdkClient(Map<String, String> credentials);

    /**
     * Build AWS Credential Provider with Session Credential or Static Credential.
     * @param credentials
     *        Represents the input of JMeter Java Request parameters.
     * @return AWSCredentialsProvider result of create Basic Session Credentials or Basic Credentials.
     */
    default AwsCredentialsProvider getAwsCredentialsProvider(Map<String, String> credentials){
        return Optional.ofNullable(credentials.get(AWSSampler.AWS_SESSION_TOKEN))
                .filter(Predicate.not(String::isEmpty))
                .map( sessionToken -> buildAWSSessionCredentials(credentials, sessionToken))
                .orElseGet(() -> buildAWSBasicCredentials(credentials));
    }

    /**
     * Build AWS Session Credential Provider object with access key, secret access key and session token.
     * @param credentials
     *        Represents the input of JMeter Java Request parameters.
     * @param sessionToken
     *        Security token when use MFA to protect programmatic calls.
     * @return AWSCredentialsProvider object created with access key, secret access key and session token.
     */
    default StaticCredentialsProvider buildAWSSessionCredentials(Map<String, String> credentials, String sessionToken){
        return StaticCredentialsProvider.create(AwsSessionCredentials.create(getAWSAccessKeyId(credentials),
                getAWSSecretAccessKey(credentials),
                sessionToken));
    }

    /**
     * Build AWS Static Credential Provider object with access key and secret access key.
     * @param credentials
     *        Represents the input of JMeter Java Request parameters.
     * @return AWSCredentialsProvider object created with access key, secret access key
     */
    default StaticCredentialsProvider buildAWSBasicCredentials(Map<String, String> credentials){
        return StaticCredentialsProvider.create(AwsBasicCredentials.create(getAWSAccessKeyId(credentials),
                getAWSSecretAccessKey(credentials)));
    }

}

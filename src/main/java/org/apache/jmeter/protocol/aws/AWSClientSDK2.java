package org.apache.jmeter.protocol.aws;

import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
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
     * Build AWS Credential Provider with Session Credential, Static Credential, or Default Credential Chain.
     * @param credentials
     *        Represents the input of JMeter Java Request parameters.
     * @return AWSCredentialsProvider result of create Basic Session Credentials, Basic Credentials, or Default Credentials.
     */
    default AwsCredentialsProvider getAwsCredentialsProvider(Map<String, String> credentials){
        
        // Check if explicit credentials are provided
        boolean hasExplicitAccessKey = Optional.ofNullable(credentials.get(AWSSampler.AWS_ACCESS_KEY_ID))
                .filter(Predicate.not(String::isEmpty))
                .isPresent();
        
        boolean hasExplicitSecretKey = Optional.ofNullable(credentials.get(AWSSampler.AWS_SECRET_ACCESS_KEY))
                .filter(Predicate.not(String::isEmpty))
                .isPresent();
        
        // If explicit credentials are provided, use static credentials
        if (hasExplicitAccessKey && hasExplicitSecretKey) {
            return Optional.ofNullable(credentials.get(AWSSampler.AWS_SESSION_TOKEN))
                    .filter(Predicate.not(String::isEmpty))
                    .map(sessionToken -> buildAWSSessionCredentials(credentials, sessionToken))
                    .orElse(buildAWSBasicCredentials(credentials));
        }
        
        // Check if a specific profile is configured (not default)
        String profileName = credentials.get(AWSSampler.AWS_CONFIG_PROFILE);
        boolean hasSpecificProfile = Optional.ofNullable(profileName)
                .filter(Predicate.not(String::isEmpty))
                .filter(profile -> !AWSSampler.AWS_DEFAULT_PROFILE.equals(profile))
                .isPresent();
        
        // If a specific profile is configured, use ProfileCredentialsProvider
        if (hasSpecificProfile) {
            return ProfileCredentialsProvider.builder()
                    .profileName(profileName)
                    .build();
        }
        
        // Otherwise, use the default credential provider chain which includes:
        // 1. Environment variables (AWS_ACCESS_KEY_ID, AWS_SECRET_ACCESS_KEY, AWS_SESSION_TOKEN)
        // 2. Java system properties 
        // 3. Shared credentials file (~/.aws/credentials)
        // 4. Amazon ECS container credentials (ECS task role)
        // 5. Amazon EC2 Instance profile credentials
        return DefaultCredentialsProvider.create();
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

        return StaticCredentialsProvider.create(AwsSessionCredentials.create(
                credentials.get(AWSSampler.AWS_ACCESS_KEY_ID),
                credentials.get(AWSSampler.AWS_SECRET_ACCESS_KEY),
                sessionToken));
    }

    /**
     * Build AWS Static Credential Provider object with access key and secret access key.
     * @param credentials
     *        Represents the input of JMeter Java Request parameters.
     * @return AWSCredentialsProvider object created with access key, secret access key
     */
    default StaticCredentialsProvider buildAWSBasicCredentials(Map<String, String> credentials){

        return StaticCredentialsProvider.create(AwsBasicCredentials.create(
                credentials.get(AWSSampler.AWS_ACCESS_KEY_ID),
                credentials.get(AWSSampler.AWS_SECRET_ACCESS_KEY)));
    }

}

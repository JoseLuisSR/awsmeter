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
        
        // If explicit credentials are provided, use static credentials
        if (hasExplicitCredentials(credentials)) {
            return buildStaticCredentialsProvider(credentials);
        }
        
        // If a specific profile is configured, use ProfileCredentialsProvider
        if (hasSpecificProfile(credentials)) {
            return buildProfileCredentialsProvider(credentials);
        }
        
        // Otherwise, use the default credential provider chain
        return DefaultCredentialsProvider.create();
    }

    /**
     * Build static credentials provider, choosing between session credentials and basic credentials
     * based on the presence of a session token.
     * @param credentials
     *        Represents the input of JMeter Java Request parameters.
     * @return StaticCredentialsProvider with session or basic credentials.
     */
    default AwsCredentialsProvider buildStaticCredentialsProvider(Map<String, String> credentials) {
        return Optional.ofNullable(credentials.get(AWSSampler.AWS_SESSION_TOKEN))
                .filter(Predicate.not(String::isEmpty))
                .map(sessionToken -> buildAWSSessionCredentials(credentials, sessionToken))
                .orElse(buildAWSBasicCredentials(credentials));
    }

    /**
     * Build AWS Profile Credentials Provider using the specified profile name.
     * @param credentials
     *        Represents the input of JMeter Java Request parameters.
     * @return ProfileCredentialsProvider configured with the specified profile name.
     */
    default ProfileCredentialsProvider buildProfileCredentialsProvider(Map<String, String> credentials) {
        String profileName = credentials.get(AWSSampler.AWS_CONFIG_PROFILE);
        return ProfileCredentialsProvider.builder()
                .profileName(profileName)
                .build();
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

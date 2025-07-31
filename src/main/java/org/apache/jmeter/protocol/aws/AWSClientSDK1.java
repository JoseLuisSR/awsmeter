package org.apache.jmeter.protocol.aws;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicSessionCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
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
     * Build AWS Credential Provider with Session Credential, Static Credential, or Default Credential Chain.
     * @param credentials
     *        Represents the input of JMeter Java Request parameters.
     * @return AWSCredentialsProvider result of create Basic Session Credentials, Basic Credentials, or Default Credentials.
     */
    default AWSCredentialsProvider getAWSCredentialsProvider(Map<String, String> credentials){
        
        // If explicit credentials are provided, use static credentials
        if (hasExplicitCredentials(credentials)) {
            return createExplicitCredentialsProvider(credentials);
        }
        
        // If a specific profile is configured, use ProfileCredentialsProvider
        if (hasSpecificProfile(credentials)) {
            String profileName = credentials.get(AWSSampler.AWS_CONFIG_PROFILE);
            return new ProfileCredentialsProvider(profileName);
        }
        
        // Otherwise, use the default credential provider chain which includes:
        // 1. Environment variables (AWS_ACCESS_KEY_ID, AWS_SECRET_ACCESS_KEY, AWS_SESSION_TOKEN)
        // 2. Java system properties 
        // 3. Shared credentials file (~/.aws/credentials)  
        // 4. Amazon ECS container credentials (ECS task role)
        // 5. Amazon EC2 Instance profile credentials
        return DefaultAWSCredentialsProviderChain.getInstance();
    }

    /**
     * Creates an AWS credentials provider using explicit credentials (access key and secret key).
     * If a session token is provided, creates session credentials; otherwise, creates basic credentials.
     * @param credentials
     *        Map containing the AWS configuration parameters from JMeter
     * @return AWSCredentialsProvider with session or basic credentials
     */
    default AWSCredentialsProvider createExplicitCredentialsProvider(Map<String, String> credentials) {
        return Optional.ofNullable(credentials.get(AWSSampler.AWS_SESSION_TOKEN))
                .filter(Predicate.not(String::isEmpty))
                .map(sessionToken -> buildSessionAWSCredentials(credentials, sessionToken))
                .orElse(buildBasicAWSCredentials(credentials));
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

        return new AWSStaticCredentialsProvider(new BasicSessionCredentials(
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
    default AWSCredentialsProvider buildBasicAWSCredentials(Map<String, String> credentials){

        return new AWSStaticCredentialsProvider(new BasicAWSCredentials(
                credentials.get(AWSSampler.AWS_ACCESS_KEY_ID),
                credentials.get(AWSSampler.AWS_SECRET_ACCESS_KEY)));
    }

}

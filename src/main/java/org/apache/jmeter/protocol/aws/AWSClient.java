package org.apache.jmeter.protocol.aws;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.regions.providers.DefaultAwsRegionProviderChain;

import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Interface segregation with common implementation between AWS SDK 1 and 2.
 * @author JoseLuisSR
 * @since 01/27/2021
 * @see "https://github.com/JoseLuisSR/awsmeter"
 */
public interface AWSClient {

    Logger log = LoggerFactory.getLogger(AWSClient.class);

    /**
     * Get AWS Region from input of JMeter Java Request parameter or from credentials file.
     * @param credentials
     *        Represents the input of JMeter Java Request parameters.
     * @return AWS Region String.
     */
    default String getAWSRegion(Map<String, String> credentials){

        return Optional.ofNullable(credentials.get(AWSSampler.AWS_REGION))
                .filter(Predicate.not(String::isEmpty))
                .orElseGet(() -> getDefaultAwsRegionProviderChain.apply(credentials)
                        .getRegion()
                        .toString());
    }

    /**
     * Get AWS IAM user Access Key id from input of JMeter Java Request parameter or from credentials file.
     * @param credentials
     *        Represents the input of JMeter Java Request parameters.
     * @return AWS IAM user Access Key id String.
     */
    default String getAWSAccessKeyId(Map<String, String> credentials){

        return Optional.ofNullable(credentials.get(AWSSampler.AWS_ACCESS_KEY_ID))
                .filter(Predicate.not(String::isEmpty))
                .orElseGet(() -> getProfileCredentialsProvider.apply(credentials)
                        .resolveCredentials()
                        .accessKeyId());
    }

    /**
     * Get AWS IAM Secret Access Key from input of JMeter Java Request parameter or from credentials file.
     * @param credentials
     *        Represents the input of JMeter Java Request parameters.
     * @return AWS IAM user Secret Access Key String.
     */
    default String getAWSSecretAccessKey(Map<String, String> credentials){

        return Optional.ofNullable(credentials.get(AWSSampler.AWS_SECRET_ACCESS_KEY))
                .filter(Predicate.not(String::isEmpty))
                .orElseGet(() -> getProfileCredentialsProvider.apply(credentials)
                        .resolveCredentials()
                        .secretAccessKey());
    }

    /**
     * Get AWS Session Token from input of JMeter Java Request parameter or from credentials file.
     * @param credentials
     *        Represents the input of JMeter Java Request parameters.
     * @return AWS Session Token String.
     */
    default String getAWSSessionToken(Map<String, String> credentials){

        return Optional.ofNullable(credentials.get(AWSSampler.AWS_SESSION_TOKEN))
                .filter(Predicate.not(String::isEmpty))
                .orElseGet(() -> {
                    try {
                        return ((AwsSessionCredentials)getProfileCredentialsProvider.apply(credentials)
                                .resolveCredentials())
                                .sessionToken();
                    } catch (Exception e) {
                        return null;
                    }
                });
    }

    /**
     * Get custom AWS endpoint from input of JMeter Java Request parameter.
     * @param credentials
     *        Represents the input of JMeter Java Request parameters.
     * @return AWS endpoint String.
     */
    default String getAWSEndpoint(Map<String, String> credentials, String service, String region){
        return Optional.ofNullable(credentials.get(AWSSampler.AWS_ENDPOINT_CUSTOM))
                .filter(Predicate.not(String::isEmpty))
                .orElse(String.format(AWSSampler.AWS_ENDPOINT, service, region));
    }

    /**
     * Function to get DefaultAwsRegionProviderChain from profile file.
     */
    Function<Map<String, String>, DefaultAwsRegionProviderChain> getDefaultAwsRegionProviderChain = credentials ->
            DefaultAwsRegionProviderChain.builder()
                    .profileName(credentials.get(AWSSampler.AWS_CONFIG_PROFILE))
                    .build();

    /**
     * Function to get ProfileCredentialsProvider from profile file.
     */
    Function<Map<String, String>, ProfileCredentialsProvider> getProfileCredentialsProvider = credentials ->
            ProfileCredentialsProvider.builder()
                    .profileName(credentials.get(AWSSampler.AWS_CONFIG_PROFILE))
                    .build();

}

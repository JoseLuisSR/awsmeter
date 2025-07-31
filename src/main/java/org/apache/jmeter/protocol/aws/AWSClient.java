package org.apache.jmeter.protocol.aws;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.regions.providers.DefaultAwsRegionProviderChain;

import java.util.Map;
import java.util.Optional;
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
     * Get AWS Region from input of JMeter Java Request parameter or from default provider chain.
     * @param credentials
     *        Represents the input of JMeter Java Request parameters.
     * @return AWS Region String.
     */
    default String getAWSRegion(Map<String, String> credentials){
        return Optional.ofNullable(credentials.get(AWSSampler.AWS_REGION))
                .filter(Predicate.not(String::isEmpty))
                .orElseGet(() -> getRegionFromProviderChain(credentials));
    }

    /**
     * Gets AWS region from the provider chain, considering the configured profile.
     * @param credentials
     *        Represents the input of JMeter Java Request parameters.
     * @return AWS Region String from provider chain.
     */
    default String getRegionFromProviderChain(Map<String, String> credentials) {

        if (hasSpecificProfile(credentials)) {
            return getRegionWithProfile(credentials);
        } else {
            return getRegionWithDefaultProviderChain();
        }
    }

    /**
     * Checks if a specific AWS profile (not the default profile) is configured.
     * @param credentials
     *        Map containing the AWS configuration parameters from JMeter
     * @return true if a specific profile is configured, false if default or empty
     */
    default boolean hasSpecificProfile(Map<String, String> credentials) {
        
        String profileName = credentials.get(AWSSampler.AWS_CONFIG_PROFILE);
        return Optional.ofNullable(profileName)
                .filter(Predicate.not(String::isEmpty))
                .filter(profile -> !AWSSampler.AWS_DEFAULT_PROFILE.equals(profile))
                .isPresent();
    }

    /**
     * Gets AWS region using the specified profile.
     * This will use the DefaultAwsRegionProviderChain with the provided profile name.
     * @param credentials
     *        Represents the input of JMeter Java Request parameters.
     * @return AWS Region String from specified profile.
     */
    default String getRegionWithProfile(Map<String, String> credentials) {

        String profileName = credentials.get(AWSSampler.AWS_CONFIG_PROFILE);
        return DefaultAwsRegionProviderChain.builder()
                .profileName(profileName)
                .build()
                .getRegion()
                .toString();
    }

    /**
     * Gets AWS region using the default provider chain.
     * This will check environment variables, instance metadata, etc.
     * @return AWS Region String from default provider chain.
     */
    default String getRegionWithDefaultProviderChain() {
        return DefaultAwsRegionProviderChain.builder()
                .build()
                .getRegion()
                .toString();
    }


    /**
     * Checks if explicit AWS credentials (access key and secret key) are provided in the configuration.
     * @param credentials
     *        Map containing the AWS configuration parameters from JMeter
     * @return true if both access key and secret key are present and not empty, false otherwise
     */
    default boolean hasExplicitCredentials(Map<String, String> credentials) {
        boolean hasExplicitAccessKey = isParameterPresent(credentials, AWSSampler.AWS_ACCESS_KEY_ID);
        boolean hasExplicitSecretKey = isParameterPresent(credentials, AWSSampler.AWS_SECRET_ACCESS_KEY);
        
        return hasExplicitAccessKey && hasExplicitSecretKey;
    }

    /**
     * Utility method to check if a parameter is present and not empty in the credentials map.
     * @param credentials
     *        Map containing the AWS configuration parameters from JMeter
     * @param parameterKey
     *        The key of the parameter to check
     * @return true if the parameter exists and is not empty, false otherwise
     */
    default boolean isParameterPresent(Map<String, String> credentials, String parameterKey) {
        return Optional.ofNullable(credentials.get(parameterKey))
                .filter(Predicate.not(String::isEmpty))
                .isPresent();
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

}

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
                .orElseGet(() -> {
                    // Check if a specific profile is configured (not default)
                    String profileName = credentials.get(AWSSampler.AWS_CONFIG_PROFILE);
                    boolean hasSpecificProfile = Optional.ofNullable(profileName)
                            .filter(Predicate.not(String::isEmpty))
                            .filter(profile -> !AWSSampler.AWS_DEFAULT_PROFILE.equals(profile))
                            .isPresent();
                    
                    if (hasSpecificProfile) {
                        // Use profile-specific region provider
                        return DefaultAwsRegionProviderChain.builder()
                                .profileName(profileName)
                                .build()
                                .getRegion()
                                .toString();
                    } else {
                        // Use default region provider chain (without profile constraint)
                        // This will check environment variables, instance metadata, etc.
                        return DefaultAwsRegionProviderChain.builder()
                                .build()
                                .getRegion()
                                .toString();
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

}

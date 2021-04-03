package org.apache.jmeter.protocol.aws;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
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
     * Get AWS Region from input of JMeter Java Request parameter or from credentials file.
     * @param credentials
     *        Represents the input of JMeter Java Request parameters.
     * @return AWS Region String.
     */
    default String getAWSRegion(Map<String, String> credentials){
        return Optional.ofNullable(credentials.get(AWSSampler.AWS_REGION))
                .filter(Predicate.not(String::isEmpty))
                .orElseGet( () -> {
                    log.info("Looking the region.");
                    return DefaultAwsRegionProviderChain.builder()
                            .profileName(credentials.get(AWSSampler.AWS_CONFIG_PROFILE))
                            .build()
                            .getRegion().toString();
                });
    }

    /**
     * Get AWS IAM user Access Key Id from input of JMeter Java Request parameter or from credentials file.
     * @param credentials
     *        Represents the input of JMeter Java Request parameters.
     * @return AWS IAM user Access Key Id String
     */
    default String getAWSAccessKeyId(Map<String, String> credentials){
        return Optional.ofNullable(credentials.get(AWSSampler.AWS_ACCESS_KEY_ID))
                .filter(Predicate.not(String::isEmpty))
                .orElseGet(() ->{
                    log.info("Looking aws credentials access key id.");
                    return DefaultCredentialsProvider.builder()
                            .profileName(credentials.get(AWSSampler.AWS_CONFIG_PROFILE))
                            .build()
                            .resolveCredentials()
                            .accessKeyId();
                });
    }

    /**
     * Get AWS IAM Secret Access Key from input of JMeter Java Request parameter or from credentials file.
     * @param credentials
     *        Represents the input of JMeter Java Request parameters.
     * @return AWS IAM user Secret Access Key String
     */
    default String getAWSSecretAccessKey(Map<String, String> credentials){
        return Optional.ofNullable(credentials.get(AWSSampler.AWS_SECRET_ACCESS_KEY))
                .filter(Predicate.not(String::isEmpty))
                .orElseGet(() ->{
                    log.info("Looking aws credentials secret access key.");
                    return DefaultCredentialsProvider.builder()
                            .profileName(credentials.get(AWSSampler.AWS_CONFIG_PROFILE))
                            .build()
                            .resolveCredentials()
                            .secretAccessKey();
                });
    }

}

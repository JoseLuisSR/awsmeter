package org.apache.jmeter.protocol.aws;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.providers.DefaultAwsRegionProviderChain;

import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

public interface AWSClient {

    Logger log = LoggerFactory.getLogger(AWSClient.class);

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

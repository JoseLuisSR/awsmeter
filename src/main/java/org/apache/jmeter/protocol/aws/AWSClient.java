package org.apache.jmeter.protocol.aws;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.core.SdkClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.regions.providers.DefaultAwsRegionProviderChain;

import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

public interface AWSClient {

    Logger log = LoggerFactory.getLogger(AWSClient.class);

    SdkClient createSdkClient(Map<String, String> credentials);

    default Region awsRegion(Map<String, String> credentials){

        return Region.of(Optional.ofNullable(credentials.get(AWSSampler.AWS_REGION))
                .filter(Predicate.not(String::isEmpty))
                .orElseGet( () -> {
                    log.info("Looking the region.");
                    return DefaultAwsRegionProviderChain.builder()
                            .profileName(credentials.get(AWSSampler.AWS_CONFIG_PROFILE))
                            .build()
                            .getRegion().toString();
                }));
    }

    default AwsCredentialsProvider awsCredentialProvider(Map<String, String> credentials){

        String accessKeyId = Optional.ofNullable(credentials.get(AWSSampler.AWS_ACCESS_KEY_ID))
                .filter(Predicate.not(String::isEmpty))
                .orElseGet(() ->{
                    log.info("Looking aws credentials access key id.");
                    return DefaultCredentialsProvider.builder()
                            .profileName(credentials.get(AWSSampler.AWS_CONFIG_PROFILE))
                            .build()
                            .resolveCredentials()
                            .accessKeyId();
                    });

        String secretAccessKey = Optional.ofNullable(credentials.get(AWSSampler.AWS_SECRET_ACCESS_KEY))
                .filter(Predicate.not(String::isEmpty))
                .orElseGet(() ->{
                    log.info("Looking aws credentials secret access key.");
                    return DefaultCredentialsProvider.builder()
                            .profileName(credentials.get(AWSSampler.AWS_CONFIG_PROFILE))
                            .build()
                            .resolveCredentials()
                            .secretAccessKey();
                });

        Optional<String> sessionToken = Optional.ofNullable(credentials.get(AWSSampler.AWS_SESSION_TOKEN))
                .filter(Predicate.not(String::isEmpty));

        if( sessionToken.isPresent() )
            return StaticCredentialsProvider.create(AwsSessionCredentials.create(accessKeyId, secretAccessKey, sessionToken.get()));
        else
            return StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKeyId, secretAccessKey));

    }

}

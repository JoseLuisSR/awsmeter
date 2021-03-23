package org.apache.jmeter.protocol.aws;

import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.core.SdkClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.regions.providers.DefaultAwsRegionProviderChain;

import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

public interface AWSClientSDK2 extends AWSClient{

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

        return Optional.ofNullable(credentials.get(AWSSampler.AWS_SESSION_TOKEN))
                .filter(Predicate.not(String::isEmpty))
                .map( sessionToken -> buildAWSSessionCredentials(credentials, sessionToken))
                .orElseGet(() -> buildAWSBasicCredentials(credentials));
    }

    default StaticCredentialsProvider buildAWSSessionCredentials(Map<String, String> credentials, String sessionToken){
        return StaticCredentialsProvider.create(AwsSessionCredentials.create(getAWSAccessKeyId(credentials),
                getAWSSecretAccessKey(credentials),
                sessionToken));
    }

    default StaticCredentialsProvider buildAWSBasicCredentials(Map<String, String> credentials){
        return StaticCredentialsProvider.create(AwsBasicCredentials.create(getAWSAccessKeyId(credentials),
                getAWSSecretAccessKey(credentials)));
    }

}

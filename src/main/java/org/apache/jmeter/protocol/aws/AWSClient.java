package org.apache.jmeter.protocol.aws;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.auth.credentials.*;
import software.amazon.awssdk.core.SdkClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.regions.providers.DefaultAwsRegionProviderChain;

import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

public interface AWSClient {

    Logger log = LoggerFactory.getLogger(AWSClient.class);

    String AWS_ACCESS_KEY_ID = "aws_access_key_id";

    String AWS_SECRET_ACCESS_KEY = "aws_secret_access_key";

    String AWS_SESSION_TOKEN = "aws_session_token";

    String AWS_REGION = "aws_region";

    String AWS_CONFIG_PROFILE = "aws_configure_profile";

    String AWS_DEFAULT_PROFILE = "default";

    SdkClient createSdkClient(Map<String, String> credentials);

    default Region awsRegion(Map<String, String> credentials){

        return Region.of(Optional.ofNullable(credentials.get(AWS_REGION))
                .filter(Predicate.not(String::isEmpty))
                .orElseGet( () -> {
                    log.info("Looking the region.");
                    return DefaultAwsRegionProviderChain.builder()
                            .profileName(credentials.get(AWS_CONFIG_PROFILE))
                            .build()
                            .getRegion().toString();
                }));
    }

    default AwsCredentialsProvider awsCredentialProvider(Map<String, String> credentials){

        String accessKeyId = Optional.ofNullable(credentials.get(AWS_ACCESS_KEY_ID))
                .filter(Predicate.not(String::isEmpty))
                .orElseGet(() ->{
                    log.info("Looking aws credentials access key id.");
                    return DefaultCredentialsProvider.builder()
                            .profileName(credentials.get(AWS_CONFIG_PROFILE))
                            .build()
                            .resolveCredentials()
                            .accessKeyId();
                    });

        String secretAccessKey = Optional.ofNullable(credentials.get(AWS_SECRET_ACCESS_KEY))
                .filter(Predicate.not(String::isEmpty))
                .orElseGet(() ->{
                    log.info("Looking aws credentials secret access key.");
                    return DefaultCredentialsProvider.builder()
                            .profileName(credentials.get(AWS_CONFIG_PROFILE))
                            .build()
                            .resolveCredentials()
                            .secretAccessKey();
                });

        Optional<String> sessionToken = Optional.ofNullable(credentials.get(AWS_SESSION_TOKEN))
                .filter(Predicate.not(String::isEmpty));

        if( sessionToken.isPresent() )
            return StaticCredentialsProvider.create(AwsSessionCredentials.create(accessKeyId, secretAccessKey, sessionToken.get()));
        else
            return StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKeyId, secretAccessKey));

    }

}

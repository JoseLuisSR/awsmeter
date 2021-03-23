package org.apache.jmeter.protocol.aws;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicSessionCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.STSSessionCredentialsProvider;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.client.builder.AwsSyncClientBuilder;
import com.amazonaws.regions.AwsProfileRegionProvider;
import com.amazonaws.regions.AwsRegionProvider;
import com.amazonaws.regions.Regions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

public interface AWSClientSDK1 {

    Logger log = LoggerFactory.getLogger(AWSClientSDK1.class);

    AwsSyncClientBuilder createAWSClient(Map<String, String> credentials);

    default Regions getAWSRegion(Map<String, String> credentials){

        return Regions.fromName(Optional.ofNullable(credentials.get(AWSSampler.AWS_REGION))
                .filter(Predicate.not(String::isEmpty))
                .orElseGet(() -> {
                    log.info("Looking the region.");
                    AwsRegionProvider awsRegionProvider = new AwsProfileRegionProvider(credentials
                            .get(AWSSampler.AWS_CONFIG_PROFILE));
                    return awsRegionProvider.getRegion();
                }));
    }

    default AWSCredentialsProvider getAWSCredentials(Map<String, String> credentials){

        return Optional.ofNullable(credentials.get(AWSSampler.AWS_SESSION_TOKEN))
                .filter(Predicate.not(String::isEmpty))
                .map( sessionToken -> buildSessionAWSCredentials(credentials, sessionToken))
                .orElseGet( () -> buildBasicAWSCredentials(credentials));
    }

    default AWSCredentialsProvider buildSessionAWSCredentials(Map<String, String> credentials, String sessionToken){

        return new STSSessionCredentialsProvider(new BasicSessionCredentials(getAWSAccessKeyId(credentials),
                getAWSSecretKey(credentials),
                sessionToken));
    }

    default AWSCredentialsProvider buildBasicAWSCredentials(Map<String, String> credentials){
        return new AWSStaticCredentialsProvider(new BasicAWSCredentials(getAWSAccessKeyId(credentials),
                getAWSSecretKey(credentials)));
    }

    default String getAWSAccessKeyId(Map<String, String> credentials){
        return Optional.ofNullable(credentials.get(AWSSampler.AWS_ACCESS_KEY_ID))
                .filter(Predicate.not(String::isEmpty))
                .orElseGet(() -> {
                    log.info("Looking aws credentials access key id.");
                    ProfileCredentialsProvider credentialsProvider = new ProfileCredentialsProvider(credentials
                            .get(AWSSampler.AWS_CONFIG_PROFILE));
                    return credentialsProvider
                            .getCredentials()
                            .getAWSAccessKeyId();
                });
    }

    default String getAWSSecretKey(Map<String, String> credentials){
        return Optional.ofNullable(credentials.get(AWSSampler.AWS_SECRET_ACCESS_KEY))
                .filter(Predicate.not(String::isEmpty))
                .orElseGet(() -> {
                    log.info("Looking aws credentials secret access key.");
                    ProfileCredentialsProvider credentialsProvider = new ProfileCredentialsProvider(credentials
                            .get(AWSSampler.AWS_CONFIG_PROFILE));
                    return credentialsProvider
                            .getCredentials()
                            .getAWSSecretKey();
                });
    }
}

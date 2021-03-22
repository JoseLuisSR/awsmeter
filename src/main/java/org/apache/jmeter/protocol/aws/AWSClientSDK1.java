package org.apache.jmeter.protocol.aws;

import com.amazonaws.AmazonWebServiceClient;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicSessionCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.regions.AwsProfileRegionProvider;
import com.amazonaws.regions.AwsRegionProvider;
import com.amazonaws.regions.Regions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

public interface AWSClientSDK1 {

    Logger log = LoggerFactory.getLogger(AWSClient.class);

    AmazonWebServiceClient createAWSClient(Map<String, String> credentials);

    default Regions getAWSRegion(Map<String, String> credentials){

        return Regions.valueOf(Optional.ofNullable(credentials.get(AWSSampler.AWS_REGION))
                .filter(Predicate.not(String::isEmpty))
                .orElseGet(() -> {
                    AwsRegionProvider awsRegionProvider = new AwsProfileRegionProvider(credentials
                            .get(AWSSampler.AWS_CONFIG_PROFILE));
                    return awsRegionProvider.getRegion();
                }));
    }

    default AWSCredentials getAWSCredentials(Map<String, String> credentials){

        return Optional.ofNullable(credentials.get(AWSSampler.AWS_SESSION_TOKEN))
                .filter(Predicate.not(String::isEmpty))
                .map( sessionToken -> buildSessionAWSCredentials(credentials, sessionToken))
                .orElseGet( () -> buildBasicAWSCredentials(credentials));
    }

    default AWSCredentials buildSessionAWSCredentials(Map<String, String> credentials, String sessionToken){
        return new BasicSessionCredentials(getAWSAccessKeyId(credentials),
                getAWSSecretKey(credentials),
                sessionToken);
    }

    default AWSCredentials buildBasicAWSCredentials(Map<String, String> credentials){
        return new BasicAWSCredentials(getAWSAccessKeyId(credentials),
                getAWSSecretKey(credentials));
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

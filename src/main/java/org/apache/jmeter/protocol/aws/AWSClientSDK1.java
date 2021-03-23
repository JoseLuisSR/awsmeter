package org.apache.jmeter.protocol.aws;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicSessionCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.STSSessionCredentialsProvider;
import com.amazonaws.client.builder.AwsSyncClientBuilder;

import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

public interface AWSClientSDK1 extends AWSClient{

    AwsSyncClientBuilder createAWSClient(Map<String, String> credentials);

    default AWSCredentialsProvider getAWSCredentials(Map<String, String> credentials){
        return Optional.ofNullable(credentials.get(AWSSampler.AWS_SESSION_TOKEN))
                .filter(Predicate.not(String::isEmpty))
                .map( sessionToken -> buildSessionAWSCredentials(credentials, sessionToken))
                .orElseGet( () -> buildBasicAWSCredentials(credentials));
    }

    default AWSCredentialsProvider buildSessionAWSCredentials(Map<String, String> credentials, String sessionToken){
        return new STSSessionCredentialsProvider(new BasicSessionCredentials(getAWSAccessKeyId(credentials),
                getAWSSecretAccessKey(credentials),
                sessionToken));
    }

    default AWSCredentialsProvider buildBasicAWSCredentials(Map<String, String> credentials){
        return new AWSStaticCredentialsProvider(new BasicAWSCredentials(getAWSAccessKeyId(credentials),
                getAWSSecretAccessKey(credentials)));
    }

}

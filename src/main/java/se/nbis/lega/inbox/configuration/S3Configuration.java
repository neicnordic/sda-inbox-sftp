package se.nbis.lega.inbox.configuration;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.Protocol;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * S3 beans definitions.
 */
@Configuration
public class S3Configuration {

    private String s3Region;
    private String s3Endpoint;
    private String s3AccessKey;
    private String s3SecretKey;
    private boolean useSSL;

    @ConditionalOnExpression("!'${inbox.s3.access-key}'.isEmpty() && !'${inbox.s3.secret-key}'.isEmpty()")
    @Bean
    public AmazonS3 amazonS3() {
        AwsClientBuilder.EndpointConfiguration endpointConfiguration = new AwsClientBuilder.EndpointConfiguration(s3Endpoint, s3Region);
        AWSCredentials credentials = new BasicAWSCredentials(s3AccessKey, s3SecretKey);
        AWSStaticCredentialsProvider awsStaticCredentialsProvider = new AWSStaticCredentialsProvider(credentials);
        ClientConfiguration clientConfiguration = new ClientConfiguration();
        clientConfiguration.setProtocol(useSSL ? Protocol.HTTPS : Protocol.HTTP);
        return AmazonS3ClientBuilder.standard()
                .withEndpointConfiguration(endpointConfiguration)
                .withCredentials(awsStaticCredentialsProvider)
                .withClientConfiguration(clientConfiguration)
                .withPathStyleAccessEnabled(true)
                .build();
    }

    @Value("${inbox.s3.region}")
    public void setS3Region(String s3Region) {
        this.s3Region = s3Region;
    }

    @Value("${inbox.s3.endpoint}")
    public void setS3Endpoint(String s3Endpoint) {
        this.s3Endpoint = s3Endpoint;
    }

    @Value("${inbox.s3.access-key}")
    public void setS3AccessKey(String s3AccessKey) {
        this.s3AccessKey = s3AccessKey;
    }

    @Value("${inbox.s3.secret-key}")
    public void setS3SecretKey(String s3SecretKey) {
        this.s3SecretKey = s3SecretKey;
    }

    @Value("${inbox.s3.use-ssl}")
    public void setUseSSL(boolean useSSL) {
        this.useSSL = useSSL;
    }

}

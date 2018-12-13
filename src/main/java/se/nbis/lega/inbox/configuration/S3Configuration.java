package se.nbis.lega.inbox.configuration;

import io.minio.MinioClient;
import io.minio.errors.InvalidEndpointException;
import io.minio.errors.InvalidPortException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * S3 beans definitions.
 */
@Configuration
public class S3Configuration {

    private String s3Region;
    private String s3Host;
    private String s3Port;
    private String s3AccessKey;
    private String s3SecretKey;
    private boolean useSSL;

    @ConditionalOnExpression("!'${inbox.s3.access-key}'.isEmpty() && !'${inbox.s3.secret-key}'.isEmpty()")
    @Bean
    public MinioClient minioClient() throws InvalidPortException, InvalidEndpointException {
        return new MinioClient(s3Host, Integer.valueOf(s3Port), s3AccessKey, s3SecretKey, s3Region, useSSL);
    }

    @Value("${inbox.s3.region}")
    public void setS3Region(String s3Region) {
        this.s3Region = s3Region;
    }

    @Value("${inbox.s3.host}")
    public void setS3Host(String s3Host) {
        this.s3Host = s3Host;
    }

    @Value("${inbox.s3.port}")
    public void setS3Port(String s3Port) {
        this.s3Port = s3Port;
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

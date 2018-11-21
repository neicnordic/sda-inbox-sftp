package se.nbis.lega.inbox.configuration;

import com.amazonaws.util.StringUtils;
import io.minio.MinioClient;
import io.minio.errors.InvalidEndpointException;
import io.minio.errors.InvalidPortException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * Common beans definitions.
 */
@Configuration
public class CommonConfiguration {

    private String s3Host;
    private String s3AccessKey;
    private String s3SecretKey;
    private boolean useSSL;

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    @Bean
    public MinioClient minioClient() throws InvalidPortException, InvalidEndpointException {
        return StringUtils.isNullOrEmpty(s3AccessKey) ? null : new MinioClient(s3Host, s3AccessKey, s3SecretKey, useSSL);
    }

    @Value("${inbox.s3.host}")
    public void setS3Host(String s3Host) {
        this.s3Host = s3Host;
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

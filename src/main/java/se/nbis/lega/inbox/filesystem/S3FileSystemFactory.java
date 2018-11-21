package se.nbis.lega.inbox.filesystem;

import com.amazonaws.Protocol;
import com.upplication.s3fs.AmazonS3Factory;
import com.upplication.s3fs.S3FileSystemProvider;
import io.minio.MinioClient;
import lombok.extern.slf4j.Slf4j;
import org.apache.sshd.common.file.FileSystemFactory;
import org.apache.sshd.common.session.Session;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.nio.file.FileSystem;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
public class S3FileSystemFactory implements FileSystemFactory {

    private String s3Host;
    private String s3Port;
    private String s3AccessKey;
    private String s3SecretKey;
    private boolean useSSL;
    private String pathStyleAccess;

    private MinioClient minioClient;

    @Override
    public FileSystem createFileSystem(Session session) {
        String username = session.getUsername();

        try {
            if (!minioClient.bucketExists(username)) {
                minioClient.makeBucket(username);
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new RuntimeException(e.getMessage(), e);
        }

        String uri = String.format("s3://%s:%s/%s", s3Host, s3Port, username);
        log.info("Inbox initialized: {}", uri);

        Map<String, String> env = new HashMap<>();
        env.put(AmazonS3Factory.PROTOCOL, useSSL ? Protocol.HTTPS.name() : Protocol.HTTP.name());
        env.put(AmazonS3Factory.PATH_STYLE_ACCESS, pathStyleAccess);
        env.put(AmazonS3Factory.ACCESS_KEY, s3AccessKey);
        env.put(AmazonS3Factory.SECRET_KEY, s3SecretKey);

        return new S3FileSystemProvider().newFileSystem(URI.create(uri), env);
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

    @Value("${inbox.s3.path-style-access}")
    public void setPathStyleAccess(String pathStyleAccess) {
        this.pathStyleAccess = pathStyleAccess;
    }

    @Autowired(required = false)
    public void setMinioClient(MinioClient minioClient) {
        this.minioClient = minioClient;
    }

}

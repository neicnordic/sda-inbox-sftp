package se.nbis.lega.inbox.filesystem;

import com.upplication.s3fs.AmazonS3Factory;
import com.upplication.s3fs.S3FileSystemProvider;
import lombok.extern.slf4j.Slf4j;
import org.apache.sshd.common.file.FileSystemFactory;
import org.apache.sshd.common.session.Session;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.net.URI;
import java.nio.file.FileSystem;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
public class S3FileSystemFactory implements FileSystemFactory {

    private String s3URI;
    private String s3AccessKey;
    private String s3SecretKey;

    @Override
    public FileSystem createFileSystem(Session session) {
        String username = session.getUsername();
        String root;
        if (s3URI.endsWith(File.separator)) {
            root = s3URI + username;
        } else {
            root = s3URI + File.separator + username;
        }
        log.info("Inbox initialized: {}", root);
        Map<String, String> env = new HashMap<>();
        env.put(AmazonS3Factory.ACCESS_KEY, s3AccessKey);
        env.put(AmazonS3Factory.SECRET_KEY, s3SecretKey);

        return new S3FileSystemProvider().newFileSystem(URI.create(root), env);
    }

    @Value("${inbox.s3.uri}")
    public void setS3URI(String s3URI) {
        this.s3URI = s3URI;
    }

    @Value("${inbox.s3.access-key}")
    public void setS3AccessKey(String s3AccessKey) {
        this.s3AccessKey = s3AccessKey;
    }

    @Value("${inbox.s3.secret-key}")
    public void setS3SecretKey(String s3SecretKey) {
        this.s3SecretKey = s3SecretKey;
    }

}

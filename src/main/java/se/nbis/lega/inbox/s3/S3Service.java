package se.nbis.lega.inbox.s3;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.transfer.TransferManagerBuilder;
import com.amazonaws.services.s3.transfer.Upload;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;

import java.nio.file.Path;

@Slf4j
@ConditionalOnBean(AmazonS3.class)
@Service
public class S3Service {

    private AmazonS3 amazonS3;

    public void prepareBucket(String username) {
        try {
            if (!amazonS3.doesBucketExistV2(username)) {
                amazonS3.createBucket(username);
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    public void upload(String bucket, Path path) {
        Upload upload = TransferManagerBuilder.standard().withS3Client(amazonS3).build().upload(bucket, getKey(path), path.toFile());
        log.info(upload.getDescription());
    }

    private String getKey(Path path) {
        String key = path.toString();
        if (key.startsWith("/")) {
            key = key.substring(1);
        }
        return key;
    }

    @Autowired
    public void setAmazonS3(AmazonS3 amazonS3) {
        this.amazonS3 = amazonS3;
    }

}

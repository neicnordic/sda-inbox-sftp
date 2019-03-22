package se.nbis.lega.inbox.s3;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.amazonaws.services.s3.transfer.TransferManagerBuilder;
import com.amazonaws.services.s3.transfer.Upload;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.util.Collection;
import java.util.stream.Collectors;

/**
 * Service for communicating with S3 backend.
 * Optional bean: initialized only if S3 keys are present in the context.
 */
@Slf4j
@ConditionalOnExpression("!'${inbox.s3.access-key}'.isEmpty() && !'${inbox.s3.secret-key}'.isEmpty()")
@Service
public class S3Service {

    private AmazonS3 amazonS3;

    /**
     * Creates a bucket with a username if it doesn't exist yet.
     *
     * @param bucket Bucket name.
     */
    public void prepareBucket(String bucket) {
        try {
            if (!amazonS3.doesBucketExistV2(bucket)) {
                amazonS3.createBucket(bucket);
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    public Collection<String> listKeys(String bucket) {
        return amazonS3.listObjects(bucket).getObjectSummaries().stream().map(S3ObjectSummary::getKey).collect(Collectors.toSet());
    }

    /**
     * Uploads a file to a specified bucket synchronously.
     *
     * @param bucket Bucket name.
     * @param key    S3 key. If not specified - obtained from path.
     * @param path   Path of the file.
     * @param sync   <code>true</code> for synchronous upload, <code>false</code> otherwise.
     */
    public void upload(String bucket, String key, Path path, boolean sync) throws InterruptedException {
        log.info("Initializing S3 upload, sync = {}", sync);
        Upload upload = TransferManagerBuilder.standard().withS3Client(amazonS3).build().upload(bucket, key == null ? getKey(path) : key, path.toFile());
        if (sync) {
            log.info("Waiting for upload to finish: {}", upload.getDescription());
            upload.waitForUploadResult();
        }
        log.info(upload.getDescription());
    }

    /**
     * Moves a file from one location to another.
     *
     * @param bucket  Bucket name.
     * @param srcPath Source location.
     * @param dstPath Destination location.
     */
    public void move(String bucket, Path srcPath, Path dstPath) {
        log.info("Moving {} to {}", getKey(srcPath), getKey(dstPath));
        if (dstPath.toFile().isDirectory()) {
            moveFolder(bucket, srcPath, dstPath);
        } else {
            moveFile(bucket, srcPath, dstPath);
        }
    }

    private void moveFolder(String bucket, Path srcPath, Path dstPath) {
        ObjectListing objectListing = amazonS3.listObjects(bucket, getKey(srcPath));
        for (S3ObjectSummary s3ObjectSummary : objectListing.getObjectSummaries()) {
            String srcKey = s3ObjectSummary.getKey();
            String dstKey = getKey(dstPath) + srcKey.replace(getKey(srcPath) + "/", "/");
            amazonS3.copyObject(bucket, srcKey, bucket, dstKey);
            amazonS3.deleteObject(bucket, srcKey);
        }
    }

    private void moveFile(String bucket, Path srcPath, Path dstPath) {
        amazonS3.copyObject(bucket, getKey(srcPath), bucket, getKey(dstPath));
        amazonS3.deleteObject(bucket, getKey(srcPath));
    }

    /**
     * Removes file by path.
     *
     * @param bucket Bucket name.
     * @param path   Path to file to remove.
     */
    public void remove(String bucket, Path path) {
        log.info("Removing object {}", getKey(path));
        amazonS3.deleteObject(bucket, getKey(path));
    }

    /**
     * Converts filesystem <code>path</code> to S3 key.
     *
     * @param path Filesystem <code>path</code>.
     * @return S3 key.
     */
    public String getKey(Path path) {
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

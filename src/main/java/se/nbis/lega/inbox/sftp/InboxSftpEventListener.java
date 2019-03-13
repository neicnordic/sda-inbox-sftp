package se.nbis.lega.inbox.sftp;

import com.amazonaws.services.s3.AmazonS3;
import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.sshd.server.session.ServerSession;
import org.apache.sshd.server.subsystem.sftp.FileHandle;
import org.apache.sshd.server.subsystem.sftp.Handle;
import org.apache.sshd.server.subsystem.sftp.SftpEventListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;
import se.nbis.lega.inbox.pojo.EncryptedIntegrity;
import se.nbis.lega.inbox.pojo.FileDescriptor;
import se.nbis.lega.inbox.pojo.Operation;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.CopyOption;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.apache.commons.codec.digest.MessageDigestAlgorithms.MD5;
import static org.apache.commons.codec.digest.MessageDigestAlgorithms.SHA_256;
import static se.nbis.lega.inbox.pojo.Operation.*;

/**
 * <code>SftpEventListener</code> implementation that publishes message to MQ upon file uploading completion.
 * Optional bean: initialized only if <code>AmazonS3</code> is NOT present in the context.
 */
@Slf4j
@ConditionalOnMissingBean(AmazonS3.class)
@Component
public class InboxSftpEventListener implements SftpEventListener {

    public static final List<String> SUPPORTED_ALGORITHMS = Arrays.asList(
            MD5,
            SHA_256.toLowerCase().replace("-", "")
    );

    protected String exchange;
    protected String routingKeyChecksums;
    protected String routingKeyFiles;

    protected Gson gson;
    protected RabbitTemplate rabbitTemplate;

    /**
     * {@inheritDoc}
     */
    @Override
    public void initialized(ServerSession session, int version) {
        log.info("SFTP session initialized for user: {}", session.getUsername());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void destroying(ServerSession session) {
        log.info("SFTP session closed for user: {}", session.getUsername());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void blocked(ServerSession session, String remoteHandle, FileHandle localHandle, long offset, long length, int mask, Throwable thrown) {
        log.info("User {} blocked file: {}", session.getUsername(), localHandle.getFile());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void unblocked(ServerSession session, String remoteHandle, FileHandle localHandle, long offset, long length, Throwable thrown) {
        log.info("User {} unblocked file: {}", session.getUsername(), localHandle.getFile());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void created(ServerSession session, Path path, Map<String, ?> attrs, Throwable thrown) {
        log.info("User {} created directory: {}", session.getUsername(), path);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void removed(ServerSession session, Path path, Throwable thrown) {
        log.info("User {} removed entry: {}", session.getUsername(), path);
        try {
            processFile(REMOVE, session.getUsername(), null, path);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void linked(ServerSession session, Path source, Path target, boolean symLink, Throwable thrown) {
        log.info("User {} linked {} to {}", session.getUsername(), source, target);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void modifiedAttributes(ServerSession session, Path path, Map<String, ?> attrs, Throwable thrown) {
        log.info("User {} modified attributes of {}: ", session.getUsername(), path, attrs);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void written(ServerSession session, String remoteHandle, FileHandle localHandle, long offset, byte[] data, int dataOffset, int dataLen, Throwable thrown) {
        if (thrown != null) {
            log.error(thrown.getMessage(), thrown);
        } else {
            session.getProperties().put(localHandle.getFile().toString(), true);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void moved(ServerSession session, Path srcPath, Path dstPath, Collection<CopyOption> opts, Throwable thrown) {
        if (thrown != null) {
            log.error(thrown.getMessage(), thrown);
        } else {
            log.info("User {} moved entry {} to {}", session.getUsername(), srcPath, dstPath);
            try {
                processFile(RENAME, session.getUsername(), srcPath, dstPath);
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void close(ServerSession session, String remoteHandle, Handle localHandle) {
        Path path = localHandle.getFile();
        boolean fileModified = session.getBooleanProperty(path.toString(), false);
        if (fileModified) {
            try {
                closed(session, remoteHandle, localHandle);
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
        }
    }

    /**
     * Triggered when file was closed after modification (not after reading).
     *
     * @param session      SFTP session.
     * @param remoteHandle Remote handle.
     * @param localHandle  Local handle.
     * @throws IOException In case of an IO error.
     */
    protected void closed(ServerSession session, String remoteHandle, Handle localHandle) throws IOException, InterruptedException {
        Path path = localHandle.getFile();
        processFile(UPLOAD, session.getUsername(), null, path);
        session.getProperties().remove(path.toString());
    }

    /**
     * Handles file event.
     *
     * @param operation The type of file event.
     * @param username  Username.
     * @param srcPath   Old path of the affected file.
     * @param dstPath   New path of the affected file.
     * @throws IOException In case of an IO error.
     */
    protected void processFile(Operation operation, String username, Path srcPath, Path dstPath) throws IOException {
        File file = dstPath.toFile();
        String extension = FilenameUtils.getExtension(file.getName());
        log.info("File {} affected by user {}", dstPath.toString(), username);
        if (REMOVE == operation) {
            FileDescriptor fileDescriptor = new FileDescriptor();
            fileDescriptor.setUser(username);
            fileDescriptor.setFilePath(getFilePath(dstPath));
            fileDescriptor.setOperation(operation.name().toLowerCase());
            publishMessage(file, extension, fileDescriptor);
        } else if (file.exists() && file.isFile()) {
            FileDescriptor fileDescriptor = new FileDescriptor();
            fileDescriptor.setUser(username);
            fileDescriptor.setFilePath(getFilePath(dstPath));
            fileDescriptor.setFileLastModified(file.lastModified() / 1000);
            fileDescriptor.setFileSize(FileUtils.sizeOf(file));
            fileDescriptor.setOperation(operation.name().toLowerCase());
            if (RENAME == operation) {
                fileDescriptor.setOldPath(getFilePath(srcPath));
            }
            String digest = DigestUtils.sha256Hex(FileUtils.openInputStream(file));
            fileDescriptor.setEncryptedIntegrity(new EncryptedIntegrity[]{
                    new EncryptedIntegrity(SHA_256.toLowerCase().replace("-", ""), digest)
            });
            publishMessage(file, extension, fileDescriptor);
        }
    }

    /**
     * Publishes message to MQ.
     *
     * @param file           Affected file.
     * @param extension      Affected file's extension.
     * @param fileDescriptor File descriptor to serialize and send.
     * @throws IOException In case of an IO error.
     */
    protected void publishMessage(File file, String extension, FileDescriptor fileDescriptor) throws IOException {
        if (SUPPORTED_ALGORITHMS.contains(extension.toLowerCase()) || SUPPORTED_ALGORITHMS.contains(extension.toUpperCase())) {
            String content = FileUtils.readFileToString(file, Charset.defaultCharset());
            fileDescriptor.setContent(content);
            rabbitTemplate.convertAndSend(exchange, routingKeyChecksums, gson.toJson(fileDescriptor));
        } else {
            rabbitTemplate.convertAndSend(exchange, routingKeyFiles, gson.toJson(fileDescriptor));
        }
    }

    protected String getFilePath(Path path) {
        return path.toFile().getAbsolutePath();
    }

    @Value("${inbox.mq.exchange}")
    public void setExchange(String exchange) {
        this.exchange = exchange;
    }

    @Value("${inbox.mq.routing-key.checksums}")
    public void setRoutingKeyChecksums(String routingKeyChecksums) {
        this.routingKeyChecksums = routingKeyChecksums;
    }

    @Value("${inbox.mq.routing-key.files}")
    public void setRoutingKeyFiles(String routingKeyFiles) {
        this.routingKeyFiles = routingKeyFiles;
    }

    @Autowired
    public void setGson(Gson gson) {
        this.gson = gson;
    }

    @Autowired
    public void setRabbitTemplate(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

}

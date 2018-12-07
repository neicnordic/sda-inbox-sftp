package se.nbis.lega.inbox.sftp;

import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.codec.digest.MessageDigestAlgorithms;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.sshd.server.session.ServerSession;
import org.apache.sshd.server.subsystem.sftp.FileHandle;
import org.apache.sshd.server.subsystem.sftp.Handle;
import org.apache.sshd.server.subsystem.sftp.SftpEventListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import se.nbis.lega.inbox.pojo.EncryptedIntegrity;
import se.nbis.lega.inbox.pojo.FileDescriptor;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.CopyOption;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * Component that composes and publishes message to MQ upon file uploading completion.
 */
@Slf4j
@Component
public class InboxSftpEventListener implements SftpEventListener {

    private static final List<String> SUPPORTED_ALGORITHMS = Arrays.asList(MessageDigestAlgorithms.MD5, MessageDigestAlgorithms.SHA_256);

    private String exchange;
    private String routingKeyChecksums;
    private String routingKeyFiles;
    private Gson gson;
    private RabbitTemplate rabbitTemplate;

    /**
     * {@inheritDoc}
     */
    @Override
    public void written(ServerSession session, String remoteHandle, FileHandle localHandle, long offset, byte[] data, int dataOffset, int dataLen, Throwable thrown) throws IOException {
        if (thrown != null) {
            log.error(thrown.getMessage(), thrown);
        } else {
            handleFileCreationModification(session, localHandle.getFile().toFile());
        }
    }

    private void handleFileCreationModification(ServerSession session, File file) {
        if (file.exists() && file.isFile()) {
            boolean fileModified = session.getBooleanProperty(file.getPath(), false);
            if (!fileModified) {
                session.getProperties().put(file.getPath(), true);
                log.info("File {} created or modified", file.getPath());
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void moved(ServerSession session, Path srcPath, Path dstPath, Collection<CopyOption> opts, Throwable thrown) throws IOException {
        if (thrown != null) {
            log.error(thrown.getMessage(), thrown);
        } else {
            // TODO: Think about what to do with the source location (or a case of file removal).
            processUploadedFile(session.getUsername(), dstPath.toFile());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void close(ServerSession session, String remoteHandle, Handle localHandle) {
        File file = localHandle.getFile().toFile();
        boolean fileModified = session.getBooleanProperty(file.getPath(), false);
        if (fileModified) {
            try {
                processUploadedFile(session.getUsername(), file);
                session.getProperties().remove(file.getPath());
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
        }
    }

    private void processUploadedFile(String username, File file) throws IOException {
        if (file.exists() && file.isFile()) {
            log.info("File {} uploaded or moved by user {}", file.getAbsolutePath(), username);
            String extension = FilenameUtils.getExtension(file.getName());
            FileDescriptor fileDescriptor = new FileDescriptor();
            fileDescriptor.setUser(username);
            fileDescriptor.setFilePath(file.getAbsolutePath());
            if (SUPPORTED_ALGORITHMS.contains(extension.toLowerCase()) || SUPPORTED_ALGORITHMS.contains(extension.toUpperCase())) {
                String digest = FileUtils.readFileToString(file, Charset.defaultCharset());
                fileDescriptor.setContent(digest);
                rabbitTemplate.convertAndSend(exchange, routingKeyChecksums, gson.toJson(fileDescriptor));
            } else {
                fileDescriptor.setFileSize(FileUtils.sizeOf(file));
                String digest = DigestUtils.md5Hex(FileUtils.openInputStream(file));
                fileDescriptor.setEncryptedIntegrity(new EncryptedIntegrity(digest, MessageDigestAlgorithms.MD5));
                rabbitTemplate.convertAndSend(exchange, routingKeyFiles, gson.toJson(fileDescriptor));
            }
        }
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

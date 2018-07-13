package se.nbis.lega.inbox.sftp;

import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.codec.digest.MessageDigestAlgorithms;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.sshd.server.session.ServerSession;
import org.apache.sshd.server.subsystem.sftp.AbstractSftpEventListenerAdapter;
import org.apache.sshd.server.subsystem.sftp.Handle;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import se.nbis.lega.inbox.pojo.EncryptedIntegrity;
import se.nbis.lega.inbox.pojo.FileDescriptor;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.List;

/**
 * Component that composes and publishes message to MQ upon file uploading completion.
 */
@Slf4j
@Component
public class InboxSftpEventListener extends AbstractSftpEventListenerAdapter {

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
    public void close(ServerSession session, String remoteHandle, Handle localHandle) {
        File file = localHandle.getFile().toFile();
        if (file.exists() && file.isFile()) {
            try {
                processUploadedFile(session.getUsername(), file);
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
        }
        super.close(session, remoteHandle, localHandle);
    }

    private void processUploadedFile(String username, File file) throws IOException {
        log.info(String.format("File %s uploaded by user %s", file.getAbsolutePath(), username));
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

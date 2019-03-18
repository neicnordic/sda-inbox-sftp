package se.nbis.lega.inbox.s3;

import com.amazonaws.services.s3.AmazonS3;
import lombok.extern.slf4j.Slf4j;
import org.apache.sshd.server.session.ServerSession;
import org.apache.sshd.server.subsystem.sftp.Handle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;
import se.nbis.lega.inbox.sftp.InboxSftpEventListener;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.CopyOption;
import java.nio.file.Path;
import java.util.Collection;

/**
 * <code>SftpEventListener</code> implementation with support for S3 operations.
 * Optional bean: initialized only if <code>AmazonS3</code> is present in the context.
 */
@Slf4j
@ConditionalOnBean(AmazonS3.class)
@Component
public class S3SftpEventListener extends InboxSftpEventListener {

    private S3Service s3Service;

    @PostConstruct
    @Override
    public void init() {
        log.info("Initializing {}", this.getClass());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void initialized(ServerSession session, int version) {
        s3Service.prepareBucket(session.getUsername());
        super.initialized(session, version);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void removed(ServerSession session, Path path, Throwable thrown) {
        s3Service.remove(session.getUsername(), path);
        super.removed(session, path, thrown);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void moved(ServerSession session, Path srcPath, Path dstPath, Collection<CopyOption> opts, Throwable thrown) {
        s3Service.move(session.getUsername(), srcPath, dstPath);
        super.moved(session, srcPath, dstPath, opts, thrown);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void closed(ServerSession session, String remoteHandle, Handle localHandle) throws IOException, InterruptedException {
        s3Service.upload(session.getUsername(), null, localHandle.getFile(), true);
        super.closed(session, remoteHandle, localHandle);
    }

    @Override
    protected String getFilePath(Path path) {
        return s3Service.getKey(path);
    }

    @Autowired
    public void setS3Service(S3Service s3Service) {
        this.s3Service = s3Service;
    }

}

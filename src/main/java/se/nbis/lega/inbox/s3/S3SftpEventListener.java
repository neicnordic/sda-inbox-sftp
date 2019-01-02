package se.nbis.lega.inbox.s3;

import lombok.extern.slf4j.Slf4j;
import org.apache.sshd.server.session.ServerSession;
import org.apache.sshd.server.subsystem.sftp.Handle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;
import se.nbis.lega.inbox.sftp.InboxSftpEventListener;

import java.io.IOException;
import java.nio.file.CopyOption;
import java.nio.file.Path;
import java.util.Collection;

@Slf4j
@ConditionalOnBean(S3Service.class)
@Component
public class S3SftpEventListener extends InboxSftpEventListener {

    private S3Service s3Service;

    @Override
    public void initialized(ServerSession session, int version) {
        s3Service.prepareBucket(session.getUsername());
        super.initialized(session, version);
    }

    @Override
    public void removed(ServerSession session, Path path, Throwable thrown) {
        s3Service.remove(session.getUsername(), path);
        super.removed(session, path, thrown);
    }

    @Override
    public void moved(ServerSession session, Path srcPath, Path dstPath, Collection<CopyOption> opts, Throwable thrown) {
        s3Service.move(session.getUsername(), srcPath, dstPath);
        super.moved(session, srcPath, dstPath, opts, thrown);
    }

    @Override
    protected void closed(ServerSession session, String remoteHandle, Handle localHandle) throws IOException {
        s3Service.upload(session.getUsername(), localHandle.getFile());
        super.closed(session, remoteHandle, localHandle);
    }

    @Autowired
    public void setS3Service(S3Service s3Service) {
        this.s3Service = s3Service;
    }

}

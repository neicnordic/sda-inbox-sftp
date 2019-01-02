package se.nbis.lega.inbox.s3;

import lombok.extern.slf4j.Slf4j;
import org.apache.sshd.server.session.ServerSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;
import se.nbis.lega.inbox.sftp.InboxSftpEventListener;

import java.io.IOException;
import java.nio.file.Path;

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
    protected void processUploadedFile(String username, Path path) throws IOException {
        s3Service.upload(username, path);
        super.processUploadedFile(username, path);
    }

    @Autowired
    public void setS3Service(S3Service s3Service) {
        this.s3Service = s3Service;
    }

}

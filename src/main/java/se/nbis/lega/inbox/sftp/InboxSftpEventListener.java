package se.nbis.lega.inbox.sftp;

import lombok.extern.slf4j.Slf4j;
import org.apache.sshd.server.session.ServerSession;
import org.apache.sshd.server.subsystem.sftp.AbstractSftpEventListenerAdapter;
import org.apache.sshd.server.subsystem.sftp.Handle;
import org.springframework.stereotype.Component;

import java.io.File;

@Slf4j
@Component
public class InboxSftpEventListener extends AbstractSftpEventListenerAdapter {

    @Override
    public void close(ServerSession session, String remoteHandle, Handle localHandle) {
        File file = localHandle.getFile().toFile();
        if (file.exists() && file.isFile()) {
            log.info(file.getAbsolutePath());
        }
        super.close(session, remoteHandle, localHandle);
    }

}

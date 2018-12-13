package se.nbis.lega.inbox.s3;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;
import se.nbis.lega.inbox.sftp.InboxSftpEventListener;

@Slf4j
@ConditionalOnBean(S3Service.class)
@Component
public class S3SftpEventListener extends InboxSftpEventListener {


}

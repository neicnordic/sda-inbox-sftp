package se.nbis.lega.inbox.sftp;

import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.transport.verification.PromiscuousVerifier;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

import static se.nbis.lega.inbox.sftp.TestInboxApplication.PASSWORD;
import static se.nbis.lega.inbox.sftp.TestInboxApplication.USERNAME;

@SpringBootTest(classes = TestInboxApplication.class)
@TestPropertySource(locations = "classpath:application.properties")
@RunWith(SpringRunner.class)
public class InboxAuthenticatorTest {

    private int inboxPort;

    @Test
    public void authenticatePassword() throws IOException {
        SSHClient ssh = new SSHClient();
        ssh.addHostKeyVerifier(new PromiscuousVerifier());
        ssh.connect("localhost", inboxPort);
        ssh.authPassword(USERNAME, PASSWORD);
        Assert.assertNotNull(ssh.newSFTPClient());
    }

    @Test
    public void authenticatePublicKey() throws IOException, URISyntaxException {
        SSHClient ssh = new SSHClient();
        ssh.addHostKeyVerifier(new PromiscuousVerifier());
        ssh.connect("localhost", inboxPort);
        ClassLoader classloader = Thread.currentThread().getContextClassLoader();
        File privateKey = new File(classloader.getResource(USERNAME + ".sec").toURI());
        ssh.authPublickey(USERNAME, privateKey.getPath());
        Assert.assertNotNull(ssh.newSFTPClient());
    }

    @Value("${inbox.port}")
    public void setInboxPort(int inboxPort) {
        this.inboxPort = inboxPort;
    }

}
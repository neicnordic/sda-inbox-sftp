package se.nbis.lega.inbox.sftp;

import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.transport.verification.PromiscuousVerifier;
import net.schmizz.sshj.userauth.UserAuthException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.UUID;

import static org.junit.Assert.assertNotNull;
import static se.nbis.lega.inbox.sftp.TestInboxApplication.PASSWORD;
import static se.nbis.lega.inbox.sftp.TestInboxApplication.USERNAME;

@SpringBootTest(classes = TestInboxApplication.class)
@TestPropertySource(locations = "classpath:application.properties")
@RunWith(SpringRunner.class)
public class InboxAuthenticatorTest {

    private int inboxPort;

    private SSHClient ssh;

    @Before
    public void setUp() throws IOException {
        ssh = new SSHClient();
        ssh.addHostKeyVerifier(new PromiscuousVerifier());
        ssh.connect("localhost", inboxPort);
    }

    @After
    public void tearDown() throws IOException {
        ssh.close();
    }

    @Test
    public void authenticatePasswordSuccess() throws IOException {
        ssh.authPassword(USERNAME, PASSWORD);
        assertNotNull(ssh.newSFTPClient());
    }

    @Test
    public void authenticatePublicKeySuccess() throws IOException, URISyntaxException {
        ClassLoader classloader = Thread.currentThread().getContextClassLoader();
        File privateKey = new File(classloader.getResource(USERNAME + ".sec").toURI());
        ssh.authPublickey(USERNAME, privateKey.getPath());
        assertNotNull(ssh.newSFTPClient());
    }

    @Test(expected = UserAuthException.class)
    public void authenticatePasswordFail() throws IOException {
        ssh.authPassword(UUID.randomUUID().toString(), PASSWORD);
    }

    @Test(expected = UserAuthException.class)
    public void authenticatePublicKeyFail() throws IOException, URISyntaxException {
        ClassLoader classloader = Thread.currentThread().getContextClassLoader();
        File privateKey = new File(classloader.getResource(USERNAME + ".sec").toURI());
        ssh.authPublickey(UUID.randomUUID().toString(), privateKey.getPath());
    }

    @Value("${inbox.port}")
    public void setInboxPort(int inboxPort) {
        this.inboxPort = inboxPort;
    }

}
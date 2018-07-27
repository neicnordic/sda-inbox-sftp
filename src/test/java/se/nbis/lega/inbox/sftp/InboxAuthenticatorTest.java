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
import se.nbis.lega.inbox.pojo.PasswordHashingAlgorithm;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.UUID;

import static org.junit.Assert.assertNotNull;

@SpringBootTest(classes = TestInboxApplication.class)
@TestPropertySource(locations = "classpath:application.properties")
@RunWith(SpringRunner.class)
public class InboxAuthenticatorTest extends InboxTest {

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
    public void authenticatePasswordMD5() throws IOException, URISyntaxException {
        mockCEGAEndpoint(username, password, PasswordHashingAlgorithm.MD5);
        ssh.authPassword(username, password);
        assertNotNull(ssh.newSFTPClient());
    }

    @Test
    public void authenticatePasswordSHA256() throws IOException, URISyntaxException {
        mockCEGAEndpoint(username, password, PasswordHashingAlgorithm.SHA256);
        ssh.authPassword(username, password);
        assertNotNull(ssh.newSFTPClient());
    }

    @Test
    public void authenticatePasswordSHA512() throws IOException, URISyntaxException {
        mockCEGAEndpoint(username, password, PasswordHashingAlgorithm.SHA512);
        ssh.authPassword(username, password);
        assertNotNull(ssh.newSFTPClient());
    }

    @Test
    public void authenticatePasswordBlowfish() throws IOException, URISyntaxException {
        mockCEGAEndpoint(username, password, PasswordHashingAlgorithm.BLOWFISH);
        ssh.authPassword(username, password);
        assertNotNull(ssh.newSFTPClient());
    }

    @Test(expected = UserAuthException.class)
    public void authenticatePasswordFail() throws IOException {
        ssh.authPassword(username, UUID.randomUUID().toString());
    }

    @Test
    public void authenticatePublicKey() throws IOException, URISyntaxException {
        ClassLoader classloader = Thread.currentThread().getContextClassLoader();
        File privateKey = new File(classloader.getResource("key.sec").toURI());
        ssh.authPublickey(username, privateKey.getPath());
        assertNotNull(ssh.newSFTPClient());
    }

    @Test(expected = UserAuthException.class)
    public void authenticatePublicKeyFail() throws IOException, URISyntaxException {
        ClassLoader classloader = Thread.currentThread().getContextClassLoader();
        File privateKey = new File(classloader.getResource("key.sec").toURI());
        ssh.authPublickey(UUID.randomUUID().toString(), privateKey.getPath());
    }

    @Value("${inbox.port}")
    public void setInboxPort(int inboxPort) {
        this.inboxPort = inboxPort;
    }

}
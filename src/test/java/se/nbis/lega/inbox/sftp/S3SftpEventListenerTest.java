package se.nbis.lega.inbox.sftp;

import com.amazonaws.services.s3.AmazonS3;
import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.sftp.SFTPClient;
import net.schmizz.sshj.transport.verification.PromiscuousVerifier;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.codec.digest.MessageDigestAlgorithms;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import se.nbis.lega.inbox.pojo.EncryptedIntegrity;
import se.nbis.lega.inbox.pojo.FileDescriptor;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.concurrent.BlockingQueue;

import static org.junit.Assert.*;

@SpringBootTest(classes = S3StorageInboxApplication.class)
@TestPropertySource(locations = "classpath:s3-storage.application.properties")
@RunWith(SpringRunner.class)
public class S3SftpEventListenerTest extends InboxTest {

    private int inboxPort;
    private BlockingQueue<FileDescriptor> fileBlockingQueue;
    private BlockingQueue<FileDescriptor> hashBlockingQueue;
    private AmazonS3 amazonS3;

    private File file;
    private File hash;
    private SSHClient ssh;
    private SFTPClient sftpClient;

    @Before
    public void setUp() throws IOException {
        File dataFolder = new File(System.getProperty("user.dir"));
        file = File.createTempFile("data", ".raw", dataFolder);
        file.deleteOnExit();
        FileUtils.writeStringToFile(file, "hello", Charset.defaultCharset());
        hash = File.createTempFile("data", ".md5", dataFolder);
        hash.deleteOnExit();
        FileUtils.writeStringToFile(hash, "hello", Charset.defaultCharset());

        ssh = new SSHClient();
        ssh.addHostKeyVerifier(new PromiscuousVerifier());
        ssh.connect("localhost", inboxPort);
        ssh.authPassword(username, password);
        sftpClient = ssh.newSFTPClient();
    }

    @After
    public void tearDown() throws IOException {
        ssh.close();
    }

    @Test
    public void uploadFile() throws IOException {
        sftpClient.put(file.getAbsolutePath(), file.getName());

        FileDescriptor fileDescriptor = fileBlockingQueue.poll();
        assertNotNull(fileDescriptor);
        assertEquals(username, fileDescriptor.getUser());
        assertEquals(file.getName(), fileDescriptor.getFilePath());
        assertTrue(amazonS3.doesObjectExist(fileDescriptor.getUser(), fileDescriptor.getFilePath()));
        assertNull(fileDescriptor.getContent());
        assertEquals(FileUtils.sizeOf(file), fileDescriptor.getFileSize());
        EncryptedIntegrity encryptedIntegrity = fileDescriptor.getEncryptedIntegrity();
        assertNotNull(encryptedIntegrity);
        assertEquals(MessageDigestAlgorithms.MD5, encryptedIntegrity.getAlgorithm());
        assertEquals(DigestUtils.md5Hex(FileUtils.openInputStream(file)), encryptedIntegrity.getChecksum());
    }

    @Test
    public void uploadHash() throws IOException {
        sftpClient.put(hash.getAbsolutePath(), hash.getName());

        FileDescriptor fileDescriptor = hashBlockingQueue.poll();
        assertNotNull(fileDescriptor);
        assertEquals(username, fileDescriptor.getUser());
        assertEquals(hash.getName(), fileDescriptor.getFilePath());
        assertTrue(amazonS3.doesObjectExist(fileDescriptor.getUser(), fileDescriptor.getFilePath()));
        assertEquals(FileUtils.readFileToString(hash, Charset.defaultCharset()), fileDescriptor.getContent());
        assertEquals(0, fileDescriptor.getFileSize());
        EncryptedIntegrity encryptedIntegrity = fileDescriptor.getEncryptedIntegrity();
        assertNull(encryptedIntegrity);
    }

    @Test
    public void moveFile() throws IOException {
        sftpClient.put(file.getAbsolutePath(), file.getName());

        fileBlockingQueue.poll();
        sftpClient.mkdir("test");

        sftpClient.rename(file.getName(), "test/" + file.getName());

        FileDescriptor fileDescriptor = fileBlockingQueue.poll();
        assertNotNull(fileDescriptor);
        assertEquals(username, fileDescriptor.getUser());
        assertEquals("test/" + file.getName(), fileDescriptor.getFilePath());
        assertTrue(amazonS3.doesObjectExist(fileDescriptor.getUser(), fileDescriptor.getFilePath()));
        assertNull(fileDescriptor.getContent());
        assertEquals(FileUtils.sizeOf(file), fileDescriptor.getFileSize());
        EncryptedIntegrity encryptedIntegrity = fileDescriptor.getEncryptedIntegrity();
        assertNotNull(encryptedIntegrity);
        assertEquals(MessageDigestAlgorithms.MD5, encryptedIntegrity.getAlgorithm());
        assertEquals(DigestUtils.md5Hex(FileUtils.openInputStream(file)), encryptedIntegrity.getChecksum());
    }

    @Value("${inbox.port}")
    public void setInboxPort(int inboxPort) {
        this.inboxPort = inboxPort;
    }

    @Autowired
    public void setFileBlockingQueue(BlockingQueue<FileDescriptor> fileBlockingQueue) {
        this.fileBlockingQueue = fileBlockingQueue;
    }

    @Autowired
    public void setHashBlockingQueue(BlockingQueue<FileDescriptor> hashBlockingQueue) {
        this.hashBlockingQueue = hashBlockingQueue;
    }

    @Autowired
    public void setAmazonS3(AmazonS3 amazonS3) {
        this.amazonS3 = amazonS3;
    }

}

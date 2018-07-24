package se.nbis.lega.inbox.sftp;

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
import static se.nbis.lega.inbox.sftp.TestInboxApplication.PASSWORD;
import static se.nbis.lega.inbox.sftp.TestInboxApplication.USERNAME;

@SpringBootTest(classes = TestInboxApplication.class)
@TestPropertySource(locations = "classpath:application.properties")
@RunWith(SpringRunner.class)
public class InboxSftpEventListenerTest {

    private String inboxFolder;
    private int inboxPort;
    private BlockingQueue<FileDescriptor> fileBlockingQueue;
    private BlockingQueue<FileDescriptor> hashBlockingQueue;

    private File file;
    private File hash;
    private SSHClient ssh;
    private SFTPClient sftpClient;

    @Before
    public void setUp() throws IOException {
        File dataFolder = new File(inboxFolder);
        file = File.createTempFile("data", ".raw", dataFolder);
        file.deleteOnExit();
        FileUtils.writeStringToFile(file, "hello", Charset.defaultCharset());
        hash = File.createTempFile("data", ".md5", dataFolder);
        hash.deleteOnExit();
        FileUtils.writeStringToFile(hash, "hello", Charset.defaultCharset());

        ssh = new SSHClient();
        ssh.addHostKeyVerifier(new PromiscuousVerifier());
        ssh.connect("localhost", inboxPort);
        ssh.authPassword(USERNAME, PASSWORD);
        sftpClient = ssh.newSFTPClient();
    }

    @After
    public void tearDown() throws IOException {
        ssh.close();
        File userFolder = new File(inboxFolder + "/" + USERNAME + "/");
        FileUtils.deleteDirectory(userFolder);
    }

    @Test
    public void uploadFile() throws IOException {
        sftpClient.put(file.getAbsolutePath(), file.getName());

        FileDescriptor fileDescriptor = fileBlockingQueue.poll();
        assertNotNull(fileDescriptor);
        assertEquals(USERNAME, fileDescriptor.getUser());
        assertEquals(inboxFolder + "/" + USERNAME + "/" + file.getName(), fileDescriptor.getFilePath());
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
        assertEquals(USERNAME, fileDescriptor.getUser());
        assertEquals(inboxFolder + "/" + USERNAME + "/" + hash.getName(), fileDescriptor.getFilePath());
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
        assertEquals(USERNAME, fileDescriptor.getUser());
        assertEquals(inboxFolder + "/" + USERNAME + "/test/" + file.getName(), fileDescriptor.getFilePath());
        assertNull(fileDescriptor.getContent());
        assertEquals(FileUtils.sizeOf(file), fileDescriptor.getFileSize());
        EncryptedIntegrity encryptedIntegrity = fileDescriptor.getEncryptedIntegrity();
        assertNotNull(encryptedIntegrity);
        assertEquals(MessageDigestAlgorithms.MD5, encryptedIntegrity.getAlgorithm());
        assertEquals(DigestUtils.md5Hex(FileUtils.openInputStream(file)), encryptedIntegrity.getChecksum());
    }

    @Value("${inbox.directory}")
    public void setInboxFolder(String inboxFolder) {
        this.inboxFolder = inboxFolder;
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

}
package se.nbis.lega.inbox.sftp;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import se.nbis.lega.inbox.pojo.Credentials;

import java.io.IOException;

import static se.nbis.lega.inbox.sftp.TestInboxApplication.*;

@SpringBootTest(classes = TestInboxApplication.class)
@TestPropertySource(locations = "classpath:application.properties")
@RunWith(SpringRunner.class)
public class CredentialsProviderTest {

    private CredentialsProvider credentialsProvider;

    @Test
    public void getCredentials() throws IOException {
        Credentials credentials = credentialsProvider.getCredentials(USERNAME);
        Assert.assertEquals(PASSWORD_HASH, credentials.getPasswordHash());
        Assert.assertEquals(PUBLIC_KEY, credentials.getPublicKey());
    }

    @Autowired
    public void setCredentialsProvider(CredentialsProvider credentialsProvider) {
        this.credentialsProvider = credentialsProvider;
    }

}
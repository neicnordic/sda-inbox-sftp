package se.nbis.lega.inbox.sftp;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import se.nbis.lega.inbox.pojo.Credentials;

import java.io.IOException;
import java.net.URISyntaxException;

import static org.junit.Assert.assertEquals;

@SpringBootTest(classes = TestInboxApplication.class)
@TestPropertySource(locations = "classpath:application.properties")
@RunWith(SpringRunner.class)
public class CredentialsProviderTest extends InboxTest {

    private CredentialsProvider credentialsProvider;

    @Test
    public void getCredentials() throws IOException, URISyntaxException {
        Credentials credentials = credentialsProvider.getCredentials(username);
        assertEquals(passwordHash, credentials.getPasswordHash());
        assertEquals(pubKey, credentials.getPublicKey());
    }

    @Autowired
    public void setCredentialsProvider(CredentialsProvider credentialsProvider) {
        this.credentialsProvider = credentialsProvider;
    }

}
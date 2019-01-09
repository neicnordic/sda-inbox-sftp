package se.nbis.lega.inbox.sftp;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.client.RestClientException;
import se.nbis.lega.inbox.pojo.Credentials;
import se.nbis.lega.inbox.pojo.KeyAlgorithm;
import se.nbis.lega.inbox.pojo.PasswordHashingAlgorithm;

import java.io.IOException;
import java.net.URISyntaxException;

import static org.junit.Assert.assertEquals;

@RunWith(SpringRunner.class)
public class CredentialsProviderTest extends InboxTest {

    private CredentialsProvider credentialsProvider;

    @Test
    public void getCredentialsSuccess() throws IOException, URISyntaxException {
        Credentials credentials = credentialsProvider.getCredentials(username);
        assertEquals(passwordHash, credentials.getPasswordHash());
        assertEquals(publicKey, credentials.getPublicKey());
    }

    @Test(expected = RestClientException.class)
    public void getCredentialsFail() throws IOException, URISyntaxException {
        mockCEGAEndpoint(username, password, PasswordHashingAlgorithm.BLOWFISH, KeyAlgorithm.RSA, HttpStatus.BAD_REQUEST);
        credentialsProvider.getCredentials(username);
    }

    @Autowired
    public void setCredentialsProvider(CredentialsProvider credentialsProvider) {
        this.credentialsProvider = credentialsProvider;
    }

}

package se.nbis.lega.inbox.sftp;

import com.google.gson.Gson;
import org.apache.http.HttpHeaders;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import se.nbis.lega.inbox.pojo.Credentials;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Base64;

/**
 * Component that queries CEGA for user credentials.
 */
@Component
public class CredentialsProvider {

    private String cegaEndpoint;
    private String cegaCredentials;

    private RestTemplate restTemplate;
    private Gson gson;

    /**
     * Queries CEGA REST endpoint to obtain user credentials.
     *
     * @param username Target user name.
     * @return User's credentials.
     * @throws IOException        In case we can't read from remote endpoint.
     * @throws URISyntaxException In case URL is in a wrong format.
     */
    public Credentials getCredentials(String username) throws IOException, URISyntaxException {
        URL url = new URL(cegaEndpoint + username);
        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
        headers.set(HttpHeaders.AUTHORIZATION, "Basic " + Base64.getEncoder().encodeToString(cegaCredentials.getBytes()));
        String response = restTemplate.exchange(url.toURI(), HttpMethod.GET, new HttpEntity<>(headers), String.class).getBody();
        return gson.fromJson(response, Credentials.class);
    }

    @Value("${inbox.cega.endpoint}")
    public void setCegaEndpoint(String cegaEndpoint) {
        this.cegaEndpoint = cegaEndpoint;
    }

    @Value("${inbox.cega.credentials}")
    public void setCegaCredentials(String cegaCredentials) {
        this.cegaCredentials = cegaCredentials;
    }

    @Autowired
    public void setRestTemplate(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Autowired
    public void setGson(Gson gson) {
        this.gson = gson;
    }

}

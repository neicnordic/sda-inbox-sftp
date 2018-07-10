package se.nbis.lega.inbox.sftp;

import com.google.gson.Gson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import se.nbis.lega.inbox.pojo.Credentials;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.Base64;

@Component
public class CredentialsProvider {

    private String cegaEndpoint;
    private String cegaCredentials;

    private Gson gson;

    public Credentials getCredentials(String username) throws IOException {
        URL url = new URL(cegaEndpoint + username);
        URLConnection urlConnection = url.openConnection();
        urlConnection.setRequestProperty("Authorization", "Basic " + Base64.getEncoder().encodeToString(cegaCredentials.getBytes()));
        return gson.fromJson(new InputStreamReader(urlConnection.getInputStream()), Credentials.class);
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
    public void setGson(Gson gson) {
        this.gson = gson;
    }

}

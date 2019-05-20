package se.nbis.lega.inbox.tsd;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
public class FileUploadService {

    public static final String TSD_AUTH_API_URL = "https://api.tsd.usit.no/%s/%s/auth/basic/token?type=import";
    public static final String TSD_FILE_API_URL = "https://api.tsd.usit.no/%s/%s/files/";

    protected String tsdAPIVersion;
    protected String tsdProjectName;
    protected String tsdAPIKey;

    protected RestTemplate restTemplate;

    protected String getFileUploadJWT() {
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + tsdAPIKey);
        HttpEntity<String> entity = new HttpEntity<>("parameters", headers);
        ResponseEntity<Map> response = restTemplate.exchange(String.format(TSD_AUTH_API_URL, tsdAPIVersion, tsdProjectName), HttpMethod.POST, entity, Map.class);
        return String.valueOf(response.getBody().get("token"));
    }

    @Value("${tsd.api.version}")
    public void setTsdAPIVersion(String tsdAPIVersion) {
        this.tsdAPIVersion = tsdAPIVersion;
    }

    @Value("${tsd.project.name}")
    public void setTsdProjectName(String tsdProjectName) {
        this.tsdProjectName = tsdProjectName;
    }

    @Value("${tsd.api.key}")
    public void setTsdAPIKey(String tsdAPIKey) {
        this.tsdAPIKey = tsdAPIKey;
    }

    @Autowired
    public void setRestTemplate(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

}

package se.nbis.lega.inbox.sftp;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import org.apache.http.HttpHeaders;
import org.apache.http.entity.ContentType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;

import java.net.MalformedURLException;
import java.net.URL;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;

/**
 * Spring Boot application's main class with some configuration and some beans defined.
 *
 * @author Dmytro Titov
 */
@ComponentScan(basePackages = "se.nbis.lega.inbox")
@SpringBootApplication
public class TestInboxApplication {

    public static final String USERNAME = "john";
    public static final String PASSWORD = "8dXfV24MTxirq7zU";
    public static final String PASSWORD_HASH = "$1$/PMv2qfQ$prOKm0CyJYCU2Xyo6nmII/";
    public static final String PUBLIC_KEY = "ssh-rsa AAAAB3NzaC1yc2EAAAADAQABAAABAQCna+LZl62x/z5eHj/bwaOivbHt+scRTrSHqHhp+4eUB3L9PTAJfMr5O/1JfZ6JRRm5bAvbx96HN7/JzmR5OzXWpk9Ii0fjPbmGtdxAbImAfZoLLsFu3Q1h4T1ohhrK2eoiLhgWQHcQBZ2KTE6V9pQw7ErxAKGokgk8YyCI/ISz92Pyr8QtrUjn+TjgB2hgD5gZC0lYzITkwkuuVxOv/kddutscRgvIVuSQUgoRdlvh9eyL4qRozVjQRcEoeLnRN0d4fqPQKhg4VzhdpcWyIFGkSM1fLAqeipOS2Y5/HcZdLeaZXIQAsaTHFhIwsrF++jQuyKrwwcAeYpXJywUpky3Z";

    private String cegaEndpoint;
    private String cegaCredentials;

    public static void main(String[] args) {
        SpringApplication.run(TestInboxApplication.class, args);
    }

    @Bean
    public WireMockServer wireMockServer() throws MalformedURLException {
        URL url = new URL(cegaEndpoint);
        WireMockServer wireMockServer = new WireMockServer(
                wireMockConfig().port(url.getPort())
        );
        wireMockServer.start();
        WireMock.configureFor(url.getPort());
        String login = cegaCredentials.split(":")[0];
        String password = cegaCredentials.split(":")[1];
        stubFor(get(urlEqualTo(url.getPath() + USERNAME))
                .withBasicAuth(login, password)
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.toString())
                        .withBody(String.format("{'password_hash': '%s','pubkey': '%s'}", PASSWORD_HASH, PUBLIC_KEY))));
        return wireMockServer;
    }

    @Value("${inbox.cega.endpoint}")
    public void setCegaEndpoint(String cegaEndpoint) {
        this.cegaEndpoint = cegaEndpoint;
    }

    @Value("${inbox.cega.credentials}")
    public void setCegaCredentials(String cegaCredentials) {
        this.cegaCredentials = cegaCredentials;
    }

}

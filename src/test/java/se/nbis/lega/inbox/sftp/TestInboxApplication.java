package se.nbis.lega.inbox.sftp;

import com.google.gson.Gson;
import org.apache.http.HttpHeaders;
import org.mockito.stubbing.Answer;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import se.nbis.lega.inbox.pojo.FileDescriptor;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Base64;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Test Spring Boot application's main class with some configuration and some beans defined.
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
    private String exchange;
    private String routingKeyChecksums;
    private String routingKeyFiles;

    private Gson gson;

    public static void main(String[] args) {
        SpringApplication.run(TestInboxApplication.class, args);
    }

    @Bean
    public RabbitTemplate rabbitTemplate() {
        RabbitTemplate mock = mock(RabbitTemplate.class);
        doAnswer((Answer<Void>) invocationOnMock -> {
            String routingKey = invocationOnMock.getArgument(1).toString();
            BlockingQueue<FileDescriptor> queue = routingKey.equals(routingKeyFiles) ? fileBlockingQueue() : hashBlockingQueue();
            queue.put(gson.fromJson(invocationOnMock.getArgument(2).toString(), FileDescriptor.class));
            return null;
        }).when(mock).convertAndSend(eq(exchange), anyString(), anyString());
        return mock;
    }

    @Bean
    public RestTemplate restTemplate() throws MalformedURLException, URISyntaxException {
        RestTemplate mock = mock(RestTemplate.class);
        URL url = new URL(cegaEndpoint + USERNAME);
        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
        headers.set(HttpHeaders.AUTHORIZATION, "Basic " + Base64.getEncoder().encodeToString(cegaCredentials.getBytes()));
        when(mock.exchange(url.toURI(), HttpMethod.GET, new HttpEntity<>(headers), String.class)).thenReturn(new ResponseEntity<>(String.format("{'password_hash': '%s','pubkey': '%s'}", PASSWORD_HASH, PUBLIC_KEY), HttpStatus.OK));
        return mock;
    }

    @Bean
    public BlockingQueue<FileDescriptor> fileBlockingQueue() {
        return new LinkedBlockingQueue<>();
    }

    @Bean
    public BlockingQueue<FileDescriptor> hashBlockingQueue() {
        return new LinkedBlockingQueue<>();
    }

    @Value("${inbox.cega.endpoint}")
    public void setCegaEndpoint(String cegaEndpoint) {
        this.cegaEndpoint = cegaEndpoint;
    }

    @Value("${inbox.cega.credentials}")
    public void setCegaCredentials(String cegaCredentials) {
        this.cegaCredentials = cegaCredentials;
    }

    @Value("${inbox.mq.exchange}")
    public void setExchange(String exchange) {
        this.exchange = exchange;
    }

    @Value("${inbox.mq.routing-key.checksums}")
    public void setRoutingKeyChecksums(String routingKeyChecksums) {
        this.routingKeyChecksums = routingKeyChecksums;
    }

    @Value("${inbox.mq.routing-key.files}")
    public void setRoutingKeyFiles(String routingKeyFiles) {
        this.routingKeyFiles = routingKeyFiles;
    }

    @Autowired
    public void setGson(Gson gson) {
        this.gson = gson;
    }

}

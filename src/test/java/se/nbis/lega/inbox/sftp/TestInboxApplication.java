package se.nbis.lega.inbox.sftp;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.google.gson.Gson;
import io.arivera.oss.embedded.rabbitmq.EmbeddedRabbitMq;
import io.arivera.oss.embedded.rabbitmq.EmbeddedRabbitMqConfig;
import org.apache.http.HttpHeaders;
import org.apache.http.entity.ContentType;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import se.nbis.lega.inbox.pojo.FileDescriptor;

import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;

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
    public static final String FILE_QUEUE = "fileQueue";
    public static final String HASH_QUEUE = "hashQueue";

    private String cegaEndpoint;
    private String cegaCredentials;
    private int rabbitMQPort;
    private String exchange;
    private String routingKeyChecksums;
    private String routingKeyFiles;

    private Gson gson;

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

    @Bean
    public EmbeddedRabbitMq embeddedRabbitMq() {
        EmbeddedRabbitMqConfig config = new EmbeddedRabbitMqConfig.Builder().port(rabbitMQPort).build();
        EmbeddedRabbitMq rabbitMq = new EmbeddedRabbitMq(config);
        rabbitMq.start();
        return rabbitMq;
    }

    @Bean
    public Queue fileQueue() {
        return new Queue(FILE_QUEUE, false);
    }

    @Bean
    public Queue hashQueue() {
        return new Queue(HASH_QUEUE, false);
    }

    @Bean
    public TopicExchange exchange() {
        return new TopicExchange(exchange);
    }

    @Bean
    public Binding fileBinding(Queue fileQueue, TopicExchange exchange) {
        return BindingBuilder.bind(fileQueue).to(exchange).with(routingKeyFiles);
    }

    @Bean
    public Binding hashBinding(Queue hashQueue, TopicExchange exchange) {
        return BindingBuilder.bind(hashQueue).to(exchange).with(routingKeyChecksums);
    }

    @Bean
    public SimpleMessageListenerContainer fileListener(ConnectionFactory connectionFactory) {
        SimpleMessageListenerContainer container = new SimpleMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.setQueueNames(FILE_QUEUE);
        container.setMessageListener(message -> {
            try {
                fileBlockingQueue().put(gson.fromJson(new String(message.getBody(), Charset.defaultCharset()), FileDescriptor.class));
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });
        return container;
    }

    @Bean
    public SimpleMessageListenerContainer hashListener(ConnectionFactory connectionFactory) {
        SimpleMessageListenerContainer container = new SimpleMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.setQueueNames(HASH_QUEUE);
        container.setMessageListener(message -> {
            try {
                hashBlockingQueue().put(gson.fromJson(new String(message.getBody(), Charset.defaultCharset()), FileDescriptor.class));
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });
        return container;
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

    @Value("${spring.rabbitmq.port}")
    public void setRabbitMQPort(int rabbitMQPort) {
        this.rabbitMQPort = rabbitMQPort;
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

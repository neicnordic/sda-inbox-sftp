package se.nbis.lega.inbox;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Spring Boot application's main class with some configuration and some beans defined.
 *
 * @author Dmytro Titov
 */
@Slf4j
@SpringBootApplication
public class InboxApplication {

    public static void main(String[] args) {
        SpringApplication.run(InboxApplication.class, args);
    }

}

package in.skillsync.session;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.ComponentScan;

/**
 * SkillSync Session Service
 * Manages the complete mentoring session lifecycle:
 * REQUESTED → ACCEPTED / REJECTED / CANCELLED → COMPLETED
 * Publishes RabbitMQ events on every state transition.
 * Port: 9085
 * Swagger UI: http://localhost:9085/swagger-ui.html
 */
@SpringBootApplication
@EnableDiscoveryClient
@ComponentScan(basePackages = {
        "in.skillsync.session",
        "in.skillsync.common"
})
public class SessionServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(SessionServiceApplication.class, args);
    }
}

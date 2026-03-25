package in.skillsync.notification;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.ComponentScan;

/**
 * SkillSync Notification Service
 * Consumes events from RabbitMQ and sends email + in-app notifications.
 * Port: 8088
 * Swagger UI: http://localhost:8088/swagger-ui.html
 */
@SpringBootApplication
@EnableDiscoveryClient
@ComponentScan(basePackages = {
        "in.skillsync.notification",
        "in.skillsync.common"
})
public class NotificationServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(NotificationServiceApplication.class, args);
    }
}

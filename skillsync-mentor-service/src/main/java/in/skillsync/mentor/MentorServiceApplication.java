package in.skillsync.mentor;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.ComponentScan;

/**
 * SkillSync Mentor Service
 * Handles mentor onboarding, profile management, and discovery.
 * Port: 8083
 * Swagger UI: http://localhost:8083/swagger-ui.html
 */
@SpringBootApplication
@EnableDiscoveryClient
@EnableCaching
@ComponentScan(basePackages = {
        "in.skillsync.mentor",
        "in.skillsync.common"
})
public class MentorServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(MentorServiceApplication.class, args);
    }
}

package in.skillsync.skill;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.ComponentScan;

/**
 * SkillSync Skill Service
 * Centralised skill catalogue. Admin creates skills; mentors tag them.
 * Port: 8084
 * Swagger UI: http://localhost:8084/swagger-ui.html
 */
@SpringBootApplication
@EnableDiscoveryClient
@EnableCaching
@ComponentScan(basePackages = {
        "in.skillsync.skill",
        "in.skillsync.common"
})
public class SkillServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(SkillServiceApplication.class, args);
    }
}

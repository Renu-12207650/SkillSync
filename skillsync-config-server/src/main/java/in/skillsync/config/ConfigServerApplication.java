package in.skillsync.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.config.server.EnableConfigServer;

/**
 * SkillSync Centralised Configuration Server.
 * Reads configuration from GitHub: https://github.com/Renu-12207650/skillsync-configs
 * Start this SECOND after Eureka Server.
 * Access config at: http://localhost:8888/{service-name}/default
 */
@SpringBootApplication
@EnableConfigServer
@EnableDiscoveryClient
public class ConfigServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(ConfigServerApplication.class, args);
    }
}

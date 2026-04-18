package in.skillsync.common.config;
 
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
 
import java.util.List;
 
/**
 * Base Swagger/OpenAPI 3 configuration shared across all services.
 * Forces all services to advertise the API Gateway (localhost:9080) as the server URL
 * so that Swagger UI always routes requests through the gateway — never directly to services.
 */
@Configuration
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT",
        description = "Enter JWT token obtained from POST /auth/login"
)
public class SwaggerConfig {
 
    @Bean
    @ConditionalOnMissingBean(OpenAPI.class)
    public OpenAPI defaultOpenAPI() {
        return new OpenAPI()
                .servers(List.of(
                        new Server()
                                .url("http://localhost:9080")
                                .description("API Gateway — all requests go here")
                ))
                .info(new Info()
                        .title("SkillSync API")
                        .description("SkillSync - Peer Learning & Mentor Matching Platform")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("SkillSync Team")
                                .email("renudhankhar8559@gmail.com")));
    }
}
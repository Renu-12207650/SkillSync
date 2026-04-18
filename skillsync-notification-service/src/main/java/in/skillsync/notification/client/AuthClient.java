package in.skillsync.notification.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "skillsync-auth-service")
public interface AuthClient {
    @GetMapping("/auth/internal/email/{userId}")
    String getUserEmail(@PathVariable("userId") Long userId);
}
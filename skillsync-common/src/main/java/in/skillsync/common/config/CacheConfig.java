package in.skillsync.common.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Default caching configuration using Spring's built-in ConcurrentMapCacheManager.
 * No Redis or external infrastructure required.
 * Uses @ConditionalOnMissingBean so services that define their own
 * CacheManager bean will not get a conflict.
 *
 * To upgrade to Redis: replace ConcurrentMapCacheManager with RedisCacheManager.
 * Zero changes to business code needed.
 */
@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    @ConditionalOnMissingBean(CacheManager.class)
    public CacheManager cacheManager() {
        return new ConcurrentMapCacheManager(
                "userProfiles",
                "mentorProfiles",
                "skills"
        );
    }
}

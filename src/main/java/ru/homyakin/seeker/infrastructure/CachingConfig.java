package ru.homyakin.seeker.infrastructure;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.List;

@EnableCaching
@Configuration
public class CachingConfig {

    @Bean
    public CacheManager cacheManager() {
        final var manager = new SimpleCacheManager();
        final var telegramStatisticCache = createCache(TELEGRAM_STATISTIC, Duration.ofMinutes(15), 1);
        manager.setCaches(List.of(telegramStatisticCache));
        return manager;
    }

    private CaffeineCache createCache(String name, Duration ttl, int maxSize) {
        return new CaffeineCache(
            name,
            Caffeine.newBuilder().maximumSize(maxSize).expireAfterWrite(ttl).build()
        );
    }

    public static final String TELEGRAM_STATISTIC = "telegramStatistic";
}

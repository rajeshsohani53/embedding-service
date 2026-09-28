package com.raj.embeddingservice.v2.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.stats.CacheStats;

import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;

@Component
public class EmbeddingCache {

    private final Cache<String, List<Float>> cache = Caffeine.newBuilder()
            .maximumSize(10_000)
            .expireAfterWrite(Duration.ofHours(1))
            .recordStats()
            .build();

    public List<Float> get(String text) {
        return cache.getIfPresent(text);
    }
    public void put(String text, List<Float> vector) {
        cache.put(text, vector);
    }
    public CacheStats stats() {
        return cache.stats();
    }


}
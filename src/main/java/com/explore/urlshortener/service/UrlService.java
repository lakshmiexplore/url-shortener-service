package com.explore.urlshortener.service;

import com.explore.urlshortener.dto.ShortenRequest;
import com.explore.urlshortener.dto.ShortenResponse;
import com.explore.urlshortener.entity.Url;
import com.explore.urlshortener.repository.UrlRepository;
import com.explore.urlshortener.util.Base62;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;

@Service
@RequiredArgsConstructor
@Slf4j
public class UrlService {

    private final UrlRepository urlRepository;
    private final StringRedisTemplate redisTemplate;

    private static final String CACHE_PREFIX = "url:";
    private static final Duration CACHE_TTL = Duration.ofDays(7);

    @Transactional
    public ShortenResponse shortenUrl(ShortenRequest request, String baseUrl) {
        // 1. Save placeholder to generate unique BigSerial ID
        Url url = Url.builder()
                .originalUrl(request.url())
                .build();
        url = urlRepository.save(url);

        // 2. Base62 encode the ID to create deterministic shortCode
        String shortCode = Base62.encode(url.getId());
        url.setShortCode(shortCode);
        urlRepository.save(url);

        // 3. Populate Redis asynchronously/directly for sub-10ms subsequent reads
        redisTemplate.opsForValue().set(CACHE_PREFIX + shortCode, request.url(), CACHE_TTL);

        return new ShortenResponse(
                shortCode,
                baseUrl + "/" + shortCode,
                request.url()
        );
    }

    public String getOriginalUrl(String shortCode) {
        String cacheKey = CACHE_PREFIX + shortCode;

        // 1. Check Redis Cache
        String cachedUrl = redisTemplate.opsForValue().get(cacheKey);
        if (cachedUrl != null) {
            log.info("Cache hit for shortCode: {}", shortCode);
            return cachedUrl;
        }

        // 2. Fallback to PostgreSQL
        log.info("Cache miss for shortCode: {}. Querying database.", shortCode);
        Url url = urlRepository.findByShortCode(shortCode)
                .orElseThrow(() -> new IllegalArgumentException("Short code not found: " + shortCode));

        // 3. Re-hydrate Redis cache
        redisTemplate.opsForValue().set(cacheKey, url.getOriginalUrl(), CACHE_TTL);

        return url.getOriginalUrl();
    }
}
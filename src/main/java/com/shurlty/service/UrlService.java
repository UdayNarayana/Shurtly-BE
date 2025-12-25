package com.shurlty.service;

import com.shurlty.entity.UrlEntity;
import com.shurlty.repository.ShurltyRepo;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class UrlService {

    private static final char[] ALPHANUM =
            "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789".toCharArray();
    private static final Set<String> ALLOWED_SCHEMES = Set.of("http", "https");

    private final ShurltyRepo repo;

    @Value("${app.url.default-expiry-days:30}")
    private long defaultExpiryDays;

    public UrlService(ShurltyRepo repo) {
        this.repo = repo;
    }

    /** Creates a short URL record with a new 7-char code and default expiry. */
    public UrlEntity createShortUrl(String rawLongUrl) {
        String longUrl = canonicalizeAndValidate(rawLongUrl);

        // Generate a unique code (retry on the rare collision)
        String code = null;
        UrlEntity saved = null;
        int attempts = 0;
        while (attempts < 5) {
            attempts++;
            code = generateRandomCode();
            try {
                UrlEntity entity = new UrlEntity();
                entity.setCode(code);
                entity.setLongUrl(longUrl);
                entity.setCreatedAt(Instant.now());
                entity.setExpiresAt(Instant.now().plus(Duration.ofDays(defaultExpiryDays)));
                saved = repo.save(entity);
                break;
            } catch (DataIntegrityViolationException dup) {
                // code collision on UNIQUE constraint: try again with a new code
                saved = null;
            }
        }
        if (saved == null) {
            throw new IllegalStateException("Failed to generate a unique short code. Please try again.");
        }
        return saved;
    }

    /** Returns the destination URL if found & not expired, else throws typed exceptions. */
    public String resolveActiveLongUrl(String code) throws NotFoundException, ExpiredException {
        Optional<UrlEntity> opt = repo.findByCode(code);
        if (opt.isEmpty()) throw new NotFoundException();
        UrlEntity e = opt.get();
        if (e.getExpiresAt() != null && e.getExpiresAt().isBefore(Instant.now())) {
            throw new ExpiredException();
        }
        return e.getLongUrl();
    }

    // ===== Helpers =====

    private String generateRandomCode() {
        var rnd = ThreadLocalRandom.current();
        var sb = new StringBuilder(7);
        for (int i = 0; i < 7; i++) {
            sb.append(ALPHANUM[rnd.nextInt(ALPHANUM.length)]);
        }
        return sb.toString();
    }

    private String canonicalizeAndValidate(String raw) {
        if (raw == null) throw new IllegalArgumentException("URL is required");
        String s = raw.trim();
        if (s.length() > 2048) throw new IllegalArgumentException("URL too long");
        URI uri;
        try {
            uri = URI.create(s);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid URL");
        }
        String scheme = uri.getScheme();
        if (scheme == null || !ALLOWED_SCHEMES.contains(scheme.toLowerCase())) {
            throw new IllegalArgumentException("URL must start with http:// or https://");
        }
        if (uri.getHost() == null || uri.getHost().isBlank()) {
            throw new IllegalArgumentException("URL must include a valid host");
        }
        // Minimal canonicalization: return as-is (you can normalize host/ports later)
        return uri.toString();
    }

    // Typed exceptions so the controller can map to 404/410.
    public static class NotFoundException extends Exception {}
    public static class ExpiredException extends Exception {}
}

package com.shurlty.service;

import com.shurlty.entity.UrlEntity;
import com.shurlty.entity.UserEntity;
import com.shurlty.repository.UrlRepo;
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

    private final UrlRepo repo;
    private final CodeGenerator codeGenerator;

    @Value("${app.url.default-expiry-days:30}")
    private long defaultExpiryDays;

    public UrlService(UrlRepo repo, CodeGenerator codeGenerator) {
        this.repo = repo;
        this.codeGenerator = codeGenerator;
    }

    /** Creates a short URL record with a new 7-char code and default expiry. */
    public UrlEntity createShortUrl(String rawLongUrl, UserEntity owner, Integer expiresInDays) {
        String longUrl = canonicalizeAndValidate(rawLongUrl);

        long days;
        if (expiresInDays == null) {
            days = defaultExpiryDays; // default 30
        } else {
            if (expiresInDays < 1) throw new IllegalArgumentException("expiresInDays must be >= 1");
            if (expiresInDays > 365) throw new IllegalArgumentException("expiresInDays must be <= 365");
            days = expiresInDays;
        }

        Instant now = Instant.now();
        Instant expiry = now.plus(Duration.ofDays(days));

        String code;
        UrlEntity saved = null;

        // retry on collision
        for (int attempts = 0; attempts < 5; attempts++) {
            code = codeGenerator.nextCode(); //redis global counter
            try {
                UrlEntity entity = new UrlEntity();
                entity.setOwner(owner);
                entity.setCode(code);
                entity.setLongUrl(longUrl);
                entity.setCreatedAt(now);
                entity.setExpiresAt(expiry);
                saved = repo.save(entity);
                break;
            } catch (DataIntegrityViolationException dup) {
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

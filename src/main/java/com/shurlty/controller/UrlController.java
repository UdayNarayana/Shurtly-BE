package com.shurlty.controller;

import com.shurlty.entity.UrlEntity;
import com.shurlty.repository.ShurltyRepo;
import com.shurlty.service.UrlService;
import com.shurlty.service.UrlService.ExpiredException;
import com.shurlty.service.UrlService.NotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.time.Instant;
import java.util.Map;

@RestController
public class UrlController {

    private final UrlService service;
    private final ShurltyRepo repo;

    public UrlController(UrlService service, ShurltyRepo repo) {
        this.service = service;
        this.repo = repo;
    }

    /** Create a short URL (default expiry = now + 30 days). */
    @PostMapping("/api/shorten")
    public ResponseEntity<?> create(@RequestBody Map<String, String> body, HttpServletRequest req) {
        try {
            String longUrl = body.get("longUrl");
            UrlEntity saved = service.createShortUrl(longUrl);

            String shortUrl = buildBaseUrl(req) + saved.getCode();

            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                    "code", saved.getCode(),
                    "shortUrl", shortUrl,
                    "expiresAt", saved.getExpiresAt().toString()
            ));
        } catch (IllegalArgumentException badInput) {
            // e.g., invalid URL, too long, wrong scheme, etc.
            return ResponseEntity.badRequest().body(Map.of("error", badInput.getMessage()));
        } catch (Exception ex) {
            return ResponseEntity.status(500).body(Map.of("error", "Internal error"));
        }
    }

    /** Redirect to the original long URL if active; 410 if expired; 404 if unknown. */
    @GetMapping("/{code}")
    public ResponseEntity<Void> redirect(@PathVariable String code) {
        try {
            String longUrl = service.resolveActiveLongUrl(code);
            return ResponseEntity.status(302)
                    .location(URI.create(longUrl))
                    .build();
        } catch (ExpiredException e) {
            return ResponseEntity.status(410).build(); // Gone
        } catch (NotFoundException e) {
            return ResponseEntity.status(404).build(); // Not Found
        }
    }

    /** Optional helper endpoint to view a code’s metadata (handy for your UI). */
    @GetMapping("/api/links/{code}")
    public ResponseEntity<?> getOne(@PathVariable String code) {
        return repo.findByCode(code)
                .<ResponseEntity<?>>map(e -> ResponseEntity.ok(Map.of(
                        "code", e.getCode(),
                        "longUrl", e.getLongUrl(),
                        "createdAt", e.getCreatedAt().toString(),
                        "expiresAt", e.getExpiresAt().toString(),
                        "expired", e.getExpiresAt().isBefore(Instant.now())
                )))
                .orElse(ResponseEntity.status(404).body(Map.of("error", "Not found")));
    }

    // ---- helpers ----
    private static String buildBaseUrl(HttpServletRequest req) {
        // e.g., http://localhost:8080/
        StringBuffer url = req.getRequestURL();
        String uri = req.getRequestURI();
        String base = url.substring(0, url.length() - uri.length()) + "/";
        return base;
    }
}


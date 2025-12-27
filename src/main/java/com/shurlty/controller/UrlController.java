package com.shurlty.controller;

import com.shurlty.entity.UrlEntity;
import com.shurlty.entity.UserEntity;
import com.shurlty.repository.UrlRepo;
import com.shurlty.service.UrlService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
public class UrlController {

    private final UrlService service;
    private final UrlRepo repo;

    public UrlController(UrlService service, UrlRepo repo) {
        this.service = service;
        this.repo = repo;
    }

    /** Create a short URL (AUTH REQUIRED; user_id is NOT nullable). */
    @PostMapping("/shorten")
    public ResponseEntity<?> create(@RequestBody Map<String, Object> body, HttpServletRequest req) {
        try {
            UserEntity me = currentUser();

            String longUrl = body.get("longUrl") == null ? null : body.get("longUrl").toString();

            // Optional Phase-2: allow custom expiry days; default stays 30 in service
            Integer expiresInDays = null;
            if (body.get("expiresInDays") != null) {
                expiresInDays = Integer.parseInt(body.get("expiresInDays").toString());
            }

            UrlEntity saved = service.createShortUrl(longUrl, me, expiresInDays);

            String shortUrl = buildBaseUrl(req) + saved.getCode();

            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                    "code", saved.getCode(),
                    "shortUrl", shortUrl,
                    "expiresAt", saved.getExpiresAt().toString()
            ));
        } catch (IllegalArgumentException badInput) {
            return ResponseEntity.badRequest().body(Map.of("error", badInput.getMessage()));
        } catch (Exception ex) {
            return ResponseEntity.status(500).body(Map.of("error", "Internal error"));
        }
    }

    /** My links (AUTH REQUIRED). */
    @GetMapping("/links")
    public ResponseEntity<?> mine() {

        
        UserEntity me = currentUser();
        List<UrlEntity> list = repo.findAllByOwnerOrderByCreatedAtDesc(me);

        return ResponseEntity.ok(list.stream().map(e -> Map.of(
                "code", e.getCode(),
                "shortUrl", e.getCode(),          // FE can prefix base domain if you want
                "longUrl", e.getLongUrl(),
                "createdAt", e.getCreatedAt().toString(),
                "expiresAt", e.getExpiresAt().toString()
        )).toList());
    }

    /** Get a single link metadata (AUTH REQUIRED, owner-only). */
    @GetMapping("/links/{code}")
    public ResponseEntity<?> getOne(@PathVariable String code) {
        UserEntity me = currentUser();
        return repo.findByCodeAndOwner(code, me)
                .<ResponseEntity<?>>map(e -> ResponseEntity.ok(Map.of(
                        "code", e.getCode(),
                        "longUrl", e.getLongUrl(),
                        "createdAt", e.getCreatedAt().toString(),
                        "expiresAt", e.getExpiresAt().toString()
                )))
                .orElse(ResponseEntity.status(404).body(Map.of("error", "Not found")));
    }

    // ---- helpers ----

    private static String buildBaseUrl(HttpServletRequest req) {
        // e.g., http://localhost:8080/
        StringBuffer url = req.getRequestURL();
        String uri = req.getRequestURI();
        return url.substring(0, url.length() - uri.length()) + "/";
    }

    private static UserEntity currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof UserEntity)) {
            throw new IllegalStateException("Unauthenticated");
        }
        return (UserEntity) auth.getPrincipal();
    }
}

package com.shurlty.controller;

import com.shurlty.service.UrlService;
import com.shurlty.service.UrlService.ExpiredException;
import com.shurlty.service.UrlService.NotFoundException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
public class RedirectController {

    private final UrlService service;

    public RedirectController(UrlService service) {
        this.service = service;
    }

    /** Public redirect: 302 if active, 410 if expired, 404 if not found. */
    @GetMapping("/{code}")
    public ResponseEntity<Void> redirect(@PathVariable String code) {
        try {
            String longUrl = service.resolveActiveLongUrl(code);
            return ResponseEntity.status(302)
                    .location(URI.create(longUrl))
                    .build();
        } catch (ExpiredException e) {
            return ResponseEntity.status(410).build();
        } catch (NotFoundException e) {
            return ResponseEntity.status(404).build();
        }
    }
}

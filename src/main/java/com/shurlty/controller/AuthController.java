package com.shurlty.controller;

import com.shurlty.entity.UserEntity;
import com.shurlty.security.JwtService;
import com.shurlty.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
@CrossOrigin("*")
public class AuthController {

    private final UserService userService;
    private final JwtService jwtService;

    public AuthController(UserService userService, JwtService jwtService) {
        this.userService = userService;
        this.jwtService = jwtService;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody Map<String, String> body) {
        UserEntity u = userService.register(body.get("name"), body.get("email"), body.get("password"));
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(Map.of("message", "User registered", "userId", u.getId()));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> body) {
        UserEntity u = userService.authenticate(body.get("email"), body.get("password"));
        String token = jwtService.createToken(u.getId(), u.getEmail());
        return ResponseEntity.ok(Map.of("token", token));
    }
}

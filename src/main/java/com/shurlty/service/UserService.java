package com.shurlty.service;

import com.shurlty.entity.UserEntity;
import com.shurlty.repository.UserRepo;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class UserService {

    private final UserRepo userRepo;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepo userRepo, PasswordEncoder passwordEncoder) {
        this.userRepo = userRepo;
        this.passwordEncoder = passwordEncoder;
    }

    public UserEntity register(String emailRaw, String passwordRaw) {
        String email = normalizeEmail(emailRaw);
        String password = passwordRaw == null ? "" : passwordRaw.trim();

        if (email.isBlank() || password.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email and password are required");
        }
        if (password.length() < 8) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password must be at least 8 characters");
        }
        if (userRepo.existsByEmail(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already in use");
        }

        UserEntity u = new UserEntity();
        u.setEmail(email);
        u.setPasswordHash(passwordEncoder.encode(password));
        return userRepo.save(u);
    }

    public UserEntity authenticate(String emailRaw, String passwordRaw) {
        String email = normalizeEmail(emailRaw);
        String password = passwordRaw == null ? "" : passwordRaw.trim();

        UserEntity u = userRepo.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));

        if (!passwordEncoder.matches(password, u.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        }

        return u;
    }

    private String normalizeEmail(String emailRaw) {
        return emailRaw == null ? "" : emailRaw.trim().toLowerCase();
    }
}

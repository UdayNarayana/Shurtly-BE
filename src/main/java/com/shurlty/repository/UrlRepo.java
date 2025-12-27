package com.shurlty.repository;

import com.shurlty.entity.UrlEntity;
import com.shurlty.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface UrlRepo extends JpaRepository<UrlEntity, Long> {

    // Find a URL by its short code
    Optional<UrlEntity> findByCode(String code);

    // Ownership-safe lookup (PATCH/DELETE should use this)
    Optional<UrlEntity> findByCodeAndOwner(String code, UserEntity owner);

    // "My Links" listing
    List<UrlEntity> findAllByOwnerOrderByCreatedAtDesc(UserEntity owner);

    // For future cleanup job
    long deleteAllByExpiresAtBefore(Instant cutoff);
}

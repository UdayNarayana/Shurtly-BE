package com.shurlty.repository;

import com.shurlty.entity.UrlEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ShurltyRepo extends JpaRepository<UrlEntity, Long> {

    // Find a URL by its short code
    Optional<UrlEntity> findByCode(String code);
}

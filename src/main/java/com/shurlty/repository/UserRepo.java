package com.shurlty.repository;

import com.shurlty.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepo extends JpaRepository<UserEntity, Long> {

    // Used for login/register checks
    Optional<UserEntity> findByEmail(String email);

    boolean existsByEmail(String email);
}

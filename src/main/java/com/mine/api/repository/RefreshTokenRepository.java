package com.mine.api.repository;

import com.mine.api.domain.RefreshToken;
import org.springframework.data.repository.CrudRepository;

import java.util.Optional;

public interface RefreshTokenRepository extends CrudRepository<RefreshToken, String> {
    Optional<RefreshToken> findByToken(String token);

    java.util.List<RefreshToken> findByUsername(String username);
}

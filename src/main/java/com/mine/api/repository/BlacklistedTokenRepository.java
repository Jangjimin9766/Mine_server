package com.mine.api.repository;

import com.mine.api.domain.BlacklistedToken;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BlacklistedTokenRepository extends JpaRepository<BlacklistedToken, String> {
    boolean existsByToken(String token);
}

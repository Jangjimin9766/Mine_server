package com.mine.api.domain;

import lombok.Builder;
import lombok.Getter;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.index.Indexed;

import java.io.Serializable;
import java.util.UUID;

@Getter
@RedisHash(value = "refreshToken", timeToLive = 604800) // 7 days
public class RefreshToken implements Serializable {

    @Id
    private String token;

    @Indexed
    private String username;

    @Builder
    public RefreshToken(String username) {
        this.token = UUID.randomUUID().toString();
        this.username = username;
    }
}

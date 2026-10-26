package com.game.speed.auth.repository;

import com.game.speed.auth.entity.RevokedToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;

public interface RevokedTokenRepository extends JpaRepository<RevokedToken, Long> {
    boolean existsByTokenHashAndExpiresAtAfter(String tokenHash, Instant now);

    long deleteByExpiresAtBefore(Instant now);
}

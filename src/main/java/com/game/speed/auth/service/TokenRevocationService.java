package com.game.speed.auth.service;

import com.game.speed.auth.entity.RevokedToken;
import com.game.speed.auth.repository.RevokedTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;

@Service
@RequiredArgsConstructor
public class TokenRevocationService {
    private final RevokedTokenRepository revokedTokenRepository;

    @Transactional
    public void revoke(String token, Instant expiresAt) {
        Instant now = Instant.now();
        revokedTokenRepository.deleteByExpiresAtBefore(now);
        String tokenHash = hash(token);
        if (!revokedTokenRepository.existsByTokenHashAndExpiresAtAfter(tokenHash, now)) {
            RevokedToken revokedToken = new RevokedToken();
            revokedToken.setTokenHash(tokenHash);
            revokedToken.setExpiresAt(expiresAt);
            revokedTokenRepository.save(revokedToken);
        }
    }

    @Transactional(readOnly = true)
    public boolean isRevoked(String token) {
        return revokedTokenRepository.existsByTokenHashAndExpiresAtAfter(hash(token), Instant.now());
    }

    private String hash(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }
}

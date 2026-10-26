package com.game.speed.auth.controller;

import com.game.speed.auth.dto.JwtAuthResponse;
import com.game.speed.auth.dto.LoginPlayerDTO;
import com.game.speed.common.response.ApiError;
import com.game.speed.auth.security.JwtTokenProvider;
import com.game.speed.auth.service.TokenRevocationService;
import com.game.speed.player.dto.CreatePlayerDTO;
import com.game.speed.player.service.PlayerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final PlayerService playerService;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final TokenRevocationService tokenRevocationService;

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody CreatePlayerDTO request) {
        var player = playerService.createNewPlayer(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(playerService.getPlayerById(player.getId()));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginPlayerDTO data) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(data.getUsername(), data.getPassword()));
            SecurityContextHolder.getContext().setAuthentication(authentication);
            return ResponseEntity.ok(new JwtAuthResponse(jwtTokenProvider.generateToken(authentication)));
        } catch (AuthenticationException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiError(401, "INVALID_CREDENTIALS", "Sai username hoặc password", java.util.Map.of()));
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestHeader("Authorization") String authorization) {
        String token = authorization.substring("Bearer ".length());
        Instant expiresAt = jwtTokenProvider.getExpiration(token);
        tokenRevocationService.revoke(token, expiresAt);
        SecurityContextHolder.clearContext();
        return ResponseEntity.noContent().build();
    }
}

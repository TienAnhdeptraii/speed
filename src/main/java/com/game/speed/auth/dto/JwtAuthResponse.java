package com.game.speed.auth.dto;

public record JwtAuthResponse(String accessToken, String type) {
    public JwtAuthResponse(String accessToken) {
        this(accessToken, "Bearer");
    }
}

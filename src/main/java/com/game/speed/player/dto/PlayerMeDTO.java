package com.game.speed.player.dto;

public record PlayerMeDTO(Long id, String username, String name, int level,
                          boolean vip, boolean online, WalletDTO wallet) {
    public record WalletDTO(int coupon, int zcoin, int gold, int luckyPoint) {}
}

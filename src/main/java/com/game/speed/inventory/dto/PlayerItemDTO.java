package com.game.speed.inventory.dto;

public record PlayerItemDTO(Long id, int itemId, int quantity, int avaIPeriod,
                            boolean status, int obtainTime,
                            boolean expired, boolean selected) {}

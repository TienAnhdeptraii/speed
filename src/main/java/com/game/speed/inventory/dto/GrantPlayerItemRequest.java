package com.game.speed.inventory.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

public record GrantPlayerItemRequest(
        @Positive int itemId,
        @Positive int quantity,
        @Min(-1) int avaIPeriod,
        @NotNull Boolean status,
        @PositiveOrZero int obtainTime
) {}

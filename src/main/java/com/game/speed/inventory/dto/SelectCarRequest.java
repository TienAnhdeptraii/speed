package com.game.speed.inventory.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record SelectCarRequest(@NotNull @Positive Long playerItemId) {}

package com.game.speed.box.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.UUID;

public record OpenBoxRequest(@NotNull @Positive Long playerItemId, @NotNull UUID requestId) {}

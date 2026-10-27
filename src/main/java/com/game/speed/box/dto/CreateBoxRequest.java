package com.game.speed.box.dto;

import com.game.speed.box.config.BoxProperties.Reward;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import java.util.List;

public record CreateBoxRequest(
        @Positive int itemId,
        @NotEmpty List<@NotNull @Valid RewardRequest> rewards
) {
    public record RewardRequest(
            @NotNull @Positive Integer itemId,
            @NotNull @Positive Integer quantity,
            @NotNull @Min(-1) Integer avaIPeriod,
            @NotNull Boolean status,
            @NotNull @PositiveOrZero Integer obtainTime,
            @NotNull @Positive Integer weight
    ) {
        public Reward toReward() {
            return new Reward(itemId, quantity, avaIPeriod, status, obtainTime, weight);
        }
    }
}

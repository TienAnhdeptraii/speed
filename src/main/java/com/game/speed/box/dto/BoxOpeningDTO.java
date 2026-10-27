package com.game.speed.box.dto;

import com.game.speed.box.entity.BoxOpening;
import java.time.Instant;

// A receipt, not live inventory: repeated requests return the original awarded duration.
public record BoxOpeningDTO(Long id, String requestId, Long boxPlayerItemId, int boxItemId,
                            int remainingBoxQuantity, RewardDTO reward, Instant openedAt) {
    public record RewardDTO(Long playerItemId, int itemId, String category, String vehicleCategory,
                            int quantity, int avaIPeriod,
                            boolean status, int obtainTime) {}

    public static BoxOpeningDTO from(BoxOpening opening) {
        return new BoxOpeningDTO(opening.getId(), opening.getRequestId(), opening.getBoxPlayerItemId(),
                opening.getBoxItemId(), opening.getRemainingBoxQuantity(),
                new RewardDTO(opening.getRewardPlayerItemId(), opening.getRewardItemId(), opening.getRewardCategory(),
                        opening.getRewardVehicleCategory(),
                        opening.getRewardQuantity(), opening.getRewardAvaIPeriod(),
                        opening.isRewardStatus(), opening.getRewardObtainTime()), opening.getOpenedAt());
    }
}

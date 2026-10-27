package com.game.speed.box.service;

import com.game.speed.box.config.BoxProperties.Reward;
import com.game.speed.box.dto.BoxOpeningDTO;
import com.game.speed.box.entity.BoxOpening;
import com.game.speed.box.repository.BoxOpeningRepository;
import com.game.speed.inventory.config.InventoryRules;
import com.game.speed.inventory.entity.PlayerItem;
import com.game.speed.inventory.repository.InventoryRepository;
import com.game.speed.inventory.service.InventoryService;
import com.game.speed.player.repository.PlayerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.time.Clock;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BoxService {
    private final PlayerRepository players;
    private final InventoryRepository items;
    private final BoxOpeningRepository openings;
    private final InventoryService inventory;
    private final InventoryRules inventoryRules;
    private final BoxCatalogService boxCatalog;
    private final BoxRewardPicker picker;
    private final Clock inventoryClock;

    @Transactional
    public BoxOpeningDTO open(String username, Long playerItemId, UUID requestId) {
        if (playerItemId == null || playerItemId <= 0 || requestId == null) {
            throw new IllegalArgumentException("Owned box ID and request ID are required");
        }
        // Same lock order as selecting a car. Serializes opens for this player,
        // including requests with different keys targeting the final box.
        var player = players.findByUsernameForUpdate(username)
                .orElseThrow(() -> error(HttpStatus.NOT_FOUND, "Player not found"));
        String key = requestId.toString();
        var previous = openings.findByPlayerIdAndRequestId(player.getId(), key);
        if (previous.isPresent()) {
            if (!previous.get().getBoxPlayerItemId().equals(playerItemId)) {
                throw error(HttpStatus.CONFLICT, "Request ID was already used for another box");
            }
            return BoxOpeningDTO.from(previous.get());
        }
        PlayerItem box = items.findOwnedForUpdate(playerItemId, player.getId())
                .orElseThrow(() -> error(HttpStatus.NOT_FOUND, "Owned box not found"));
        var now = inventoryClock.instant();
        if (box.getQuantity() <= 0 || box.getAvaIPeriod() < -1 || box.getObtainTime() < 0
                || (box.getAvaIPeriod() > 0
                && (box.getExpiresAt() == null || !box.getExpiresAt().isAfter(now)))) {
            throw error(HttpStatus.BAD_REQUEST, "Box is empty or expired");
        }
        List<Reward> configured = boxCatalog.rewardsFor(box.getItemId());
        if (configured == null || configured.isEmpty()) {
            throw error(HttpStatus.BAD_REQUEST, "Item is not a configured box");
        }
        List<Reward> rewards = List.copyOf(configured);
        // Validate the entire table before drawing or consuming anything.
        for (Reward reward : rewards) {
            if (reward.itemId() <= 0 || reward.quantity() <= 0 || reward.weight() <= 0
                    || reward.avaIPeriod() < -1 || reward.obtainTime() < 0
                    || (reward.avaIPeriod() > 0 && reward.obtainTime() == 0)
                    || (reward.avaIPeriod() != 0 && reward.quantity() != 1)
                    || (inventoryRules.getCarItemIds().contains(reward.itemId()) && reward.quantity() != 1)) {
                throw error(HttpStatus.INTERNAL_SERVER_ERROR, "Invalid box reward configuration");
            }
        }
        Reward reward = picker.pick(rewards);
        int remaining = box.getQuantity();
        if (box.getAvaIPeriod() != -1) remaining -= 1;
        // PlayerItem requires a positive quantity; remove exhausted ownership rows.
        if (remaining == 0) {
            items.delete(box);
            if (box.getId().equals(player.getSelectedPlayerItemId())) player.setSelectedPlayerItemId(null);
        } else {
            box.setQuantity(remaining);
            items.save(box);
        }
        var granted = inventory.grantItem(player.getId(), reward.itemId(), reward.quantity(),
                reward.avaIPeriod(), reward.status(), reward.obtainTime());
        BoxOpening opening = new BoxOpening();
        opening.setPlayerId(player.getId());
        opening.setRequestId(key);
        opening.setBoxPlayerItemId(playerItemId);
        opening.setBoxItemId(box.getItemId());
        opening.setRemainingBoxQuantity(remaining);
        opening.setRewardPlayerItemId(granted.id());
        opening.setRewardItemId(reward.itemId());
        opening.setRewardCategory(inventoryRules.categoryFor(reward.itemId()).name());
        var vehicleCategory = inventoryRules.vehicleCategoryFor(reward.itemId());
        opening.setRewardVehicleCategory(vehicleCategory == null ? null : vehicleCategory.name());
        opening.setRewardQuantity(reward.quantity());
        opening.setRewardAvaIPeriod(reward.avaIPeriod());
        opening.setRewardStatus(reward.status());
        opening.setRewardObtainTime(reward.obtainTime());
        opening.setOpenedAt(now);
        return BoxOpeningDTO.from(openings.save(opening));
    }

    private ResponseStatusException error(HttpStatus status, String message) {
        return new ResponseStatusException(status, message);
    }
}

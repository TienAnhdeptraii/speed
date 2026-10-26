package com.game.speed.inventory.service;

import com.game.speed.inventory.config.InventoryRules;
import com.game.speed.inventory.dto.PlayerItemDTO;
import com.game.speed.inventory.entity.PlayerItem;
import com.game.speed.inventory.repository.InventoryRepository;
import com.game.speed.player.entity.Player;
import com.game.speed.player.repository.PlayerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.time.Clock;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class InventoryService {
    private final PlayerRepository players;
    private final InventoryRepository items;
    private final InventoryRules rules;
    private final Clock inventoryClock;

    @Transactional(readOnly = true)
    public List<PlayerItemDTO> getInventory(String username) {
        Player player = findPlayer(username);
        Instant now = inventoryClock.instant();
        return items.findAllByPlayerIdOrderByIdAsc(player.getId()).stream()
                .map(item -> toDTO(item, player, now)).toList();
    }

    @Transactional(readOnly = true)
    public Optional<PlayerItemDTO> getSelectedCar(String username) {
        Player player = findPlayer(username);
        if (player.getSelectedPlayerItemId() == null) return Optional.empty();
        Instant now = inventoryClock.instant();
        return items.findByIdAndPlayerId(player.getSelectedPlayerItemId(), player.getId())
                .filter(item -> isUsableCar(item, now))
                .map(item -> toDTO(item, player, now));
    }

    @Transactional
    public PlayerItemDTO selectCar(String username, Long playerItemId) {
        Player player = players.findByUsernameForUpdate(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Player not found"));
        PlayerItem item = items.findOwnedForUpdate(playerItemId, player.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Owned item not found"));
        Instant now = inventoryClock.instant();
        if (!isUsableCar(item, now)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Item is not an available car");
        }
        player.setSelectedPlayerItemId(item.getId());
        players.save(player);
        return toDTO(item, player, now);
    }

    // Trusted server callers only. Each grant creates a separate ownership record.
    // Reward/purchase callers must handle request idempotency in their own transaction.
    @Transactional
    public PlayerItemDTO grantItem(Long playerId, int itemId, int quantity,
                                  int avaIPeriod, boolean status, int obtainTime) {
        if (itemId <= 0 || quantity <= 0 || avaIPeriod < -1 || obtainTime < 0
                || (avaIPeriod > 0 && obtainTime == 0)) {
            throw new IllegalArgumentException("Invalid item, quantity or remaining duration");
        }
        if (rules.getCarItemIds().contains(itemId) && quantity != 1) {
            throw new IllegalArgumentException("Cars must be granted individually");
        }
        Player player = players.findById(playerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Player not found"));
        PlayerItem item = new PlayerItem(playerId, itemId, quantity, avaIPeriod, status, obtainTime);
        Instant now = inventoryClock.instant();
        if (avaIPeriod > 0) {
            item.setExpiresAt(now.plusSeconds(obtainTime));
        }
        return toDTO(items.save(item), player, now);
    }

    private Player findPlayer(String username) {
        return players.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Player not found"));
    }

    private int remainingSeconds(PlayerItem item, Instant now) {
        if (item.getAvaIPeriod() <= 0) return item.getObtainTime();
        // Legacy timed records need an explicit backfill, never restart their timer on reads.
        if (item.getExpiresAt() == null) return 0;
        long seconds = Math.max(0, item.getExpiresAt().getEpochSecond() - now.getEpochSecond());
        return (int) Math.min(seconds, Integer.MAX_VALUE);
    }

    private boolean isExpired(PlayerItem item, Instant now) {
        return item.getAvaIPeriod() > 0
                && (item.getExpiresAt() == null || !item.getExpiresAt().isAfter(now));
    }

    private boolean isUsableCar(PlayerItem item, Instant now) {
        return rules.getCarItemIds().contains(item.getItemId()) && item.getQuantity() > 0
                && item.getAvaIPeriod() >= -1 && item.getObtainTime() >= 0 && !isExpired(item, now);
    }

    private PlayerItemDTO toDTO(PlayerItem item, Player player, Instant now) {
        return new PlayerItemDTO(item.getId(), item.getItemId(), item.getQuantity(),
                item.getAvaIPeriod(), item.isStatus(), remainingSeconds(item, now),
                isExpired(item, now), Objects.equals(item.getId(), player.getSelectedPlayerItemId())
                && isUsableCar(item, now));
    }
}

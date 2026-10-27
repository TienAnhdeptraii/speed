package com.game.speed.inventory.service;

import com.game.speed.inventory.config.InventoryRules;
import com.game.speed.inventory.dto.PlayerItemDTO;
import com.game.speed.inventory.entity.PlayerItem;
import com.game.speed.inventory.entity.ItemCategory;
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
import java.time.Duration;

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

    // Trusted server callers only. Merge quantities or ownership durations by item.
    // Reward/purchase callers must handle request idempotency in their own transaction.
    @Transactional
    public PlayerItemDTO grantItem(Long playerId, int itemId, int quantity,
                                  int avaIPeriod, boolean status, int obtainTime) {
        ItemCategory category = rules.categoryFor(itemId);
        return grantItem(playerId, itemId, category, quantity, avaIPeriod, status, obtainTime);
    }

    @Transactional
    public PlayerItemDTO grantItem(Long playerId, int itemId, ItemCategory category, int quantity,
                                  int avaIPeriod, boolean status, int obtainTime) {
        if (category == null) throw new IllegalArgumentException("Item category is required");
        if (itemId <= 0 || quantity <= 0 || avaIPeriod < -1 || obtainTime < 0
                || (avaIPeriod > 0 && obtainTime == 0)) {
            throw new IllegalArgumentException("Invalid item, quantity or remaining duration");
        }
        if (rules.getCarItemIds().contains(itemId) && quantity != 1) {
            throw new IllegalArgumentException("Cars must be granted individually");
        }
        if (rules.categoryFor(itemId) != category) {
            throw new IllegalArgumentException("Item category does not match the configured item type");
        }
        if (avaIPeriod != 0 && quantity != 1) {
            throw new IllegalArgumentException("Timed and permanent items must be granted individually");
        }
        // Lock the owner even when no stack exists, preventing concurrent first grants
        // from creating separate stacks. Keep the same owner-then-item lock order as boxes.
        Player player = players.findByIdForUpdate(playerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Player not found"));
        Instant now = inventoryClock.instant();
        if (avaIPeriod == 0 && category != ItemCategory.VEHICLE) {
            var stacks = items.findStacksForUpdate(playerId, itemId, status, obtainTime, category);
            if (!stacks.isEmpty()) {
                long total = quantity;
                for (PlayerItem stack : stacks) {
                    if (stack.getQuantity() < 0) {
                        throw new ResponseStatusException(HttpStatus.CONFLICT, "Invalid inventory quantity");
                    }
                    total += stack.getQuantity();
                    if (total > Integer.MAX_VALUE) {
                        throw new ResponseStatusException(HttpStatus.CONFLICT, "Inventory quantity limit exceeded");
                    }
                }
                PlayerItem stack = stacks.getFirst();
                stack.setQuantity((int) total);
                // Fold compatible legacy duplicates into the oldest ownership record.
                if (stacks.size() > 1) items.deleteAll(stacks.subList(1, stacks.size()));
                return toDTO(items.save(stack), player, now);
            }
        }
        if (avaIPeriod != 0) {
            var owned = items.findDurationItemsForUpdate(playerId, itemId, status, category);
            if (!owned.isEmpty()) {
                // Preserve the equipped ownership ID when consolidating legacy duplicates.
                PlayerItem target = owned.stream()
                        .filter(item -> Objects.equals(item.getId(), player.getSelectedPlayerItemId()))
                        .findFirst().orElse(owned.getFirst());
                boolean permanent = avaIPeriod == -1
                        || owned.stream().anyMatch(item -> item.getAvaIPeriod() == -1);
                Duration remaining = Duration.ofSeconds(obtainTime);
                if (!permanent) {
                    for (PlayerItem existing : owned) {
                        if (existing.getExpiresAt() != null && existing.getExpiresAt().isAfter(now)) {
                            remaining = remaining.plus(Duration.between(now, existing.getExpiresAt()));
                        }
                    }
                    if (remaining.compareTo(Duration.ofSeconds(Integer.MAX_VALUE)) > 0) {
                        throw new ResponseStatusException(HttpStatus.CONFLICT, "Inventory duration limit exceeded");
                    }
                }
                target.setQuantity(1);
                target.setVehicleCategory(rules.vehicleCategoryFor(itemId));
                applySubcategories(target, itemId);
                target.setAvaIPeriod(permanent ? -1 : avaIPeriod);
                target.setObtainTime(permanent ? 0 : (int) remaining.getSeconds());
                target.setExpiresAt(permanent ? null : now.plus(remaining));
                var duplicates = owned.stream().filter(item -> !item.getId().equals(target.getId())).toList();
                if (!duplicates.isEmpty()) items.deleteAll(duplicates);
                return toDTO(items.save(target), player, now);
            }
        }
        PlayerItem item = new PlayerItem(playerId, itemId, category, quantity, avaIPeriod, status,
                avaIPeriod == -1 ? 0 : obtainTime);
        item.setVehicleCategory(rules.vehicleCategoryFor(itemId));
        applySubcategories(item, itemId);
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
        var definition = rules.definitionFor(item.getItemId());
        return new PlayerItemDTO(item.getId(), item.getItemId(),
                item.getCategory() != null ? item.getCategory()
                        : rules.categoryFor(item.getItemId()),
                subcategory(item, definition),
                item.getQuantity(),
                item.getAvaIPeriod(), item.isStatus(), remainingSeconds(item, now),
                isExpired(item, now), Objects.equals(item.getId(), player.getSelectedPlayerItemId())
                && isUsableCar(item, now));
    }

    private String subcategory(PlayerItem item, com.game.speed.inventory.config.ItemDefinition definition) {
        return switch (item.getCategory() != null ? item.getCategory() : definition.category()) {
            case VEHICLE -> item.getVehicleCategory() != null ? item.getVehicleCategory().name()
                    : enumName(definition.vehicleCategory());
            case OUTFIT -> item.getOutfitCategory() != null ? item.getOutfitCategory().name()
                    : enumName(definition.outfitCategory());
            case RUNE -> item.getRuneCategory() != null ? item.getRuneCategory().name()
                    : enumName(definition.runeCategory());
            case EFFECT -> item.getEffectCategory() != null ? item.getEffectCategory().name()
                    : enumName(definition.effectCategory());
            case UTILITY -> item.getUtilityCategory() != null ? item.getUtilityCategory().name()
                    : enumName(definition.utilityCategory());
            case PET -> item.getPetCategory() != null ? item.getPetCategory().name()
                    : enumName(definition.petCategory());
            case ITEM -> null;
        };
    }

    private String enumName(Enum<?> value) {
        return value == null ? null : value.name();
    }

    private void applySubcategories(PlayerItem item, int itemId) {
        var definition = rules.definitionFor(itemId);
        item.setVehicleCategory(definition.vehicleCategory());
        item.setOutfitCategory(definition.outfitCategory());
        item.setRuneCategory(definition.runeCategory());
        item.setEffectCategory(definition.effectCategory());
        item.setUtilityCategory(definition.utilityCategory());
        item.setPetCategory(definition.petCategory());
    }
}

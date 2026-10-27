package com.game.speed.box;

import com.game.speed.box.config.BoxProperties;
import com.game.speed.box.config.BoxProperties.Reward;
import com.game.speed.box.entity.BoxOpening;
import com.game.speed.box.repository.BoxOpeningRepository;
import com.game.speed.box.service.BoxRewardPicker;
import com.game.speed.box.service.BoxService;
import com.game.speed.inventory.config.InventoryRules;
import com.game.speed.inventory.dto.PlayerItemDTO;
import com.game.speed.inventory.entity.PlayerItem;
import com.game.speed.inventory.repository.InventoryRepository;
import com.game.speed.inventory.service.InventoryService;
import com.game.speed.player.entity.Player;
import com.game.speed.player.repository.PlayerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class BoxServiceTests {
    final PlayerRepository players = mock(PlayerRepository.class);
    final InventoryRepository items = mock(InventoryRepository.class);
    final BoxOpeningRepository openings = mock(BoxOpeningRepository.class);
    final InventoryService inventory = mock(InventoryService.class);
    final InventoryRules rules = new InventoryRules();
    final BoxProperties config = new BoxProperties();
    final BoxRewardPicker picker = mock(BoxRewardPicker.class);
    final Instant now = Instant.parse("2026-10-26T10:00:00Z");
    final BoxService service = new BoxService(players, items, openings, inventory, rules,
            config, picker, Clock.fixed(now, ZoneOffset.UTC));
    final UUID key = UUID.randomUUID();
    final PlayerItem box = new PlayerItem(7L, 90001, 2, 0, true, 0);
    final Reward reward = new Reward(13703, 1, 1, true, 86400, 100);

    @BeforeEach
    void setup() {
        Player player = new Player();
        player.setId(7L);
        box.setId(42L);
        rules.setCarItemIds(Set.of(13703));
        config.getRewards().put(90001, List.of(reward));
        when(players.findByUsernameForUpdate("racer")).thenReturn(Optional.of(player));
        when(items.findOwnedForUpdate(42L, 7L)).thenReturn(Optional.of(box));
        when(picker.pick(anyList())).thenReturn(reward);
        when(inventory.grantItem(7L, 13703, 1, 1, true, 86400))
                .thenReturn(new PlayerItemDTO(80L, 13703, com.game.speed.inventory.entity.ItemCategory.VEHICLE,
                        null, 1, 1, true, 86400, false, false));
        when(openings.save(any())).thenAnswer(invocation -> {
            BoxOpening receipt = invocation.getArgument(0);
            receipt.setId(10L);
            return receipt;
        });
    }

    @Test
    void consumesOneBoxAndRecordsReward() {
        var result = service.open("racer", 42L, key);
        assertEquals(1, box.getQuantity());
        assertEquals(1, result.remainingBoxQuantity());
        assertEquals(13703, result.reward().itemId());
        assertEquals(86400, result.reward().obtainTime());
        assertEquals(now, result.openedAt());
        verify(items).save(box);
        verify(openings).save(any());
    }

    @Test
    void finalBoxIsDeletedAndRetryReturnsReceiptWithoutGrantingAgain() {
        box.setQuantity(1);
        var first = service.open("racer", 42L, key);
        var captor = org.mockito.ArgumentCaptor.forClass(BoxOpening.class);
        verify(openings).save(captor.capture());
        when(openings.findByPlayerIdAndRequestId(7L, key.toString())).thenReturn(Optional.of(captor.getValue()));
        assertEquals(first, service.open("racer", 42L, key));
        verify(items).delete(box);
        verify(items, times(1)).findOwnedForUpdate(42L, 7L);
        verify(inventory, times(1)).grantItem(7L, 13703, 1, 1, true, 86400);
        assertEquals(0, first.remainingBoxQuantity());
    }

    @Test
    void reusedKeyForAnotherBoxIsConflict() {
        BoxOpening prior = new BoxOpening();
        prior.setBoxPlayerItemId(99L);
        when(openings.findByPlayerIdAndRequestId(7L, key.toString())).thenReturn(Optional.of(prior));
        assertEquals(409, assertThrows(ResponseStatusException.class,
                () -> service.open("racer", 42L, key)).getStatusCode().value());
        verifyNoInteractions(items, inventory, picker);
    }

    @Test
    void otherPlayersBoxCannotBeOpened() {
        assertEquals(404, assertThrows(ResponseStatusException.class,
                () -> service.open("racer", 99L, key)).getStatusCode().value());
        verifyNoInteractions(inventory, picker);
    }

    @Test
    void expiredBoxAndMissingDeadlineAreRejected() {
        box.setAvaIPeriod(1);
        box.setExpiresAt(now);
        assertThrows(ResponseStatusException.class, () -> service.open("racer", 42L, key));
        box.setExpiresAt(null);
        assertThrows(ResponseStatusException.class, () -> service.open("racer", 42L, key));
        verifyNoInteractions(inventory, picker);
        verify(items, never()).delete(any());
    }

    @Test
    void unconfiguredBoxAndInvalidRewardTableDoNotConsumeItems() {
        config.getRewards().clear();
        assertEquals(400, assertThrows(ResponseStatusException.class,
                () -> service.open("racer", 42L, key)).getStatusCode().value());
        config.getRewards().put(90001, List.of(reward, new Reward(13703, 2, -1, true, 0, 1)));
        assertEquals(500, assertThrows(ResponseStatusException.class,
                () -> service.open("racer", 42L, key)).getStatusCode().value());
        assertEquals(2, box.getQuantity());
        verifyNoInteractions(inventory, picker);
    }
}

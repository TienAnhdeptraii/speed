package com.game.speed.inventory;

import com.game.speed.inventory.config.InventoryRules;
import com.game.speed.inventory.entity.PlayerItem;
import com.game.speed.inventory.repository.InventoryRepository;
import com.game.speed.inventory.service.InventoryService;
import com.game.speed.player.entity.Player;
import com.game.speed.player.repository.PlayerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.time.Clock;
import java.time.Instant;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class InventoryServiceTests {
    private final PlayerRepository players = mock(PlayerRepository.class);
    private final InventoryRepository items = mock(InventoryRepository.class);
    private final InventoryRules rules = new InventoryRules();
    private final Clock clock = mock(Clock.class);
    private final Instant now = Instant.parse("2026-10-26T10:00:00Z");
    private final InventoryService service = new InventoryService(players, items, rules, clock);
    private Player player;
    private PlayerItem car;

    @BeforeEach
    void setUp() {
        when(clock.instant()).thenReturn(now);
        rules.setCarItemIds(Set.of(13703, 21901, 12720, 26931, 24859));
        player = new Player();
        player.setId(7L);
        car = new PlayerItem();
        car.setId(42L);
        car.setPlayerId(7L);
        car.setItemId(13703);
        car.setAvaIPeriod(-1);
        when(players.findByUsername("racer")).thenReturn(Optional.of(player));
        when(players.findByUsernameForUpdate("racer")).thenReturn(Optional.of(player));
    }

    @Test
    void selectsOwnedPermanentCarAndCanSelectItAgain() {
        when(items.findOwnedForUpdate(42L, 7L)).thenReturn(Optional.of(car));
        assertTrue(service.selectCar("racer", 42L).selected());
        assertTrue(service.selectCar("racer", 42L).selected());
        assertEquals(42L, player.getSelectedPlayerItemId());
        verify(items, never()).save(any());
    }

    @Test
    void cannotSelectAnotherPlayersItem() {
        when(items.findOwnedForUpdate(99L, 7L)).thenReturn(Optional.empty());
        var error = assertThrows(ResponseStatusException.class, () -> service.selectCar("racer", 99L));
        assertEquals(404, error.getStatusCode().value());
        verify(players, never()).save(any());
    }

    @Test
    void rejectsExpiredCarWithoutChangingSelection() {
        player.setSelectedPlayerItemId(11L);
        car.setAvaIPeriod(1);
        car.setObtainTime(0);
        car.setExpiresAt(now);
        when(items.findOwnedForUpdate(42L, 7L)).thenReturn(Optional.of(car));
        assertEquals(400, assertThrows(ResponseStatusException.class,
                () -> service.selectCar("racer", 42L)).getStatusCode().value());
        assertEquals(11L, player.getSelectedPlayerItemId());
        verify(players, never()).save(any());
    }

    @Test
    void rejectsNonCarAndZeroQuantity() {
        when(items.findOwnedForUpdate(42L, 7L)).thenReturn(Optional.of(car));
        car.setItemId(999);
        assertThrows(ResponseStatusException.class, () -> service.selectCar("racer", 42L));
        car.setItemId(13703);
        car.setQuantity(0);
        assertThrows(ResponseStatusException.class, () -> service.selectCar("racer", 42L));
        verify(players, never()).save(any());
    }

    @Test
    void expiredSelectionIsNotReturnedAsUsable() {
        player.setSelectedPlayerItemId(42L);
        car.setAvaIPeriod(1);
        car.setObtainTime(0);
        car.setExpiresAt(now.minusSeconds(1));
        when(items.findByIdAndPlayerId(42L, 7L)).thenReturn(Optional.of(car));
        when(items.findAllByPlayerIdOrderByIdAsc(7L)).thenReturn(List.of(car));
        assertTrue(service.getSelectedCar("racer").isEmpty());
        var result = service.getInventory("racer");
        assertTrue(result.getFirst().expired());
        assertFalse(result.getFirst().selected());
    }

    @Test
    void grantsPreserveRemainingSecondsAndStatusWithoutAutoEquip() {
        when(players.findById(7L)).thenReturn(Optional.of(player));
        when(items.save(any(PlayerItem.class))).thenAnswer(invocation -> {
            PlayerItem item = invocation.getArgument(0);
            item.setId(43L);
            return item;
        });
        var temporary = service.grantItem(7L, 13703, 1, 1, true, 3600);
        var permanent = service.grantItem(7L, 13703, 1, -1, false, 0);
        assertEquals(3600, temporary.obtainTime());
        assertEquals(1, temporary.avaIPeriod());
        assertTrue(temporary.status());
        assertEquals(0, permanent.obtainTime());
        assertEquals(-1, permanent.avaIPeriod());
        assertFalse(permanent.status());
        assertFalse(permanent.expired());
        assertFalse(temporary.selected());
        verify(items, times(2)).save(any(PlayerItem.class));
    }

    @Test
    void invalidGrantsDoNotWriteInventory() {
        assertThrows(IllegalArgumentException.class, () -> service.grantItem(7L, 13703, 2, -1, true, 0));
        assertThrows(IllegalArgumentException.class, () -> service.grantItem(7L, 999, 0, 0, true, 0));
        assertThrows(IllegalArgumentException.class,
                () -> service.grantItem(7L, 999, 1, 1, true, -1));
        assertThrows(IllegalArgumentException.class,
                () -> service.grantItem(7L, 999, 1, 1, true, 0));
        assertThrows(IllegalArgumentException.class,
                () -> service.grantItem(7L, 999, 1, -2, true, 0));
        verifyNoInteractions(items);
    }

    @Test
    void timedCarWithRemainingSecondsCanBeSelectedWithoutDecrementingOnRead() {
        car.setAvaIPeriod(1);
        car.setObtainTime(1);
        car.setExpiresAt(now.plusSeconds(1));
        when(items.findOwnedForUpdate(42L, 7L)).thenReturn(Optional.of(car));
        var result = service.selectCar("racer", 42L);
        assertTrue(result.selected());
        assertFalse(result.expired());
        assertEquals(1, result.obtainTime());
        assertEquals(1, car.getObtainTime());
    }

    @Test
    void quantityBasedItemWithZeroSecondsIsNotExpired() {
        car.setAvaIPeriod(0);
        car.setObtainTime(0);
        when(items.findAllByPlayerIdOrderByIdAsc(7L)).thenReturn(List.of(car));
        assertFalse(service.getInventory("racer").getFirst().expired());
    }

    @Test
    void countdownContinuesOfflineWithoutWritingOrResettingExpiration() {
        car.setAvaIPeriod(1);
        car.setObtainTime(3600);
        car.setExpiresAt(now.plusSeconds(3600));
        player.setSelectedPlayerItemId(42L);
        when(items.findAllByPlayerIdOrderByIdAsc(7L)).thenReturn(List.of(car));
        when(items.findByIdAndPlayerId(42L, 7L)).thenReturn(Optional.of(car));
        assertEquals(3600, service.getInventory("racer").getFirst().obtainTime());
        when(clock.instant()).thenReturn(now.plusSeconds(600));
        assertEquals(3000, service.getInventory("racer").getFirst().obtainTime());
        when(clock.instant()).thenReturn(now.plusSeconds(3600));
        var expired = service.getInventory("racer").getFirst();
        assertEquals(0, expired.obtainTime());
        assertTrue(expired.expired());
        assertFalse(expired.selected());
        assertTrue(service.getSelectedCar("racer").isEmpty());
        assertEquals(now.plusSeconds(3600), car.getExpiresAt());
        assertEquals(3600, car.getObtainTime());
        verify(items, never()).save(any());
    }

    @Test
    void grantPersistsAbsoluteDeadlineOnlyForTimedItems() {
        when(players.findById(7L)).thenReturn(Optional.of(player));
        when(items.save(any(PlayerItem.class))).thenAnswer(invocation -> invocation.getArgument(0));
        service.grantItem(7L, 13703, 1, 1, true, 86400);
        service.grantItem(7L, 13703, 1, -1, true, 0);
        var records = org.mockito.ArgumentCaptor.forClass(PlayerItem.class);
        verify(items, times(2)).save(records.capture());
        assertEquals(now.plusSeconds(86400), records.getAllValues().get(0).getExpiresAt());
        assertNull(records.getAllValues().get(1).getExpiresAt());
    }

    @Test
    void legacyTimedItemWithoutDeadlineCannotBeEquipped() {
        car.setAvaIPeriod(1);
        car.setObtainTime(3600);
        when(items.findOwnedForUpdate(42L, 7L)).thenReturn(Optional.of(car));
        assertThrows(ResponseStatusException.class, () -> service.selectCar("racer", 42L));
        assertNull(car.getExpiresAt());
        verify(players, never()).save(any());
    }
}

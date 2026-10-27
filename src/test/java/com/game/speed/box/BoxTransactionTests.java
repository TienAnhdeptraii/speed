package com.game.speed.box;

import com.game.speed.box.config.BoxProperties;
import com.game.speed.box.config.BoxProperties.Reward;
import com.game.speed.box.repository.BoxOpeningRepository;
import com.game.speed.box.service.BoxService;
import com.game.speed.inventory.entity.PlayerItem;
import com.game.speed.inventory.repository.InventoryRepository;
import com.game.speed.inventory.service.InventoryService;
import com.game.speed.player.entity.Player;
import com.game.speed.player.repository.PlayerRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import java.util.List;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

// Uses the same configured MySQL database as contextLoads; fixtures are cleaned up.
@SpringBootTest(properties = {
        "app.jwt-secret=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY="
})
class BoxTransactionTests {
    @Autowired BoxService boxes;
    @Autowired PlayerRepository players;
    @Autowired InventoryRepository items;
    @Autowired BoxOpeningRepository openings;
    @Autowired BoxProperties config;
    @Autowired PlatformTransactionManager transactionManager;
    @MockitoSpyBean InventoryService inventory;

    @Test
    @org.springframework.transaction.annotation.Transactional
    void duplicatePermanentRewardsReuseOwnershipAndRetryDoesNotExtendTime() {
        int boxCode = 900022;
        var previous = config.getRewards().put(boxCode,
                List.of(new Reward(13703, 1, 1, true, 604800, 1)));
        try {
            Player player = new Player();
            player.setUsername("duration-" + UUID.randomUUID());
            player.setName("Duration test");
            player.setPassword("unused-test-password");
            players.saveAndFlush(player);
            var box = items.saveAndFlush(new PlayerItem(player.getId(), boxCode, 4, 0, true, 0));
            UUID key = UUID.randomUUID();
            var first = boxes.open(player.getUsername(), box.getId(), key);
            var rewardId = first.reward().playerItemId();
            var deadline = items.findById(rewardId).orElseThrow().getExpiresAt();
            boxes.open(player.getUsername(), box.getId(), key);
            assertEquals(deadline, items.findById(rewardId).orElseThrow().getExpiresAt());
            var second = boxes.open(player.getUsername(), box.getId(), UUID.randomUUID());
            assertEquals(rewardId, second.reward().playerItemId());
            assertEquals(deadline.plusSeconds(604800), items.findById(rewardId).orElseThrow().getExpiresAt());
            config.getRewards().put(boxCode, List.of(new Reward(13703, 1, -1, true, 0, 1)));
            assertEquals(rewardId, boxes.open(player.getUsername(), box.getId(), UUID.randomUUID()).reward().playerItemId());
            assertEquals(rewardId, boxes.open(player.getUsername(), box.getId(), UUID.randomUUID()).reward().playerItemId());
            items.flush();
            var owned = items.findAllByPlayerIdOrderByIdAsc(player.getId());
            assertEquals(1, owned.size());
            assertEquals(-1, owned.getFirst().getAvaIPeriod());
            assertNull(owned.getFirst().getExpiresAt());
        } finally {
            if (previous == null) config.getRewards().remove(boxCode);
            else config.getRewards().put(boxCode, previous);
        }
    }

    @Test
    @org.springframework.transaction.annotation.Transactional
    void quantityRewardsStackWhileReceiptsKeepOnlyAwardedQuantity() {
        int boxCode = 900020;
        int rewardCode = 900021;
        var previous = config.getRewards().put(boxCode,
                List.of(new Reward(rewardCode, 5, 0, true, 0, 1)));
        try {
            Player player = new Player();
            player.setUsername("stack-" + UUID.randomUUID());
            player.setName("Stack test");
            player.setPassword("unused-test-password");
            players.saveAndFlush(player);
            var ownedBox = items.saveAndFlush(new PlayerItem(player.getId(), boxCode, 2, 0, true, 0));
            var firstKey = UUID.randomUUID();
            var first = boxes.open(player.getUsername(), ownedBox.getId(), firstKey);
            var second = boxes.open(player.getUsername(), ownedBox.getId(), UUID.randomUUID());
            assertEquals(first.reward().playerItemId(), second.reward().playerItemId());
            assertEquals(5, first.reward().quantity());
            assertEquals(5, second.reward().quantity());
            assertEquals(first, boxes.open(player.getUsername(), ownedBox.getId(), firstKey));
            items.flush();
            var owned = items.findAllByPlayerIdOrderByIdAsc(player.getId());
            assertEquals(1, owned.size());
            assertEquals(10, owned.getFirst().getQuantity());
            // A different status and a timed variant must stay separate.
            inventory.grantItem(player.getId(), rewardCode, 3, 0, false, 0);
            inventory.grantItem(player.getId(), rewardCode, 1, 1, true, 86400);
            assertEquals(3, items.findAllByPlayerIdOrderByIdAsc(player.getId()).size());
        } finally {
            if (previous == null) config.getRewards().remove(boxCode);
            else config.getRewards().put(boxCode, previous);
        }
    }

    @Test
    void failedGrantRollsBackConsumptionAndRetryCanSucceed() {
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        int boxCode = 900019;
        String username = "box-test-" + UUID.randomUUID();
        UUID requestId = UUID.randomUUID();
        var previous = config.getRewards().put(boxCode,
                List.of(new Reward(13703, 1, -1, true, 0, 1)));
        Long[] ids = tx.execute(status -> {
            Player player = new Player();
            player.setUsername(username);
            player.setName("Box transaction test");
            player.setPassword("unused-test-password");
            players.saveAndFlush(player);
            PlayerItem box = new PlayerItem(player.getId(), boxCode, 1, 0, true, 0);
            items.saveAndFlush(box);
            return new Long[]{player.getId(), box.getId()};
        });
        try {
            doAnswer(invocation -> {
                invocation.callRealMethod();
                throw new IllegalStateException("Simulated failure after granting reward");
            }).when(inventory).grantItem(eq(ids[0]), eq(13703), eq(1), eq(-1), eq(true), eq(0));
            assertThrows(IllegalStateException.class, () -> boxes.open(username, ids[1], requestId));
            tx.executeWithoutResult(status -> {
                assertEquals(1, items.findById(ids[1]).orElseThrow().getQuantity());
                assertEquals(1, items.findAllByPlayerIdOrderByIdAsc(ids[0]).size());
                assertTrue(openings.findByPlayerIdAndRequestId(ids[0], requestId.toString()).isEmpty());
            });
            doCallRealMethod().when(inventory).grantItem(eq(ids[0]), eq(13703), eq(1), eq(-1), eq(true), eq(0));
            var receipt = boxes.open(username, ids[1], requestId);
            assertEquals(receipt, boxes.open(username, ids[1], requestId));
            assertFalse(items.existsById(ids[1]));
            assertEquals(1, items.findAllByPlayerIdOrderByIdAsc(ids[0]).size());
        } finally {
            tx.executeWithoutResult(status -> {
                openings.findByPlayerIdAndRequestId(ids[0], requestId.toString()).ifPresent(openings::delete);
                items.deleteAll(items.findAllByPlayerIdOrderByIdAsc(ids[0]));
                players.deleteById(ids[0]);
            });
            if (previous == null) config.getRewards().remove(boxCode);
            else config.getRewards().put(boxCode, previous);
        }
    }
}

package com.game.speed.inventory.repository;

import com.game.speed.inventory.entity.PlayerItem;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface InventoryRepository extends JpaRepository<PlayerItem, Long> {
    List<PlayerItem> findAllByPlayerIdOrderByIdAsc(Long playerId);
    Optional<PlayerItem> findByIdAndPlayerId(Long id, Long playerId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from PlayerItem i where i.playerId = :playerId and i.itemId = :itemId "
            + "and (i.avaIPeriod = -1 or i.avaIPeriod > 0) and i.status = :status "
            + "and i.category = :category order by i.id")
    List<PlayerItem> findDurationItemsForUpdate(@Param("playerId") Long playerId,
                                               @Param("itemId") int itemId, @Param("status") boolean status,
                                               @Param("category") com.game.speed.inventory.entity.ItemCategory category);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from PlayerItem i where i.playerId = :playerId and i.itemId = :itemId "
            + "and i.avaIPeriod = 0 and i.status = :status and i.obtainTime = :obtainTime "
            + "and i.expiresAt is null and i.category = :category order by i.id")
    List<PlayerItem> findStacksForUpdate(@Param("playerId") Long playerId, @Param("itemId") int itemId,
                                        @Param("status") boolean status, @Param("obtainTime") int obtainTime,
                                        @Param("category") com.game.speed.inventory.entity.ItemCategory category);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from PlayerItem i where i.id = :id and i.playerId = :playerId")
    Optional<PlayerItem> findOwnedForUpdate(@Param("id") Long id, @Param("playerId") Long playerId);
}

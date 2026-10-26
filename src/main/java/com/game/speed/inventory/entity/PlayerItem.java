package com.game.speed.inventory.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;
import java.time.Instant;

@Entity
@Table(name = "player_item", indexes = @Index(name = "idx_player_item_owner", columnList = "player_id"))
@Getter
@Setter
public class PlayerItem {
    public PlayerItem() {}
    public PlayerItem(Long playerId, int itemId,int quantity, int avaIPeriod, boolean status, int obtainTime){
        this.itemId = itemId;
        this.playerId = playerId;
        this.quantity = quantity;
        this.avaIPeriod = avaIPeriod;
        this.status = status;
        this.obtainTime = obtainTime;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "player_id", nullable = false, updatable = false)
    private Long playerId;

    @Positive
    @Column(name = "item_id", nullable = false, updatable = false)
    private int itemId;

    @Positive
    @Column(nullable = false)
    private int quantity = 1;

    @Min(-1)
    @Column(name = "avai_period") //-1 vĩnh viễn, 0 số lượng ,1 ngày
    private int avaIPeriod;

    @Column(name = "status")
    private boolean status;

    @Column(name = "obtain_time")
    @PositiveOrZero
    // Granted duration in seconds. Timed API responses are calculated from expiresAt.
    private int obtainTime = 0;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @PrePersist
    private void initializeExpiration() {
        // Also support trusted callers that create ownership records directly.
        if (avaIPeriod > 0 && expiresAt == null) {
            expiresAt = Instant.now().plusSeconds(Math.max(0, obtainTime));
        }
    }

}

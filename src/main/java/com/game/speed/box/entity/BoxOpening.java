package com.game.speed.box.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.Instant;

@Entity
@Table(name = "box_opening", uniqueConstraints = @UniqueConstraint(
        name = "uk_box_opening_request", columnNames = {"player_id", "request_id"}))
@Getter
@Setter
public class BoxOpening {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "player_id", nullable = false)
    private Long playerId;
    @Column(name = "request_id", nullable = false, length = 36)
    private String requestId;
    @Column(nullable = false)
    private Long boxPlayerItemId;
    @Column(nullable = false)
    private int boxItemId;
    @Column(nullable = false)
    private int remainingBoxQuantity;
    @Column(nullable = false)
    private Long rewardPlayerItemId;
    @Column(nullable = false)
    private int rewardItemId;
    // Nullable for history rows created before category metadata was added.
    @Column(name = "reward_category", length = 20)
    private String rewardCategory;
    @Column(name = "reward_vehicle_category", length = 20)
    private String rewardVehicleCategory;
    @Column(nullable = false)
    private int rewardQuantity;
    @Column(nullable = false)
    private int rewardAvaIPeriod;
    @Column(nullable = false)
    private boolean rewardStatus;
    @Column(nullable = false)
    private int rewardObtainTime;
    @Column(nullable = false)
    private Instant openedAt;
}

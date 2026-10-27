package com.game.speed.box.entity;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "box_definition")
@Getter
public class BoxDefinition {
    @Id
    @Column(name = "item_id")
    private int itemId;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "box_definition_reward", joinColumns = @JoinColumn(name = "box_item_id"))
    @OrderColumn(name = "reward_position")
    private List<BoxRewardEntry> rewards = new ArrayList<>();

    protected BoxDefinition() {}

    public BoxDefinition(int itemId, List<BoxRewardEntry> rewards) {
        this.itemId = itemId;
        this.rewards.addAll(rewards);
    }
}

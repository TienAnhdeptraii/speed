package com.game.speed.box.entity;

import com.game.speed.box.config.BoxProperties.Reward;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Getter;

@Embeddable
@Getter
public class BoxRewardEntry {
    @Column(name = "reward_item_id", nullable = false)
    private int itemId;
    @Column(nullable = false)
    private int quantity;
    @Column(name = "avai_period", nullable = false)
    private int avaIPeriod;
    @Column(nullable = false)
    private boolean status;
    @Column(name = "obtain_time", nullable = false)
    private int obtainTime;
    @Column(nullable = false)
    private int weight;

    protected BoxRewardEntry() {}

    public BoxRewardEntry(Reward reward) {
        itemId = reward.itemId();
        quantity = reward.quantity();
        avaIPeriod = reward.avaIPeriod();
        status = reward.status();
        obtainTime = reward.obtainTime();
        weight = reward.weight();
    }

    public Reward toReward() {
        return new Reward(itemId, quantity, avaIPeriod, status, obtainTime, weight);
    }
}

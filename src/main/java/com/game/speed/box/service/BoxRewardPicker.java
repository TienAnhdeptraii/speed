package com.game.speed.box.service;

import com.game.speed.box.config.BoxProperties.Reward;
import org.springframework.stereotype.Component;
import java.security.SecureRandom;
import java.util.List;

@Component
public class BoxRewardPicker {
    private final SecureRandom random = new SecureRandom();

    public Reward pick(List<Reward> rewards) {
        long total = 0;
        for (Reward reward : rewards) {
            if (reward.weight() <= 0) throw new IllegalArgumentException("Reward weight must be positive");
            total = Math.addExact(total, reward.weight());
        }
        long ticket = random.nextLong(total);
        for (Reward reward : rewards) {
            if (ticket < reward.weight()) return reward;
            ticket -= reward.weight();
        }
        throw new IllegalStateException("No reward selected");
    }
}

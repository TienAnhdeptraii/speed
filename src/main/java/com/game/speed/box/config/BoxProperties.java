package com.game.speed.box.config;

import lombok.Getter;
import lombok.Setter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
@Setter
public class BoxProperties {
    // Key = client itemId of the box, loaded from the reward JSON file.
    private Map<Integer, List<Reward>> rewards = new HashMap<>();

    public record Reward(int itemId, int quantity, int avaIPeriod,
                         boolean status, int obtainTime, int weight) {}
}

package com.game.speed.inventory.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import java.util.HashSet;
import java.util.Set;

@Component
@ConfigurationProperties(prefix = "game.inventory")
@Getter
@Setter
public class InventoryRules {
    private Set<Integer> carItemIds = new HashSet<>();
}

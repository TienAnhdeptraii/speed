package com.game.speed.inventory.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.time.Clock;

@Configuration
public class InventoryTimeConfig {
    @Bean
    public Clock inventoryClock() {
        return Clock.systemUTC();
    }
}

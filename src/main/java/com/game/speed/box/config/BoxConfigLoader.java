package com.game.speed.box.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Configuration(proxyBeanMethods = false)
public class BoxConfigLoader {
    public record BoxFile(Map<Integer, List<BoxProperties.Reward>> boxes) {}

    @Bean
    public BoxProperties boxProperties(
            @Value("${game.box.config:classpath:boxes.json}") Resource resource) {
        return load(resource);
    }

    public static BoxProperties load(Resource resource) {
        var mapper = JsonMapper.builder()
                .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .enable(DeserializationFeature.FAIL_ON_MISSING_CREATOR_PROPERTIES)
                .enable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES)
                .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
                .build();
        try (var input = resource.getInputStream()) {
            BoxFile file = mapper.readValue(input, BoxFile.class);
            if (file == null || file.boxes() == null) throw new IllegalArgumentException("Missing boxes object");
            Map<Integer, List<BoxProperties.Reward>> validated = new HashMap<>();
            for (var entry : file.boxes().entrySet()) {
                if (entry.getKey() == null || entry.getKey() <= 0
                        || entry.getValue() == null || entry.getValue().isEmpty()) {
                    throw new IllegalArgumentException("Invalid box ID or empty reward list: " + entry.getKey());
                }
                for (var reward : entry.getValue()) {
                    if (reward == null || reward.itemId() <= 0 || reward.quantity() <= 0
                            || reward.weight() <= 0 || reward.avaIPeriod() < -1 || reward.obtainTime() < 0
                            || (reward.avaIPeriod() != 0 && reward.quantity() != 1)
                            || (reward.avaIPeriod() > 0 && reward.obtainTime() == 0)) {
                        throw new IllegalArgumentException("Invalid reward in box " + entry.getKey());
                    }
                }
                validated.put(entry.getKey(), List.copyOf(entry.getValue()));
            }
            BoxProperties properties = new BoxProperties();
            properties.setRewards(validated);
            return properties;
        } catch (Exception exception) {
            throw new IllegalStateException("Cannot load box rewards from " + resource.getDescription(), exception);
        }
    }
}

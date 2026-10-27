package com.game.speed.box;

import com.game.speed.box.config.BoxProperties;
import org.junit.jupiter.api.Test;
import com.game.speed.box.config.BoxConfigLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;
import java.nio.charset.StandardCharsets;
import static org.junit.jupiter.api.Assertions.*;

class BoxPropertiesTests {
    private BoxProperties parse(String json) {
        return BoxConfigLoader.load(new ByteArrayResource(json.getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    void loadsActualBoxFileThroughSpring() {
        new ApplicationContextRunner().withUserConfiguration(BoxConfigLoader.class)
                .withPropertyValues("game.box.config=classpath:boxes.json").run(context -> {
            assertNull(context.getStartupFailure());
            var rewards = context.getBean(BoxProperties.class).getRewards().get(18247);
            assertEquals(5, rewards.size());
            assertEquals(java.util.Set.of(13703, 21901, 12720, 26931, 24859),
                    rewards.stream().map(BoxProperties.Reward::itemId).collect(java.util.stream.Collectors.toSet()));
            assertTrue(rewards.stream().allMatch(reward -> reward.weight() == 20
                    && reward.avaIPeriod() == -1 && reward.quantity() == 1));
        });
    }

    @Test
    void allowsSameItemWithDifferentDurationsAndQuantityRewards() {
        var config = parse("""
                {"boxes":{"18247":[
                  {"itemId":13703, "quantity":1,"avaIPeriod":1,"status":true,"obtainTime":259200,"weight":40},
                  {"itemId":13703, "quantity":1,"avaIPeriod":1,"status":true,"obtainTime":604800,"weight":30},
                  {"itemId":13703, "quantity":1,"avaIPeriod":-1,"status":true,"obtainTime":0,"weight":10},
                  {"itemId":999, "quantity":5,"avaIPeriod":0,"status":true,"obtainTime":0,"weight":20}
                ]}}
                """);
        var rewards = config.getRewards().get(18247);
        assertEquals(259200, rewards.get(0).obtainTime());
        assertEquals(604800, rewards.get(1).obtainTime());
        assertEquals(5, rewards.get(3).quantity());
    }

    @Test
    void rejectsMissingFileAndMalformedOrIncompleteConfiguration() {
        assertThrows(IllegalStateException.class,
                () -> BoxConfigLoader.load(new ClassPathResource("missing-box-config.json")));
        for (String json : new String[]{"broken", "{}", "{\"boxes\":null}",
                "{\"boxes\":{\"18247\":[]}}", "{\"boxes\":{\"18247\":[{}]}}"}) {
            assertThrows(IllegalStateException.class, () -> parse(json));
        }
    }

    @Test
    void rejectsInvalidRewardAndUnknownFields() {
        String json = """
                {"boxes":{"18247":[
                  {"itemId":13703, "quantity":1,"avaIPeriod":1,"status":true,"obtainTime":86400,"weight":20}
                ]}}
                """;
        assertThrows(IllegalStateException.class, () -> parse(json.replace("\"weight\":20", "\"weight\":0")));
        assertThrows(IllegalStateException.class, () -> parse(json.replace("\"obtainTime\":86400", "\"obtainTime\":0")));
        assertThrows(IllegalStateException.class, () -> parse(json.replace("\"quantity\":1", "\"quantity\":2")));
        assertThrows(IllegalStateException.class, () -> parse(json.replace("\"status\":true", "\"stats\":true")));
    }
}

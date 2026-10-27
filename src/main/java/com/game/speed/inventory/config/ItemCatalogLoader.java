package com.game.speed.inventory.config;

import com.game.speed.inventory.entity.ItemCategory;
import org.springframework.core.io.Resource;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;
import java.util.Map;

public final class ItemCatalogLoader {
    private ItemCatalogLoader() {}

    public static Map<Integer, ItemDefinition> load(Resource resource) {
        var mapper = JsonMapper.builder()
                .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .enable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES)
                .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
                .build();
        try (var input = resource.getInputStream()) {
            ItemCatalogFile file = mapper.readValue(input, ItemCatalogFile.class);
            if (file == null || file.items() == null) throw new IllegalArgumentException("Missing items object");
            for (var entry : file.items().entrySet()) {
                if (entry.getKey() == null || entry.getKey() <= 0 || entry.getValue() == null
                        || entry.getValue().category() == null) {
                    throw new IllegalArgumentException("Invalid item definition: " + entry.getKey());
                }
                var definition = entry.getValue();
                int subtypeCount = 0;
                if (definition.vehicleCategory() != null) {
                    subtypeCount++;
                    if (definition.category() != ItemCategory.VEHICLE)
                        throw new IllegalArgumentException("vehicleCategory requires VEHICLE: " + entry.getKey());
                }
                if (definition.outfitCategory() != null) {
                    subtypeCount++;
                    if (definition.category() != ItemCategory.OUTFIT)
                        throw new IllegalArgumentException("outfitCategory requires OUTFIT: " + entry.getKey());
                }
                if (definition.runeCategory() != null) {
                    subtypeCount++;
                    if (definition.category() != ItemCategory.RUNE)
                        throw new IllegalArgumentException("runeCategory requires RUNE: " + entry.getKey());
                }
                if (definition.effectCategory() != null) {
                    subtypeCount++;
                    if (definition.category() != ItemCategory.EFFECT)
                        throw new IllegalArgumentException("effectCategory requires EFFECT: " + entry.getKey());
                }
                if (definition.utilityCategory() != null) {
                    subtypeCount++;
                    if (definition.category() != ItemCategory.UTILITY)
                        throw new IllegalArgumentException("utilityCategory requires UTILITY: " + entry.getKey());
                }
                if (definition.petCategory() != null) {
                    subtypeCount++;
                    if (definition.category() != ItemCategory.PET)
                        throw new IllegalArgumentException("petCategory requires PET: " + entry.getKey());
                }
                if (subtypeCount > 1)
                    throw new IllegalArgumentException("Only one subcategory may be set: " + entry.getKey());
            }
            return Map.copyOf(file.items());
        } catch (Exception exception) {
            throw new IllegalStateException("Cannot load item catalog from " + resource.getDescription(), exception);
        }
    }
}

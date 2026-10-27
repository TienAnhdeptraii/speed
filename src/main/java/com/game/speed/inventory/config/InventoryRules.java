package com.game.speed.inventory.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import java.util.HashSet;
import java.util.Set;
import java.util.Map;
import com.game.speed.inventory.entity.ItemCategory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import jakarta.annotation.PostConstruct;

@Component
@ConfigurationProperties(prefix = "game.inventory")
@Getter
@Setter
public class InventoryRules {
    private Set<Integer> carItemIds = new HashSet<>();
    private Set<Integer> runeItemIds = new HashSet<>();
    private Map<Integer, ItemCategory> itemCategories = new java.util.HashMap<>();
    private Map<Integer, ItemDefinition> itemDefinitions = Map.of();

    @Value("${game.inventory.item-config:classpath:items.json}")
    private Resource itemConfig;

    @PostConstruct
    void loadItemDefinitions() {
        itemDefinitions = ItemCatalogLoader.load(itemConfig);
    }

    public ItemCategory categoryFor(int itemId) {
        ItemDefinition definition = itemDefinitions.get(itemId);
        if (definition != null) return definition.category();
        ItemCategory configured = itemCategories.get(itemId);
        if (configured != null) return configured;
        if (carItemIds.contains(itemId)) return ItemCategory.VEHICLE;
        if (runeItemIds.contains(itemId)) return ItemCategory.RUNE;
        return ItemCategory.ITEM;
    }

    public ItemCategory.VehicleCategory vehicleCategoryFor(int itemId) {
        ItemDefinition definition = itemDefinitions.get(itemId);
        return definition == null ? null : definition.vehicleCategory();
    }

    public ItemDefinition definitionFor(int itemId) {
        ItemDefinition definition = itemDefinitions.get(itemId);
        if (definition != null) return definition;
        return new ItemDefinition(categoryFor(itemId), null, null, null, null, null, null);
    }
}

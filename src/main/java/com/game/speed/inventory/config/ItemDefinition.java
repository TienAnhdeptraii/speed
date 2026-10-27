package com.game.speed.inventory.config;

import com.game.speed.inventory.entity.ItemCategory;

public record ItemDefinition(
        ItemCategory category,
        ItemCategory.VehicleCategory vehicleCategory,
        ItemCategory.OutfitCategory outfitCategory,
        ItemCategory.RuneCategory runeCategory,
        ItemCategory.EffectCategory effectCategory,
        ItemCategory.UtilityCategory utilityCategory,
        ItemCategory.PetCategory petCategory
) {}

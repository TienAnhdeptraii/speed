package com.game.speed.inventory.entity;

public enum ItemCategory {
    VEHICLE,
    RUNE,
    OUTFIT,
    EFFECT,
    UTILITY,
    PET,
    ITEM;

    public enum VehicleCategory {
        S, A, B, C, D,
        L1, L2, L3,
        M1, M2, M3,
        ENGINE, DECAL, PAINT
    }

    public enum OutfitCategory {
        SET,
        HAIR,
        EXPRESSION,
        TOP,
        BOTTOM,
        SHOES,
        HEADWEAR,
        BACK_ACCESSORY,
        HELD_ITEM,
        HAND_ACCESSORY,
        GLASSES,
        EARRINGS,
        FACE_ACCESSORY,
        WINGS,
        FOOT_GLOW,
        TIRE_MARK
    }

    public enum RuneCategory {
        RED,
        BLUE,
        YELLOW,
        PURPLE,
        OTHER
    }

    public enum EffectCategory {
        AURA,
        TRAIL,
        BOOST,
        WING,
        FOOT_GLOW,
        TIRE_MARK,
        OTHER
    }

    public enum UtilityCategory {
        CONSUMABLE,
        BOX,
        MATERIAL,
        TICKET,
        CURRENCY,
        TOKEN,
        GIFT,
        OTHER
    }

    public enum PetCategory {
        PET,
        PET_FOOD,
        PET_EQUIPMENT,
        PET_SKILL,
        PET_MATERIAL,
        OTHER
    }
}

package com.game.speed.inventory.dto;

import com.game.speed.inventory.entity.ItemCategory;

public record PlayerItemDTO(Long id, int itemId,
                            ItemCategory category,
                            String subcategory,
                            int quantity, int avaIPeriod,
                            boolean status, int obtainTime,
                            boolean expired, boolean selected) {}

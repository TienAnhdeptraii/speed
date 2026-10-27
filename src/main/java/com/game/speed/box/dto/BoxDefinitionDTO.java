package com.game.speed.box.dto;

import com.game.speed.box.config.BoxProperties.Reward;

import java.util.List;

public record BoxDefinitionDTO(int itemId, List<Reward> rewards) {}

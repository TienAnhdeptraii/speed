package com.game.speed.box.service;

import com.game.speed.box.config.BoxProperties;
import com.game.speed.box.config.BoxProperties.Reward;
import com.game.speed.box.dto.BoxDefinitionDTO;
import com.game.speed.box.dto.CreateBoxRequest;
import com.game.speed.box.entity.BoxDefinition;
import com.game.speed.box.entity.BoxRewardEntry;
import com.game.speed.box.repository.BoxDefinitionRepository;
import com.game.speed.inventory.config.InventoryRules;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BoxCatalogService {
    private final BoxDefinitionRepository definitions;
    private final BoxProperties configuredBoxes;
    private final InventoryRules inventoryRules;

    @Transactional
    public BoxDefinitionDTO create(CreateBoxRequest request) {
        if (request.itemId() <= 0 || request.rewards() == null || request.rewards().isEmpty()) {
            throw new IllegalArgumentException("Box ID and rewards are required");
        }
        if (configuredBoxes.getRewards().containsKey(request.itemId()) || definitions.existsById(request.itemId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Box ID already exists");
        }
        List<Reward> rewards = request.rewards().stream().map(reward -> {
            if (reward == null || reward.itemId() == null || reward.quantity() == null
                    || reward.avaIPeriod() == null || reward.status() == null
                    || reward.obtainTime() == null || reward.weight() == null) {
                throw new IllegalArgumentException("Reward fields are required");
            }
            return reward.toReward();
        }).toList();
        for (Reward reward : rewards) {
            if (reward.itemId() <= 0 || reward.quantity() <= 0 || reward.weight() <= 0
                    || reward.avaIPeriod() < -1 || reward.obtainTime() < 0
                    || (reward.avaIPeriod() > 0 && reward.obtainTime() == 0)
                    || (reward.avaIPeriod() != 0 && reward.quantity() != 1)
                    || (inventoryRules.getCarItemIds().contains(reward.itemId()) && reward.quantity() != 1)) {
                throw new IllegalArgumentException("Invalid box reward");
            }
        }
        BoxDefinition definition = new BoxDefinition(request.itemId(),
                rewards.stream().map(BoxRewardEntry::new).toList());
        try {
            definitions.saveAndFlush(definition);
        } catch (DataIntegrityViolationException exception) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Box ID already exists", exception);
        }
        return new BoxDefinitionDTO(request.itemId(), List.copyOf(rewards));
    }

    @Transactional(readOnly = true)
    public List<Reward> rewardsFor(int boxItemId) {
        return definitions.findById(boxItemId)
                .map(definition -> definition.getRewards().stream()
                        .map(BoxRewardEntry::toReward).toList())
                .orElseGet(() -> configuredBoxes.getRewards().get(boxItemId));
    }
}

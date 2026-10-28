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
import java.util.Map;
import java.util.TreeMap;

@Service
@RequiredArgsConstructor
public class BoxCatalogService {
    private final BoxDefinitionRepository definitions;
    private final BoxProperties configuredBoxes;
    private final InventoryRules inventoryRules;

    @Transactional
    public BoxDefinitionDTO create(CreateBoxRequest request) {
        List<Reward> rewards = validatedRewards(request);
        if (configuredBoxes.getRewards().containsKey(request.itemId()) || definitions.existsById(request.itemId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Box ID already exists");
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

    @Transactional
    public BoxDefinitionDTO update(int itemId, CreateBoxRequest request) {
        if (itemId <= 0 || request.itemId() != itemId) {
            throw new IllegalArgumentException("Box ID must match the URL and cannot be changed");
        }
        List<Reward> rewards = validatedRewards(request);
        BoxDefinition definition = definitions.findById(itemId).orElse(null);
        if (definition == null) {
            if (!configuredBoxes.getRewards().containsKey(itemId)) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Box not found");
            }
            // An edit to a JSON box is stored as a database override.
            definition = new BoxDefinition(itemId, List.of());
        }
        definition.replaceRewards(rewards.stream().map(BoxRewardEntry::new).toList());
        definitions.saveAndFlush(definition);
        return new BoxDefinitionDTO(itemId, List.copyOf(rewards));
    }

    private List<Reward> validatedRewards(CreateBoxRequest request) {
        if (request.itemId() <= 0 || request.rewards() == null || request.rewards().isEmpty()) {
            throw new IllegalArgumentException("Box ID and rewards are required");
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
        return rewards;
    }

    @Transactional(readOnly = true)
    public List<Reward> rewardsFor(int boxItemId) {
        return definitions.findById(boxItemId)
                .map(definition -> definition.getRewards().stream()
                        .map(BoxRewardEntry::toReward).toList())
                .orElseGet(() -> configuredBoxes.getRewards().get(boxItemId));
    }

    @Transactional(readOnly = true)
    public List<BoxDefinitionDTO> list() {
        Map<Integer, BoxDefinitionDTO> boxes = new TreeMap<>();
        configuredBoxes.getRewards().forEach((itemId, rewards) ->
                boxes.put(itemId, new BoxDefinitionDTO(itemId, List.copyOf(rewards))));
        definitions.findAll().forEach(definition ->
                boxes.put(definition.getItemId(), new BoxDefinitionDTO(definition.getItemId(),
                        definition.getRewards().stream().map(BoxRewardEntry::toReward).toList())));
        return List.copyOf(boxes.values());
    }
}

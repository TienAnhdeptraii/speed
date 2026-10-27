package com.game.speed.inventory.controller;

import com.game.speed.inventory.dto.GrantPlayerItemRequest;
import com.game.speed.inventory.dto.PlayerItemDTO;
import com.game.speed.inventory.service.InventoryService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.validation.annotation.Validated;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@Validated
@RequestMapping("/api/admin/players/{playerId}/inventory")
@RequiredArgsConstructor
public class AdminInventoryController {
    private final InventoryService inventory;

    @PostMapping
    public ResponseEntity<PlayerItemDTO> grantItem(
            @PathVariable @Positive Long playerId,
            @Valid @RequestBody GrantPlayerItemRequest request) {
        PlayerItemDTO granted = inventory.grantItem(playerId, request.itemId(), request.quantity(),
                request.avaIPeriod(), request.status(), request.obtainTime());
        return ResponseEntity.status(HttpStatus.CREATED).body(granted);
    }
}

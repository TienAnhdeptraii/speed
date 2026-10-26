package com.game.speed.inventory.controller;

import com.game.speed.inventory.dto.PlayerItemDTO;
import com.game.speed.inventory.dto.SelectCarRequest;
import com.game.speed.inventory.service.InventoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/players/me")
@RequiredArgsConstructor
public class InventoryController {
    private final InventoryService inventory;

    @GetMapping("/inventory")
    public List<PlayerItemDTO> getInventory(Authentication authentication) {
        return inventory.getInventory(authentication.getName());
    }

    @GetMapping("/selected-car")
    public ResponseEntity<PlayerItemDTO> getSelectedCar(Authentication authentication) {
        return inventory.getSelectedCar(authentication.getName())
                .map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.noContent().build());
    }

    @PutMapping("/selected-car")
    public PlayerItemDTO selectCar(Authentication authentication, @Valid @RequestBody SelectCarRequest request) {
        return inventory.selectCar(authentication.getName(), request.playerItemId());
    }
}

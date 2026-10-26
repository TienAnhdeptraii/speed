package com.game.speed.player.controller;

import com.game.speed.player.dto.PlayerInfoDTO;
import com.game.speed.player.dto.PlayerMeDTO;
import org.springframework.security.core.Authentication;
import com.game.speed.player.service.PlayerService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/players")
@RequiredArgsConstructor
public class PlayerController {

    // Tiêm PlayerService vào Controller thông qua Lombok @RequiredArgsConstructor
    private final PlayerService playerService;

    @GetMapping("/me")
    public ResponseEntity<PlayerMeDTO> getCurrentPlayer(Authentication authentication) {
        return ResponseEntity.ok(playerService.getCurrentPlayer(authentication.getName()));
    }


    //Lấy thông tin user đẻ bỏ lên screen
    @GetMapping("/{id}")
    public ResponseEntity<PlayerInfoDTO> getPlayer(@PathVariable Long id) {
        PlayerInfoDTO player = playerService.getPlayerById(id);
        return ResponseEntity.ok(player);
    }
}

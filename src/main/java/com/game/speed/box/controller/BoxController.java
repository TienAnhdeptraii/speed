package com.game.speed.box.controller;

import com.game.speed.box.dto.BoxOpeningDTO;
import com.game.speed.box.dto.OpenBoxRequest;
import com.game.speed.box.service.BoxService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/players/me/boxes")
@RequiredArgsConstructor
public class BoxController {
    private final BoxService boxes;

    @PostMapping("/open")
    public BoxOpeningDTO open(Authentication authentication, @Valid @RequestBody OpenBoxRequest request) {
        return boxes.open(authentication.getName(), request.playerItemId(), request.requestId());
    }
}

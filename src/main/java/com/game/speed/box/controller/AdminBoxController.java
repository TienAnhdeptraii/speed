package com.game.speed.box.controller;

import com.game.speed.box.dto.BoxDefinitionDTO;
import com.game.speed.box.dto.CreateBoxRequest;
import com.game.speed.box.service.BoxCatalogService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/boxes")
@RequiredArgsConstructor
public class AdminBoxController {
    private final BoxCatalogService boxes;

    @PostMapping
    public ResponseEntity<BoxDefinitionDTO> create(@Valid @RequestBody CreateBoxRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(boxes.create(request));
    }
}

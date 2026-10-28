package com.game.speed.box.controller;

import com.game.speed.box.dto.BoxDefinitionDTO;
import com.game.speed.box.dto.CreateBoxRequest;
import com.game.speed.box.service.BoxCatalogService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController
@RequestMapping("/api/admin/boxes")
@RequiredArgsConstructor
public class AdminBoxController {
    private final BoxCatalogService boxes;

    @GetMapping
    public List<BoxDefinitionDTO> list() {
        return boxes.list();
    }

    @PostMapping
    public ResponseEntity<BoxDefinitionDTO> create(@Valid @RequestBody CreateBoxRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(boxes.create(request));
    }

    @PutMapping("/{itemId}")
    public BoxDefinitionDTO update(@PathVariable int itemId, @Valid @RequestBody CreateBoxRequest request) {
        return boxes.update(itemId, request);
    }
}

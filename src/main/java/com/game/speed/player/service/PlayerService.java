package com.game.speed.player.service;

import com.game.speed.inventory.entity.PlayerItem;
import com.game.speed.inventory.entity.ItemCategory;
import com.game.speed.inventory.config.InventoryRules;
import com.game.speed.inventory.repository.InventoryRepository;
import com.game.speed.money.entity.Money;
import com.game.speed.money.repository.MoneyRepository;
import com.game.speed.player.dto.CreatePlayerDTO;
import com.game.speed.player.dto.PlayerInfoDTO;
import com.game.speed.player.dto.PlayerMeDTO;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import com.game.speed.player.entity.Player;
import com.game.speed.player.exception.UsernameAlreadyTakenException;
import com.game.speed.player.repository.PlayerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PlayerService {
    private final PlayerRepository playerRepository;
    private final MoneyRepository moneyRepository;
    private final PasswordEncoder passwordEncoder;
    private final InventoryRepository inventoryRepository;
    private final InventoryRules inventoryRules;

    @Transactional
    public Player createNewPlayer(CreatePlayerDTO request) {
        if (request.getUsername() == null || request.getUsername().isBlank()
                || request.getPassword() == null || request.getPassword().isBlank()) {
            throw new IllegalArgumentException("Username and password are required");
        }
        if (!request.getPassword().equals(request.getConfirmpassword())) {
            throw new IllegalArgumentException("Passwords do not match");
        }
        if (playerRepository.existsByUsername(request.getUsername())) {
            throw new UsernameAlreadyTakenException(request.getUsername());
        }

        Player player = new Player();
        player.setUsername(request.getUsername());
        player.setName(request.getUsername());
        player.setPassword(passwordEncoder.encode(request.getPassword()));
        Player savedPlayer = playerRepository.save(player);

        Money wallet = new Money();
        wallet.setPlayerId(savedPlayer.getId());
        PlayerItem invent = new PlayerItem(savedPlayer.getId(),13703,ItemCategory.VEHICLE,1,-1,true,0);
        var itemDefinition = inventoryRules.definitionFor(13703);
        invent.setVehicleCategory(itemDefinition.vehicleCategory());
        invent.setOutfitCategory(itemDefinition.outfitCategory());
        invent.setRuneCategory(itemDefinition.runeCategory());
        invent.setEffectCategory(itemDefinition.effectCategory());
        invent.setUtilityCategory(itemDefinition.utilityCategory());
        invent.setPetCategory(itemDefinition.petCategory());
        savedPlayer.setSelectedPlayerItemId(invent.getId());
        moneyRepository.save(wallet);
        inventoryRepository.save(invent);
        return savedPlayer;
    }

    public PlayerInfoDTO getPlayerById(Long id) {
        Player player = playerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Player not found: " + id));
        return new PlayerInfoDTO(id,player.getName()
                                                ,player.getLevel()
                                                ,player.isOnline()
                                                ,player.isVip());
    }

    @Transactional(readOnly = true)
    public PlayerMeDTO getCurrentPlayer(String username) {
        Player player = playerRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Player not found"));
        Money wallet = moneyRepository.findByPlayerId(player.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Player wallet not found"));
        return new PlayerMeDTO(player.getId(), player.getUsername(), player.getName(),
                player.getLevel(), player.isVip(), player.isOnline(),
                new PlayerMeDTO.WalletDTO(wallet.getCoupon(), wallet.getZcoin(),
                        wallet.getGold(), wallet.getLuckyPoint()));
    }

    public boolean existByUsername(String username) {
        return playerRepository.existsByUsername(username);
    }
}

package com.game.speed.player;

import com.game.speed.auth.security.JwtAuthenticationFilter;
import com.game.speed.auth.security.JwtTokenProvider;
import com.game.speed.auth.security.SecurityConfig;
import com.game.speed.auth.service.TokenRevocationService;
import com.game.speed.money.entity.Money;
import com.game.speed.inventory.repository.InventoryRepository;
import com.game.speed.money.repository.MoneyRepository;
import com.game.speed.player.controller.PlayerController;
import com.game.speed.player.entity.Player;
import com.game.speed.player.repository.PlayerRepository;
import com.game.speed.player.service.PlayerService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PlayerController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, PlayerService.class})
class PlayerMeTests {
    @Autowired MockMvc mvc;
    @MockitoBean PlayerRepository players;
    @MockitoBean MoneyRepository wallets;
    @MockitoBean InventoryRepository inventory;
    @MockitoBean JwtTokenProvider tokens;
    @MockitoBean TokenRevocationService revocations;
    @MockitoBean UserDetailsService userDetails;

    @Test
    void returnsAuthenticatedPlayersProfileAndWalletIgnoringSuppliedId() throws Exception {
        Player player = new Player();
        player.setId(7L);
        player.setUsername("racer");
        player.setName("Speed Racer");
        player.setPassword("must-not-be-exposed");
        Money wallet = new Money();
        wallet.setPlayerId(7L);
        wallet.setGold(100);
        wallet.setCoupon(20);
        wallet.setZcoin(30);
        wallet.setLuckyPoint(40);
        when(players.findByUsername("racer")).thenReturn(Optional.of(player));
        when(wallets.findByPlayerId(7L)).thenReturn(Optional.of(wallet));

        mvc.perform(get("/api/players/me").param("id", "999").with(user("racer")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(7))
                .andExpect(jsonPath("$.username").value("racer"))
                .andExpect(jsonPath("$.name").value("Speed Racer"))
                .andExpect(jsonPath("$.level").value(1))
                .andExpect(jsonPath("$.vip").value(false))
                .andExpect(jsonPath("$.online").value(false))
                .andExpect(jsonPath("$.wallet.gold").value(100))
                .andExpect(jsonPath("$.wallet.coupon").value(20))
                .andExpect(jsonPath("$.wallet.zcoin").value(30))
                .andExpect(jsonPath("$.wallet.luckyPoint").value(40))
                .andExpect(jsonPath("$.password").doesNotExist());
        verify(players, never()).findById(anyLong());
        verify(wallets).findByPlayerId(7L);
    }

    @Test
    void rejectsUnauthenticatedRequests() throws Exception {
        mvc.perform(get("/api/players/me")).andExpect(status().isForbidden());
        verifyNoInteractions(players, wallets);
    }

    @Test
    void returnsNotFoundForMissingPlayer() throws Exception {
        when(players.findByUsername("missing")).thenReturn(Optional.empty());
        mvc.perform(get("/api/players/me").with(user("missing")))
                .andExpect(status().isNotFound());
        verifyNoInteractions(wallets);
    }

    @Test
    void returnsNotFoundInsteadOfInventingBalanceForMissingWallet() throws Exception {
        Player player = new Player();
        player.setId(7L);
        when(players.findByUsername("racer")).thenReturn(Optional.of(player));
        when(wallets.findByPlayerId(7L)).thenReturn(Optional.empty());
        mvc.perform(get("/api/players/me").with(user("racer")))
                .andExpect(status().isNotFound());
        verify(wallets, never()).save(any());
    }
}

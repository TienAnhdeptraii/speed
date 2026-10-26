package com.game.speed.inventory;

import com.game.speed.auth.security.JwtAuthenticationFilter;
import com.game.speed.auth.security.JwtTokenProvider;
import com.game.speed.auth.security.SecurityConfig;
import com.game.speed.auth.service.TokenRevocationService;
import com.game.speed.inventory.controller.InventoryController;
import com.game.speed.inventory.dto.PlayerItemDTO;
import com.game.speed.inventory.service.InventoryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import java.util.List;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(InventoryController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class InventoryControllerTests {
    @Autowired MockMvc mvc;
    @MockitoBean InventoryService inventory;
    @MockitoBean JwtTokenProvider tokens;
    @MockitoBean TokenRevocationService revocations;
    @MockitoBean UserDetailsService userDetails;

    @Test
    void inventoryUsesAuthenticatedUsername() throws Exception {
        when(inventory.getInventory("racer")).thenReturn(List.of());
        mvc.perform(get("/api/players/me/inventory").param("playerId", "999").with(user("racer")))
                .andExpect(status().isOk()).andExpect(content().json("[]"));
        verify(inventory).getInventory("racer");
    }

    @Test
    void selectingCarUsesOwnershipRecordAndAuthenticatedUsername() throws Exception {
        when(inventory.selectCar("racer", 42L))
                .thenReturn(new PlayerItemDTO(42L, 13703, 1, -1, true, 0, false, true));
        mvc.perform(put("/api/players/me/selected-car").with(user("racer"))
                .contentType(MediaType.APPLICATION_JSON).content("{\"playerItemId\":42}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.itemId").value(13703))
                .andExpect(jsonPath("$.avaIPeriod").value(-1))
                .andExpect(jsonPath("$.status").value(true))
                .andExpect(jsonPath("$.obtainTime").value(0))
                .andExpect(jsonPath("$.expiresAt").doesNotExist())
                .andExpect(jsonPath("$.selected").value(true));
        verify(inventory).selectCar("racer", 42L);
    }

    @Test
    void validatesSelectionRequest() throws Exception {
        for (String body : List.of("{}", "{\"playerItemId\":0}", "{\"playerItemId\":-1}")) {
            mvc.perform(put("/api/players/me/selected-car").with(user("racer"))
                    .contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isBadRequest());
        }
        verifyNoInteractions(inventory);
    }

    @Test
    void allRoutesRequireAuthentication() throws Exception {
        mvc.perform(get("/api/players/me/inventory")).andExpect(status().isForbidden());
        mvc.perform(get("/api/players/me/selected-car")).andExpect(status().isForbidden());
        mvc.perform(put("/api/players/me/selected-car").contentType(MediaType.APPLICATION_JSON)
                .content("{\"playerItemId\":42}")).andExpect(status().isForbidden());
        verifyNoInteractions(inventory);
    }
}

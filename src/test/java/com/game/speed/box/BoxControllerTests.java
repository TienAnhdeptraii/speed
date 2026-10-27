package com.game.speed.box;

import com.game.speed.auth.security.*;
import com.game.speed.auth.service.TokenRevocationService;
import com.game.speed.box.controller.BoxController;
import com.game.speed.box.service.BoxService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import java.util.UUID;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(BoxController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class BoxControllerTests {
    @Autowired MockMvc mvc;
    @MockitoBean BoxService boxes;
    @MockitoBean JwtTokenProvider tokens;
    @MockitoBean TokenRevocationService revocations;
    @MockitoBean UserDetailsService users;

    @Test
    void usesAuthenticatedPlayer() throws Exception {
        String key = "5b4273a9-e03d-4196-ae1b-51c282647e20";
        mvc.perform(post("/api/players/me/boxes/open").with(user("racer"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"playerItemId\":42,\"requestId\":\"" + key + "\",\"playerId\":999}"))
                .andExpect(status().isOk());
        verify(boxes).open("racer", 42L, UUID.fromString(key));
    }

    @Test
    void rejectsAnonymousAndMalformedRequests() throws Exception {
        mvc.perform(post("/api/players/me/boxes/open").contentType(MediaType.APPLICATION_JSON)
                .content("{}")).andExpect(status().isForbidden());
        for (String body : new String[]{"{}", "{\"playerItemId\":0,\"requestId\":null}",
                "{\"playerItemId\":42,\"requestId\":\"bad-key\"}"}) {
            mvc.perform(post("/api/players/me/boxes/open").with(user("racer"))
                    .contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isBadRequest());
        }
        verifyNoInteractions(boxes);
    }
}

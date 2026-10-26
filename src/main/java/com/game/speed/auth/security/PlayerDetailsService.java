package com.game.speed.auth.security;

import com.game.speed.player.repository.PlayerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PlayerDetailsService implements UserDetailsService {
    private final PlayerRepository playerRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return playerRepository.findByUsername(username)
                .map(player -> User.withUsername(player.getUsername())
                        .password(player.getPassword())
                        .roles("PLAYER")
                        .build())
                .orElseThrow(() -> new UsernameNotFoundException("Player not found"));
    }
}

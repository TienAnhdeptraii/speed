package com.game.speed.auth.dto;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoginPlayerDTO {
    @NotBlank
    @Size(max = 16)
    String username;
    @NotBlank
    @Size(max = 16)
    String password;
}

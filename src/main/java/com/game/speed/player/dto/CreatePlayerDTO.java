package com.game.speed.player.dto;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreatePlayerDTO {
    @NotBlank
    @Size(max =  16)
    String username;
    @NotBlank
    @Size(max =  16)
    String password;
    @NotBlank
    @Size(max =  16)
    String confirmpassword;
}

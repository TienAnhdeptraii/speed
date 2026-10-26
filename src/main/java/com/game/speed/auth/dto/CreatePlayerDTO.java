package com.game.speed.auth.dto;


import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreatePlayerDTO {
    String username;
    String password;
    String confirmpassword;
}

package com.game.speed.money.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "player_money")
@Getter
@Setter
public class Money {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Liên kết mềm đến ID của người chơi.
    // Thuộc tính unique = true đảm bảo quan hệ 1-1 (Mỗi người chơi chỉ có duy nhất 1 ví tiền)
    @Column(name = "player_id", nullable = false, unique = true)
    private Long playerId;

    @Column(name = "coupon")
    private int coupon = 0; // Thường dùng cho tiền cày chay =)) mà auto 9999 r

    @Column(name = "zcoin")
    private int zcoin = 0;  // Tiền tệ đặc biệt trong game (Nạp)

    @Column(name = "gold")
    private int gold = 0;   // Tiền cày cuốc cơ bản

    @Column(name = "lucky_point")
    private int luckyPoint = 0; // Điểm may mắn (dùng cho vòng quay/gacha)

    // Khai báo Getters và Setters (hoặc dùng @Data / @Getter @Setter của Lombok)
}
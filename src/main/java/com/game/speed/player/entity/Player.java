    package com.game.speed.player.entity;

    import jakarta.persistence.*;
    import lombok.Getter;
    import lombok.Setter;

    @Entity
    @Table(name = "player")
    @Getter
    @Setter
    public class Player {
        @Column(name = "selected_player_item_id")
        private Long selectedPlayerItemId;

        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id; // Đóng vai trò là player_id (Khóa chính)

        @Column(name = "username", nullable = false, unique = true, updatable = false, length = 50)
        private String username;

        @Column(name = "password")
        private String password;

        @Column(name = "name", nullable = false, length = 50)
        private String name; // Tên nhân vật trong game

        @Column(name = "level")
        private int level = 1; // Mặc định level 1 khi mới tạo

        @Column(name = "is_vip")
        private boolean isVip = false; // Trạng thái VIP (true/false)

        @Column(name = "is_online")
        private boolean isOnline = false; // Trạng thái đang online/offline

        // Có thể cân nhắc dùng @Getter, @Setter của thư viện Lombok để code ngắn gọn hơn
    }

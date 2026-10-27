package com.game.speed.player.repository;

import com.game.speed.player.entity.Player;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.*;
import java.util.Optional;

@Repository
public interface PlayerRepository extends JpaRepository<Player, Long> {
    @Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Player p where p.id = :id")
    Optional<Player> findByIdForUpdate(@Param("id") Long id);
    @Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Player p where p.username = :username")
    Optional<Player> findByUsernameForUpdate(@Param("username") String username);
    // Spring Data JPA đã tự động cung cấp sẵn các hàm cơ bản như save(), findById(), findAll(), deleteById()
    // Lấy thông tin người chơi theo ID
    boolean existsByUsername(String username);
    Optional<Player> findByUsername(String username);
}

package com.game.speed.money.repository;

import com.game.speed.money.entity.Money;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface MoneyRepository extends JpaRepository<Money,Long> {
    Optional<Money> findByPlayerId(Long playerId);
}

package com.game.speed.box.repository;

import com.game.speed.box.entity.BoxOpening;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import java.util.Optional;

public interface BoxOpeningRepository extends JpaRepository<BoxOpening, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<BoxOpening> findByPlayerIdAndRequestId(Long playerId, String requestId);
}

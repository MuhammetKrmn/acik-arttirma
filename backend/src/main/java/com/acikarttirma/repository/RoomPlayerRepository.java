package com.acikarttirma.repository;

import com.acikarttirma.model.RoomPlayer;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoomPlayerRepository extends JpaRepository<RoomPlayer, Long> {

    /** Odadaki oyuncular koltuk sırasıyla (1, 2, 3). İhaleyi sırayla bu oyuncular açar. */
    List<RoomPlayer> findByRoom_IdOrderBySeatNumber(Long roomId);

    Optional<RoomPlayer> findByRoom_IdAndUser_Id(Long roomId, Long userId);

    long countByRoom_Id(Long roomId);
}
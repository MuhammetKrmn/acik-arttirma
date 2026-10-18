package com.acikarttirma.repository;

import com.acikarttirma.model.UserSquad;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserSquadRepository extends JpaRepository<UserSquad, Long> {

    Optional<UserSquad> findByRoomPlayer_Id(Long roomPlayerId);
}
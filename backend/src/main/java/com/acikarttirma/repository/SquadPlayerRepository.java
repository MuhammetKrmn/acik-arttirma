package com.acikarttirma.repository;

import com.acikarttirma.model.SquadPlayer;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SquadPlayerRepository extends JpaRepository<SquadPlayer, Long> {

    List<SquadPlayer> findBySquad_Id(Long squadId);
}
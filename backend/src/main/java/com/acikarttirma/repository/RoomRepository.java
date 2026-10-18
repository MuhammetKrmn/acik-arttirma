package com.acikarttirma.repository;

import com.acikarttirma.model.Room;
import com.acikarttirma.model.enums.RoomStatus;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoomRepository extends JpaRepository<Room, Long> {

    List<Room> findByStatus(RoomStatus status);
}
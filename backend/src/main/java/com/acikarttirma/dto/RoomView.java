package com.acikarttirma.dto;

import com.acikarttirma.model.enums.RoomStatus;
import java.util.List;

/** Odanın herkese açık görünümü. Kart, sıra ve reyting bilgisi içermez. */
public record RoomView(
        Long roomId,
        RoomStatus status,
        String conceptName,
        int startingBudget,
        List<PlayerView> players) {

    public record PlayerView(Long roomPlayerId, Long userId, int seatNumber, String username, int budget) {
    }
}
package com.acikarttirma.dto;

import com.acikarttirma.model.enums.RoomStatus;
import java.util.List;

/**
 * Lobinin anlık durumu: kim odada, kim hazır, oyun ne zaman kendiliğinden başlayacak.
 * countdownDeadlineMs oda dolana kadar null'dur. Kalan süre = countdownDeadlineMs - serverNowMs.
 */
public record LobbyView(
        Long roomId,
        RoomStatus status,
        String conceptName,
        int startingBudget,
        List<LobbyPlayer> players,
        Long countdownDeadlineMs,
        long serverNowMs) {

    public record LobbyPlayer(Long roomPlayerId, Long userId, int seatNumber, String username, boolean ready) {
    }
}
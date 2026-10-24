package com.acikarttirma.dto;

import com.acikarttirma.model.enums.AcquisitionType;
import com.acikarttirma.model.enums.Position;
import com.acikarttirma.model.enums.Slot;
import java.util.List;

/**
 * Oyun sonu sonucu. Reytingler oyun bittiği için artık görünür; bu kayıt sadece
 * oda FINISHED olunca üretilir.
 */
public record GameResultView(
        Long roomId,
        boolean draw,
        List<Long> winnerRoomPlayerIds,
        List<PlayerResult> players) {

    public record PlayerResult(Long roomPlayerId, Long userId, int seatNumber, String username,
                               int totalRating, int remainingBudget, int totalSpent, boolean winner,
                               List<SquadEntry> squad) {
    }

    public record SquadEntry(Slot slot, String playerName, String nationality, String photoUrl, String club,
                             Position position, int overallRating, int paidPrice,
                             AcquisitionType acquisitionType) {
    }
}
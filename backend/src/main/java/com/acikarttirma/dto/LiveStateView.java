package com.acikarttirma.dto;

import com.acikarttirma.model.enums.Slot;
import com.acikarttirma.model.enums.Tier;
import java.util.List;

/** Canlı oyunun anlık görüntüsü: geç bağlanan veya kopan istemci kendini buradan senkronlar. */
public record LiveStateView(
        boolean finished,
        int processedAuctions,
        int totalAuctions,
        CurrentAuction current,
        List<PlayerBudget> players) {

    public record CurrentAuction(int sequenceNo, Slot slot, Tier tier, AuctionPayloads.CardInfo card,
                                 int price, Long leaderRoomPlayerId, long deadlineMs, long serverNowMs) {
    }

    public record PlayerBudget(Long roomPlayerId, Long userId, int seatNumber, String username,
                               int budget, boolean connected, List<Slot> filledSlots) {
    }
}
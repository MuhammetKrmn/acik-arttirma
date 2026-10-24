package com.acikarttirma.dto;

import com.acikarttirma.model.enums.AcquisitionType;
import com.acikarttirma.model.enums.Position;
import com.acikarttirma.model.enums.Slot;
import com.acikarttirma.model.enums.Tier;
import java.util.List;
import java.util.Map;

/**
 * Olayların içeriği. Hiçbirinde reyting veya fiziksel değerler (pac, sho...) yoktur,
 * bunlar oyun sonuna kadar gizlidir.
 */
public final class AuctionPayloads {

    private AuctionPayloads() {
    }

    /** İhaleye çıkan kartın herkese açık bilgisi. */
    public record CardInfo(Long cardId, String playerName, String nationality, String photoUrl,
                           String club, Position position, int startingPrice) {
    }

    public record GameStarted(int totalAuctions) {
    }

    public record AuctionStarted(int sequenceNo, int totalAuctions, Slot slot, Tier tier, CardInfo card,
                                 int price, Long openerRoomPlayerId, long deadlineMs, long serverNowMs) {
    }

    public record BidAccepted(int sequenceNo, int price, Long leaderRoomPlayerId, int increment,
                              long deadlineMs, long serverNowMs) {
    }

    /** Sadece teklifi veren oyuncuya gider. Güncel fiyat ve lider de içindedir (kendini senkronlasın). */
    public record BidRejected(String reason, int price, Long leaderRoomPlayerId) {
    }

    public record PlayerWithdrew(Long roomPlayerId) {
    }

    public record WithdrawRejected(String reason) {
    }

    public record AuctionClosed(int sequenceNo, Slot slot, Long winnerRoomPlayerId, int price,
                                Map<Long, Integer> budgets) {
    }

    public record AuctionSkipped(int sequenceNo, Slot slot, String reason) {
    }

    public record AuctionsFinished(Map<Long, Integer> budgets) {
    }

    /** player_disconnected / player_reconnected olaylarının içeriği. */
    public record PlayerConnection(Long roomPlayerId) {
    }

    /** Tier 3 aşamasında bir oyuncunun bir slotuna ne verildi (T3 kartı veya U19), kaça. */
    public record Tier3Entry(Long roomPlayerId, Slot slot, AcquisitionType type, int price, CardInfo card) {
    }

    public record Tier3Assigned(List<Tier3Entry> entries, Map<Long, Integer> budgets) {
    }
}
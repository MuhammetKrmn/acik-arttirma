package com.acikarttirma.service;

import com.acikarttirma.model.enums.Position;
import com.acikarttirma.model.enums.Slot;
import com.acikarttirma.model.enums.Tier;
import java.util.List;

/** Oyun başlarken DB'den bir kez okunan, düz (entity olmayan) görüntü. */
public record GameSnapshot(Long roomId, List<PlayerSnapshot> players, List<ItemSnapshot> items) {

    public record PlayerSnapshot(Long roomPlayerId, Long userId, int seat, String username, int budget) {
    }

    /** İhaleye çıkacak bir kart (tier 1 ve 2). sequenceNo ihale sırasıdır. */
    public record ItemSnapshot(Long itemId, int sequenceNo, Slot slot, Tier tier, Long cardId,
                               String playerName, String nationality, String photoUrl, String club,
                               Position position, int startingPrice) {
    }
}
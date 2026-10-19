package com.acikarttirma.engine;

import com.acikarttirma.model.enums.Slot;

/** Kapanan ihalenin sonucu: kim, hangi slotu, kaça aldı. */
public record AuctionOutcome(long winnerId, int price, Slot slot) {
}
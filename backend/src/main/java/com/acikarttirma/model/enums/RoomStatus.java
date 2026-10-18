package com.acikarttirma.model.enums;

/** Oda durum makinesi: LOBBY -> SETUP -> AUCTION_ACTIVE -> AUCTION_CLOSED -> TIER3_ASSIGN -> (sonraki ihale) -> FINISHED */
public enum RoomStatus {
    LOBBY, SETUP, AUCTION_ACTIVE, AUCTION_CLOSED, TIER3_ASSIGN, FINISHED
}
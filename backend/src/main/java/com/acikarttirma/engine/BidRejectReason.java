package com.acikarttirma.engine;

/** Bir teklifin neden reddedildiğini anlatır. */
public enum BidRejectReason {
    AUCTION_CLOSED,
    UNKNOWN_PLAYER,
    PLAYER_WITHDRAWN,
    PLAYER_DISCONNECTED,
    SLOT_FULL,
    INVALID_INCREMENT,
    STALE_PRICE,
    ALREADY_LEADER,
    INSUFFICIENT_BUDGET
}
package com.acikarttirma.engine;

/** Geri çekilme isteğinin neden reddedildiğini anlatır. */
public enum WithdrawRejectReason {
    AUCTION_CLOSED,
    UNKNOWN_PLAYER,
    LEADER_CANNOT_WITHDRAW,
    ALREADY_WITHDRAWN
}
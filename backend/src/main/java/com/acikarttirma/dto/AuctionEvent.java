package com.acikarttirma.dto;

/** WebSocket'ten giden her olayın zarfı: { "type": "...", "data": { ... } }. */
public record AuctionEvent(String type, Object data) {

    public static final String GAME_STARTED = "game_started";
    public static final String AUCTION_STARTED = "auction_started";
    public static final String BID_ACCEPTED = "bid_accepted";
    public static final String BID_REJECTED = "bid_rejected";
    public static final String PLAYER_WITHDREW = "player_withdrew";
    public static final String WITHDRAW_REJECTED = "withdraw_rejected";
    public static final String AUCTION_CLOSED = "auction_closed";
    public static final String AUCTION_SKIPPED = "auction_skipped";
    public static final String AUCTIONS_FINISHED = "auctions_finished";
    public static final String PLAYER_DISCONNECTED = "player_disconnected";
    public static final String PLAYER_RECONNECTED = "player_reconnected";
    public static final String TIER3_ASSIGNED = "tier3_assigned";
    public static final String GAME_FINISHED = "game_finished";

    public static AuctionEvent of(String type, Object data) {
        return new AuctionEvent(type, data);
    }
}
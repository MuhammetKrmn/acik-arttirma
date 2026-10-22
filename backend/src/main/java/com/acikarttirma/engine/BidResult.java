package com.acikarttirma.engine;

/**
 * Bir teklifin sonucu. Kabul de red de ihalenin GÜNCEL fiyatını ve liderini taşır;
 * böylece reddedilen istemci kendini hemen senkronlayabilir.
 */
public record BidResult(boolean accepted, BidRejectReason reason, int price, long leaderId) {

    public static BidResult accepted(int price, long leaderId) {
        return new BidResult(true, null, price, leaderId);
    }

    public static BidResult rejected(BidRejectReason reason, int price, long leaderId) {
        return new BidResult(false, reason, price, leaderId);
    }
}
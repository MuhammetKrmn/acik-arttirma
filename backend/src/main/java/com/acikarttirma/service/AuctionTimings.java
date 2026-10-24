package com.acikarttirma.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** İhale süreleri (ms). Testlerde kısaltmak için application ayarlarından değiştirilebilir. */
@Component
public class AuctionTimings {

    private final long firstDelayMs;
    private final long nextDelayMs;
    private final long bidTimeoutMs;

    public AuctionTimings(@Value("${auction.first-delay-ms:5000}") long firstDelayMs,
                          @Value("${auction.next-delay-ms:3000}") long nextDelayMs,
                          @Value("${auction.bid-timeout-ms:10000}") long bidTimeoutMs) {
        this.firstDelayMs = firstDelayMs;
        this.nextDelayMs = nextDelayMs;
        this.bidTimeoutMs = bidTimeoutMs;
    }

    /** Oyun başlayınca ilk ihaleye kadar bekleme (istemciler abone olsun). */
    public long getFirstDelayMs() { return firstDelayMs; }

    /** Bir ihale kapanınca sonrakinin açılmasına kadar bekleme. */
    public long getNextDelayMs() { return nextDelayMs; }

    /** Son kabul edilen tekliften sonra ihalenin kapanmasına kadar süre. */
    public long getBidTimeoutMs() { return bidTimeoutMs; }
}
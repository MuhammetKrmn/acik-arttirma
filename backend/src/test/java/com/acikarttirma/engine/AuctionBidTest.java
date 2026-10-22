package com.acikarttirma.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.acikarttirma.model.enums.Slot;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Adım 2: teklif kuralları. Spring yok, DB yok. */
class AuctionBidTest {

    private static final Slot SLOT = Slot.ST;

    private PlayerState p1 = new PlayerState(1, 1000);
    private PlayerState p2 = new PlayerState(2, 1000);
    private PlayerState p3 = new PlayerState(3, 1000);

    /** 1 numaralı oyuncu 30'dan açar. */
    private Auction auction() {
        return new Auction(SLOT, 30, List.of(p1, p2, p3), 1);
    }

    @Test
    void acanOyuncuAcilisFiyatiylaLiderOlur() {
        Auction a = auction();
        assertEquals(30, a.getCurrentPrice());
        assertEquals(1, a.getLeaderId());
        assertEquals(AuctionPhase.ACTIVE, a.getPhase());
    }

    @Test
    void gecerliTeklifKabulEdilirVeLiderDegisir() {
        Auction a = auction();
        BidResult r = a.placeBid(2, 30, 5);
        assertTrue(r.accepted());
        assertEquals(35, r.price());
        assertEquals(2, r.leaderId());
        assertEquals(35, a.getCurrentPrice());
        assertEquals(2, a.getLeaderId());
    }

    @Test
    void uc_artisinDaUcuKabulEdilir() {
        Auction a = auction();
        assertTrue(a.placeBid(2, 30, 1).accepted());   // 31
        assertTrue(a.placeBid(3, 31, 5).accepted());   // 36
        assertTrue(a.placeBid(1, 36, 10).accepted());  // 46
        assertEquals(46, a.getCurrentPrice());
    }

    @Test
    void gecersizArtisReddedilir() {
        Auction a = auction();
        for (int bad : new int[] {0, 2, 3, 7, 20, -5}) {
            BidResult r = a.placeBid(2, 30, bad);
            assertFalse(r.accepted(), "artış " + bad + " reddedilmeli");
            assertEquals(BidRejectReason.INVALID_INCREMENT, r.reason());
        }
        assertEquals(30, a.getCurrentPrice());
    }

    @Test
    void eskiFiyatliTeklifReddedilirVeGuncelDurumDoner() {
        Auction a = auction();
        a.placeBid(2, 30, 5); // fiyat 35, lider 2
        BidResult r = a.placeBid(3, 30, 5); // 3 numara eski fiyatı gördü
        assertFalse(r.accepted());
        assertEquals(BidRejectReason.STALE_PRICE, r.reason());
        assertEquals(35, r.price());
        assertEquals(2, r.leaderId());
    }

    @Test
    void liderKendiUstuneTeklifVeremez() {
        Auction a = auction();
        BidResult r = a.placeBid(1, 30, 1);
        assertFalse(r.accepted());
        assertEquals(BidRejectReason.ALREADY_LEADER, r.reason());
    }

    @Test
    void fiyatArtisBudgeti_asamaz_ama_tamBudgetYeter() {
        PlayerState poor = new PlayerState(2, 36);
        Auction a = new Auction(SLOT, 30, List.of(p1, poor, p3), 1);

        BidResult tooMuch = a.placeBid(2, 30, 10); // 40 > 36
        assertFalse(tooMuch.accepted());
        assertEquals(BidRejectReason.INSUFFICIENT_BUDGET, tooMuch.reason());

        assertTrue(a.placeBid(2, 30, 5).accepted());  // 35 <= 36
        a.placeBid(3, 35, 1);                           // 36, lider 3
        assertTrue(a.placeBid(1, 36, 1).accepted());   // 37 (1'in bütçesi yeterli)
        BidResult exact = a.placeBid(2, 37, 1);        // 38 > 36
        assertFalse(exact.accepted());
    }

    @Test
    void tamBudgetKadarTeklifKabulEdilir() {
        PlayerState p = new PlayerState(2, 35);
        Auction a = new Auction(SLOT, 30, List.of(p1, p, p3), 1);
        assertTrue(a.placeBid(2, 30, 5).accepted()); // 35 == bütçe
    }

    @Test
    void slotuDolanOyuncuTeklifVeremez() {
        p2.fillSlot(SLOT);
        BidResult r = auction().placeBid(2, 30, 1);
        assertFalse(r.accepted());
        assertEquals(BidRejectReason.SLOT_FULL, r.reason());
    }

    @Test
    void baglantisiKopanOyuncuTeklifVeremez() {
        p2.setConnected(false);
        BidResult r = auction().placeBid(2, 30, 1);
        assertFalse(r.accepted());
        assertEquals(BidRejectReason.PLAYER_DISCONNECTED, r.reason());
    }

    @Test
    void ihaledeOlmayanOyuncuReddedilir() {
        BidResult r = auction().placeBid(99, 30, 1);
        assertFalse(r.accepted());
        assertEquals(BidRejectReason.UNKNOWN_PLAYER, r.reason());
    }

    @Test
    void acanOyuncuSlotuDoluysaIhaleAcilamaz() {
        p1.fillSlot(SLOT);
        assertThrows(IllegalArgumentException.class, this::auction);
    }

    @Test
    void acanOyuncununButcesiAcilisaYetmiyorsaIhaleAcilamaz() {
        PlayerState poor = new PlayerState(1, 20);
        assertThrows(IllegalArgumentException.class,
                () -> new Auction(SLOT, 30, List.of(poor, p2, p3), 1));
    }
}
package com.acikarttirma.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.acikarttirma.model.enums.Slot;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Adım 3: geri çekilme, ihalenin bitmesi, kopma. */
class AuctionCloseTest {

    private static final Slot SLOT = Slot.ST;

    private PlayerState p1 = new PlayerState(1, 1000);
    private PlayerState p2 = new PlayerState(2, 1000);
    private PlayerState p3 = new PlayerState(3, 1000);

    private Auction auction() {
        return new Auction(SLOT, 30, List.of(p1, p2, p3), 1);
    }

    @Test
    void liderCekilemez() {
        WithdrawResult r = auction().withdraw(1);
        assertFalse(r.accepted());
        assertEquals(WithdrawRejectReason.LEADER_CANNOT_WITHDRAW, r.reason());
    }

    @Test
    void geridekalanOyuncuCekilebilir() {
        Auction a = auction();
        a.placeBid(2, 30, 5); // lider 2, 1 geride kaldı
        assertTrue(a.withdraw(1).accepted());
    }

    @Test
    void cekilenOyuncuBirDahaTeklifVeremez() {
        Auction a = auction();
        assertTrue(a.withdraw(2).accepted());
        BidResult r = a.placeBid(2, 30, 1);
        assertFalse(r.accepted());
        assertEquals(BidRejectReason.PLAYER_WITHDRAWN, r.reason());
    }

    @Test
    void ikinciKezCekilmekReddedilir() {
        Auction a = auction();
        a.withdraw(2);
        WithdrawResult r = a.withdraw(2);
        assertFalse(r.accepted());
        assertEquals(WithdrawRejectReason.ALREADY_WITHDRAWN, r.reason());
    }

    @Test
    void ihaledeOlmayanCekilemez() {
        WithdrawResult r = auction().withdraw(99);
        assertFalse(r.accepted());
        assertEquals(WithdrawRejectReason.UNKNOWN_PLAYER, r.reason());
    }

    @Test
    void herkesCekilinceIhaleBeklemedenBiter() {
        Auction a = auction();
        assertFalse(a.isDecided());
        a.withdraw(2);
        assertFalse(a.isDecided()); // 3 hâlâ yarışabilir
        a.withdraw(3);
        assertTrue(a.isDecided());
    }

    @Test
    void tekAktifOyuncuAcilisFiyatiylaKartiAlir() {
        p2.fillSlot(SLOT);
        p3.fillSlot(SLOT);
        Auction a = auction();
        assertTrue(a.isDecided());
        AuctionOutcome o = a.close();
        assertEquals(1, o.winnerId());
        assertEquals(30, o.price());
        assertEquals(970, p1.getBudget());
    }

    @Test
    void butcesiFiyatinUstunuAsmayanOyuncuYarisamaz() {
        PlayerState poor = new PlayerState(2, 30); // 31 veremez
        Auction a = new Auction(SLOT, 30, List.of(p1, poor, p3), 1);
        assertFalse(a.isDecided());
        a.withdraw(3);
        assertTrue(a.isDecided());
    }

    @Test
    void fiyatYukselinceButcesiYetmeyenYarisiDisindaKalir() {
        PlayerState p2b = new PlayerState(2, 40);
        Auction a = new Auction(SLOT, 30, List.of(p1, p2b, p3), 1);
        assertTrue(a.placeBid(3, 30, 10).accepted()); // fiyat 40, lider 3
        assertFalse(a.isDecided());                   // 1 hâlâ yarışabilir
        a.withdraw(1);
        assertTrue(a.isDecided());                    // 2'nin bütçesi 41'e yetmez
    }

    @Test
    void kapanincaKazananOderVeSlotuDolar() {
        Auction a = auction();
        a.placeBid(2, 30, 10); // 40
        AuctionOutcome o = a.close();
        assertEquals(2, o.winnerId());
        assertEquals(40, o.price());
        assertEquals(SLOT, o.slot());
        assertEquals(960, p2.getBudget());
        assertTrue(p2.hasFilled(SLOT));
        assertEquals(1000, p1.getBudget());
        assertEquals(AuctionPhase.CLOSED, a.getPhase());
    }

    @Test
    void kapaliIhaledeIslemYapilamazVeTekrarKapatilamaz() {
        Auction a = auction();
        a.close();
        assertEquals(BidRejectReason.AUCTION_CLOSED, a.placeBid(2, 30, 1).reason());
        assertEquals(WithdrawRejectReason.AUCTION_CLOSED, a.withdraw(2).reason());
        assertThrows(IllegalStateException.class, a::close);
    }

    @Test
    void kopanOyuncuCekilmisSayilirVeDonseDeGiremez() {
        Auction a = auction();
        a.onPlayerDisconnected(2);
        assertFalse(a.placeBid(2, 30, 1).accepted());
        p2.setConnected(true); // geri geldi
        assertEquals(BidRejectReason.PLAYER_WITHDRAWN, a.placeBid(2, 30, 1).reason());
    }

    @Test
    void liderKopsaTeklifiGecerliKalir() {
        Auction a = auction();
        a.onPlayerDisconnected(1);
        assertEquals(1, a.getLeaderId());
        assertFalse(a.isDecided()); // 2 ve 3 hâlâ yarışabilir
        a.withdraw(2);
        a.withdraw(3);
        assertTrue(a.isDecided());
        AuctionOutcome o = a.close();
        assertEquals(1, o.winnerId());
        assertEquals(970, p1.getBudget());
    }

    @Test
    void herkesKopuncaKalanLiderKazanir() {
        Auction a = auction();
        a.onPlayerDisconnected(2);
        a.onPlayerDisconnected(3);
        assertTrue(a.isDecided());
    }
}
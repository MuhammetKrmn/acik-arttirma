package com.acikarttirma.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.acikarttirma.dto.AuctionEvent;
import com.acikarttirma.dto.GuestResponse;
import com.acikarttirma.dto.LobbyView;
import com.acikarttirma.dto.RoomView;
import com.acikarttirma.exception.ApiException;
import com.acikarttirma.model.enums.RoomStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

/**
 * F8: lobi. Hızlı eşleşme üç kişiyi aynı odaya toplar, oda dolunca sayaç başlar.
 * Herkes hazır olunca oyun hemen, kimse hazır olmasa da sayaç bitince kendiliğinden başlar.
 * Lobi sayacı kısaltılmıştır (2,5 sn).
 */
@SpringBootTest(properties = {
        "auction.first-delay-ms=100",
        "auction.next-delay-ms=100",
        "auction.bid-timeout-ms=1500",
        "lobby.countdown-ms=2500"
})
@ActiveProfiles("dev")
@Import(FlowTestSupport.RecorderConfig.class)
class LobbyFlowTest extends FlowTestSupport {

    @Autowired
    private LobbyService lobbyService;

    @BeforeEach
    void eskiLobileriKapat() {
        // Elle yapılan denemelerden kalan açık odalar hızlı eşleşmeye karışmasın.
        jdbc.update("UPDATE rooms SET created_at = created_at - interval '1 day' WHERE status = 'LOBBY'");
    }

    private RoomView eslestir(GuestResponse guest) {
        RoomView room = lobbyService.quickJoin(guest.playerToken());
        if (!createdRooms.contains(room.roomId())) {
            createdRooms.add(room.roomId());
        }
        return room;
    }

    @Test
    void hizliEslesmeUcKisiyiAyniOdayaToplarSayacBitinceOyunBaslar() throws Exception {
        GuestResponse a = yeniMisafir();
        GuestResponse b = yeniMisafir();
        GuestResponse c = yeniMisafir();

        RoomView r1 = eslestir(a);
        RoomView r2 = eslestir(b);
        RoomView r3 = eslestir(c);
        Long roomId = r1.roomId();

        assertEquals(roomId, r2.roomId());
        assertEquals(roomId, r3.roomId());
        assertEquals(RoomStatus.SETUP, r3.status());
        assertEquals(3, r3.players().size());

        // Aynı kişi tekrar basarsa yeni oda açılmaz, beklediği oda döner.
        assertEquals(roomId, lobbyService.quickJoin(a.playerToken()).roomId());

        LobbyView lobby = lobbyService.getLobby(roomId);
        assertNotNull(lobby.countdownDeadlineMs(), "oda dolunca sayaç başlamalı");
        assertTrue(lobby.players().stream().noneMatch(LobbyView.LobbyPlayer::ready));
        assertTrue(countRoomEvents(roomId, AuctionEvent.LOBBY_UPDATED) >= 3);

        // Kimse hazır demedi: sayaç (2,5 sn) bitince oyun kendiliğinden başlar.
        bekle(roomId, AuctionEvent.GAME_STARTED, 6000);
        bekle(roomId, AuctionEvent.AUCTION_STARTED, 3000);
    }

    @Test
    void ucOyuncuHazirOluncaOyunHemenBaslar() throws Exception {
        GuestResponse a = yeniMisafir();
        GuestResponse b = yeniMisafir();
        GuestResponse c = yeniMisafir();
        Long roomId = eslestir(a).roomId();
        eslestir(b);
        eslestir(c);

        lobbyService.setReady(roomId, a.playerToken(), true);
        LobbyView view = lobbyService.setReady(roomId, b.playerToken(), true);
        assertEquals(2, view.players().stream().filter(LobbyView.LobbyPlayer::ready).count());

        // İptal edip tekrar hazır olabilir.
        view = lobbyService.setReady(roomId, b.playerToken(), false);
        assertEquals(1, view.players().stream().filter(LobbyView.LobbyPlayer::ready).count());
        lobbyService.setReady(roomId, b.playerToken(), true);
        assertEquals(-1, indexOf(roomId, AuctionEvent.GAME_STARTED), "iki kişi hazırken oyun başlamamalı");

        lobbyService.setReady(roomId, c.playerToken(), true);
        // Sayaç 2,5 sn, oyun ondan çok önce başlamış olmalı.
        bekle(roomId, AuctionEvent.GAME_STARTED, 1500);

        ApiException e = assertThrows(ApiException.class,
                () -> lobbyService.setReady(roomId, a.playerToken(), true));
        assertEquals("GAME_ALREADY_STARTED", e.getCode());
    }

    @Test
    void odadaOlmayanHazirOlamaz() {
        GuestResponse a = yeniMisafir();
        GuestResponse outsider = yeniMisafir();
        Long roomId = eslestir(a).roomId();

        ApiException e = assertThrows(ApiException.class,
                () -> lobbyService.setReady(roomId, outsider.playerToken(), true));
        assertEquals("NOT_IN_ROOM", e.getCode());

        LobbyView lobby = lobbyService.getLobby(roomId);
        assertEquals(RoomStatus.LOBBY, lobby.status());
        assertFalse(lobby.players().get(0).ready());
        assertEquals(null, lobby.countdownDeadlineMs(), "oda dolmadan sayaç başlamaz");
    }
}
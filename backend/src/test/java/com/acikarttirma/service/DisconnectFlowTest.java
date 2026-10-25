package com.acikarttirma.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.acikarttirma.dto.AuctionEvent;
import com.acikarttirma.dto.AuctionPayloads;
import com.acikarttirma.dto.GuestResponse;
import com.acikarttirma.dto.LiveStateView;
import com.acikarttirma.exception.ApiException;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

/**
 * F6: kopma ve geri bağlanma. İhale süresi 2 sn: bir ihale kopma yüzünden hemen kapanıyorsa
 * 2 sn dolmadan kapanmış olmalı. WebSocket yerine orkestratörün kopma metotları doğrudan çağrılır.
 */
@SpringBootTest(properties = {
        "auction.first-delay-ms=100",
        "auction.next-delay-ms=100",
        "auction.bid-timeout-ms=2000"
})
@ActiveProfiles("dev")
@Import(FlowTestSupport.RecorderConfig.class)
class DisconnectFlowTest extends FlowTestSupport {

    private List<GuestResponse> rivals(Setup s, AuctionPayloads.AuctionStarted started) {
        return s.guests().stream()
                .filter(g -> !roomPlayerIdOf(s.room(), g.userId()).equals(started.openerRoomPlayerId()))
                .toList();
    }

    @Test
    void tumRakiplerKopunca_ihaleHemenKapanir() throws Exception {
        Setup s = kurVeBaslat(31L);
        Long roomId = s.room().roomId();
        AuctionPayloads.AuctionStarted started =
                (AuctionPayloads.AuctionStarted) bekle(roomId, AuctionEvent.AUCTION_STARTED, 5000).data();

        long t0 = System.nanoTime();
        for (GuestResponse rival : rivals(s, started)) {
            orchestrator.onPlayerDisconnected(rival.userId());
        }
        // 2 sn'lik süre dolmadan kapanmalı: karşısına çıkacak kimse kalmadı.
        AuctionPayloads.AuctionClosed closed =
                (AuctionPayloads.AuctionClosed) bekle(roomId, AuctionEvent.AUCTION_CLOSED, 1500).data();
        long elapsedMs = (System.nanoTime() - t0) / 1_000_000;

        assertTrue(elapsedMs < 1500);
        assertEquals(started.openerRoomPlayerId(), closed.winnerRoomPlayerId());
        assertEquals(2, (int) countRoomEvents(roomId, AuctionEvent.PLAYER_DISCONNECTED));
    }

    @Test
    void kopupDonenOyuncuYurumekteOlanIhaleyeGiremez() throws Exception {
        Setup s = kurVeBaslat(32L);
        Long roomId = s.room().roomId();
        AuctionPayloads.AuctionStarted started =
                (AuctionPayloads.AuctionStarted) bekle(roomId, AuctionEvent.AUCTION_STARTED, 5000).data();
        GuestResponse rival = rivals(s, started).get(0);

        orchestrator.onPlayerDisconnected(rival.userId());
        orchestrator.onPlayerConnected(rival.userId());
        bekle(roomId, AuctionEvent.PLAYER_DISCONNECTED, 2000);
        bekle(roomId, AuctionEvent.PLAYER_RECONNECTED, 2000);

        // Aynı ihalede teklif veremez: kopunca o ihaleden çekilmiş sayıldı.
        orchestrator.placeBid(roomId, rival.userId(), started.price(), 1);
        Recorded rejected = recorder.all.stream()
                .filter(r -> rival.userId().equals(r.userId())
                        && r.event().type().equals(AuctionEvent.BID_REJECTED))
                .findFirst().orElseThrow();
        assertEquals("PLAYER_WITHDRAWN", ((AuctionPayloads.BidRejected) rejected.event().data()).reason());

        // Ama bağlı görünür, sonraki ihalelere katılabilir.
        LiveStateView state = orchestrator.getState(roomId);
        boolean connected = state.players().stream()
                .filter(p -> p.userId().equals(rival.userId()))
                .findFirst().orElseThrow().connected();
        assertTrue(connected);
    }

    @Test
    void oyunBitmedenSonucIstenemez() {
        Setup s = kurVeBaslat(33L);
        ApiException e = assertThrows(ApiException.class, () -> resultService.getResult(s.room().roomId()));
        assertEquals("GAME_NOT_FINISHED", e.getCode());
    }
}
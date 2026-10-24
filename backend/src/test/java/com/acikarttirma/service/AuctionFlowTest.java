package com.acikarttirma.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.acikarttirma.dto.AuctionEvent;
import com.acikarttirma.dto.AuctionPayloads;
import com.acikarttirma.dto.GuestResponse;
import com.acikarttirma.dto.LiveStateView;
import com.acikarttirma.dto.RoomView;
import com.acikarttirma.exception.ApiException;
import com.acikarttirma.model.AuctionItem;
import com.acikarttirma.model.RoomPlayer;
import com.acikarttirma.model.enums.AuctionStatus;
import com.acikarttirma.repository.AuctionItemRepository;
import com.acikarttirma.repository.RoomPlayerRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

/**
 * F5: canlı ihale akışı. WebSocket yerine olayları kaydeden bir yayıncı kullanılır.
 * Bu testler TRANSACTIONAL DEĞİLDİR (sayaç ayrı thread'de DB'ye yazar), bu yüzden
 * her test sonunda oluşturduğu odayı ve kullanıcıları kendisi siler.
 * Süreler kısaltılmıştır.
 */
@SpringBootTest(properties = {
        "auction.first-delay-ms=100",
        "auction.next-delay-ms=100",
        "auction.bid-timeout-ms=1500"
})
@ActiveProfiles("dev")
@Import(AuctionFlowTest.RecorderConfig.class)
class AuctionFlowTest {

    record Recorded(Long roomId, Long userId, AuctionEvent event) {
    }

    static class RecordingPublisher implements AuctionEventPublisher {
        final List<Recorded> all = new CopyOnWriteArrayList<>();

        @Override
        public void toRoom(Long roomId, AuctionEvent event) {
            all.add(new Recorded(roomId, null, event));
        }

        @Override
        public void toUser(Long userId, AuctionEvent event) {
            all.add(new Recorded(null, userId, event));
        }
    }

    @TestConfiguration
    static class RecorderConfig {
        @Bean
        @Primary
        RecordingPublisher recordingPublisher() {
            return new RecordingPublisher();
        }
    }

    record Setup(RoomView room, List<GuestResponse> guests) {
    }

    @Autowired
    private GuestService guestService;
    @Autowired
    private RoomService roomService;
    @Autowired
    private AuctionOrchestrator orchestrator;
    @Autowired
    private RecordingPublisher recorder;
    @Autowired
    private AuctionItemRepository auctionItemRepository;
    @Autowired
    private RoomPlayerRepository roomPlayerRepository;
    @Autowired
    private JdbcTemplate jdbc;

    private final List<Long> createdRooms = new ArrayList<>();
    private final List<Long> createdUsers = new ArrayList<>();

    @AfterEach
    void temizle() {
        for (Long roomId : createdRooms) {
            orchestrator.stopGame(roomId);
            jdbc.update("DELETE FROM squad_players WHERE squad_id IN (SELECT squad_id FROM user_squads "
                    + "WHERE room_player_id IN (SELECT room_player_id FROM room_players WHERE room_id = ?))", roomId);
            jdbc.update("DELETE FROM user_squads WHERE room_player_id IN "
                    + "(SELECT room_player_id FROM room_players WHERE room_id = ?)", roomId);
            jdbc.update("DELETE FROM auction_items WHERE room_id = ?", roomId);
            jdbc.update("DELETE FROM room_players WHERE room_id = ?", roomId);
            jdbc.update("DELETE FROM rooms WHERE room_id = ?", roomId);
        }
        for (Long userId : createdUsers) {
            jdbc.update("DELETE FROM users WHERE user_id = ?", userId);
        }
        createdRooms.clear();
        createdUsers.clear();
    }

    private GuestResponse yeniMisafir() {
        GuestResponse guest = guestService.login("a" + UUID.randomUUID().toString().substring(0, 8));
        createdUsers.add(guest.userId());
        return guest;
    }

    /** 3 misafir, dolu oda (SETUP), oyun başlatıldı. */
    private Setup kurVeBaslat(long seed) {
        List<GuestResponse> guests = List.of(yeniMisafir(), yeniMisafir(), yeniMisafir());
        RoomView room = roomService.createRoom(guests.get(0).playerToken(), seed);
        createdRooms.add(room.roomId());
        roomService.joinRoom(guests.get(1).playerToken(), room.roomId());
        room = roomService.joinRoom(guests.get(2).playerToken(), room.roomId());
        orchestrator.startGame(room.roomId(), guests.get(0).playerToken());
        return new Setup(room, guests);
    }

    private Long roomPlayerIdOf(RoomView room, Long userId) {
        return room.players().stream()
                .filter(p -> p.userId().equals(userId))
                .findFirst().orElseThrow().roomPlayerId();
    }

    private AuctionEvent bekle(Long roomId, String type) throws InterruptedException {
        long end = System.currentTimeMillis() + 5000;
        while (System.currentTimeMillis() < end) {
            for (Recorded r : recorder.all) {
                if (roomId.equals(r.roomId()) && r.event().type().equals(type)) {
                    return r.event();
                }
            }
            Thread.sleep(25);
        }
        throw new AssertionError("Olay gelmedi: " + type);
    }

    @Test
    void ayniAndaTeklifteTekKazananCikarFiyatTutarliKalir() throws Exception {
        Setup s = kurVeBaslat(11L);
        Long roomId = s.room().roomId();
        AuctionPayloads.AuctionStarted started =
                (AuctionPayloads.AuctionStarted) bekle(roomId, AuctionEvent.AUCTION_STARTED).data();
        int price = started.price();

        List<GuestResponse> bidders = s.guests().stream()
                .filter(g -> !roomPlayerIdOf(s.room(), g.userId()).equals(started.openerRoomPlayerId()))
                .toList();

        // Açan oyuncu hariç iki oyuncu, her biri 3 teklifi AYNI ANDA, aynı fiyat beklentisiyle gönderir.
        ExecutorService pool = Executors.newFixedThreadPool(6);
        CountDownLatch gate = new CountDownLatch(1);
        List<Future<Void>> futures = new ArrayList<>();
        for (GuestResponse bidder : bidders) {
            for (int i = 0; i < 3; i++) {
                Callable<Void> task = () -> {
                    gate.await();
                    orchestrator.placeBid(roomId, bidder.userId(), price, 1);
                    return null;
                };
                futures.add(pool.submit(task));
            }
        }
        gate.countDown();
        for (Future<Void> f : futures) {
            f.get(5, TimeUnit.SECONDS);
        }
        pool.shutdown();

        long accepted = recorder.all.stream()
                .filter(r -> roomId.equals(r.roomId()) && r.event().type().equals(AuctionEvent.BID_ACCEPTED))
                .count();
        assertEquals(1, accepted);

        List<Long> bidderIds = bidders.stream().map(GuestResponse::userId).toList();
        long stale = recorder.all.stream()
                .filter(r -> r.userId() != null && bidderIds.contains(r.userId())
                        && r.event().type().equals(AuctionEvent.BID_REJECTED))
                .filter(r -> ((AuctionPayloads.BidRejected) r.event().data()).reason().equals("STALE_PRICE"))
                .count();
        assertEquals(5, stale);

        LiveStateView state = orchestrator.getState(roomId);
        assertNotNull(state.current());
        assertEquals(price + 1, state.current().price());
    }

    @Test
    void sureDolunca_ihaleKapanirKartSatilirButceDuser() throws Exception {
        Setup s = kurVeBaslat(12L);
        Long roomId = s.room().roomId();
        AuctionPayloads.AuctionStarted started =
                (AuctionPayloads.AuctionStarted) bekle(roomId, AuctionEvent.AUCTION_STARTED).data();
        AuctionPayloads.AuctionClosed closed =
                (AuctionPayloads.AuctionClosed) bekle(roomId, AuctionEvent.AUCTION_CLOSED).data();

        // Kimse teklif vermedi: açan oyuncu açılış fiyatına kartı alır.
        assertEquals(started.openerRoomPlayerId(), closed.winnerRoomPlayerId());
        assertEquals(started.price(), closed.price());

        AuctionItem item = auctionItemRepository
                .findByRoom_IdAndSequenceNo(roomId, (short) started.sequenceNo()).orElseThrow();
        assertEquals(AuctionStatus.SOLD, item.getStatus());
        assertEquals(started.price(), item.getSoldPrice().intValue());

        RoomPlayer winner = roomPlayerRepository.findById(started.openerRoomPlayerId()).orElseThrow();
        assertEquals(RoomService.STARTING_BUDGET - started.price(), winner.getCurrentBudget());
    }

    @Test
    void ayniOdaIkiKezBaslatilamaz() {
        Setup s = kurVeBaslat(13L);
        ApiException e = assertThrows(ApiException.class,
                () -> orchestrator.startGame(s.room().roomId(), s.guests().get(1).playerToken()));
        assertEquals("GAME_ALREADY_STARTED", e.getCode());
    }

    @Test
    void setupOlmayanOdaBaslatilamaz() {
        GuestResponse guest = yeniMisafir();
        RoomView room = roomService.createRoom(guest.playerToken(), 14L);
        createdRooms.add(room.roomId());
        ApiException e = assertThrows(ApiException.class,
                () -> orchestrator.startGame(room.roomId(), guest.playerToken()));
        assertEquals("ROOM_NOT_IN_SETUP", e.getCode());
    }
}
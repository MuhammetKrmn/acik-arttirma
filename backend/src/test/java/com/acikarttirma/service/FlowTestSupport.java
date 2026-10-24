package com.acikarttirma.service;

import com.acikarttirma.dto.AuctionEvent;
import com.acikarttirma.dto.GuestResponse;
import com.acikarttirma.dto.RoomView;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * F6 testlerinin ortak yardımcıları: olayları kaydeden yayıncı, oda kurma, bekleme, temizlik.
 * Bu testler TRANSACTIONAL DEĞİLDİR (sayaçlar ayrı thread'de DB'ye yazar), bu yüzden her testten
 * sonra oluşturdukları oda ve kullanıcıları kendileri silerler.
 */
abstract class FlowTestSupport {

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
    protected GuestService guestService;
    @Autowired
    protected RoomService roomService;
    @Autowired
    protected AuctionOrchestrator orchestrator;
    @Autowired
    protected ResultService resultService;
    @Autowired
    protected RecordingPublisher recorder;
    @Autowired
    protected JdbcTemplate jdbc;

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
            jdbc.update("UPDATE rooms SET winner_room_player_id = NULL WHERE room_id = ?", roomId);
            jdbc.update("DELETE FROM room_players WHERE room_id = ?", roomId);
            jdbc.update("DELETE FROM rooms WHERE room_id = ?", roomId);
        }
        for (Long userId : createdUsers) {
            jdbc.update("DELETE FROM users WHERE user_id = ?", userId);
        }
        createdRooms.clear();
        createdUsers.clear();
    }

    protected GuestResponse yeniMisafir() {
        GuestResponse guest = guestService.login("a" + UUID.randomUUID().toString().substring(0, 8));
        createdUsers.add(guest.userId());
        return guest;
    }

    /** 3 misafir, dolu oda (SETUP). Oyun başlatılmaz. */
    protected Setup kur(long seed) {
        List<GuestResponse> guests = List.of(yeniMisafir(), yeniMisafir(), yeniMisafir());
        RoomView room = roomService.createRoom(guests.get(0).playerToken(), seed);
        createdRooms.add(room.roomId());
        roomService.joinRoom(guests.get(1).playerToken(), room.roomId());
        room = roomService.joinRoom(guests.get(2).playerToken(), room.roomId());
        return new Setup(room, guests);
    }

    protected Setup kurVeBaslat(long seed) {
        Setup s = kur(seed);
        orchestrator.startGame(s.room().roomId(), s.guests().get(0).playerToken());
        return s;
    }

    protected Long roomPlayerIdOf(RoomView room, Long userId) {
        return room.players().stream()
                .filter(p -> p.userId().equals(userId))
                .findFirst().orElseThrow().roomPlayerId();
    }

    /** Odaya giden belirli türde ilk olayı bekler. */
    protected AuctionEvent bekle(Long roomId, String type, long timeoutMs) throws InterruptedException {
        long end = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < end) {
            for (Recorded r : recorder.all) {
                if (roomId.equals(r.roomId()) && r.event().type().equals(type)) {
                    return r.event();
                }
            }
            Thread.sleep(20);
        }
        throw new AssertionError("Olay gelmedi: " + type);
    }

    /** Odaya giden belirli türde olayın kayıt listesindeki sırası, yoksa -1. */
    protected int indexOf(Long roomId, String type) {
        int i = 0;
        for (Recorded r : recorder.all) {
            if (roomId.equals(r.roomId()) && r.event().type().equals(type)) {
                return i;
            }
            i++;
        }
        return -1;
    }

    protected long countRoomEvents(Long roomId, String type) {
        return recorder.all.stream()
                .filter(r -> roomId.equals(r.roomId()) && r.event().type().equals(type))
                .count();
    }
}
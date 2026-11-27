package com.acikarttirma.service;

import com.acikarttirma.dto.AuctionEvent;
import com.acikarttirma.dto.LobbyView;
import com.acikarttirma.dto.RoomView;
import com.acikarttirma.exception.ApiException;
import com.acikarttirma.model.User;
import com.acikarttirma.model.enums.RoomStatus;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

/**
 * Lobi: hızlı eşleşme, "hazırım" ve oyunu kendiliğinden başlatan sayaç.
 * Oda dolunca (SETUP) sayaç başlar. Üç oyuncu da hazır olunca ya da sayaç bitince
 * oyunu sunucu başlatır, herkes aynı game_started olayıyla arenaya geçer.
 * Lobi durumu bellekte tutulur, her değişiklik /topic/rooms/{roomId} üzerinden lobby_updated olarak yayınlanır.
 */
@Service
public class LobbyService {

    private static final Logger log = LoggerFactory.getLogger(LobbyService.class);
    /** Oyun başladıktan sonra lobi kaydı bu kadar süre sonra bellekten silinir. */
    private static final long CLEANUP_DELAY_MS = 10 * 60 * 1000L;

    private final RoomService roomService;
    private final GuestService guestService;
    private final AuctionOrchestrator orchestrator;
    private final AuctionEventPublisher publisher;
    private final ScheduledExecutorService scheduler;
    private final long countdownMs;

    private final Map<Long, LobbyState> lobbies = new ConcurrentHashMap<>();
    /** Hızlı eşleşme tek tek yapılır: iki kişi aynı anda gelirse ikisi de aynı odaya düşer. */
    private final ReentrantLock matchLock = new ReentrantLock();

    private static final class LobbyState {
        final Set<Long> readyUserIds = new HashSet<>();
        Long deadlineMs;
        ScheduledFuture<?> timer;
        boolean started;
    }

    public LobbyService(RoomService roomService,
                        GuestService guestService,
                        AuctionOrchestrator orchestrator,
                        AuctionEventPublisher publisher,
                        @Qualifier("auctionScheduler") ScheduledExecutorService scheduler,
                        @Value("${lobby.countdown-ms:30000}") long countdownMs) {
        this.roomService = roomService;
        this.guestService = guestService;
        this.orchestrator = orchestrator;
        this.publisher = publisher;
        this.scheduler = scheduler;
        this.countdownMs = countdownMs;
    }

    // ------------------------------------------------------------------ dışarıya açık işlemler

    /** Açık bir odaya sokar, yoksa yeni oda kurar. Zaten bekleyen bir odadaysan o odayı döner. */
    public RoomView quickJoin(String token) {
        RoomView room;
        matchLock.lock();
        try {
            room = roomService.quickJoin(token);
        } finally {
            matchLock.unlock();
        }
        onRoomChanged(room);
        return room;
    }

    /** Oda kuruldu veya biri katıldı: lobi durumunu yayınlar, oda dolduysa sayacı başlatır. */
    public void onRoomChanged(RoomView room) {
        LobbyView view;
        boolean startNow;
        synchronized (this) {
            LobbyState state = lobbies.computeIfAbsent(room.roomId(), id -> new LobbyState());
            if (state.started) {
                return;
            }
            if (room.status() == RoomStatus.SETUP && state.timer == null) {
                state.deadlineMs = System.currentTimeMillis() + countdownMs;
                state.timer = scheduler.schedule(() -> onCountdownEnd(room.roomId()),
                        countdownMs, TimeUnit.MILLISECONDS);
            }
            view = toView(room, state);
            startNow = allReady(room, state) && markStarted(room.roomId(), state);
        }
        publisher.toRoom(room.roomId(), AuctionEvent.of(AuctionEvent.LOBBY_UPDATED, view));
        if (startNow) {
            start(room);
        }
    }

    /** "Hazırım" / "İptal". Üç oyuncu da hazırsa oyun hemen başlar. */
    public LobbyView setReady(Long roomId, String token, boolean ready) {
        User user = guestService.authenticate(token);
        RoomView room = roomService.getRoom(roomId);
        boolean member = room.players().stream().anyMatch(p -> p.userId().equals(user.getId()));
        if (!member) {
            throw new ApiException(HttpStatus.FORBIDDEN, "NOT_IN_ROOM", "Bu odanın oyuncusu değilsin");
        }
        if (room.status() != RoomStatus.LOBBY && room.status() != RoomStatus.SETUP) {
            throw new ApiException(HttpStatus.CONFLICT, "GAME_ALREADY_STARTED", "Oyun zaten başladı");
        }
        LobbyView view;
        boolean startNow;
        synchronized (this) {
            LobbyState state = lobbies.computeIfAbsent(roomId, id -> new LobbyState());
            if (state.started) {
                throw new ApiException(HttpStatus.CONFLICT, "GAME_ALREADY_STARTED", "Oyun zaten başladı");
            }
            if (ready) {
                state.readyUserIds.add(user.getId());
            } else {
                state.readyUserIds.remove(user.getId());
            }
            view = toView(room, state);
            startNow = allReady(room, state) && markStarted(roomId, state);
        }
        publisher.toRoom(roomId, AuctionEvent.of(AuctionEvent.LOBBY_UPDATED, view));
        if (startNow) {
            start(room);
        }
        return view;
    }

    /** Lobinin anlık durumu (sayfa yenilenince veya ilk açılışta). */
    public LobbyView getLobby(Long roomId) {
        RoomView room = roomService.getRoom(roomId);
        synchronized (this) {
            LobbyState state = lobbies.get(roomId);
            return toView(room, state != null ? state : new LobbyState());
        }
    }

    /** Oyun lobi dışından (POST /start) başlatıldıysa sayaç durdurulur. */
    public void onStartedElsewhere(Long roomId) {
        synchronized (this) {
            LobbyState state = lobbies.computeIfAbsent(roomId, id -> new LobbyState());
            markStarted(roomId, state);
        }
    }

    // ------------------------------------------------------------------ iç akış

    private void onCountdownEnd(Long roomId) {
        try {
            RoomView room = roomService.getRoom(roomId);
            boolean startNow;
            synchronized (this) {
                LobbyState state = lobbies.get(roomId);
                startNow = state != null && room.status() == RoomStatus.SETUP && markStarted(roomId, state);
            }
            if (startNow) {
                start(room);
            }
        } catch (RuntimeException e) {
            log.warn("Lobi sayacı bitti ama oyun başlatılamadı, oda={}: {}", roomId, e.getMessage());
        }
    }

    /** Kilit tutulurken çağrılır. Daha önce başlatılmadıysa true döner, sayacı durdurur. */
    private boolean markStarted(Long roomId, LobbyState state) {
        if (state.started) {
            return false;
        }
        state.started = true;
        if (state.timer != null) {
            state.timer.cancel(false);
        }
        scheduler.schedule(() -> lobbies.remove(roomId), CLEANUP_DELAY_MS, TimeUnit.MILLISECONDS);
        return true;
    }

    /** Oyunu 1. koltuktaki oyuncu adına başlatır. game_started olayını orkestratör yayınlar. */
    private void start(RoomView room) {
        try {
            Long starterUserId = room.players().get(0).userId();
            orchestrator.startGameFor(room.roomId(), starterUserId);
        } catch (RuntimeException e) {
            log.warn("Oyun lobiden başlatılamadı, oda={}: {}", room.roomId(), e.getMessage());
        }
    }

    private boolean allReady(RoomView room, LobbyState state) {
        return room.status() == RoomStatus.SETUP
                && room.players().size() == RoomService.ROOM_SIZE
                && room.players().stream().allMatch(p -> state.readyUserIds.contains(p.userId()));
    }

    private LobbyView toView(RoomView room, LobbyState state) {
        List<LobbyView.LobbyPlayer> players = room.players().stream()
                .map(p -> new LobbyView.LobbyPlayer(p.roomPlayerId(), p.userId(), p.seatNumber(), p.username(),
                        state.readyUserIds.contains(p.userId())))
                .toList();
        return new LobbyView(room.roomId(), room.status(), room.conceptName(), room.startingBudget(), players,
                state.deadlineMs, System.currentTimeMillis());
    }
}
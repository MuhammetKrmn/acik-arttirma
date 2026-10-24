package com.acikarttirma.service;

import com.acikarttirma.dto.AuctionEvent;
import com.acikarttirma.dto.AuctionPayloads;
import com.acikarttirma.dto.LiveStateView;
import com.acikarttirma.engine.Auction;
import com.acikarttirma.engine.AuctionOutcome;
import com.acikarttirma.engine.BidRejectReason;
import com.acikarttirma.engine.BidResult;
import com.acikarttirma.engine.PlayerState;
import com.acikarttirma.engine.WithdrawResult;
import com.acikarttirma.exception.ApiException;
import com.acikarttirma.model.User;
import com.acikarttirma.model.enums.Slot;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

/**
 * Canlı ihalenin yöneticisi. Oda başına bir kilit tutar; teklif, çekilme ve sayaç hep bu kilidin
 * içinde, tek tek işlenir. Kural kararlarını motor (Auction) verir, bu sınıf sıralar, sayar,
 * kaydeder ve yayınlar.
 */
@Service
public class AuctionOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(AuctionOrchestrator.class);

    private final Map<Long, LiveGame> games = new ConcurrentHashMap<>();

    private final GuestService guestService;
    private final AuctionPersistence persistence;
    private final AuctionEventPublisher publisher;
    private final AuctionTimings timings;
    private final ScheduledExecutorService scheduler;

    public AuctionOrchestrator(GuestService guestService,
                               AuctionPersistence persistence,
                               AuctionEventPublisher publisher,
                               AuctionTimings timings,
                               @Qualifier("auctionScheduler") ScheduledExecutorService scheduler) {
        this.guestService = guestService;
        this.persistence = persistence;
        this.publisher = publisher;
        this.timings = timings;
        this.scheduler = scheduler;
    }

    // ------------------------------------------------------------------ oyunu başlat / durdur

    /** SETUP durumundaki odayı başlatır. Odanın herhangi bir oyuncusu başlatabilir. */
    public void startGame(Long roomId, String token) {
        User user = guestService.authenticate(token);
        if (games.containsKey(roomId)) {
            throw new ApiException(HttpStatus.CONFLICT, "GAME_ALREADY_STARTED", "Oyun zaten başladı");
        }
        GameSnapshot snapshot = persistence.beginGame(roomId, user.getId());
        LiveGame game = new LiveGame(snapshot);
        games.put(roomId, game);

        publisher.toRoom(roomId, AuctionEvent.of(AuctionEvent.GAME_STARTED,
                new AuctionPayloads.GameStarted(game.items.size())));
        scheduler.schedule(() -> openNext(game), timings.getFirstDelayMs(), TimeUnit.MILLISECONDS);
    }

    /** Canlı oyunu bellekten kaldırır, sayacı iptal eder (testler ve temizlik için). */
    public void stopGame(Long roomId) {
        LiveGame game = games.remove(roomId);
        if (game == null) {
            return;
        }
        game.lock.lock();
        try {
            game.stopped = true;
            if (game.timer != null) {
                game.timer.cancel(false);
            }
        } finally {
            game.lock.unlock();
        }
    }

    // ------------------------------------------------------------------ oyuncu hamleleri

    public void placeBid(Long roomId, Long userId, int expectedPrice, int increment) {
        LiveGame game = requireGame(roomId);
        game.lock.lock();
        try {
            Long roomPlayerId = requirePlayer(game, userId);
            if (game.stopped || game.current == null) {
                publisher.toUser(userId, AuctionEvent.of(AuctionEvent.BID_REJECTED,
                        new AuctionPayloads.BidRejected(BidRejectReason.AUCTION_CLOSED.name(), 0, null)));
                return;
            }
            BidResult result = game.current.placeBid(roomPlayerId, expectedPrice, increment);
            if (result.accepted()) {
                startTimer(game);
                publisher.toRoom(roomId, AuctionEvent.of(AuctionEvent.BID_ACCEPTED,
                        new AuctionPayloads.BidAccepted(game.currentItem.sequenceNo(), result.price(),
                                result.leaderId(), increment, game.deadlineMs, System.currentTimeMillis())));
                if (game.current.isDecided()) {
                    closeCurrent(game);
                }
            } else {
                publisher.toUser(userId, AuctionEvent.of(AuctionEvent.BID_REJECTED,
                        new AuctionPayloads.BidRejected(result.reason().name(), result.price(),
                                result.leaderId())));
            }
        } finally {
            game.lock.unlock();
        }
    }

    public void withdraw(Long roomId, Long userId) {
        LiveGame game = requireGame(roomId);
        game.lock.lock();
        try {
            Long roomPlayerId = requirePlayer(game, userId);
            if (game.stopped || game.current == null) {
                publisher.toUser(userId, AuctionEvent.of(AuctionEvent.WITHDRAW_REJECTED,
                        new AuctionPayloads.WithdrawRejected("AUCTION_CLOSED")));
                return;
            }
            WithdrawResult result = game.current.withdraw(roomPlayerId);
            if (result.accepted()) {
                publisher.toRoom(roomId, AuctionEvent.of(AuctionEvent.PLAYER_WITHDREW,
                        new AuctionPayloads.PlayerWithdrew(roomPlayerId)));
                if (game.current.isDecided()) {
                    closeCurrent(game);
                }
            } else {
                publisher.toUser(userId, AuctionEvent.of(AuctionEvent.WITHDRAW_REJECTED,
                        new AuctionPayloads.WithdrawRejected(result.reason().name())));
            }
        } finally {
            game.lock.unlock();
        }
    }

    /** Anlık durum: istemci kendini buradan senkronlar. */
    public LiveStateView getState(Long roomId) {
        LiveGame game = requireGame(roomId);
        game.lock.lock();
        try {
            LiveStateView.CurrentAuction currentView = null;
            if (game.current != null) {
                GameSnapshot.ItemSnapshot item = game.currentItem;
                currentView = new LiveStateView.CurrentAuction(item.sequenceNo(), item.slot(), item.tier(),
                        cardInfo(item), game.current.getCurrentPrice(), game.current.getLeaderId(),
                        game.deadlineMs, System.currentTimeMillis());
            }
            List<LiveStateView.PlayerBudget> players = new ArrayList<>();
            for (GameSnapshot.PlayerSnapshot ps : game.playerSnapshots) {
                PlayerState state = game.players.get(ps.roomPlayerId());
                List<Slot> filled = Arrays.stream(Slot.values()).filter(state::hasFilled).toList();
                players.add(new LiveStateView.PlayerBudget(ps.roomPlayerId(), ps.userId(), ps.seat(),
                        ps.username(), state.getBudget(), filled));
            }
            return new LiveStateView(game.finished, game.cursor, game.items.size(), currentView, players);
        } finally {
            game.lock.unlock();
        }
    }

    // ------------------------------------------------------------------ iç akış (kilit tutulurken)

    /** Sıradaki ihaleyi açar. Zamanlayıcı thread'inden çağrılır. */
    private void openNext(LiveGame game) {
        game.lock.lock();
        try {
            if (game.stopped) {
                return;
            }
            doOpenNext(game);
        } catch (RuntimeException e) {
            log.error("İhale açılamadı, oda={}", game.roomId, e);
        } finally {
            game.lock.unlock();
        }
    }

    private void doOpenNext(LiveGame game) {
        while (game.cursor < game.items.size()) {
            GameSnapshot.ItemSnapshot item = game.items.get(game.cursor++);
            Optional<PlayerState> opener = game.rotation.pickOpener(item.slot(), item.startingPrice());
            if (opener.isEmpty()) {
                // Kimse açamıyor: kart satılmaz, sıradakine geçilir (slot sahipsiz kalır).
                persistence.markItemUnsold(item.itemId());
                publisher.toRoom(game.roomId, AuctionEvent.of(AuctionEvent.AUCTION_SKIPPED,
                        new AuctionPayloads.AuctionSkipped(item.sequenceNo(), item.slot(), "NO_OPENER")));
                continue;
            }
            persistence.markItemActive(item.itemId());
            game.currentItem = item;
            game.current = new Auction(item.slot(), item.startingPrice(), game.players.values(),
                    opener.get().getId());
            startTimer(game);
            publisher.toRoom(game.roomId, AuctionEvent.of(AuctionEvent.AUCTION_STARTED,
                    new AuctionPayloads.AuctionStarted(item.sequenceNo(), game.items.size(), item.slot(),
                            item.tier(), cardInfo(item), item.startingPrice(), opener.get().getId(),
                            game.deadlineMs, System.currentTimeMillis())));
            if (game.current.isDecided()) {
                closeCurrent(game); // karşısına çıkabilecek kimse yok
            }
            return;
        }
        game.finished = true;
        publisher.toRoom(game.roomId, AuctionEvent.of(AuctionEvent.AUCTIONS_FINISHED,
                new AuctionPayloads.AuctionsFinished(budgets(game))));
    }

    /** Sayacı baştan başlatır: her kabul edilen teklifte süre sıfırlanır. */
    private void startTimer(LiveGame game) {
        if (game.timer != null) {
            game.timer.cancel(false);
        }
        long version = ++game.timerVersion;
        game.deadlineMs = System.currentTimeMillis() + timings.getBidTimeoutMs();
        game.timer = scheduler.schedule(() -> onTimeout(game, version),
                timings.getBidTimeoutMs(), TimeUnit.MILLISECONDS);
    }

    private void onTimeout(LiveGame game, long version) {
        game.lock.lock();
        try {
            if (game.stopped || game.current == null || game.timerVersion != version) {
                return; // eski veya iptal edilmiş sayaç
            }
            closeCurrent(game);
        } catch (RuntimeException e) {
            log.error("İhale kapatılamadı, oda={}", game.roomId, e);
        } finally {
            game.lock.unlock();
        }
    }

    /** Lider kartı alır: motor kapatır, DB'ye yazılır, sonuç yayınlanır, sıradaki ihale planlanır. */
    private void closeCurrent(LiveGame game) {
        if (game.timer != null) {
            game.timer.cancel(false);
            game.timer = null;
        }
        game.timerVersion++;
        GameSnapshot.ItemSnapshot item = game.currentItem;
        AuctionOutcome outcome = game.current.close();
        game.current = null;
        game.currentItem = null;
        try {
            persistence.recordSale(item.itemId(), outcome.winnerId(), outcome.price());
            publisher.toRoom(game.roomId, AuctionEvent.of(AuctionEvent.AUCTION_CLOSED,
                    new AuctionPayloads.AuctionClosed(item.sequenceNo(), item.slot(), outcome.winnerId(),
                            outcome.price(), budgets(game))));
        } finally {
            scheduler.schedule(() -> openNext(game), timings.getNextDelayMs(), TimeUnit.MILLISECONDS);
        }
    }

    // ------------------------------------------------------------------ yardımcılar

    private LiveGame requireGame(Long roomId) {
        LiveGame game = games.get(roomId);
        if (game == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "GAME_NOT_STARTED", "Bu oda için oyun başlamadı");
        }
        return game;
    }

    private Long requirePlayer(LiveGame game, Long userId) {
        Long roomPlayerId = game.roomPlayerByUser.get(userId);
        if (roomPlayerId == null) {
            throw new ApiException(HttpStatus.FORBIDDEN, "NOT_IN_ROOM", "Bu odanın oyuncusu değilsin");
        }
        return roomPlayerId;
    }

    private AuctionPayloads.CardInfo cardInfo(GameSnapshot.ItemSnapshot item) {
        return new AuctionPayloads.CardInfo(item.cardId(), item.playerName(), item.nationality(),
                item.photoUrl(), item.club(), item.position(), item.startingPrice());
    }

    private Map<Long, Integer> budgets(LiveGame game) {
        Map<Long, Integer> result = new LinkedHashMap<>();
        for (PlayerState p : game.players.values()) {
            result.put(p.getId(), p.getBudget());
        }
        return result;
    }
}
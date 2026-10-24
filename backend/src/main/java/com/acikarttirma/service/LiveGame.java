package com.acikarttirma.service;

import com.acikarttirma.engine.Auction;
import com.acikarttirma.engine.OpenerRotation;
import com.acikarttirma.engine.PlayerState;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Bir odanın bellekteki canlı oyunu. Tüm alanlara SADECE {@link #lock} tutulurken dokunulur.
 * (Not: sunucu yeniden başlarsa canlı oyun bellekten kaybolur, v1 sınırlaması.)
 */
final class LiveGame {

    final Long roomId;
    final ReentrantLock lock = new ReentrantLock();

    final List<GameSnapshot.ItemSnapshot> items;
    final List<GameSnapshot.PlayerSnapshot> playerSnapshots;
    /** roomPlayerId -> motorun oyuncu durumu (koltuk sırasıyla). */
    final Map<Long, PlayerState> players = new LinkedHashMap<>();
    /** userId -> roomPlayerId */
    final Map<Long, Long> roomPlayerByUser = new HashMap<>();
    final OpenerRotation rotation;

    /** Sıradaki ihalenin index'i (= şimdiye kadar işlenen ihale sayısı). */
    int cursor = 0;
    GameSnapshot.ItemSnapshot currentItem;
    Auction current;
    long deadlineMs;
    /** Eski sayaçların geç tetiklenmesini engellemek için her sayaç yenilenişinde artar. */
    long timerVersion = 0;
    ScheduledFuture<?> timer;
    boolean finished = false;
    boolean stopped = false;

    LiveGame(GameSnapshot snapshot) {
        this.roomId = snapshot.roomId();
        this.items = snapshot.items();
        this.playerSnapshots = snapshot.players();
        for (GameSnapshot.PlayerSnapshot p : snapshot.players()) {
            players.put(p.roomPlayerId(), new PlayerState(p.roomPlayerId(), p.budget()));
            roomPlayerByUser.put(p.userId(), p.roomPlayerId());
        }
        this.rotation = new OpenerRotation(new ArrayList<>(players.values()));
    }
}
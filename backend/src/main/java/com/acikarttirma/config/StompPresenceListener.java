package com.acikarttirma.config;

import com.acikarttirma.service.AuctionOrchestrator;
import java.security.Principal;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

/**
 * WebSocket bağlantılarını izler. Bir kullanıcının birden çok sekmesi olabilir: kullanıcı ancak
 * son bağlantısı kapanınca "koptu" sayılır, ilk bağlantısı açılınca "geri geldi" sayılır.
 */
@Component
public class StompPresenceListener {

    private static final Logger log = LoggerFactory.getLogger(StompPresenceListener.class);

    private final AuctionOrchestrator orchestrator;
    /** userId -> açık WebSocket oturumları */
    private final Map<Long, Set<String>> sessionsByUser = new ConcurrentHashMap<>();

    public StompPresenceListener(AuctionOrchestrator orchestrator) {
        this.orchestrator = orchestrator;
    }

    @EventListener
    public void onConnected(SessionConnectedEvent event) {
        Long userId = userId(event.getUser());
        if (userId == null) {
            return;
        }
        String sessionId = StompHeaderAccessor.wrap(event.getMessage()).getSessionId();
        sessionsByUser.computeIfAbsent(userId, id -> ConcurrentHashMap.newKeySet()).add(sessionId);
        try {
            orchestrator.onPlayerConnected(userId);
        } catch (RuntimeException e) {
            log.error("Bağlanma işlenemedi, kullanıcı={}", userId, e);
        }
    }

    @EventListener
    public void onDisconnected(SessionDisconnectEvent event) {
        Long userId = userId(event.getUser());
        if (userId == null) {
            return;
        }
        boolean[] lastOne = {false};
        sessionsByUser.computeIfPresent(userId, (id, sessions) -> {
            sessions.remove(event.getSessionId());
            if (sessions.isEmpty()) {
                lastOne[0] = true;
                return null;
            }
            return sessions;
        });
        if (lastOne[0]) {
            try {
                orchestrator.onPlayerDisconnected(userId);
            } catch (RuntimeException e) {
                log.error("Kopma işlenemedi, kullanıcı={}", userId, e);
            }
        }
    }

    private Long userId(Principal principal) {
        if (principal == null) {
            return null;
        }
        try {
            return Long.valueOf(principal.getName());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
package com.acikarttirma.config;

import com.acikarttirma.model.User;
import com.acikarttirma.service.GuestService;
import java.security.Principal;
import org.springframework.context.annotation.Lazy;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

/**
 * STOMP bağlantısını doğrular: CONNECT çerçevesinde X-Player-Token başlığı gerekir.
 * Geçerliyse bağlantının kimliği (Principal) userId olur, geçersizse bağlantı reddedilir.
 */
@Component
public class StompAuthInterceptor implements ChannelInterceptor {

    public static final String TOKEN_HEADER = "X-Player-Token";

    private final GuestService guestService;

    public StompAuthInterceptor(@Lazy GuestService guestService) {
        this.guestService = guestService;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            User user = guestService.authenticate(accessor.getFirstNativeHeader(TOKEN_HEADER));
            accessor.setUser(new StompPrincipal(String.valueOf(user.getId())));
        }
        return message;
    }

    private record StompPrincipal(String name) implements Principal {
        @Override
        public String getName() {
            return name;
        }
    }
}
package com.acikarttirma.controller;

import com.acikarttirma.dto.BidMessage;
import com.acikarttirma.exception.ApiException;
import com.acikarttirma.exception.GlobalExceptionHandler;
import com.acikarttirma.service.AuctionOrchestrator;
import java.security.Principal;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.stereotype.Controller;

/** WebSocket (STOMP) mesajları: /app/rooms/{roomId}/bid ve /app/rooms/{roomId}/withdraw */
@Controller
public class AuctionMessageController {

    private final AuctionOrchestrator orchestrator;

    public AuctionMessageController(AuctionOrchestrator orchestrator) {
        this.orchestrator = orchestrator;
    }

    @MessageMapping("/rooms/{roomId}/bid")
    public void bid(@DestinationVariable Long roomId, @Payload BidMessage message, Principal principal) {
        orchestrator.placeBid(roomId, userId(principal), message.expectedPrice(), message.increment());
    }

    @MessageMapping("/rooms/{roomId}/withdraw")
    public void withdraw(@DestinationVariable Long roomId, Principal principal) {
        orchestrator.withdraw(roomId, userId(principal));
    }

    /** İş kuralı hataları (ör. odada değilsin) sadece gönderene /user/queue/errors ile döner. */
    @MessageExceptionHandler(ApiException.class)
    @SendToUser("/queue/errors")
    public GlobalExceptionHandler.ErrorResponse onApiError(ApiException e) {
        return new GlobalExceptionHandler.ErrorResponse(e.getCode(), e.getMessage());
    }

    private Long userId(Principal principal) {
        return Long.valueOf(principal.getName());
    }
}
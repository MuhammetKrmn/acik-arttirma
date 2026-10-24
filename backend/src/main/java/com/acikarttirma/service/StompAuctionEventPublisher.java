package com.acikarttirma.service;

import com.acikarttirma.dto.AuctionEvent;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
public class StompAuctionEventPublisher implements AuctionEventPublisher {

    private final SimpMessagingTemplate messagingTemplate;

    public StompAuctionEventPublisher(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    /** İstemci abone olur: /topic/rooms/{roomId} */
    @Override
    public void toRoom(Long roomId, AuctionEvent event) {
        messagingTemplate.convertAndSend("/topic/rooms/" + roomId, event);
    }

    /** İstemci abone olur: /user/queue/bids */
    @Override
    public void toUser(Long userId, AuctionEvent event) {
        messagingTemplate.convertAndSendToUser(String.valueOf(userId), "/queue/bids", event);
    }
}
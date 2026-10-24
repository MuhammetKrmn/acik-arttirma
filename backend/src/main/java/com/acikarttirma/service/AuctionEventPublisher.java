package com.acikarttirma.service;

import com.acikarttirma.dto.AuctionEvent;

/** Olayları istemcilere yayar. Gerçek uygulama STOMP kullanır, testler kayıt tutan bir sürüm koyar. */
public interface AuctionEventPublisher {

    /** Odadaki herkese. */
    void toRoom(Long roomId, AuctionEvent event);

    /** Sadece tek bir kullanıcıya (ör. reddedilen teklif). */
    void toUser(Long userId, AuctionEvent event);
}
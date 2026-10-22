package com.acikarttirma.engine;

import com.acikarttirma.model.enums.Slot;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Tek bir ihalenin kural motoru. Saf Java: Spring, DB, saat yok.
 *
 * <p>Thread-safe DEĞİLDİR. Oda başına kilit F5'te servis katmanında alınır; bu sınıf
 * kilidin içinden çağrılır. Sayaç (10 sn) da motorun dışındadır: süre dolunca servis
 * {@link #close()} çağırır. Her olaydan sonra (teklif, çekilme, kopma) servis
 * {@link #isDecided()} sorar; true ise beklemeden {@link #close()} çağırır.
 */
public final class Auction {

    public static final Set<Integer> ALLOWED_INCREMENTS = Set.of(1, 5, 10);

    /** Bir oyuncunun teklif verebilmesi için fiyatın üstüne en az bu kadar bütçesi olmalı. */
    private static final int MIN_INCREMENT = 1;

    private final Slot slot;
    private final Map<Long, PlayerState> players = new LinkedHashMap<>();
    private final Set<Long> withdrawn = new HashSet<>();
    private int currentPrice;
    private long leaderId;
    private AuctionPhase phase = AuctionPhase.ACTIVE;

    /** Açan oyuncu açılış fiyatını otomatik teklif eder ve ilk lider olur. */
    public Auction(Slot slot, int openingPrice, Collection<PlayerState> participants, long openerId) {
        if (openingPrice <= 0) {
            throw new IllegalArgumentException("Açılış fiyatı pozitif olmalı: " + openingPrice);
        }
        for (PlayerState p : participants) {
            players.put(p.getId(), p);
        }
        PlayerState opener = players.get(openerId);
        if (opener == null) {
            throw new IllegalArgumentException("Açan oyuncu katılımcılar arasında yok: " + openerId);
        }
        if (opener.hasFilled(slot) || opener.getBudget() < openingPrice) {
            throw new IllegalArgumentException("Açan oyuncu bu ihaleyi açamaz (slot dolu veya bütçe yetersiz)");
        }
        this.slot = slot;
        this.currentPrice = openingPrice;
        this.leaderId = openerId;
    }

    public BidResult placeBid(long playerId, int expectedPrice, int increment) {
        if (phase != AuctionPhase.ACTIVE) {
            return reject(BidRejectReason.AUCTION_CLOSED);
        }
        PlayerState player = players.get(playerId);
        if (player == null) {
            return reject(BidRejectReason.UNKNOWN_PLAYER);
        }
        if (withdrawn.contains(playerId)) {
            return reject(BidRejectReason.PLAYER_WITHDRAWN);
        }
        if (!player.isConnected()) {
            return reject(BidRejectReason.PLAYER_DISCONNECTED);
        }
        if (player.hasFilled(slot)) {
            return reject(BidRejectReason.SLOT_FULL);
        }
        if (!ALLOWED_INCREMENTS.contains(increment)) {
            return reject(BidRejectReason.INVALID_INCREMENT);
        }
        if (expectedPrice != currentPrice) {
            return reject(BidRejectReason.STALE_PRICE);
        }
        if (playerId == leaderId) {
            return reject(BidRejectReason.ALREADY_LEADER);
        }
        if (currentPrice + increment > player.getBudget()) {
            return reject(BidRejectReason.INSUFFICIENT_BUDGET);
        }
        currentPrice += increment;
        leaderId = playerId;
        return BidResult.accepted(currentPrice, leaderId);
    }

    /** Geride kalan oyuncu çekilir; bir daha bu ihaleye giremez. Lider çekilemez. */
    public WithdrawResult withdraw(long playerId) {
        if (phase != AuctionPhase.ACTIVE) {
            return WithdrawResult.rejected(WithdrawRejectReason.AUCTION_CLOSED);
        }
        if (!players.containsKey(playerId)) {
            return WithdrawResult.rejected(WithdrawRejectReason.UNKNOWN_PLAYER);
        }
        if (playerId == leaderId) {
            return WithdrawResult.rejected(WithdrawRejectReason.LEADER_CANNOT_WITHDRAW);
        }
        if (!withdrawn.add(playerId)) {
            return WithdrawResult.rejected(WithdrawRejectReason.ALREADY_WITHDRAWN);
        }
        return WithdrawResult.ok();
    }

    /**
     * Oyuncunun bağlantısı koptu. Lider değilse bu ihale için çekilmiş sayılır (geri
     * dönse de giremez). Lider ise teklifi geçerli kalır.
     */
    public void onPlayerDisconnected(long playerId) {
        PlayerState player = players.get(playerId);
        if (player == null) {
            return;
        }
        player.setConnected(false);
        if (playerId != leaderId) {
            withdrawn.add(playerId);
        }
    }

    /**
     * Liderin karşısına çıkabilecek kimse kalmadıysa true: herkes çekildi, kopuk, slotu
     * dolu veya bütçesi fiyatın üstüne çıkmaya yetmiyor. Beklemeden kapatılabilir.
     */
    public boolean isDecided() {
        if (phase == AuctionPhase.CLOSED) {
            return true;
        }
        for (PlayerState p : players.values()) {
            if (isContender(p)) {
                return false;
            }
        }
        return true;
    }

    private boolean isContender(PlayerState p) {
        return p.getId() != leaderId
                && !withdrawn.contains(p.getId())
                && p.isConnected()
                && !p.hasFilled(slot)
                && p.getBudget() >= currentPrice + MIN_INCREMENT;
    }

    /** İhaleyi kapatır: lider kartı alır, slotu dolar, fiyat bütçesinden düşer. */
    public AuctionOutcome close() {
        if (phase == AuctionPhase.CLOSED) {
            throw new IllegalStateException("İhale zaten kapalı");
        }
        PlayerState winner = players.get(leaderId);
        winner.spend(currentPrice);
        winner.fillSlot(slot);
        phase = AuctionPhase.CLOSED;
        return new AuctionOutcome(leaderId, currentPrice, slot);
    }

    private BidResult reject(BidRejectReason reason) {
        return BidResult.rejected(reason, currentPrice, leaderId);
    }

    public Slot getSlot() {
        return slot;
    }

    public int getCurrentPrice() {
        return currentPrice;
    }

    public long getLeaderId() {
        return leaderId;
    }

    public AuctionPhase getPhase() {
        return phase;
    }
}
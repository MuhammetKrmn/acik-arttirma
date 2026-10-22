package com.acikarttirma.model;

import com.acikarttirma.model.enums.AuctionStatus;
import com.acikarttirma.model.enums.Slot;
import com.acikarttirma.model.enums.Tier;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Odaya çekilen 33 karttan biri. Tier 1 ve 2'de sequenceNo dolu (ihale sırası),
 * tier 3'te null (ihaleye çıkmaz, otomatik atanır).
 */
@Entity
@Table(name = "auction_items")
public class AuctionItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "auction_item_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "card_id", nullable = false)
    private Card card;

    /** Karttan kopyalanır, "aynı futbolcu aynı odada iki kez çıkmaz" kuralı için. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "footballer_id", nullable = false)
    private Footballer footballer;

    @Enumerated(EnumType.STRING)
    @Column(name = "slot_code", nullable = false)
    private Slot slot;

    @Convert(converter = TierConverter.class)
    @Column(name = "tier", nullable = false)
    private Tier tier;

    @Column(name = "sequence_no")
    private Short sequenceNo;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private AuctionStatus status = AuctionStatus.PENDING;

    @Column(name = "sold_price")
    private Integer soldPrice;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "winning_room_player_id")
    private RoomPlayer winningRoomPlayer;

    protected AuctionItem() {
    }

    public AuctionItem(Room room, Card card, Footballer footballer, Slot slot, Tier tier, Short sequenceNo) {
        this.room = room;
        this.card = card;
        this.footballer = footballer;
        this.slot = slot;
        this.tier = tier;
        this.sequenceNo = sequenceNo;
    }

    public Long getId() { return id; }
    public Room getRoom() { return room; }
    public Card getCard() { return card; }
    public Footballer getFootballer() { return footballer; }
    public Slot getSlot() { return slot; }
    public Tier getTier() { return tier; }
    public Short getSequenceNo() { return sequenceNo; }
    public AuctionStatus getStatus() { return status; }
    public void setStatus(AuctionStatus status) { this.status = status; }
    public Integer getSoldPrice() { return soldPrice; }
    public void setSoldPrice(Integer soldPrice) { this.soldPrice = soldPrice; }
    public RoomPlayer getWinningRoomPlayer() { return winningRoomPlayer; }
    public void setWinningRoomPlayer(RoomPlayer winningRoomPlayer) { this.winningRoomPlayer = winningRoomPlayer; }
}
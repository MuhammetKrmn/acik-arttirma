package com.acikarttirma.model;

import com.acikarttirma.model.enums.AcquisitionType;
import com.acikarttirma.model.enums.Slot;
import jakarta.persistence.Column;
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

/** Kadrodaki bir slot ve içindeki kart. Aynı U19 kartı birden çok kadroda bulunabilir. */
@Entity
@Table(name = "squad_players")
public class SquadPlayer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "squad_player_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "squad_id", nullable = false)
    private UserSquad squad;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "card_id", nullable = false)
    private Card card;

    @Enumerated(EnumType.STRING)
    @Column(name = "slot", nullable = false)
    private Slot slot;

    @Column(name = "paid_price", nullable = false)
    private int paidPrice;

    @Enumerated(EnumType.STRING)
    @Column(name = "acquisition_type", nullable = false)
    private AcquisitionType acquisitionType;

    protected SquadPlayer() {
    }

    public SquadPlayer(UserSquad squad, Card card, Slot slot, int paidPrice, AcquisitionType acquisitionType) {
        this.squad = squad;
        this.card = card;
        this.slot = slot;
        this.paidPrice = paidPrice;
        this.acquisitionType = acquisitionType;
    }

    public Long getId() { return id; }
    public UserSquad getSquad() { return squad; }
    public Card getCard() { return card; }
    public Slot getSlot() { return slot; }
    public int getPaidPrice() { return paidPrice; }
    public AcquisitionType getAcquisitionType() { return acquisitionType; }
}
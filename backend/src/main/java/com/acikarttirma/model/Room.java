package com.acikarttirma.model;

import com.acikarttirma.model.enums.RoomStatus;
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
import java.time.Instant;

@Entity
@Table(name = "rooms")
public class Room {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "room_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "concept_id", nullable = false)
    private Concept concept;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private RoomStatus status = RoomStatus.LOBBY;

    @Column(name = "starting_budget", nullable = false)
    private int startingBudget;

    /** Kart çekimi ve ihale sırası bu seed'den üretilir, aynı seed aynı odayı verir. */
    @Column(name = "seed")
    private Long seed;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "finished_at")
    private Instant finishedAt;

    /** Beraberlikte null kalabilir. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "winner_room_player_id")
    private RoomPlayer winnerRoomPlayer;

    protected Room() {
    }

    public Room(Concept concept, int startingBudget, Long seed) {
        this.concept = concept;
        this.startingBudget = startingBudget;
        this.seed = seed;
    }

    public Long getId() { return id; }
    public Concept getConcept() { return concept; }
    public RoomStatus getStatus() { return status; }
    public void setStatus(RoomStatus status) { this.status = status; }
    public int getStartingBudget() { return startingBudget; }
    public Long getSeed() { return seed; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getFinishedAt() { return finishedAt; }
    public void setFinishedAt(Instant finishedAt) { this.finishedAt = finishedAt; }
    public RoomPlayer getWinnerRoomPlayer() { return winnerRoomPlayer; }
    public void setWinnerRoomPlayer(RoomPlayer winnerRoomPlayer) { this.winnerRoomPlayer = winnerRoomPlayer; }
}
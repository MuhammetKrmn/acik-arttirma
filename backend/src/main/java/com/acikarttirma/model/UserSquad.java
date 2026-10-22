package com.acikarttirma.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

/** Oyuncunun o odadaki kadrosu. Oyuncu başına tek kadro vardır. */
@Entity
@Table(name = "user_squads")
public class UserSquad {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "squad_id")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "room_player_id", nullable = false, unique = true)
    private RoomPlayer roomPlayer;

    @Column(name = "total_spent", nullable = false)
    private int totalSpent;

    @Column(name = "total_rating", nullable = false)
    private int totalRating;

    protected UserSquad() {
    }

    public UserSquad(RoomPlayer roomPlayer) {
        this.roomPlayer = roomPlayer;
    }

    public Long getId() { return id; }
    public RoomPlayer getRoomPlayer() { return roomPlayer; }
    public int getTotalSpent() { return totalSpent; }
    public void setTotalSpent(int totalSpent) { this.totalSpent = totalSpent; }
    public int getTotalRating() { return totalRating; }
    public void setTotalRating(int totalRating) { this.totalRating = totalRating; }
}
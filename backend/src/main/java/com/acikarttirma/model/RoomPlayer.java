package com.acikarttirma.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/** Odadaki bir oyuncu (koltuk 1-3). */
@Entity
@Table(name = "room_players")
public class RoomPlayer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "room_player_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "seat_number", nullable = false)
    private short seatNumber;

    @Column(name = "current_budget", nullable = false)
    private int currentBudget;

    protected RoomPlayer() {
    }

    public RoomPlayer(Room room, User user, short seatNumber, int currentBudget) {
        this.room = room;
        this.user = user;
        this.seatNumber = seatNumber;
        this.currentBudget = currentBudget;
    }

    public Long getId() { return id; }
    public Room getRoom() { return room; }
    public User getUser() { return user; }
    public short getSeatNumber() { return seatNumber; }
    public int getCurrentBudget() { return currentBudget; }
    public void setCurrentBudget(int currentBudget) { this.currentBudget = currentBudget; }
}
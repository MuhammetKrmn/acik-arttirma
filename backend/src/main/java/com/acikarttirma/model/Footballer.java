package com.acikarttirma.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;

@Entity
@Table(name = "footballers")
public class Footballer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "footballer_id")
    private Long id;

    @Column(name = "player_name", nullable = false)
    private String playerName;

    @Column(name = "nationality")
    private String nationality;

    @Column(name = "photo_url")
    private String photoUrl;

    @Column(name = "birth_date")
    private LocalDate birthDate;

    protected Footballer() {
    }

    public Footballer(String playerName, String nationality) {
        this.playerName = playerName;
        this.nationality = nationality;
    }

    public Long getId() { return id; }
    public String getPlayerName() { return playerName; }
    public void setPlayerName(String playerName) { this.playerName = playerName; }
    public String getNationality() { return nationality; }
    public void setNationality(String nationality) { this.nationality = nationality; }
    public String getPhotoUrl() { return photoUrl; }
    public void setPhotoUrl(String photoUrl) { this.photoUrl = photoUrl; }
    public LocalDate getBirthDate() { return birthDate; }
    public void setBirthDate(LocalDate birthDate) { this.birthDate = birthDate; }
}
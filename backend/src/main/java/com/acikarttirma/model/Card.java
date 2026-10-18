package com.acikarttirma.model;

import com.acikarttirma.model.enums.Position;
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
 * Bir konsept + mevki + tier havuzundaki kart. U19 ceza kartında concept ve footballer null,
 * starting_price 0'dır (penaltyCard = true).
 *
 * <p>GİZLİLİK: overallRating oyun bitene kadar hiçbir REST/WebSocket cevabına konmaz.
 * Entity'ler istemciye doğrudan dönmez, her zaman ayrı DTO kullanılır.
 */
@Entity
@Table(name = "cards")
public class Card {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "card_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "concept_id")
    private Concept concept;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "footballer_id")
    private Footballer footballer;

    @Column(name = "club")
    private String club;

    @Enumerated(EnumType.STRING)
    @Column(name = "position", nullable = false)
    private Position position;

    @Convert(converter = TierConverter.class)
    @Column(name = "tier", nullable = false)
    private Tier tier;

    @Column(name = "overall_rating", nullable = false)
    private short overallRating;

    @Column(name = "starting_price", nullable = false)
    private int startingPrice;

    @Column(name = "is_penalty_card", nullable = false)
    private boolean penaltyCard;

    protected Card() {
    }

    public Long getId() { return id; }
    public Concept getConcept() { return concept; }
    public void setConcept(Concept concept) { this.concept = concept; }
    public Footballer getFootballer() { return footballer; }
    public void setFootballer(Footballer footballer) { this.footballer = footballer; }
    public String getClub() { return club; }
    public void setClub(String club) { this.club = club; }
    public Position getPosition() { return position; }
    public void setPosition(Position position) { this.position = position; }
    public Tier getTier() { return tier; }
    public void setTier(Tier tier) { this.tier = tier; }
    public short getOverallRating() { return overallRating; }
    public void setOverallRating(short overallRating) { this.overallRating = overallRating; }
    public int getStartingPrice() { return startingPrice; }
    public void setStartingPrice(int startingPrice) { this.startingPrice = startingPrice; }
    public boolean isPenaltyCard() { return penaltyCard; }
    public void setPenaltyCard(boolean penaltyCard) { this.penaltyCard = penaltyCard; }
}
package com.acikarttirma.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

/** Adım 6: kazanan ve beraberlik kuralı. */
class ScoringTest {

    @Test
    void enYuksekToplamReytingKazanir() {
        GameResult r = Scoring.decide(List.of(
                new PlayerScore(1, 800, 10),
                new PlayerScore(2, 850, 0),
                new PlayerScore(3, 790, 500)));
        assertEquals(List.of(2L), r.winnerIds());
        assertFalse(r.isDraw());
    }

    @Test
    void reytingEsitseKalanButceYuksekOlanKazanir() {
        GameResult r = Scoring.decide(List.of(
                new PlayerScore(1, 800, 40),
                new PlayerScore(2, 800, 90),
                new PlayerScore(3, 700, 900)));
        assertEquals(List.of(2L), r.winnerIds());
        assertFalse(r.isDraw());
    }

    @Test
    void reytingVeButceEsitseBeraberlik() {
        GameResult r = Scoring.decide(List.of(
                new PlayerScore(1, 800, 50),
                new PlayerScore(2, 800, 50),
                new PlayerScore(3, 700, 900)));
        assertEquals(List.of(1L, 2L), r.winnerIds());
        assertTrue(r.isDraw());
    }

    @Test
    void ucuDeEsitseUcuKazanir() {
        GameResult r = Scoring.decide(List.of(
                new PlayerScore(1, 800, 50),
                new PlayerScore(2, 800, 50),
                new PlayerScore(3, 800, 50)));
        assertEquals(3, r.winnerIds().size());
        assertTrue(r.isDraw());
    }

    @Test
    void dusukReytiklininButcesiNeKadarYuksekOlursaOlsunKazanamaz() {
        GameResult r = Scoring.decide(List.of(
                new PlayerScore(1, 801, 0),
                new PlayerScore(2, 800, 999)));
        assertEquals(List.of(1L), r.winnerIds());
    }

    @Test
    void bosListeHataVerir() {
        assertThrows(IllegalArgumentException.class, () -> Scoring.decide(List.of()));
    }
}
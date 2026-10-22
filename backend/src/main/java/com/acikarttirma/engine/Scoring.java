package com.acikarttirma.engine;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;

/**
 * Kazananı belirler: en yüksek toplam reyting kazanır; eşitlikte kalan bütçesi yüksek
 * olan; o da eşitse beraberlik (v1).
 */
public final class Scoring {

    private static final Comparator<PlayerScore> BEST_FIRST = Comparator
            .comparingInt(PlayerScore::totalRating)
            .thenComparingInt(PlayerScore::remainingBudget)
            .reversed();

    private Scoring() {
    }

    public static GameResult decide(Collection<PlayerScore> scores) {
        if (scores.isEmpty()) {
            throw new IllegalArgumentException("Skor listesi boş olamaz");
        }
        PlayerScore best = scores.stream().sorted(BEST_FIRST).findFirst().orElseThrow();
        List<Long> winners = scores.stream()
                .filter(s -> s.totalRating() == best.totalRating()
                        && s.remainingBudget() == best.remainingBudget())
                .map(PlayerScore::playerId)
                .toList();
        return new GameResult(winners);
    }
}
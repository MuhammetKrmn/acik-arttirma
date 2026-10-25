package com.acikarttirma.engine;

import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * U19 ceza kartı seçimi. Konseptin o mevkideki havuzundan rastgele bir kart seçer:
 * 1) aynı futbolcu aynı kadroda iki kez olmaz (CB_1 ve CB_2 için de geçerli),
 * 2) mümkünse başka oyunculara verilmemiş bir kart tercih edilir.
 * Havuz yetersizse kurallar sırayla gevşer, oyun kilitlenmez.
 */
public final class U19Picker {

    /** Havuzdaki bir U19 kartı: kart ve futbolcu kimliği. */
    public record Candidate(long cardId, long footballerId) {
    }

    private U19Picker() {
    }

    public static Candidate pick(List<Candidate> pool, Set<Long> ownedFootballerIds,
                                 Set<Long> usedCardIds, Random random) {
        if (pool.isEmpty()) {
            throw new IllegalArgumentException("U19 havuzu boş");
        }
        List<Candidate> notOwned = pool.stream()
                .filter(c -> !ownedFootballerIds.contains(c.footballerId()))
                .toList();
        List<Candidate> base = notOwned.isEmpty() ? pool : notOwned;
        List<Candidate> fresh = base.stream()
                .filter(c -> !usedCardIds.contains(c.cardId()))
                .toList();
        List<Candidate> choices = fresh.isEmpty() ? base : fresh;
        return choices.get(random.nextInt(choices.size()));
    }
}
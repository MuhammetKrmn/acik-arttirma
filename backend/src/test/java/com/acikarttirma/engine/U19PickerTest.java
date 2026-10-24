package com.acikarttirma.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.acikarttirma.engine.U19Picker.Candidate;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** F6: U19 ceza kartı seçim kuralları. */
class U19PickerTest {

    private final List<Candidate> pool = List.of(
            new Candidate(1, 101), new Candidate(2, 102), new Candidate(3, 103));

    @Test
    void kadrodaOlanFutbolcuyuTekrarVermez() {
        for (int seed = 0; seed < 50; seed++) {
            Candidate c = U19Picker.pick(pool, Set.of(101L, 102L), Set.of(), new Random(seed));
            assertEquals(3L, c.cardId());
        }
    }

    @Test
    void mumkunseBaskaOyuncuyaVerilmemisKartiTercihEder() {
        for (int seed = 0; seed < 50; seed++) {
            Candidate c = U19Picker.pick(pool, Set.of(), Set.of(1L, 2L), new Random(seed));
            assertEquals(3L, c.cardId());
        }
    }

    @Test
    void havuzYetmezseKurallarGevserKilitlenmez() {
        Set<Long> seen = new HashSet<>();
        for (int seed = 0; seed < 50; seed++) {
            Candidate c = U19Picker.pick(pool, Set.of(101L, 102L, 103L), Set.of(1L, 2L, 3L), new Random(seed));
            seen.add(c.cardId());
        }
        assertTrue(!seen.isEmpty());
    }
}
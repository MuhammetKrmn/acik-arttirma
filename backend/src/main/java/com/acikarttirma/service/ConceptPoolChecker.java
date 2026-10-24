package com.acikarttirma.service;

import com.acikarttirma.model.Card;
import com.acikarttirma.model.enums.Position;
import com.acikarttirma.model.enums.Tier;
import com.acikarttirma.repository.CardRepository;
import java.util.List;
import org.springframework.stereotype.Component;

/** Bir konsept oyuna uygun mu? Her mevki ve tier'da yeterli kart ve her mevkide U19 havuzu olmalı. */
@Component
public class ConceptPoolChecker {

    private static final int MIN_U19_PER_POSITION = 3;

    private final CardRepository cardRepository;

    public ConceptPoolChecker(CardRepository cardRepository) {
        this.cardRepository = cardRepository;
    }

    public boolean isComplete(Long conceptId) {
        List<Card> cards = cardRepository.findByConcept_IdAndPenaltyCardFalse(conceptId);
        for (Position position : Position.values()) {
            int need = (position == Position.CB) ? 2 : 1; // CB_1 ve CB_2 iki ayrı kart ister
            for (Tier tier : Tier.values()) {
                long count = cards.stream()
                        .filter(c -> c.getPosition() == position && c.getTier() == tier)
                        .count();
                if (count < need) {
                    return false;
                }
            }
            if (cardRepository.findByConcept_IdAndPositionAndPenaltyCardTrue(conceptId, position).size()
                    < MIN_U19_PER_POSITION) {
                return false;
            }
        }
        return true;
    }
}
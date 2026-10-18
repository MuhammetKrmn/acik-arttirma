package com.acikarttirma.repository;

import com.acikarttirma.model.Card;
import com.acikarttirma.model.enums.Position;
import com.acikarttirma.model.enums.Tier;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CardRepository extends JpaRepository<Card, Long> {

    /** Oda başında bir konsept + mevki + tier havuzundan rastgele çekim için havuzun tamamı. */
    List<Card> findByConcept_IdAndPositionAndTierAndPenaltyCardFalse(Long conceptId, Position position, Tier tier);

    /** Konseptin ihale edilebilir tüm kartları (U19 hariç). */
    List<Card> findByConcept_IdAndPenaltyCardFalse(Long conceptId);

    /** Tier 3'te bütçe yetmeyince verilecek U19 kartı (mevki başına tek kart). */
    List<Card> findByPenaltyCardTrue();

    List<Card> findByPositionAndPenaltyCardTrue(Position position);
}
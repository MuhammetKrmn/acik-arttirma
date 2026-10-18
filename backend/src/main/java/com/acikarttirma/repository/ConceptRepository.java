package com.acikarttirma.repository;

import com.acikarttirma.model.Concept;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConceptRepository extends JpaRepository<Concept, Long> {

    Optional<Concept> findByConceptName(String conceptName);

    /** Oda kurarken rastgele konsept seçmek için aktif konseptler. */
    List<Concept> findByActiveTrue();
}
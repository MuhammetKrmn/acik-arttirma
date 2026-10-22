package com.acikarttirma.engine;

import com.acikarttirma.model.enums.AcquisitionType;

/** Tier 3 aşamasında bir oyuncuya ne verildiği: normal T3 kartı mı, U19 ceza kartı mı, kaça. */
public record Tier3Assignment(long playerId, AcquisitionType type, int price) {
}
package com.acikarttirma.engine;

import java.util.List;

/** Kazanan(lar). Birden fazla kişi varsa beraberliktir. Reyting içermez. */
public record GameResult(List<Long> winnerIds) {

    public boolean isDraw() {
        return winnerIds.size() > 1;
    }
}
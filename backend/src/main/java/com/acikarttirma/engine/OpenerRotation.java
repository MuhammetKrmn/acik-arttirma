package com.acikarttirma.engine;

import com.acikarttirma.model.enums.Slot;
import java.util.List;
import java.util.Optional;

/**
 * İhaleyi kimin açacağını belirler: sıra Oyuncu 1, 2, 3, 1... şeklinde döner.
 * Slotu dolu, bütçesi açılış fiyatına yetmeyen veya bağlantısı kopuk oyuncu atlanır.
 * Sıra, ihaleyi GERÇEKTEN açan oyuncunun bir sonrasından devam eder.
 */
public final class OpenerRotation {

    private final List<PlayerState> seats;
    private int next = 0;

    /** Oyuncular koltuk sırasına göre (seat 1, 2, 3) verilir. */
    public OpenerRotation(List<PlayerState> playersInSeatOrder) {
        if (playersInSeatOrder.isEmpty()) {
            throw new IllegalArgumentException("En az bir oyuncu gerekli");
        }
        this.seats = List.copyOf(playersInSeatOrder);
    }

    /** Kimse açamıyorsa boş döner ve sıra yerinde kalır. */
    public Optional<PlayerState> pickOpener(Slot slot, int openingPrice) {
        int n = seats.size();
        for (int i = 0; i < n; i++) {
            int idx = (next + i) % n;
            PlayerState p = seats.get(idx);
            if (p.isConnected() && !p.hasFilled(slot) && p.getBudget() >= openingPrice) {
                next = (idx + 1) % n;
                return Optional.of(p);
            }
        }
        return Optional.empty();
    }
}
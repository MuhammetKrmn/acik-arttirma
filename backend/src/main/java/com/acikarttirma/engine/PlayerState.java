package com.acikarttirma.engine;

import com.acikarttirma.model.enums.Slot;
import java.util.EnumSet;
import java.util.Set;

/** Motorun gördüğü oyuncu: bütçe, dolu slotlar, bağlı mı. Spring/JPA bilmez. */
public final class PlayerState {

    private final long id;
    private int budget;
    private final Set<Slot> filledSlots = EnumSet.noneOf(Slot.class);
    private boolean connected = true;

    public PlayerState(long id, int budget) {
        if (budget < 0) {
            throw new IllegalArgumentException("Bütçe negatif olamaz: " + budget);
        }
        this.id = id;
        this.budget = budget;
    }

    public long getId() {
        return id;
    }

    public int getBudget() {
        return budget;
    }

    public boolean isConnected() {
        return connected;
    }

    public void setConnected(boolean connected) {
        this.connected = connected;
    }

    public boolean hasFilled(Slot slot) {
        return filledSlots.contains(slot);
    }

    public void fillSlot(Slot slot) {
        filledSlots.add(slot);
    }

    public void spend(int amount) {
        if (amount < 0 || amount > budget) {
            throw new IllegalArgumentException("Geçersiz harcama: " + amount + " (bütçe " + budget + ")");
        }
        budget -= amount;
    }
}
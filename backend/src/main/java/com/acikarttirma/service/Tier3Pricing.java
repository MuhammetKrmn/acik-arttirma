package com.acikarttirma.service;

import com.acikarttirma.model.enums.Slot;

/** Tier 3 sabit fiyatı: hücum slotları (LW, ST, RW) 30, diğer tüm slotlar 20 milyon €. */
public final class Tier3Pricing {

    public static final int ATTACK_PRICE = 30;
    public static final int OTHER_PRICE = 20;

    private Tier3Pricing() {
    }

    public static int priceFor(Slot slot) {
        return slot.isAttack() ? ATTACK_PRICE : OTHER_PRICE;
    }
}
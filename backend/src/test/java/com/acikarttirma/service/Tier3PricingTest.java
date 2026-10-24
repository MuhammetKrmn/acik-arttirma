package com.acikarttirma.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.acikarttirma.model.enums.Slot;
import org.junit.jupiter.api.Test;

/** F6: tier 3 sabit fiyat kuralı. */
class Tier3PricingTest {

    @Test
    void hucumSlotlari30() {
        assertEquals(30, Tier3Pricing.priceFor(Slot.LW));
        assertEquals(30, Tier3Pricing.priceFor(Slot.ST));
        assertEquals(30, Tier3Pricing.priceFor(Slot.RW));
    }

    @Test
    void digerTumSlotlar20() {
        for (Slot slot : Slot.values()) {
            if (!slot.isAttack()) {
                assertEquals(20, Tier3Pricing.priceFor(slot));
            }
        }
    }
}
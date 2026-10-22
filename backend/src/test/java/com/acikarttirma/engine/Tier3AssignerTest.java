package com.acikarttirma.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.acikarttirma.model.enums.AcquisitionType;
import com.acikarttirma.model.enums.Slot;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Adım 5: tier 3 otomatik atama ve U19 ceza kartı. */
class Tier3AssignerTest {

    @Test
    void slotuAlanaDokunulmaz() {
        PlayerState owner = new PlayerState(1, 500);
        owner.fillSlot(Slot.ST);
        PlayerState other = new PlayerState(2, 500);

        List<Tier3Assignment> r = Tier3Assigner.assign(Slot.ST, 30, List.of(owner, other));

        assertEquals(1, r.size());
        assertEquals(2, r.get(0).playerId());
        assertEquals(500, owner.getBudget());
    }

    @Test
    void butcesiYetenTier3KartiniOderVeSlotuDolar() {
        PlayerState p = new PlayerState(1, 500);
        List<Tier3Assignment> r = Tier3Assigner.assign(Slot.GK, 20, List.of(p));

        assertEquals(AcquisitionType.TIER3_AUTO, r.get(0).type());
        assertEquals(20, r.get(0).price());
        assertEquals(480, p.getBudget());
        assertTrue(p.hasFilled(Slot.GK));
    }

    @Test
    void butcesiTamFiyatKadarsaYineOder() {
        PlayerState p = new PlayerState(1, 30);
        List<Tier3Assignment> r = Tier3Assigner.assign(Slot.ST, 30, List.of(p));

        assertEquals(AcquisitionType.TIER3_AUTO, r.get(0).type());
        assertEquals(0, p.getBudget());
    }

    @Test
    void butceYetmezseU19CezaKartiVerilirFiyat0() {
        PlayerState p = new PlayerState(1, 29);
        List<Tier3Assignment> r = Tier3Assigner.assign(Slot.ST, 30, List.of(p));

        assertEquals(AcquisitionType.U19_PENALTY, r.get(0).type());
        assertEquals(0, r.get(0).price());
        assertEquals(29, p.getBudget());
        assertTrue(p.hasFilled(Slot.ST));
    }

    @Test
    void u19SadeceOSlotIcinVerilirDigerSlotlaraDokunmaz() {
        PlayerState p = new PlayerState(1, 5);
        Tier3Assigner.assign(Slot.ST, 30, List.of(p));
        assertTrue(p.hasFilled(Slot.ST));
        assertFalse(p.hasFilled(Slot.LW));
        assertFalse(p.hasFilled(Slot.GK));
    }

    @Test
    void slotuOlmayanHerOyuncuIcinAyriAtamaYapilir() {
        PlayerState rich = new PlayerState(1, 500);
        PlayerState poor = new PlayerState(2, 10);
        List<Tier3Assignment> r = Tier3Assigner.assign(Slot.LW, 30, List.of(rich, poor));

        assertEquals(2, r.size());
        assertEquals(AcquisitionType.TIER3_AUTO, r.get(0).type());
        assertEquals(AcquisitionType.U19_PENALTY, r.get(1).type());
    }
}
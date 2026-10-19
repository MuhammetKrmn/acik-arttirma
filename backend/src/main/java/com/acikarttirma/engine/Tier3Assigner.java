package com.acikarttirma.engine;

import com.acikarttirma.model.enums.AcquisitionType;
import com.acikarttirma.model.enums.Slot;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Bir slotun T1 ve T2 ihaleleri bitince, o slotu alamayan oyunculara tier 3 kartını
 * otomatik verir. Bütçe fiyata yetiyorsa fiyat düşer; yetmiyorsa o slot için U19 ceza
 * kartı (fiyat 0) verilir.
 */
public final class Tier3Assigner {

    private Tier3Assigner() {
    }

    public static List<Tier3Assignment> assign(Slot slot, int tier3Price, Collection<PlayerState> players) {
        List<Tier3Assignment> result = new ArrayList<>();
        for (PlayerState p : players) {
            if (p.hasFilled(slot)) {
                continue;
            }
            if (p.getBudget() >= tier3Price) {
                p.spend(tier3Price);
                p.fillSlot(slot);
                result.add(new Tier3Assignment(p.getId(), AcquisitionType.TIER3_AUTO, tier3Price));
            } else {
                p.fillSlot(slot);
                result.add(new Tier3Assignment(p.getId(), AcquisitionType.U19_PENALTY, 0));
            }
        }
        return result;
    }
}
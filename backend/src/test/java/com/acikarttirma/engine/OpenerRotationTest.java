package com.acikarttirma.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.acikarttirma.model.enums.Slot;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Adım 4: ihaleyi kimin açacağı. */
class OpenerRotationTest {

    private PlayerState p1 = new PlayerState(1, 1000);
    private PlayerState p2 = new PlayerState(2, 1000);
    private PlayerState p3 = new PlayerState(3, 1000);

    private OpenerRotation rotation() {
        return new OpenerRotation(List.of(p1, p2, p3));
    }

    private long open(OpenerRotation r, Slot slot, int price) {
        return r.pickOpener(slot, price).orElseThrow().getId();
    }

    @Test
    void sira_1_2_3_1_seklindeDoner() {
        OpenerRotation r = rotation();
        assertEquals(1, open(r, Slot.GK, 20));
        assertEquals(2, open(r, Slot.GK, 20));
        assertEquals(3, open(r, Slot.GK, 20));
        assertEquals(1, open(r, Slot.GK, 20));
    }

    @Test
    void slotuDolanOyuncuAtlanir() {
        p1.fillSlot(Slot.ST);
        OpenerRotation r = rotation();
        assertEquals(2, open(r, Slot.ST, 30)); // sıra 1'deydi, 1 atlandı
    }

    @Test
    void butcesiAcilisaYetmeyenAtlanir() {
        PlayerState poor = new PlayerState(1, 25);
        OpenerRotation r = new OpenerRotation(List.of(poor, p2, p3));
        assertEquals(2, open(r, Slot.ST, 30));
    }

    @Test
    void butcesiTamAcilisKadarOlanAcabilir() {
        PlayerState exact = new PlayerState(1, 30);
        OpenerRotation r = new OpenerRotation(List.of(exact, p2, p3));
        assertEquals(1, open(r, Slot.ST, 30));
    }

    @Test
    void baglantisiKopukOyuncuAtlanir() {
        p1.setConnected(false);
        assertEquals(2, open(rotation(), Slot.GK, 20));
    }

    @Test
    void siraGercekAcanOyuncununSonrasindanDevamEder() {
        p1.fillSlot(Slot.ST);
        OpenerRotation r = rotation();
        assertEquals(2, open(r, Slot.ST, 30)); // 1 atlandı, 2 açtı
        assertEquals(3, open(r, Slot.GK, 20)); // sıra 3'e geçti
        assertEquals(1, open(r, Slot.GK, 20));
    }

    @Test
    void kimseAcamiyorsaBosDonerVeSiraKorunur() {
        OpenerRotation r = rotation();
        assertEquals(1, open(r, Slot.GK, 20));
        p1.fillSlot(Slot.ST);
        p2.fillSlot(Slot.ST);
        p3.fillSlot(Slot.ST);
        assertTrue(r.pickOpener(Slot.ST, 30).isEmpty());
        assertEquals(2, open(r, Slot.GK, 20)); // sıra bozulmadı
    }
}
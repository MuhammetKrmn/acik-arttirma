package com.acikarttirma.model.enums;

/**
 * 4-3-3 kadrosundaki 11 slot. CB_1 ve CB_2 bağımsız iki slottur,
 * ikisi de CB mevkisinden kart alır.
 */
public enum Slot {
    GK(Position.GK),
    LB(Position.LB),
    CB_1(Position.CB),
    CB_2(Position.CB),
    RB(Position.RB),
    CDM(Position.CDM),
    CM(Position.CM),
    CAM(Position.CAM),
    LW(Position.LW),
    ST(Position.ST),
    RW(Position.RW);

    private final Position position;

    Slot(Position position) {
        this.position = position;
    }

    /** Bu slota yerleşecek kartın mevkisi. */
    public Position getPosition() {
        return position;
    }

    /** Hücum slotu mu (LW, ST, RW)? */
    public boolean isAttack() {
        return position == Position.LW || position == Position.ST || position == Position.RW;
    }
}
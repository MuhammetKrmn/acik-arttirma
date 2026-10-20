package com.acikarttirma.model.enums;

/** Kart seviyesi. T1 ve T2 ihaleye çıkar, T3 otomatik atanır. Veritabanında 1, 2, 3 olarak durur. */
public enum Tier {
    T1((short) 1),
    T2((short) 2),
    T3((short) 3);

    private final short value;

    Tier(short value) {
        this.value = value;
    }

    public short getValue() {
        return value;
    }

    public static Tier fromValue(short value) {
        for (Tier t : values()) {
            if (t.value == value) {
                return t;
            }
        }
        throw new IllegalArgumentException("Geçersiz tier değeri: " + value);
    }
}
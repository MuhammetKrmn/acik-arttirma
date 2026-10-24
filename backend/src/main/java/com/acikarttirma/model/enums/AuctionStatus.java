package com.acikarttirma.model.enums;

/**
 * PENDING: sırada, ACTIVE: şu an ihalede, SOLD: satıldı,
 * UNSOLD: kimse açamadı (satılmaz), AUTO_ASSIGNED: tier 3 olarak otomatik atandı.
 */
public enum AuctionStatus {
    PENDING, ACTIVE, SOLD, UNSOLD, AUTO_ASSIGNED
}
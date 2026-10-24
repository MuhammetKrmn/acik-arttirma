package com.acikarttirma.dto;

/** İstemcinin teklif mesajı. expectedPrice: ekranda gördüğü güncel fiyat, increment: 1, 5 veya 10. */
public record BidMessage(int expectedPrice, int increment) {
}
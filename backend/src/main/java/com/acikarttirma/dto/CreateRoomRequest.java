package com.acikarttirma.dto;

/** seed isteğe bağlı: aynı seed aynı konsepti ve aynı kart çekilişini verir (hata ayıklama için). */
public record CreateRoomRequest(Long seed) {
}
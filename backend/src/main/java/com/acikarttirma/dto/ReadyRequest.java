package com.acikarttirma.dto;

/** Hazır olma isteği. Gövde gönderilmezse veya ready boşsa "hazırım" sayılır. */
public record ReadyRequest(Boolean ready) {
}
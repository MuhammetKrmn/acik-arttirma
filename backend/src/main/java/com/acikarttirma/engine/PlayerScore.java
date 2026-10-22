package com.acikarttirma.engine;

/**
 * Oyun sonu skor girdisi. totalRating gizli reytinglerin toplamıdır; bu kayıt SADECE
 * sunucu içinde yaşar, hiçbir DTO/mesaja konmaz.
 */
public record PlayerScore(long playerId, int totalRating, int remainingBudget) {
}
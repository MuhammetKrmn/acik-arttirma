package com.acikarttirma.dto;

/** playerToken gizlidir, sadece sahibine bir kez döner. */
public record GuestResponse(Long userId, String username, String playerToken) {
}
package com.acikarttirma.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record GuestRequest(@NotBlank @Size(min = 2, max = 20) String username) {
}
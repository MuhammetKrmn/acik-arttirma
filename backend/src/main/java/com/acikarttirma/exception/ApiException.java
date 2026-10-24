package com.acikarttirma.exception;

import org.springframework.http.HttpStatus;

/** İstemciye anlamlı HTTP kodu ve hata kodu ile dönen iş kuralı hatası. */
public class ApiException extends RuntimeException {

    private final HttpStatus status;
    private final String code;

    public ApiException(HttpStatus status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }

    public HttpStatus getStatus() { return status; }
    public String getCode() { return code; }
}
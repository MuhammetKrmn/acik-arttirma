package com.acikarttirma.engine;

/** Geri çekilme isteğinin sonucu. */
public record WithdrawResult(boolean accepted, WithdrawRejectReason reason) {

    public static WithdrawResult ok() {
        return new WithdrawResult(true, null);
    }

    public static WithdrawResult rejected(WithdrawRejectReason reason) {
        return new WithdrawResult(false, reason);
    }
}
package com.empresa.security.poc.exception;

import lombok.Getter;

@Getter
public final class CryptoPocException extends RuntimeException {

    private final ErrorCode errorCode;

    public CryptoPocException(String message) {
        this(ErrorCode.CRYPTO_OPERATION_FAILED, message, null);
    }

    public CryptoPocException(String message, Throwable cause) {
        this(ErrorCode.CRYPTO_OPERATION_FAILED, message, cause);
    }

    public CryptoPocException(ErrorCode errorCode, String message) {
        this(errorCode, message, null);
    }

    public CryptoPocException(ErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public enum ErrorCode {
        CONFIGURATION_ERROR,
        INVALID_ENVELOPE,
        ENCRYPTION_ERROR,
        DECRYPTION_ERROR,
        KEY_VAULT_ERROR,
        DATABASE_ERROR,
        CRYPTO_OPERATION_FAILED
    }
}

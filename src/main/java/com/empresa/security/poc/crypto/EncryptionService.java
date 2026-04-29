package com.empresa.security.poc.crypto;

import com.azure.core.credential.TokenCredential;
import com.azure.identity.DefaultAzureCredentialBuilder;
import com.azure.security.keyvault.keys.cryptography.CryptographyClient;
import com.azure.security.keyvault.keys.cryptography.CryptographyClientBuilder;
import com.azure.security.keyvault.keys.cryptography.models.KeyWrapAlgorithm;
import com.azure.security.keyvault.keys.cryptography.models.UnwrapResult;
import com.azure.security.keyvault.keys.cryptography.models.WrapResult;
import com.empresa.security.poc.exception.CryptoPocException;
import com.empresa.security.poc.exception.CryptoPocException.ErrorCode;
import com.empresa.security.poc.model.EncryptedEnvelope;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

public final class EncryptionService {

    public static final String CONTENT_ENCRYPTION_ALGORITHM = "AES-256-GCM";
    public static final String KEY_WRAP_ALGORITHM = "RSA-OAEP-256";

    private static final String AZURE_KEY_ID_ENV = "AZURE_KEY_ID";
    private static final String AES_ALGORITHM = "AES";
    private static final String AES_GCM_TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int AES_256_KEY_LENGTH_BYTES = 32;
    private static final int GCM_TAG_LENGTH_BITS = 128;
    private static final int GCM_IV_LENGTH_BYTES = 12;

    private final SecureRandom secureRandom;
    private final String keyEncryptionKeyId;
    private final TokenCredential credential;
    private final CryptographyClient cryptographyClient;

    public EncryptionService(String keyEncryptionKeyId, TokenCredential credential) {
        this(keyEncryptionKeyId, credential, new SecureRandom());
    }

    public EncryptionService(String keyEncryptionKeyId, TokenCredential credential, SecureRandom secureRandom) {
        if (isBlank(keyEncryptionKeyId)) {
            throw new CryptoPocException(
                    ErrorCode.CONFIGURATION_ERROR,
                    "Defina AZURE_KEY_ID con el identificador versionado de la KEK RSA en Key Vault."
            );
        }
        this.keyEncryptionKeyId = keyEncryptionKeyId;
        this.credential = credential;
        this.secureRandom = secureRandom;
        this.cryptographyClient = new CryptographyClientBuilder()
                .credential(credential)
                .keyIdentifier(keyEncryptionKeyId)
                .buildClient();
    }

    public static EncryptionService fromEnvironment() {
        return new EncryptionService(
                System.getenv(AZURE_KEY_ID_ENV),
                new DefaultAzureCredentialBuilder().build()
        );
    }

    public EncryptedEnvelope encrypt(String value) {
        return encrypt(value, null);
    }

    public EncryptedEnvelope encrypt(String value, byte[] additionalAuthenticatedData) {
        if (isBlank(value)) {
            throw new CryptoPocException(ErrorCode.INVALID_ENVELOPE, "El texto a cifrar no puede estar vacio.");
        }

        byte[] dek = randomBytes(AES_256_KEY_LENGTH_BYTES);
        byte[] iv = randomBytes(GCM_IV_LENGTH_BYTES);
        byte[] wrappedDek = null;

        try {
            byte[] ciphertext = encryptAesGcm(value.getBytes(StandardCharsets.UTF_8), dek, iv, additionalAuthenticatedData);
            WrapResult wrapResult = cryptographyClient.wrapKey(KeyWrapAlgorithm.RSA_OAEP_256, dek);
            wrappedDek = wrapResult.getEncryptedKey();

            return new EncryptedEnvelope(
                    encode(ciphertext),
                    encode(wrappedDek),
                    encode(iv),
                    keyEncryptionKeyId,
                    KEY_WRAP_ALGORITHM,
                    CONTENT_ENCRYPTION_ALGORITHM,
                    GCM_TAG_LENGTH_BITS,
                    "1"
            );
        } catch (CryptoPocException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            throw new CryptoPocException(ErrorCode.KEY_VAULT_ERROR, "Key Vault no pudo envolver la DEK.", ex);
        } finally {
            wipe(dek);
            wipe(wrappedDek);
        }
    }

    public String decrypt(EncryptedEnvelope envelope) {
        return decrypt(envelope, null);
    }

    public String decrypt(EncryptedEnvelope envelope, byte[] additionalAuthenticatedData) {
        validateEnvelope(envelope);

        byte[] wrappedDek = decode(envelope.getWrappedDek(), "wrappedDek");
        byte[] dek = null;

        try {
            String unwrapKeyId = isBlank(envelope.getKeyEncryptionKeyId())
                    ? keyEncryptionKeyId
                    : envelope.getKeyEncryptionKeyId();
            UnwrapResult unwrapResult = cryptographyClientFor(unwrapKeyId).unwrapKey(KeyWrapAlgorithm.RSA_OAEP_256, wrappedDek);
            dek = unwrapResult.getKey();
            byte[] plaintext = decryptAesGcm(
                    decode(envelope.getCiphertext(), "ciphertext"),
                    dek,
                    decode(envelope.getIv(), "iv"),
                    additionalAuthenticatedData
            );
            return new String(plaintext, StandardCharsets.UTF_8);
        } catch (CryptoPocException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            throw new CryptoPocException(ErrorCode.KEY_VAULT_ERROR, "Key Vault no pudo desenvolver la DEK.", ex);
        } finally {
            wipe(wrappedDek);
            wipe(dek);
        }
    }

    private byte[] encryptAesGcm(byte[] plaintext, byte[] dek, byte[] iv, byte[] aad) {
        try {
            Cipher cipher = Cipher.getInstance(AES_GCM_TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(dek, AES_ALGORITHM), new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv));
            applyAad(cipher, aad);
            return cipher.doFinal(plaintext);
        } catch (GeneralSecurityException ex) {
            throw new CryptoPocException(ErrorCode.ENCRYPTION_ERROR, "No fue posible cifrar con AES-256-GCM.", ex);
        }
    }

    private byte[] decryptAesGcm(byte[] ciphertext, byte[] dek, byte[] iv, byte[] aad) {
        try {
            Cipher cipher = Cipher.getInstance(AES_GCM_TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(dek, AES_ALGORITHM), new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv));
            applyAad(cipher, aad);
            return cipher.doFinal(ciphertext);
        } catch (GeneralSecurityException ex) {
            throw new CryptoPocException(ErrorCode.DECRYPTION_ERROR, "No fue posible descifrar con AES-256-GCM.", ex);
        }
    }

    private byte[] randomBytes(int length) {
        byte[] bytes = new byte[length];
        secureRandom.nextBytes(bytes);
        return bytes;
    }

    private CryptographyClient cryptographyClientFor(String keyIdentifier) {
        return new CryptographyClientBuilder()
                .credential(credential)
                .keyIdentifier(keyIdentifier)
                .buildClient();
    }

    private static void validateEnvelope(EncryptedEnvelope envelope) {
        if (envelope == null) {
            throw new CryptoPocException(ErrorCode.INVALID_ENVELOPE, "El envelope cifrado no puede ser nulo.");
        }
        requireField(envelope.getCiphertext(), "ciphertext");
        requireField(envelope.getWrappedDek(), "wrappedDek");
        requireField(envelope.getIv(), "iv");
    }

    private static void requireField(String value, String fieldName) {
        if (isBlank(value)) {
            throw new CryptoPocException(ErrorCode.INVALID_ENVELOPE, "El campo " + fieldName + " es obligatorio.");
        }
    }

    private static byte[] decode(String value, String fieldName) {
        try {
            return Base64.getDecoder().decode(value);
        } catch (IllegalArgumentException ex) {
            throw new CryptoPocException(ErrorCode.INVALID_ENVELOPE, "El campo " + fieldName + " no contiene Base64 valido.", ex);
        }
    }

    private static String encode(byte[] value) {
        return Base64.getEncoder().encodeToString(value);
    }

    private static void applyAad(Cipher cipher, byte[] aad) {
        if (aad != null && aad.length > 0) {
            cipher.updateAAD(aad);
        }
    }

    private static void wipe(byte[] value) {
        if (value != null) {
            Arrays.fill(value, (byte) 0);
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}

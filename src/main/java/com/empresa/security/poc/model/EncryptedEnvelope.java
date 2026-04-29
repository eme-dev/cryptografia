package com.empresa.security.poc.model;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.empresa.security.poc.exception.CryptoPocException;
import com.empresa.security.poc.exception.CryptoPocException.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.io.IOException;

@Getter
@AllArgsConstructor
public final class EncryptedEnvelope {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final String DEFAULT_SCHEMA_VERSION = "1";
    private static final int DEFAULT_TAG_LENGTH_BITS = 128;

    private final String ciphertext;
    private final String wrappedDek;
    private final String iv;
    private final String keyEncryptionKeyId;
    private final String keyWrapAlgorithm;
    private final String contentEncryptionAlgorithm;
    private final int tagLengthBits;
    private final String schemaVersion;

    public EncryptedEnvelope(String ciphertext, String wrappedDek, String iv) {
        this(ciphertext, wrappedDek, iv, null, null, null, DEFAULT_TAG_LENGTH_BITS, DEFAULT_SCHEMA_VERSION);
    }

    public static EncryptedEnvelope fromJson(String json) {
        if (json == null || json.isBlank()) {
            throw new CryptoPocException(ErrorCode.INVALID_ENVELOPE, "La columna datamap no contiene un JSON valido.");
        }

        try {
            JsonNode node = OBJECT_MAPPER.readTree(json);
            return new EncryptedEnvelope(
                    text(node, "ciphertext"),
                    text(node, "wrappedDek"),
                    text(node, "iv"),
                    text(node, "keyEncryptionKeyId"),
                    text(node, "keyWrapAlgorithm"),
                    text(node, "contentEncryptionAlgorithm"),
                    node.path("tagLengthBits").asInt(DEFAULT_TAG_LENGTH_BITS),
                    textOrDefault(node, "schemaVersion", DEFAULT_SCHEMA_VERSION)
            );
        } catch (IOException ex) {
            throw new CryptoPocException(ErrorCode.INVALID_ENVELOPE, "La columna datamap no contiene un JSON valido.", ex);
        }
    }

    public String toJson() {
        StringBuilder json = new StringBuilder();
        json.append("{\n");
        appendStringField(json, "schemaVersion", schemaVersion, true);
        appendStringField(json, "contentEncryptionAlgorithm", contentEncryptionAlgorithm, true);
        appendStringField(json, "keyWrapAlgorithm", keyWrapAlgorithm, true);
        appendStringField(json, "keyEncryptionKeyId", keyEncryptionKeyId, true);
        json.append("  \"tagLengthBits\": ").append(tagLengthBits).append(",\n");
        appendStringField(json, "ciphertext", ciphertext, true);
        appendStringField(json, "iv", iv, true);
        appendStringField(json, "wrappedDek", wrappedDek, false);
        json.append("}");
        return json.toString();
    }

    private static void appendStringField(StringBuilder json, String name, String value, boolean trailingComma) {
        json.append("  \"")
                .append(name)
                .append("\": \"")
                .append(escapeJson(value))
                .append("\"");
        if (trailingComma) {
            json.append(",");
        }
        json.append("\n");
    }

    private static String escapeJson(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static String text(JsonNode node, String fieldName) {
        JsonNode value = node.get(fieldName);
        if (value == null || value.isNull() || value.asText().isBlank()) {
            throw new CryptoPocException(ErrorCode.INVALID_ENVELOPE, "El campo " + fieldName + " no existe en datamap.");
        }
        return value.asText();
    }

    private static String textOrDefault(JsonNode node, String fieldName, String defaultValue) {
        JsonNode value = node.get(fieldName);
        if (value == null || value.isNull() || value.asText().isBlank()) {
            return defaultValue;
        }
        return value.asText();
    }
}

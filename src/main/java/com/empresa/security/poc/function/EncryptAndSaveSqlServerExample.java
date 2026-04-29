package com.empresa.security.poc.function;

import com.empresa.security.poc.crypto.EncryptionService;
import com.empresa.security.poc.model.EncryptedEnvelope;
import com.empresa.security.poc.repository.SqlServerEnvelopeRepository;

import java.util.UUID;

public final class EncryptAndSaveSqlServerExample {

    private static final int TARGET_JSON_LENGTH = 20_000;

    private EncryptAndSaveSqlServerExample() {
    }

    public static void main(String[] args) {
        EncryptionService encryptionService = EncryptionService.fromEnvironment();
        SqlServerEnvelopeRepository repository = SqlServerEnvelopeRepository.fromEnvironment();

        repository.createTableIfNotExists();

        String businessId = args.length > 0 ? args[0] : "C-1001";
        String value = generateLargeJson(businessId, TARGET_JSON_LENGTH);

        EncryptedEnvelope envelope = encryptionService.encrypt(value);
        UUID id = repository.save(businessId, envelope);

        System.out.println("Envelope cifrado guardado en SQL Server.");
        System.out.println("Id=" + id);
        System.out.println("BusinessId=" + businessId);
        System.out.println("PlaintextLength=" + value.length());
    }

    private static String generateLargeJson(String businessId, int targetLength) {
        StringBuilder json = new StringBuilder();
        json.append("{\n");
        json.append("  \"customerId\": \"").append(businessId).append("\",\n");
        json.append("  \"documentNumber\": \"45879632\",\n");
        json.append("  \"customerType\": \"PERSONA_NATURAL\",\n");
        json.append("  \"segment\": \"PREFERENTE\",\n");
        json.append("  \"currency\": \"PEN\",\n");
        json.append("  \"account\": {\n");
        json.append("    \"accountNumber\": \"001-220-778899\",\n");
        json.append("    \"product\": \"CUENTA_AHORROS\",\n");
        json.append("    \"branch\": \"LIMA-CENTRO\",\n");
        json.append("    \"status\": \"ACTIVE\"\n");
        json.append("  },\n");
        json.append("  \"transactions\": [\n");

        for (int i = 1; i <= 80; i++) {
            json.append("    {");
            json.append("\"operationId\": \"OP-").append(String.format("%05d", i)).append("\", ");
            json.append("\"date\": \"2026-04-").append(String.format("%02d", ((i - 1) % 28) + 1)).append("\", ");
            json.append("\"channel\": \"").append(channelFor(i)).append("\", ");
            json.append("\"merchant\": \"COMERCIO_").append(String.format("%03d", i)).append("\", ");
            json.append("\"amount\": ").append(100 + (i * 7)).append(".").append(String.format("%02d", i % 100)).append(", ");
            json.append("\"description\": \"Movimiento generado para prueba de cifrado envelope con SQL Server\"");
            json.append("}");
            if (i < 80) {
                json.append(",");
            }
            json.append("\n");
        }

        json.append("  ],\n");
        json.append("  \"riskProfile\": {\n");
        json.append("    \"score\": 742,\n");
        json.append("    \"level\": \"LOW\",\n");
        json.append("    \"lastReviewDate\": \"2026-04-28\"\n");
        json.append("  },\n");
        json.append("  \"payloadPadding\": \"");

        String suffix = "\"\n}";
        int paddingLength = targetLength - json.length() - suffix.length();
        if (paddingLength < 0) {
            throw new IllegalStateException("El JSON base supera el tamano objetivo de " + targetLength + " caracteres.");
        }
        appendPadding(json, paddingLength);
        json.append(suffix);

        return json.toString();
    }

    private static String channelFor(int index) {
        switch (index % 4) {
            case 0:
                return "WEB";
            case 1:
                return "MOBILE";
            case 2:
                return "ATM";
            default:
                return "BRANCH";
        }
    }

    private static void appendPadding(StringBuilder json, int length) {
        String alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        for (int i = 0; i < length; i++) {
            json.append(alphabet.charAt(i % alphabet.length()));
        }
    }
}

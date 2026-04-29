package com.empresa.security.poc.function;

import com.empresa.security.poc.crypto.EncryptionService;
import com.empresa.security.poc.model.EncryptedEnvelope;

public final class EncryptJsonFunctionExample {

    private EncryptJsonFunctionExample() {
    }

    public static void main(String[] args) {
        EncryptionService service = EncryptionService.fromEnvironment();
        String businessJson = "{\n"
                + "  \"customerId\": \"C-1001\",\n"
                + "  \"documentNumber\": \"45879632\",\n"
                + "  \"amount\": 1500.75,\n"
                + "  \"currency\": \"PEN\"\n"
                + "}";

        EncryptedEnvelope envelope = service.encrypt(businessJson);

        System.out.println("Guardar este JSON serializado en SQL nvarchar(max):");
        System.out.println(envelope.toJson());
    }
}

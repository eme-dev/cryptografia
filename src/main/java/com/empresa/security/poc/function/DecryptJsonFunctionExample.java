package com.empresa.security.poc.function;

import com.empresa.security.poc.crypto.EncryptionService;
import com.empresa.security.poc.model.EncryptedEnvelope;

public final class DecryptJsonFunctionExample {

    private DecryptJsonFunctionExample() {
    }

    public static void main(String[] args) {
        EncryptionService service = EncryptionService.fromEnvironment();
        EncryptedEnvelope envelope = new EncryptedEnvelope(
                "zP9ZFJnLF6CE3f4F9luuGkfWSQ19ViU3FBsrjw4+gIgWS3H16z4lAl4/jD20XvyCp9Z8LG9A+uZdZwLyNe6U7HyD3RcnBYzm0SNok2P5Gb9QVQ76OYh+FYsD+4Y49Ou8y4BLIvgCf1JTODD+a+jmsNiSFAqaZg==",
                "UJSan7z/YkYR+mw5vJ0F++ARpBvd459UKR0s4G3mWujdAvDL9opK98JeyVgNL0PBXPcN1EYZfQWOaPrunjupTXClM1eny+lLFnTss4/nFRWICrvdJIgld6qaLbkmC764yC6MWzDrhvJ5PTweiyC4focOoXBw1VWbGTkfvBx3P87Xi1X6qfB9uUezCJuJzdNVX7CYvrcKpcOaiGzEVdZrYrdrMglZrqKVlXPJSX+3LW6ZnE/veAJL+WVQ/j+LfFLED3kxkwLgvMvTkGh925UfnWM4fHJb0z/llUFnuFvpzrKE7i37LKQi6GY3mI5gfQ49idXVvYj+11hgVNxAS1TgCQ==",
                "jRVuEnRlXkx22WV9",
                "https://kv-crypto-fn-east.vault.azure.net/keys/kek-json-dev",
                "RSA-OAEP-256",
                "AES-256-GCM",
                128,
                "1"
        );

        String json = service.decrypt(envelope);
        System.out.println("JSON recuperado:");
        System.out.println(json);
    }
}

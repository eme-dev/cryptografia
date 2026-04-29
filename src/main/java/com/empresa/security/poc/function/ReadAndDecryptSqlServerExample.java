package com.empresa.security.poc.function;

import com.empresa.security.poc.crypto.EncryptionService;
import com.empresa.security.poc.exception.CryptoPocException;
import com.empresa.security.poc.exception.CryptoPocException.ErrorCode;
import com.empresa.security.poc.model.EncryptedEnvelope;
import com.empresa.security.poc.repository.SqlServerEnvelopeRepository;

public final class ReadAndDecryptSqlServerExample {

    private ReadAndDecryptSqlServerExample() {
    }

    public static void main(String[] args) {
        EncryptionService encryptionService = EncryptionService.fromEnvironment();
        SqlServerEnvelopeRepository repository = SqlServerEnvelopeRepository.fromEnvironment();

        String businessId = args.length > 0 ? args[0] : "C-1001";
        EncryptedEnvelope envelope = repository.findLatestByBusinessId(businessId)
                .orElseThrow(() -> new CryptoPocException(
                        ErrorCode.DATABASE_ERROR,
                        "No existe un envelope cifrado para BusinessId=" + businessId
                ));

        String value = encryptionService.decrypt(envelope);

        System.out.println("Payload descifrado:");
        System.out.println(value);
    }
}

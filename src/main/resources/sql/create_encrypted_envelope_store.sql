IF OBJECT_ID('dbo.EncryptedEnvelopeStore', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.EncryptedEnvelopeStore (
        Id UNIQUEIDENTIFIER NOT NULL PRIMARY KEY,
        BusinessId NVARCHAR(100) NOT NULL,
        datamap NVARCHAR(MAX) NOT NULL,
        CreatedAtUtc DATETIME2 NOT NULL
            CONSTRAINT DF_EncryptedEnvelopeStore_CreatedAtUtc DEFAULT SYSUTCDATETIME()
    );

    CREATE INDEX IX_EncryptedEnvelopeStore_BusinessId_CreatedAtUtc
        ON dbo.EncryptedEnvelopeStore (BusinessId, CreatedAtUtc DESC);
END;

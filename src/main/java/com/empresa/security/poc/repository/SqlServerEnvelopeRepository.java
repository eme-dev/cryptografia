package com.empresa.security.poc.repository;

import com.empresa.security.poc.exception.CryptoPocException;
import com.empresa.security.poc.exception.CryptoPocException.ErrorCode;
import com.empresa.security.poc.model.EncryptedEnvelope;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Optional;
import java.util.UUID;

public final class SqlServerEnvelopeRepository {

    private static final String SQLSERVER_CONNECTION_STRING_ENV = "SQLSERVER_CONNECTION_STRING";
    private static final String SQL_CONNECTION_STRING_ENV = "SQL_CONNECTION_STRING";

    private final String connectionString;

    public SqlServerEnvelopeRepository(String connectionString) {
        if (connectionString == null || connectionString.isBlank()) {
            throw new CryptoPocException(
                    ErrorCode.CONFIGURATION_ERROR,
                    "Defina SQLSERVER_CONNECTION_STRING con la cadena JDBC de SQL Server."
            );
        }
        this.connectionString = connectionString;
    }

    public static SqlServerEnvelopeRepository fromEnvironment() {
        String connectionString = System.getenv(SQLSERVER_CONNECTION_STRING_ENV);
        if (connectionString == null || connectionString.isBlank()) {
            connectionString = System.getenv(SQL_CONNECTION_STRING_ENV);
        }
        return new SqlServerEnvelopeRepository(connectionString);
    }

    public void createTableIfNotExists() {
        String sql = "IF OBJECT_ID('dbo.EncryptedEnvelopeStore', 'U') IS NULL "
                + "BEGIN "
                + "CREATE TABLE dbo.EncryptedEnvelopeStore ("
                + "Id UNIQUEIDENTIFIER NOT NULL PRIMARY KEY,"
                + "BusinessId NVARCHAR(100) NOT NULL,"
                + "datamap NVARCHAR(MAX) NOT NULL,"
                + "CreatedAtUtc DATETIME2 NOT NULL CONSTRAINT DF_EncryptedEnvelopeStore_CreatedAtUtc DEFAULT SYSUTCDATETIME()"
                + "); "
                + "CREATE INDEX IX_EncryptedEnvelopeStore_BusinessId_CreatedAtUtc "
                + "ON dbo.EncryptedEnvelopeStore (BusinessId, CreatedAtUtc DESC); "
                + "END";

        try (Connection connection = openConnection();
             Statement statement = connection.createStatement()) {
            statement.execute(sql);
        } catch (SQLException ex) {
            throw new CryptoPocException(ErrorCode.DATABASE_ERROR, "No fue posible crear la tabla de envelopes cifrados.", ex);
        }
    }

    public UUID save(String businessId, EncryptedEnvelope envelope) {
        UUID id = UUID.randomUUID();
        save(id, businessId, envelope);
        return id;
    }

    public void save(UUID id, String businessId, EncryptedEnvelope envelope) {
        validateBusinessId(businessId);
        validateEnvelope(envelope);

        String sql = "INSERT INTO dbo.EncryptedEnvelopeStore (Id, BusinessId, datamap) VALUES (?, ?, ?)";

        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, id.toString());
            statement.setString(2, businessId);
            statement.setString(3, envelope.toJson());
            statement.executeUpdate();
        } catch (SQLException ex) {
            throw new CryptoPocException(ErrorCode.DATABASE_ERROR, "No fue posible guardar el envelope cifrado.", ex);
        }
    }

    public Optional<EncryptedEnvelope> findById(UUID id) {
        String sql = "SELECT TOP (1) datamap FROM dbo.EncryptedEnvelopeStore WHERE Id = ?";

        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, id.toString());
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }
                return Optional.of(EncryptedEnvelope.fromJson(resultSet.getString("datamap")));
            }
        } catch (SQLException ex) {
            throw new CryptoPocException(ErrorCode.DATABASE_ERROR, "No fue posible leer el envelope cifrado por Id.", ex);
        }
    }

    public Optional<EncryptedEnvelope> findLatestByBusinessId(String businessId) {
        validateBusinessId(businessId);

        String sql = "SELECT TOP (1) datamap FROM dbo.EncryptedEnvelopeStore "
                + "WHERE BusinessId = ? "
                + "ORDER BY CreatedAtUtc DESC";

        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, businessId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }
                return Optional.of(EncryptedEnvelope.fromJson(resultSet.getString("datamap")));
            }
        } catch (SQLException ex) {
            throw new CryptoPocException(ErrorCode.DATABASE_ERROR, "No fue posible leer el envelope cifrado por BusinessId.", ex);
        }
    }

    private Connection openConnection() throws SQLException {
        return DriverManager.getConnection(connectionString);
    }

    private static void validateBusinessId(String businessId) {
        if (businessId == null || businessId.isBlank()) {
            throw new CryptoPocException(ErrorCode.INVALID_ENVELOPE, "BusinessId es obligatorio.");
        }
    }

    private static void validateEnvelope(EncryptedEnvelope envelope) {
        if (envelope == null) {
            throw new CryptoPocException(ErrorCode.INVALID_ENVELOPE, "El envelope cifrado no puede ser nulo.");
        }
    }
}

package ch.liubai.upload.metadata;

import ch.liubai.upload.domain.UploadDescriptor;
import ch.liubai.upload.entity.FileMetadata;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;

/**
 * MySQL implementation of the metadata storage Strategy.
 *
 * Connections are acquired per operation and always returned to the pool.
 */
public class DatabaseFileMetadataStorage implements FileMetadataStorage {

    private static final String UPSERT_SQL =
            "INSERT INTO file_metadata (file_name, sha256, file_size) VALUES (?, ?, ?) " +
            "ON DUPLICATE KEY UPDATE file_name = VALUES(file_name), " +
            "file_size = VALUES(file_size), update_time = CURRENT_TIMESTAMP";

    private final DataSource dataSource;

    public DatabaseFileMetadataStorage(DataSource dataSource) {
        if (dataSource == null) {
            throw new IllegalArgumentException("MySQL存储模式需要DataSource");
        }
        this.dataSource = dataSource;
    }

    @Override
    public void addFileMetadata(FileMetadata metadata) throws Exception {
        if (metadata == null || !UploadDescriptor.isValidSha256(metadata.getSha256())) {
            throw new IllegalArgumentException("文件元数据或sha256非法");
        }
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(UPSERT_SQL)) {
            statement.setString(1, metadata.getFileName());
            statement.setString(2, metadata.getSha256().toLowerCase());
            statement.setLong(3, metadata.getFileSize());
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new Exception("Error saving file metadata", e);
        }
    }

    @Override
    public FileMetadata loadFileMetadata(String sha256) throws Exception {
        if (!UploadDescriptor.isValidSha256(sha256)) {
            throw new IllegalArgumentException("sha256必须是64位十六进制字符串");
        }
        String sql = "SELECT id, file_name, sha256, file_size, create_time, update_time " +
                "FROM file_metadata WHERE sha256 = ?";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, sha256.toLowerCase());
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return null;
                }
                return new FileMetadata(
                        resultSet.getInt("id"),
                        resultSet.getString("file_name"),
                        toLocalDateTime(resultSet.getTimestamp("create_time")),
                        toLocalDateTime(resultSet.getTimestamp("update_time")),
                        resultSet.getString("sha256"),
                        resultSet.getLong("file_size")
                );
            }
        } catch (SQLException e) {
            throw new Exception("Error loading file metadata", e);
        }
    }

    private static LocalDateTime toLocalDateTime(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }
}

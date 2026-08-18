package ch.liubai.upload.metadata;

import ch.liubai.upload.entity.FileMetadata;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * 数据库文件元数据存储
 *
 * @author ljh
 * @version 1.0
 * @since 2024/1/5 16:27
 */
public class DatabaseFileMetadataStorage implements FileMetadataStorage {

    private final Connection connection;

    public DatabaseFileMetadataStorage(Connection connection) {
        this.connection = connection;
    }

    @Override
    public void addFileMetadata(FileMetadata metadata) throws Exception {
        String sql = "INSERT INTO file_metadata (file_name, sha256, file_size) VALUES (?, ?, ?)";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, metadata.getFileName());
            statement.setString(2, metadata.getSha256());
            statement.setLong(3, metadata.getFileSize());

            statement.executeUpdate();
        } catch (SQLException e) {
            throw new Exception("Error saving file metadata", e);
        }
    }

    @Override
    public FileMetadata loadFileMetadata(String sha256) throws Exception {
        String sql = "SELECT file_name, sha256, file_size FROM file_metadata WHERE sha256 = ? ";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, sha256);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return new FileMetadata(
                            resultSet.getString("file_name"),
                            resultSet.getString("sha256"),
                            resultSet.getLong("file_size")
                    );
                } else {
                    return null;
                }
            }
        } catch (SQLException e) {
            throw new Exception("Error loading file metadata", e);
        }
    }
}

package ch.liubai.upload;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.Statement;
import java.util.Locale;

/**
 * Initializes the MySQL schema while keeping connection ownership local to each operation.
 */
final class DatabaseSchemaInitializer {

    private static final String CREATE_TABLE_SQL =
            "CREATE TABLE IF NOT EXISTS file_metadata (" +
            "id INT AUTO_INCREMENT PRIMARY KEY COMMENT '主键'," +
            "file_name VARCHAR(255) NOT NULL COMMENT '文件名'," +
            "sha256 VARCHAR(64) NOT NULL COMMENT '文件sha256'," +
            "file_size BIGINT COMMENT '文件大小(字节)'," +
            "create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'," +
            "update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'," +
            "UNIQUE KEY sha256_UNIQUE_IDX (sha256)" +
            ") COMMENT '文件元数据表'";

    private final DataSource dataSource;

    DatabaseSchemaInitializer(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    boolean isMySqlAvailable() {
        if (dataSource == null) {
            return false;
        }
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metadata = connection.getMetaData();
            String productName = metadata == null ? null : metadata.getDatabaseProductName();
            if (productName == null) {
                return false;
            }
            String normalizedName = productName.toLowerCase(Locale.ROOT);
            return normalizedName.contains("mysql") || normalizedName.contains("mariadb");
        } catch (Exception ignored) {
            return false;
        }
    }

    void initialize() {
        if (dataSource == null) {
            throw new IllegalStateException("MySQL存储模式需要DataSource");
        }
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute(CREATE_TABLE_SQL);
        } catch (Exception e) {
            throw new IllegalStateException("初始化file_metadata表失败", e);
        }
    }
}

package ch.liubai.upload;

import ch.liubai.upload.service.FileService;
import ch.liubai.upload.controller.FileController;
import ch.liubai.upload.metadata.DatabaseFileMetadataStorage;
import ch.liubai.upload.metadata.FileMetadataProperties;
import ch.liubai.upload.metadata.FileMetadataStorage;
import ch.liubai.upload.metadata.LocalFileMetadataStorage;
import ch.liubai.upload.service.impl.FileServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * 文件元数据存储工厂
 *
 * @author ljh
 * @version 1.0
 * @since 2024/1/5 16:37
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(FileMetadataWrapper.class)
public class FileMetadataStorageFactory {

    private static final Logger log = LoggerFactory.getLogger(FileMetadataStorageFactory.class);


    @Autowired(required = false)
    private DataSource dataSource;


    public static final String FILE_TABLE_NAME = "file_metadata";

    @Bean
    public FileController registerFileController(FileMetadataProperties properties) throws SQLException {
        return new FileController(getFileService(properties, getFileMetadataStorage(properties)));
    }

    /**
     * 获取文件服务
     *
     * @return 文件服务
     */
    public FileService getFileService(FileMetadataProperties properties, FileMetadataStorage fileMetadataStorage) {
        return new FileServiceImpl(properties, fileMetadataStorage);
    }


    /**
     * 根据类型获取文件元数据存储
     *
     * @return 文件元数据存储
     */
    public FileMetadataStorage getFileMetadataStorage(FileMetadataProperties properties) throws SQLException {
        String storageType = properties.getStorageType();
        if (storageType != null && !storageType.isEmpty()) {
            return createStorage(storageType, properties.getMetadataDir());
        } else {
            return createStorage(properties.getMetadataDir());
        }
    }

    /**
     * 根据类型获取文件元数据存储
     *
     * @param type 存储类型
     * @return 文件元数据存储
     */
    public FileMetadataStorage createStorage(String type, String dir) throws SQLException {
        if ("local".equalsIgnoreCase(type)) {
            return createFileStorage(dir);
        } else if ("mysql".equalsIgnoreCase(type)) {
            return new DatabaseFileMetadataStorage(dataSource == null ? null : dataSource.getConnection());
        } else {
            throw new IllegalArgumentException("不支持的文件元数据存储类型：" + type);
        }
    }

    /**
     * 创建文件元数据存储
     *
     * @return 文件元数据存储
     */
    public FileMetadataStorage createStorage(String dir) throws SQLException {
        // 检查数据库是否可用
        if (isDatabaseAvailable()) {
            // 检查数据库中是否存在文件元数据表
            if (!isTableExists(FILE_TABLE_NAME)) {
                // 如果不存在，则自动创建文件元数据表
                log.info("【upload-file】文件元数据表不存在，自动创建文件元数据表：{}",FILE_TABLE_NAME);
                createTable();
            }
            return new DatabaseFileMetadataStorage(dataSource == null ? null : dataSource.getConnection());
        }
        // 默认使用本地文件存储
        return createFileStorage(dir);
    }

    /**
     * 创建文件元数据存储
     *
     * @param dirName 文件元数据存储目录
     * @return 文件元数据存储
     */
    public FileMetadataStorage createFileStorage(String dirName) {
        // 如果 dirName 不为空，则创建 LocalFileMetadataStorage 实例
        if (dirName != null && !dirName.isEmpty()) {
            return new LocalFileMetadataStorage(dirName);
        }

        // 如果 dirName 为空，则在用户的主目录下创建一个隐藏目录作为默认路径
        String defaultPath = System.getProperty("user.home") + "/.file_metadata";

        // 检查默认路径是否存在，如果不存在，尝试创建
        Path path = Paths.get(defaultPath);
        if (!Files.exists(path)) {
            try {
                Files.createDirectories(path);
                log.info("【upload-file】创建默认元数据路径：{}", defaultPath);
            } catch (IOException e) {
                throw new RuntimeException("无法在默认路径下创建目录：" + defaultPath, e);
            }
        }

        // 检查是否有足够的权限
        if (!Files.isWritable(path)) {
            throw new RuntimeException("没有在默认路径下写入文件的权限：" + defaultPath);
        }

        // 创建并返回 LocalFileMetadataStorage 实例
        return new LocalFileMetadataStorage(defaultPath);
    }

    /**
     * 检查表是否存在
     *
     * @param tableName 表名
     * @return 表是否存在
     */
    public boolean isTableExists(String tableName) {
        try (Statement statement = dataSource.getConnection().createStatement();
             ResultSet resultSet = statement.executeQuery("SHOW TABLES LIKE '" + tableName + "'")) {
            return resultSet.next();
        } catch (Exception e) {
            throw new RuntimeException("Error while checking if table exists", e);
        }
    }

    /**
     * 创建文件元数据表
     */
    public void createTable() {
        try (Statement statement = dataSource.getConnection().createStatement()) {
            statement.execute("CREATE TABLE file_metadata\n" +
                    "(\n" +
                    "    id          INT AUTO_INCREMENT PRIMARY KEY comment '主键',\n" +
                    "    file_name   VARCHAR(255) NOT NULL comment '文件名',\n" +
                    "    sha256      VARCHAR(64)  NOT NULL comment '文件sha256',\n" +
                    "    file_size   BIGINT comment '文件大小(字节)',\n" +
                    "    create_time TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',\n" +
                    "    update_time TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',\n" +
                    "    UNIQUE KEY sha256_UNIQUE_IDX (sha256) comment 'sha256唯一索引'\n" +
                    ") comment '文件元数据表';\n");
        } catch (Exception e) {
            throw new RuntimeException("Error while creating table", e);
        }
    }

    /**
     * 检查数据库是否可用
     *
     * @return 数据库是否可用 true：可用；false：不可用
     */
    public boolean isDatabaseAvailable() {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("SELECT 1")) {
            return resultSet.next();
        } catch (Exception e) {
            return false;
        }
    }

}

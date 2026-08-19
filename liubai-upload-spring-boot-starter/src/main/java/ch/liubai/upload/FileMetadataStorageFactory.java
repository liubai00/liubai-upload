package ch.liubai.upload;

import ch.liubai.upload.metadata.DatabaseFileMetadataStorage;
import ch.liubai.upload.metadata.FileMetadataProperties;
import ch.liubai.upload.metadata.FileMetadataStorage;
import ch.liubai.upload.metadata.LocalFileMetadataStorage;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.sql.DataSource;
import java.util.Locale;

/**
 * Factory for selecting a metadata storage Strategy.
 */
public class FileMetadataStorageFactory {

    private final DataSource dataSource;
    private final ObjectMapper objectMapper;
    private final DatabaseSchemaInitializer schemaInitializer;

    public FileMetadataStorageFactory(DataSource dataSource, ObjectMapper objectMapper) {
        this.dataSource = dataSource;
        this.objectMapper = objectMapper == null
                ? new ObjectMapper().findAndRegisterModules()
                : objectMapper;
        this.schemaInitializer = new DatabaseSchemaInitializer(dataSource);
    }

    public FileMetadataStorage create(FileMetadataProperties properties) {
        if (properties == null) {
            throw new IllegalArgumentException("文件元数据配置不能为空");
        }
        String configuredType = properties.getStorageType();
        if (configuredType == null || configuredType.trim().isEmpty()) {
            return schemaInitializer.isMySqlAvailable()
                    ? createDatabaseStorage()
                    : createLocalStorage(properties.getMetadataDir());
        }

        String type = configuredType.trim().toLowerCase(Locale.ROOT);
        if ("local".equals(type)) {
            return createLocalStorage(properties.getMetadataDir());
        }
        if ("mysql".equals(type)) {
            return createDatabaseStorage();
        }
        throw new IllegalArgumentException("不支持的文件元数据存储类型：" + configuredType);
    }

    private FileMetadataStorage createDatabaseStorage() {
        if (dataSource == null) {
            throw new IllegalStateException("file.metadata.storage-type=mysql时必须配置DataSource");
        }
        schemaInitializer.initialize();
        return new DatabaseFileMetadataStorage(dataSource);
    }

    private FileMetadataStorage createLocalStorage(String directory) {
        return new LocalFileMetadataStorage(directory, objectMapper);
    }
}

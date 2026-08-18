package ch.liubai.upload.metadata;

import ch.liubai.upload.entity.FileMetadata;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;

/**
 * 本地文件元数据存储
 *
 * @author ljh
 * @version 1.0
 * @since 2024/1/4 14:04
 */
public class LocalFileMetadataStorage implements FileMetadataStorage {

    private final String metadataDirectory;
    private final ObjectMapper objectMapper;

    public LocalFileMetadataStorage(String metadataDirectory) {
        this.metadataDirectory = metadataDirectory;
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public void addFileMetadata(FileMetadata metadata) throws Exception {
        Path dirPath = Paths.get(metadataDirectory);
        if (!Files.exists(dirPath)) {
            Files.createDirectories(dirPath);
        }

        // 创建时间为空补充
        LocalDateTime now = LocalDateTime.now();
        if (metadata.getCreateTime() == null) {
            metadata.setCreateTime(now);
        }
        if (metadata.getUpdateTime() == null) {
            metadata.setUpdateTime(now);
        }

        // 保存文件元数据：文件名为SHA256，内容为JSON格式
        Path filePath = Paths.get(metadataDirectory, metadata.getSha256() + ".json");
        String json = objectMapper.writeValueAsString(metadata);
        Files.write(filePath, json.getBytes());
    }

    public void updateFileMetadata(FileMetadata fileMetadata) throws Exception {
        // 因为是覆盖式存储，所以直接调用addFileMetadata方法
        addFileMetadata(fileMetadata);
    }

    @Override
    public FileMetadata loadFileMetadata(String sha256) throws Exception {
        Path path = Paths.get(metadataDirectory, sha256 + ".json");
        if (!Files.exists(path)) {
            return null;
        }
        String json = new String(Files.readAllBytes(path));
        return objectMapper.readValue(json, FileMetadata.class);
    }


}

package ch.liubai.upload.metadata;

import ch.liubai.upload.domain.UploadDescriptor;
import ch.liubai.upload.entity.FileMetadata;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;

/**
 * Local JSON implementation of the metadata storage Strategy.
 */
public class LocalFileMetadataStorage implements FileMetadataStorage {

    private final Path metadataDirectory;
    private final ObjectMapper objectMapper;

    public LocalFileMetadataStorage(String metadataDirectory) {
        this(metadataDirectory, new ObjectMapper().findAndRegisterModules());
    }

    public LocalFileMetadataStorage(String metadataDirectory, ObjectMapper objectMapper) {
        if (metadataDirectory == null || metadataDirectory.trim().isEmpty()) {
            throw new IllegalArgumentException("metadataDirectory不能为空");
        }
        this.metadataDirectory = Paths.get(metadataDirectory).toAbsolutePath().normalize();
        this.objectMapper = objectMapper == null
                ? new ObjectMapper().findAndRegisterModules()
                : objectMapper.copy().findAndRegisterModules();
    }

    @Override
    public void addFileMetadata(FileMetadata metadata) throws Exception {
        validateMetadata(metadata);
        Files.createDirectories(metadataDirectory);

        LocalDateTime now = LocalDateTime.now();
        if (metadata.getCreateTime() == null) {
            metadata.setCreateTime(now);
        }
        metadata.setUpdateTime(now);

        Path target = resolveMetadataPath(metadata.getSha256());
        Path temporary = Files.createTempFile(metadataDirectory, metadata.getSha256(), ".json.tmp");
        try {
            String json = objectMapper.writeValueAsString(metadata);
            Files.write(temporary, json.getBytes(StandardCharsets.UTF_8));
            try {
                Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    public void updateFileMetadata(FileMetadata fileMetadata) throws Exception {
        addFileMetadata(fileMetadata);
    }

    @Override
    public FileMetadata loadFileMetadata(String sha256) throws Exception {
        Path path = resolveMetadataPath(sha256);
        if (!Files.exists(path, LinkOption.NOFOLLOW_LINKS)) {
            return null;
        }
        if (!Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)) {
            throw new IllegalStateException("元数据路径不是普通文件：" + path);
        }
        String json = new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
        return objectMapper.readValue(json, FileMetadata.class);
    }

    private Path resolveMetadataPath(String sha256) {
        if (!UploadDescriptor.isValidSha256(sha256)) {
            throw new IllegalArgumentException("sha256必须是64位十六进制字符串");
        }
        Path resolved = metadataDirectory.resolve(sha256.toLowerCase() + ".json").normalize();
        if (!resolved.startsWith(metadataDirectory)) {
            throw new IllegalArgumentException("非法元数据路径");
        }
        return resolved;
    }

    private static void validateMetadata(FileMetadata metadata) {
        if (metadata == null) {
            throw new IllegalArgumentException("文件元数据不能为空");
        }
        if (!UploadDescriptor.isValidSha256(metadata.getSha256())) {
            throw new IllegalArgumentException("文件元数据中的sha256非法");
        }
    }
}

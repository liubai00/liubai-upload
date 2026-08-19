package ch.liubai.upload.service.support;

import ch.liubai.upload.domain.UploadDescriptor;
import ch.liubai.upload.metadata.FileMetadataProperties;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Resolves upload paths and enforces that generated files stay below configured roots.
 */
public final class UploadPathResolver {

    private final Path tempDirectory;
    private final Path uploadDirectory;

    public UploadPathResolver(FileMetadataProperties properties) {
        if (properties == null) {
            throw new IllegalArgumentException("文件上传配置不能为空");
        }
        this.tempDirectory = prepareDirectory(properties.getTempDir(), "tempDir");
        this.uploadDirectory = prepareDirectory(properties.getUploadDir(), "uploadDir");
    }

    public Path resolveTempFile(UploadDescriptor descriptor) {
        return resolveBelow(tempDirectory, descriptor.getTempFileName());
    }

    public Path resolveUploadFile(UploadDescriptor descriptor) {
        return resolveBelow(uploadDirectory, descriptor.getFileName());
    }

    private static Path prepareDirectory(String configuredPath, String propertyName) {
        if (configuredPath == null || configuredPath.trim().isEmpty()) {
            throw new IllegalArgumentException(propertyName + "不能为空");
        }
        Path directory = Paths.get(configuredPath).toAbsolutePath().normalize();
        try {
            Files.createDirectories(directory);
        } catch (IOException e) {
            throw new IllegalStateException("无法创建目录" + directory, e);
        }
        if (!Files.isDirectory(directory) || !Files.isWritable(directory)) {
            throw new IllegalStateException("目录不可写：" + directory);
        }
        return directory;
    }

    private static Path resolveBelow(Path root, String fileName) {
        Path resolved = root.resolve(fileName).normalize();
        if (!resolved.startsWith(root)) {
            throw new IllegalArgumentException("非法文件路径");
        }
        return resolved;
    }
}

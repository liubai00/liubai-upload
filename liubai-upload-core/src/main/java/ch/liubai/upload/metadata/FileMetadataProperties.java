package ch.liubai.upload.metadata;

import java.nio.file.Paths;

/**
 * 文件元数据存储
 *
 * @author ljh
 * @version 1.0
 * @since 2024/1/4 14:42
 */
public class FileMetadataProperties {

    /**
     * 存储类型
     */
    private String storageType;

    /**
     * 元数据文件目录
     */
    private String metadataDir = defaultUserDirectory("metadata");

    /**
     * 临时文件目录
     */
    private String tempDir = Paths.get(System.getProperty("java.io.tmpdir"), "liubai-upload", "temp").toString();

    /**
     * 上传文件目录
     */
    private String uploadDir = defaultUserDirectory("files");

    public FileMetadataProperties() {
    }

    public FileMetadataProperties(String storageType, String metadataDir, String tempDir, String uploadDir) {
        this.storageType = storageType;
        this.metadataDir = metadataDir;
        this.tempDir = tempDir;
        this.uploadDir = uploadDir;
    }

    public String getStorageType() {
        return storageType;
    }

    public void setStorageType(String storageType) {
        this.storageType = storageType;
    }

    public String getMetadataDir() {
        return metadataDir;
    }

    public void setMetadataDir(String metadataDir) {
        this.metadataDir = metadataDir;
    }

    public String getTempDir() {
        return tempDir;
    }

    public void setTempDir(String tempDir) {
        this.tempDir = tempDir;
    }

    public String getUploadDir() {
        return uploadDir;
    }

    public void setUploadDir(String uploadDir) {
        this.uploadDir = uploadDir;
    }

    private static String defaultUserDirectory(String child) {
        return Paths.get(System.getProperty("user.home"), ".liubai-upload", child).toString();
    }
}

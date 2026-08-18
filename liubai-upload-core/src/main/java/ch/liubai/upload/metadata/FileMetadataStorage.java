package ch.liubai.upload.metadata;

import ch.liubai.upload.entity.FileMetadata;

/**
 * 文件元数据存储
 *
 * @author ljh
 * @version 1.0
 * @since 2024/1/5 16:19
 */
public interface FileMetadataStorage {

    /**
     * 新增文件元数据
     *
     * @param metadata 文件元数据
     */
    void addFileMetadata(FileMetadata metadata) throws Exception;


    /**
     * 加载文件元数据
     *
     * @param sha256 文件的SHA256
     * @return 文件元数据
     */
    FileMetadata loadFileMetadata(String sha256) throws Exception;
}

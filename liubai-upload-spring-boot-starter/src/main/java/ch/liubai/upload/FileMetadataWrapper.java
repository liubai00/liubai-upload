package ch.liubai.upload;

import ch.liubai.upload.metadata.FileMetadataProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;


/**
 * 文件元数据存储
 *
 * @author ljh
 * @version 1.0
 * @since 2024/1/22 15:57
 */
@ConfigurationProperties(prefix = "file.metadata")
public class FileMetadataWrapper extends FileMetadataProperties {
}

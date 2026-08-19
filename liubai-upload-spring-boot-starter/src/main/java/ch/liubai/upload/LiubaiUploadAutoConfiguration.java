package ch.liubai.upload;

import ch.liubai.upload.controller.FileController;
import ch.liubai.upload.metadata.FileMetadataStorage;
import ch.liubai.upload.service.FileService;
import ch.liubai.upload.service.impl.FileServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

/**
 * Spring Boot auto-configuration. Every application-level bean backs off when a user supplies
 * a custom implementation, allowing custom metadata storage Strategies.
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(FileService.class)
@EnableConfigurationProperties(FileMetadataWrapper.class)
public class LiubaiUploadAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public FileMetadataStorageFactory fileMetadataStorageFactory(
            ObjectProvider<DataSource> dataSourceProvider,
            ObjectProvider<ObjectMapper> objectMapperProvider) {
        return new FileMetadataStorageFactory(
                dataSourceProvider.getIfAvailable(),
                objectMapperProvider.getIfAvailable());
    }

    @Bean
    @ConditionalOnMissingBean(FileMetadataStorage.class)
    public FileMetadataStorage fileMetadataStorage(FileMetadataWrapper properties,
                                                   FileMetadataStorageFactory factory) {
        return factory.create(properties);
    }

    @Bean
    @ConditionalOnMissingBean(FileService.class)
    public FileService fileService(FileMetadataWrapper properties,
                                   FileMetadataStorage fileMetadataStorage) {
        return new FileServiceImpl(properties, fileMetadataStorage);
    }

    @Bean
    @ConditionalOnMissingBean(FileController.class)
    public FileController fileController(FileService fileService) {
        return new FileController(fileService);
    }
}

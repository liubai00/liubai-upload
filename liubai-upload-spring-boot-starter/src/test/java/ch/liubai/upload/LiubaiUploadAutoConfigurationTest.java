package ch.liubai.upload;

import ch.liubai.upload.controller.FileController;
import ch.liubai.upload.entity.FileMetadata;
import ch.liubai.upload.metadata.FileMetadataStorage;
import ch.liubai.upload.metadata.LocalFileMetadataStorage;
import ch.liubai.upload.service.FileService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class LiubaiUploadAutoConfigurationTest {

    @TempDir
    Path root;

    @Test
    void configuresAWorkingLocalStrategyByDefault() {
        baseContextRunner()
                .withSystemProperties(
                        "user.home=" + root,
                        "java.io.tmpdir=" + root.resolve("system-temp"))
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasSingleBean(FileMetadataStorage.class);
                    assertThat(context.getBean(FileMetadataStorage.class)).isInstanceOf(LocalFileMetadataStorage.class);
                    assertThat(context).hasSingleBean(FileService.class);
                    assertThat(context).hasSingleBean(FileController.class);
                    assertThat(root.resolve(".liubai-upload/files")).isDirectory();
                    assertThat(root.resolve("system-temp/liubai-upload/temp")).isDirectory();
                });
    }

    @Test
    void backsOffWhenTheApplicationProvidesACustomStorageStrategy() {
        contextRunner()
                .withUserConfiguration(CustomStorageConfiguration.class)
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasSingleBean(FileMetadataStorage.class);
                    assertThat(context.getBean(FileMetadataStorage.class))
                            .isSameAs(context.getBean("customMetadataStorage"));
                });
    }

    @Test
    void failsFastWhenMysqlIsExplicitButNoDataSourceExists() {
        contextRunner()
                .withPropertyValues("file.metadata.storage-type=mysql")
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure()).hasRootCauseMessage(
                            "file.metadata.storage-type=mysql时必须配置DataSource");
                });
    }

    private ApplicationContextRunner contextRunner() {
        return baseContextRunner()
                .withPropertyValues(
                        "file.metadata.storage-type=local",
                        "file.metadata.temp-dir=" + root.resolve("temp"),
                        "file.metadata.upload-dir=" + root.resolve("files"),
                        "file.metadata.metadata-dir=" + root.resolve("metadata"));
    }

    private ApplicationContextRunner baseContextRunner() {
        return new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(LiubaiUploadAutoConfiguration.class));
    }

    @Configuration(proxyBeanMethods = false)
    static class CustomStorageConfiguration {
        @Bean
        FileMetadataStorage customMetadataStorage() {
            return new FileMetadataStorage() {
                @Override
                public void addFileMetadata(FileMetadata metadata) {
                }

                @Override
                public FileMetadata loadFileMetadata(String sha256) {
                    return null;
                }
            };
        }
    }
}

# Liubai Upload - 大文件断点续传组件

[![Maven Central](https://img.shields.io/maven-central/v/ch.liubai.upload/liubai-upload-spring-boot-starter.svg)](https://search.maven.org/artifact/ch.liubai.upload/liubai-upload-spring-boot-starter)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![Java Version](https://img.shields.io/badge/Java-8+-green.svg)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-2.3.12-brightgreen.svg)](https://spring.io/projects/spring-boot)

Liubai Upload 是面向 Spring Boot 2.3 的顺序分片、断点续传组件。它使用 SHA-256 标识文件，支持本地 JSON 或 MySQL 元数据存储，并提供可直接访问的浏览器上传示例。

## 核心能力

- 真实分片续传：未完成的临时文件会保留，客户端可以从服务端确认的偏移继续上传。
- 完整性校验：完成上传前同时校验文件长度和完整文件 SHA-256。
- 严格偏移控制：除显式从 `0` 重新上传外，`startByte` 必须等于服务端已保存长度。
- 安全路径：SHA-256 使用值对象统一校验，文件路径经过规范化和目录边界检查。
- 可替换存储：`FileMetadataStorage` 使用策略模式，本地和 MySQL 实现由工厂统一选择，也可以注册自定义实现。
- 零配置启动：未配置路径时使用安全默认目录，Spring Boot Starter 自动注册所需组件。
- 并发保护：同一个文件在单个 JVM 内串行处理，防止并发分片相互覆盖。

## 快速开始

### 1. 添加依赖

```xml
<dependency>
    <groupId>ch.liubai.upload</groupId>
    <artifactId>liubai-upload-spring-boot-starter</artifactId>
    <version>1.0.1</version>
</dependency>
```

该版本面向 Java 8+ 和 Spring Boot 2.3.12。

### 2. 可选配置

完全不配置也可以启动。默认值如下：

| 配置项 | 默认值 | 说明 |
| --- | --- | --- |
| `file.metadata.storage-type` | 自动检测 | MySQL/MariaDB DataSource 可用时使用数据库，否则使用本地 JSON |
| `file.metadata.temp-dir` | `${java.io.tmpdir}/liubai-upload/temp` | 未完成分片目录 |
| `file.metadata.upload-dir` | `${user.home}/.liubai-upload/files` | 完整文件目录 |
| `file.metadata.metadata-dir` | `${user.home}/.liubai-upload/metadata` | 本地 JSON 元数据目录 |

显式使用本地存储的示例：

```yaml
file:
  metadata:
    storage-type: local
    temp-dir: /tmp/liubai-upload/temp
    upload-dir: /tmp/liubai-upload/files
    metadata-dir: /tmp/liubai-upload/metadata

# 示例页面每片为 1 MiB；为 multipart 开销留出余量。
spring:
  servlet:
    multipart:
      max-file-size: 2MB
      max-request-size: 3MB
```

使用 MySQL 时配置 `DataSource`，并将 `storage-type` 设为 `mysql`。显式选择 MySQL 但没有 `DataSource` 时，应用会快速失败，不会悄悄切换存储方式。

### 3. 启动应用

Starter 会注册：

- `GET /file/preprocess`：查询已完成文件或当前断点。
- `POST /file/upload`：上传一个顺序分片。
- `GET /liubai-upload/index.html`：仓库内置的 Vue 示例页面（启用 Spring Boot 静态资源处理时）。

## 上传协议

### 预处理

```http
GET /file/preprocess?sha256={64位十六进制SHA256}&totalBytes={文件总字节数}
```

成功响应示例：

```json
{
  "code": 20000,
  "message": "success",
  "data": {
    "uploadedBytes": 1048576,
    "currentSha256": "当前已上传部分的SHA256"
  }
}
```

客户端只有在本地文件前 `uploadedBytes` 字节的 SHA-256 与 `currentSha256` 一致时，才应从该断点继续；否则应以 `startByte=0` 重新上传。

### 上传一个分片

```http
POST /file/upload
Content-Type: multipart/form-data

sha256: 完整文件的SHA256
file: 当前分片
startByte: 当前分片在完整文件中的起始偏移
totalBytes: 完整文件总字节数
```

约束：

- `sha256` 必须是 64 位十六进制字符串。
- `startByte`、`totalBytes` 不能为负数，分片不能超过文件剩余长度。
- `startByte=0` 表示显式重新上传；否则它必须等于服务端已提交字节数。
- 中间分片成功后临时文件会保留；达到 `totalBytes` 后才执行完整 SHA-256 校验、移动文件并保存元数据。

内置页面使用 1 MiB 顺序分片，代码位于 `liubai-upload-core/src/main/resources/META-INF/resources/liubai-upload/js/app.js`。页面和接口都使用相对路径，因此部署在不同端口或 Servlet Context Path 下时不需要修改地址；示例资源也不会占用宿主应用的根首页。

## 元数据存储

### MySQL

MySQL 模式启动时会幂等创建表，也可以手动执行：

```sql
CREATE TABLE IF NOT EXISTS file_metadata
(
    id          INT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    file_name   VARCHAR(255) NOT NULL COMMENT '文件名',
    sha256      VARCHAR(64)  NOT NULL COMMENT '文件SHA256',
    file_size   BIGINT COMMENT '文件大小(字节)',
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY sha256_UNIQUE_IDX (sha256) COMMENT 'SHA256唯一索引'
) COMMENT '文件元数据表';
```

数据库实现从 `DataSource` 按操作获取连接并及时关闭，不持有长期 JDBC 连接。

### 自定义策略

注册自己的 `FileMetadataStorage` Bean 即可覆盖默认策略，自动配置会主动让位：

```java
@Component
public class CustomFileMetadataStorage implements FileMetadataStorage {

    @Override
    public void addFileMetadata(FileMetadata metadata) throws Exception {
        // 保存元数据
    }

    @Override
    public FileMetadata loadFileMetadata(String sha256) throws Exception {
        // 查询元数据
        return null;
    }
}
```

## 设计结构

```text
liubai-upload-core
├── controller       REST 参数校验与流生命周期
├── domain           SHA-256/文件大小值对象
├── metadata         元数据策略及配置
├── service          上传用例
├── service/support  安全路径解析与分段锁
├── util             流式写入、SHA-256 和原子移动
└── META-INF/resources/liubai-upload  命名空间内的上传示例

liubai-upload-spring-boot-starter
├── LiubaiUploadAutoConfiguration
├── FileMetadataStorageFactory
└── DatabaseSchemaInitializer
```

这里主要使用了策略模式（元数据存储）、工厂模式（存储选择）和值对象（上传标识与约束）。文件完成与元数据保存之间还包含补偿逻辑：如果元数据保存失败，完整文件会尽量移回临时目录，便于重试。

## 构建与测试

```bash
mvn test
```

发布相关的源码包、Javadoc 和 GPG 签名只在 `release` Profile 下执行：

```bash
mvn -Prelease deploy
```

## 部署说明

当前并发锁是 JVM 内锁，适合单实例或同一文件被路由到固定实例的部署。多实例同时接收同一文件分片时，应在应用层增加分布式锁，或把临时文件和上传状态迁移到具备并发控制的共享存储。

## 许可证

本项目基于 [Apache License 2.0](http://www.apache.org/licenses/LICENSE-2.0) 开源。

## 作者与链接

- 维护者：[刘白](mailto:1044586526@qq.com)
- [GitHub 仓库](https://github.com/1044586526/liubai-upload)
- [问题反馈](https://github.com/1044586526/liubai-upload/issues)

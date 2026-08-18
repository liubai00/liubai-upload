# Liubai Upload - 大文件断点续传组件

[![Maven Central](https://img.shields.io/maven-central/v/ch.liubai.upload/liubai-upload-spring-boot-starter.svg)](https://search.maven.org/artifact/ch.liubai.upload/liubai-upload-spring-boot-starter)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![Java Version](https://img.shields.io/badge/Java-8+-green.svg)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-2.3.12-brightgreen.svg)](https://spring.io/projects/spring-boot)

Liubai Upload 是一个高性能、易集成的大文件断点续传组件，专为 Spring Boot 应用设计。支持文件完整性校验、多种存储方式，提供开箱即用的上传解决方案。

## ✨ 核心特性

- 🚀 **断点续传**：支持网络中断后从断点继续上传，避免重复传输
- 🔒 **文件完整性校验**：基于 SHA256 算法确保文件传输完整性
- 💾 **多种存储方式**：支持本地文件存储和 MySQL 数据库存储
- 🔧 **零配置启动**：Spring Boot Starter 自动配置，开箱即用
- 📊 **智能去重**：相同文件自动识别，避免重复存储
- 🎯 **高性能**：分块上传，支持大文件高效传输
- 🛡️ **安全可靠**：完整的错误处理和数据一致性保障

## 🏗️ 项目架构

### 模块结构
```
liubai-upload-component/
├── liubai-upload-core/                           # 核心功能模块
│   └── src/main/java/ch/liubai/upload/
│       ├── controller/                           # REST API 控制器
│       │   └── FileController.java               # 文件上传控制器
│       ├── service/                              # 业务逻辑层
│       │   ├── FileService.java                  # 文件服务接口
│       │   └── impl/FileServiceImpl.java         # 文件服务实现
│       ├── entity/                               # 实体类
│       │   ├── FileMetadata.java                 # 文件元数据实体
│       │   ├── FileUploadPreprocessResponse.java # 预处理响应实体
│       │   └── ReturnVO.java                     # 统一返回对象
│       ├── enums/                                # 枚举类
│       │   ├── ErrorType.java                    # 错误类型枚举
│       │   └── UploadErrorCodeEnum.java          # 上传错误码枚举
│       ├── metadata/                             # 元数据存储
│       │   ├── FileMetadataStorage.java          # 存储接口
│       │   ├── FileMetadataProperties.java       # 配置属性
│       │   ├── LocalFileMetadataStorage.java     # 本地文件存储实现
│       │   └── DatabaseFileMetadataStorage.java  # 数据库存储实现
│       └── util/                                 # 工具类
│           └── UploadFileUtil.java               # 文件上传工具类
├── liubai-upload-spring-boot-starter/            # Spring Boot 自动配置
│   └── src/main/java/ch/liubai/upload/
│       ├── FileMetadataStorageFactory.java       # 存储工厂自动配置
│       └── FileMetadataWrapper.java              # 配置属性包装类
└── resources/                                    # 资源文件
    ├── html/index.html                           # 前端示例页面
    ├── js/app.js                                 # 前端JavaScript实现
    └── sql/file_metadata.sql                     # 数据库表结构
```

### 核心组件

- **FileController**：提供文件预处理和上传的 REST API
- **FileService**：核心业务逻辑，处理文件上传和完整性校验
- **FileMetadataStorage**：文件元数据存储抽象，支持本地和数据库存储
- **UploadFileUtil**：文件操作工具类，提供 SHA256 计算、文件移动等功能

## 🚀 快速开始

### 1. 添加依赖

在您的 `pom.xml` 中添加依赖：

```xml
<dependency>
    <groupId>ch.liubai.upload</groupId>
    <artifactId>liubai-upload-spring-boot-starter</artifactId>
    <version>1.0.1</version>
</dependency>
```

### 2. 配置参数

在 `application.yml` 中添加配置：

```yaml
file:
  metadata:
    # 存储类型：local(本地) 或 mysql(数据库)
    # 不配置时自动检测：有 DataSource 则使用 mysql，否则使用 local
    storageType: mysql
    
    # 临时文件存储路径（必填）
    tempDir: /tmp/liubai-upload/temp
    
    # 上传完成文件存储路径（必填）
    uploadDir: /tmp/liubai-upload/files
    
    # 元数据存储路径（local 模式使用）
    # 不配置时默认使用 ~/.file_metadata
    metadataDir: /tmp/liubai-upload/metadata
```

### 3. 启动应用

启动 Spring Boot 应用，组件会自动注册以下接口：

- `GET /file/preprocess` - 文件预处理接口
- `POST /file/upload` - 文件上传接口

## 📖 API 文档

### 文件预处理接口

**请求**
```http
GET /file/preprocess?sha256={文件SHA256}&totalBytes={文件大小}
```

**参数**
- `sha256`：文件的 SHA256 哈希值（必填）
- `totalBytes`：文件总字节数（必填）

**响应**
```json
{
  "code": 20000,
  "message": "success",
  "data": {
    "uploadedBytes": 1048576,    // 已上传字节数
    "currentSha256": "abc123..."  // 当前已上传部分的 SHA256
  }
}
```

### 文件上传接口

**请求**
```http
POST /file/upload
Content-Type: multipart/form-data

sha256: 文件SHA256值
file: 文件数据
startByte: 起始字节位置
totalBytes: 文件总大小
```

**响应**
```json
{
  "code": 20000,
  "message": "文件上传成功",
  "data": "success"
}
```

## 💻 前端集成示例

### HTML 页面
```html
<!DOCTYPE html>
<html>
<head>
    <title>文件上传</title>
    <script src="https://cdn.jsdelivr.net/npm/vue@2"></script>
    <script src="https://cdn.jsdelivr.net/npm/axios@1.6.5/dist/axios.min.js"></script>
    <script src="https://cdnjs.cloudflare.com/ajax/libs/crypto-js/4.0.0/crypto-js.min.js"></script>
</head>
<body>
    <div id="app">
        <input type="file" @change="handleFileChange" />
        <button @click="startUpload" :disabled="!file">开始上传</button>
        <div v-if="uploading">
            上传进度: {{ Math.round(progress * 100) }}%
        </div>
    </div>
</body>
</html>
```

### JavaScript 实现
```javascript
new Vue({
    el: '#app',
    data: {
        file: null,
        uploading: false,
        progress: 0
    },
    methods: {
        handleFileChange(event) {
            this.file = event.target.files[0];
        },
        
        async startUpload() {
            if (!this.file) return;
            
            this.uploading = true;
            try {
                // 1. 计算文件 SHA256
                const sha256 = await this.calculateSHA256(this.file);
                
                // 2. 预处理请求
                const preprocessRes = await axios.get('/file/preprocess', {
                    params: {
                        sha256: sha256,
                        totalBytes: this.file.size
                    }
                });
                
                const { uploadedBytes, currentSha256 } = preprocessRes.data.data;
                
                // 3. 检查是否需要上传
                if (uploadedBytes === this.file.size && currentSha256 === sha256) {
                    alert('文件已存在');
                    return;
                }
                
                // 4. 断点续传
                const startByte = uploadedBytes || 0;
                const fileSlice = this.file.slice(startByte);
                
                const formData = new FormData();
                formData.append('file', fileSlice);
                formData.append('sha256', sha256);
                formData.append('startByte', startByte);
                formData.append('totalBytes', this.file.size);
                
                // 5. 上传文件
                await axios.post('/file/upload', formData, {
                    headers: { 'Content-Type': 'multipart/form-data' },
                    onUploadProgress: (progressEvent) => {
                        this.progress = (startByte + progressEvent.loaded) / this.file.size;
                    }
                });
                
                alert('上传成功');
            } catch (error) {
                console.error('上传失败:', error);
                alert('上传失败');
            } finally {
                this.uploading = false;
            }
        },
        
        calculateSHA256(file) {
            return new Promise((resolve, reject) => {
                const reader = new FileReader();
                const sha256 = CryptoJS.algo.SHA256.create();
                
                reader.onload = (e) => {
                    const wordArray = CryptoJS.lib.WordArray.create(e.target.result);
                    sha256.update(wordArray);
                    resolve(sha256.finalize().toString());
                };
                
                reader.onerror = reject;
                reader.readAsArrayBuffer(file);
            });
        }
    }
});
```

## ⚙️ 高级配置

### 数据库模式

使用 MySQL 存储时，组件会自动创建 `file_metadata` 表：

```sql
CREATE TABLE file_metadata (
    id          INT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    file_name   VARCHAR(255) NOT NULL COMMENT '文件名',
    sha256      VARCHAR(64)  NOT NULL COMMENT '文件SHA256',
    file_size   BIGINT COMMENT '文件大小(字节)',
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY sha256_UNIQUE_IDX (sha256) COMMENT 'SHA256唯一索引'
) COMMENT '文件元数据表';
```

### 自定义存储实现

实现 `FileMetadataStorage` 接口来自定义存储方式：

```java
@Component
public class CustomFileMetadataStorage implements FileMetadataStorage {
    
    @Override
    public void addFileMetadata(FileMetadata metadata) throws Exception {
        // 自定义存储逻辑
    }
    
    @Override
    public FileMetadata getFileMetadata(String sha256) throws Exception {
        // 自定义查询逻辑
        return null;
    }
}
```

## 🔧 技术栈

- **Java 8+**：核心开发语言
- **Spring Boot 2.3.12**：应用框架
- **Maven**：项目构建工具
- **MySQL**：可选的元数据存储
- **Vue.js + Axios**：前端示例实现
- **CryptoJS**：前端 SHA256 计算

## 📝 更新日志

### v1.0.1 (2024-01-22)
- ✨ 新增 Spring Boot Starter 自动配置
- 🐛 修复文件完整性校验问题
- 📈 优化大文件上传性能
- 🔧 改进错误处理机制

## 🤝 贡献指南

欢迎提交 Issue 和 Pull Request！

1. Fork 本仓库
2. 创建特性分支：`git checkout -b feature/amazing-feature`
3. 提交更改：`git commit -m 'Add amazing feature'`
4. 推送分支：`git push origin feature/amazing-feature`
5. 提交 Pull Request

## 📄 许可证

本项目基于 [Apache License 2.0](http://www.apache.org/licenses/LICENSE-2.0) 许可证开源。

## 👨‍💻 作者

- **pfr** - *项目维护者* - [刘白](mailto:1044586526@qq.com)

## 🔗 相关链接

- [GitHub 仓库](https://github.com/1044586526/liubai-upload)
- [问题反馈](https://github.com/1044586526/liubai-upload/issues)

---

⭐ 如果这个项目对您有帮助，请给我们一个 Star！

CREATE TABLE file_metadata
(
    id          INT AUTO_INCREMENT PRIMARY KEY comment '主键',
    file_name   VARCHAR(255) NOT NULL comment '文件名',
    sha256      VARCHAR(64)  NOT NULL comment '文件sha256',
    file_size   BIGINT comment '文件大小(字节)',
    create_time TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY sha256_UNIQUE_IDX (sha256) comment 'sha256唯一索引'
) comment '文件元数据表';

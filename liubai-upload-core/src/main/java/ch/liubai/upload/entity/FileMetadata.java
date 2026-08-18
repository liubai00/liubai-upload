package ch.liubai.upload.entity;

import java.time.LocalDateTime;

/**
 * 文件元数据
 *
 * @author ljh
 * @version 1.0
 * @since 2024/1/5 16:23
 */
public class FileMetadata {

    /**
     * 文件ID
     */
    private Integer id;

    /**
     * 文件名
     */
    private String fileName;

    /**
     * 第一次上传时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;

    /**
     * 文件的SHA256
     */
    private String sha256;

    /**
     * 文件大小：byte
     */
    private Long fileSize;


    public FileMetadata(Integer id, String fileName, LocalDateTime createTime, LocalDateTime updateTime, String sha256, Long fileSize) {
        this.id = id;
        this.fileName = fileName;
        this.createTime = createTime;
        this.updateTime = updateTime;
        this.sha256 = sha256;
        this.fileSize = fileSize;
    }

    public FileMetadata(String fileName, String sha256, Long fileSize) {
        this.fileName = fileName;
        this.sha256 = sha256;
        this.fileSize = fileSize;
    }

    public FileMetadata() {
    }

    @Override
    public String toString() {
        return "FileMetadata{" +
                "id=" + id +
                ", fileName='" + fileName + '\'' +
                ", createTime=" + createTime +
                ", updateTime=" + updateTime +
                ", sha256='" + sha256 + '\'' +
                ", fileSize=" + fileSize +
                '}';
    }


    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }


    public String getSha256() {
        return sha256;
    }

    public void setSha256(String sha256) {
        this.sha256 = sha256;
    }

    public long getFileSize() {
        return fileSize;
    }

    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }
}

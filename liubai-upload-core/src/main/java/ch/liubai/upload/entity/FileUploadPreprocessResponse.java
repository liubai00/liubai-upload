package ch.liubai.upload.entity;

/**
 * 文件上传预处理响应
 *
 * @author ljh
 * @version 1.0
 * @since 2024/1/5 17:11
 */
public class FileUploadPreprocessResponse {

    /**
     * 文件已上传的字节数
     */
    private long uploadedBytes;

    /**
     * 文件的SHA256
     */
    private String currentSha256;


    public FileUploadPreprocessResponse(long uploadedBytes, String currentSha256) {
        this.uploadedBytes = uploadedBytes;
        this.currentSha256 = currentSha256;
    }

    public long getUploadedBytes() {
        return uploadedBytes;
    }

    public void setUploadedBytes(long uploadedBytes) {
        this.uploadedBytes = uploadedBytes;
    }

    public String getCurrentSha256() {
        return currentSha256;
    }

    public void setCurrentSha256(String currentSha256) {
        this.currentSha256 = currentSha256;
    }
}

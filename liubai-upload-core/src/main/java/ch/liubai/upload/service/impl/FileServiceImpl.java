package ch.liubai.upload.service.impl;

import ch.liubai.upload.domain.UploadDescriptor;
import ch.liubai.upload.entity.FileMetadata;
import ch.liubai.upload.entity.FileUploadPreprocessResponse;
import ch.liubai.upload.entity.ReturnVO;
import ch.liubai.upload.enums.UploadErrorCodeEnum;
import ch.liubai.upload.metadata.FileMetadataProperties;
import ch.liubai.upload.metadata.FileMetadataStorage;
import ch.liubai.upload.service.FileService;
import ch.liubai.upload.service.support.UploadLockManager;
import ch.liubai.upload.service.support.UploadPathResolver;
import ch.liubai.upload.util.UploadFileUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;

/**
 * File upload application service. Upload metadata storage is a Strategy supplied by the starter.
 */
public class FileServiceImpl implements FileService {

    private static final Logger log = LoggerFactory.getLogger(FileServiceImpl.class);

    private final FileMetadataStorage fileMetadataStorage;
    private final UploadPathResolver pathResolver;
    private final UploadLockManager lockManager;

    public FileServiceImpl(FileMetadataProperties properties, FileMetadataStorage fileMetadataStorage) {
        if (fileMetadataStorage == null) {
            throw new IllegalArgumentException("文件元数据存储不能为空");
        }
        this.fileMetadataStorage = fileMetadataStorage;
        this.pathResolver = new UploadPathResolver(properties);
        this.lockManager = new UploadLockManager();
    }

    @Override
    public ReturnVO<FileUploadPreprocessResponse> preprocessFileUpload(String sha256, long totalBytes) {
        try {
            UploadDescriptor descriptor = UploadDescriptor.of(sha256, totalBytes);
            return lockManager.execute(descriptor.getLockKey(), () -> preprocessLocked(descriptor));
        } catch (IllegalArgumentException e) {
            return ReturnVO.failWithMessage(UploadErrorCodeEnum.PARAM_EXCEPTION, e.getMessage());
        } catch (Exception e) {
            log.error("【预上传】检查文件状态失败", e);
            return ReturnVO.failWithMessage(UploadErrorCodeEnum.SYSTEM_EXCEPTION, "检查上传状态失败，请稍后重试");
        }
    }

    private ReturnVO<FileUploadPreprocessResponse> preprocessLocked(UploadDescriptor descriptor) throws Exception {
        Path uploadFile = pathResolver.resolveUploadFile(descriptor);
        if (Files.exists(uploadFile, LinkOption.NOFOLLOW_LINKS)) {
            if (isExpectedFile(uploadFile, descriptor)) {
                return ReturnVO.success(new FileUploadPreprocessResponse(
                        descriptor.getTotalBytes(), descriptor.getSha256()));
            }
            return ReturnVO.failWithMessage(UploadErrorCodeEnum.SHA256_CHECK_FAIL,
                    "正式文件完整性校验失败，请联系管理员");
        }

        Path tempFile = pathResolver.resolveTempFile(descriptor);
        if (!Files.exists(tempFile, LinkOption.NOFOLLOW_LINKS)) {
            return ReturnVO.success(new FileUploadPreprocessResponse(0, null));
        }
        if (!Files.isRegularFile(tempFile, LinkOption.NOFOLLOW_LINKS)) {
            return ReturnVO.failWithMessage(UploadErrorCodeEnum.PARAM_EXCEPTION, "临时上传路径不是普通文件");
        }

        long uploadedBytes = Files.size(tempFile);
        if (uploadedBytes > descriptor.getTotalBytes()) {
            Files.delete(tempFile);
            return ReturnVO.failWithMessage(UploadErrorCodeEnum.PARAM_EXCEPTION, "临时文件大小超过声明大小，已重置上传");
        }

        String currentSha256 = UploadFileUtil.calculateSHA256(tempFile);
        if (uploadedBytes == descriptor.getTotalBytes()) {
            if (!descriptor.getSha256().equals(currentSha256)) {
                Files.delete(tempFile);
                return ReturnVO.failWithMessage(UploadErrorCodeEnum.SHA256_CHECK_FAIL,
                        "临时文件完整性校验失败，已重置上传");
            }
            moveToFinalAndSaveMetadata(descriptor, tempFile, uploadFile, "预上传");
            return ReturnVO.success(new FileUploadPreprocessResponse(uploadedBytes, descriptor.getSha256()));
        }

        return ReturnVO.success(new FileUploadPreprocessResponse(uploadedBytes, currentSha256));
    }

    @Override
    public ReturnVO<String> uploadFile(String sha256, InputStream file, long startByte, long totalBytes) {
        return uploadFile(sha256, file, -1, startByte, totalBytes);
    }

    @Override
    public ReturnVO<String> uploadFile(String sha256, InputStream file, long contentLength,
                                       long startByte, long totalBytes) {
        try {
            UploadDescriptor descriptor = UploadDescriptor.of(sha256, totalBytes);
            validateChunk(startByte, contentLength, totalBytes);
            return lockManager.execute(descriptor.getLockKey(),
                    () -> uploadChunkLocked(descriptor, file, contentLength, startByte));
        } catch (IllegalArgumentException e) {
            return ReturnVO.failWithMessage(UploadErrorCodeEnum.PARAM_EXCEPTION, e.getMessage());
        } catch (Exception e) {
            log.error("【上传文件】上传过程中出现错误", e);
            return ReturnVO.failWithMessage(UploadErrorCodeEnum.SYSTEM_EXCEPTION, "文件上传失败，请稍后重试");
        }
    }

    private ReturnVO<String> uploadChunkLocked(UploadDescriptor descriptor, InputStream inputStream,
                                                long contentLength, long startByte) throws Exception {
        Path uploadFile = pathResolver.resolveUploadFile(descriptor);
        if (Files.exists(uploadFile, LinkOption.NOFOLLOW_LINKS)) {
            if (isExpectedFile(uploadFile, descriptor)) {
                return ReturnVO.success("文件已经存在");
            }
            return ReturnVO.failWithMessage(UploadErrorCodeEnum.SHA256_CHECK_FAIL, "正式文件完整性校验失败");
        }

        Path tempFile = pathResolver.resolveTempFile(descriptor);
        boolean tempFileExists = Files.exists(tempFile, LinkOption.NOFOLLOW_LINKS);
        if (tempFileExists && !Files.isRegularFile(tempFile, LinkOption.NOFOLLOW_LINKS)) {
            return ReturnVO.failWithMessage(UploadErrorCodeEnum.PARAM_EXCEPTION, "临时上传路径不是普通文件");
        }
        long existingBytes = tempFileExists ? Files.size(tempFile) : 0;

        if (startByte == 0 && existingBytes > 0) {
            log.info("【上传文件】从头重传文件 {}", descriptor.getFileName());
            existingBytes = 0;
        } else if (startByte != existingBytes) {
            return ReturnVO.failWithMessage(UploadErrorCodeEnum.UPLOAD_OFFSET_MISMATCH,
                    "服务端已上传" + existingBytes + "字节，请从该位置继续");
        }

        long remainingBytes = descriptor.getTotalBytes() - startByte;
        if (contentLength >= 0 && contentLength > remainingBytes) {
            return ReturnVO.failWithMessage(UploadErrorCodeEnum.PARAM_EXCEPTION, "上传分片超过文件声明大小");
        }

        if (!tempFileExists) {
            Files.createFile(tempFile);
        }
        long writtenBytes = UploadFileUtil.writeToFile(inputStream, startByte, tempFile, remainingBytes);
        if (contentLength >= 0 && writtenBytes != contentLength) {
            truncate(tempFile, startByte);
            return ReturnVO.failWithMessage(UploadErrorCodeEnum.PARAM_EXCEPTION, "上传分片实际大小与声明不一致");
        }

        long uploadedBytes = startByte + writtenBytes;
        if (uploadedBytes < descriptor.getTotalBytes()) {
            log.info("【上传文件】文件 {} 已上传 {}/{} 字节",
                    descriptor.getFileName(), uploadedBytes, descriptor.getTotalBytes());
            return ReturnVO.success("分片上传成功，已上传" + uploadedBytes + "/" + descriptor.getTotalBytes() + "字节");
        }

        String actualSha256 = UploadFileUtil.calculateSHA256(tempFile);
        if (!descriptor.getSha256().equals(actualSha256)) {
            Files.deleteIfExists(tempFile);
            return ReturnVO.failWithMessage(UploadErrorCodeEnum.SHA256_CHECK_FAIL,
                    "文件sha256校验不一致，已重置上传");
        }

        moveToFinalAndSaveMetadata(descriptor, tempFile, uploadFile, "上传文件");
        log.info("【上传文件】文件 {} 上传成功", descriptor.getFileName());
        return ReturnVO.success("文件上传成功");
    }

    private void moveToFinalAndSaveMetadata(UploadDescriptor descriptor, Path tempFile,
                                            Path uploadFile, String operationName) throws Exception {
        UploadFileUtil.moveFile(tempFile, uploadFile);
        try {
            fileMetadataStorage.addFileMetadata(new FileMetadata(
                    descriptor.getFileName(), descriptor.getSha256(), descriptor.getTotalBytes()));
        } catch (Exception metadataError) {
            try {
                if (Files.exists(uploadFile, LinkOption.NOFOLLOW_LINKS)
                        && !Files.exists(tempFile, LinkOption.NOFOLLOW_LINKS)) {
                    UploadFileUtil.moveFile(uploadFile, tempFile);
                }
            } catch (Exception rollbackError) {
                metadataError.addSuppressed(rollbackError);
                log.error("【{}】元数据保存失败且文件回滚失败：{}", operationName, descriptor.getFileName(), rollbackError);
            }
            throw metadataError;
        }
    }

    private static boolean isExpectedFile(Path file, UploadDescriptor descriptor) throws Exception {
        return Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS)
                && Files.size(file) == descriptor.getTotalBytes()
                && descriptor.getSha256().equals(UploadFileUtil.calculateSHA256(file));
    }

    private static void validateChunk(long startByte, long contentLength, long totalBytes) {
        if (startByte < 0 || startByte > totalBytes) {
            throw new IllegalArgumentException("startByte必须位于0和totalBytes之间");
        }
        if (contentLength < -1) {
            throw new IllegalArgumentException("上传分片大小非法");
        }
    }

    private static void truncate(Path file, long length) throws IOException {
        try (RandomAccessFile randomAccessFile = new RandomAccessFile(file.toFile(), "rw")) {
            randomAccessFile.setLength(length);
        }
    }
}

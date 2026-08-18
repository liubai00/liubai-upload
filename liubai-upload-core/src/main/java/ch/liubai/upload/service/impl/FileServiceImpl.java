package ch.liubai.upload.service.impl;

import ch.liubai.upload.entity.FileMetadata;
import ch.liubai.upload.entity.FileUploadPreprocessResponse;
import ch.liubai.upload.entity.ReturnVO;
import ch.liubai.upload.enums.UploadErrorCodeEnum;
import ch.liubai.upload.service.FileService;
import ch.liubai.upload.util.UploadFileUtil;
import ch.liubai.upload.metadata.FileMetadataProperties;
import ch.liubai.upload.metadata.FileMetadataStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

/**
 * 文件服务
 *
 * @author ljh
 * @version 1.0
 * @since 2024/1/5 15:04
 */
public class FileServiceImpl implements FileService {

    private static final Logger log = LoggerFactory.getLogger(FileServiceImpl.class);

    public static final String FILE_NAME_SPLIT = "_";

    /**
     * 临时文件后缀
     */
    public static final String TEMP_FILE_SUFFIX = ".tmp";

    public FileServiceImpl(FileMetadataProperties fileMetadataProperties, FileMetadataStorage fileMetadataStorage) {
        this.fileMetadataProperties = fileMetadataProperties;
        this.fileMetadataStorage = fileMetadataStorage;
    }

    private final FileMetadataProperties fileMetadataProperties;


    private final FileMetadataStorage fileMetadataStorage;

    @Override
    public ReturnVO<FileUploadPreprocessResponse> preprocessFileUpload(String sha256, long totalBytes) throws Exception {
        String fileName = sha256 + FILE_NAME_SPLIT + totalBytes;

        // 检查正式目录
        File fileInUploadDir = new File(fileMetadataProperties.getUploadDir(), fileName);
        if (fileInUploadDir.exists() && fileInUploadDir.length() == totalBytes) {
            // 文件已经存在
            return ReturnVO.success(new FileUploadPreprocessResponse(totalBytes, sha256));
        }

        // 检查临时目录
        File tempFile = new File(fileMetadataProperties.getTempDir(), fileName + TEMP_FILE_SUFFIX);
        long tempFileLength = tempFile.length();
        if (tempFile.exists()) {
            // 计算临时文件的sha256
            String tempFileSha256 = UploadFileUtil.calculateSHA256(tempFile);
            // 判断临时文件是否完整
            if (UploadFileUtil.isFileComplete(tempFileSha256, tempFile) && tempFileLength == totalBytes) {
                // 文件已经在临时文件中是完整的了，但未在正式路径中，这时候直接移动文件到正式路径
                moveFileToUploadFileWithSaveMetadata(sha256, totalBytes, tempFile, fileName, "预上传");
                log.info("【预上传】文件 {} 临时文件已存在，转移到正式文件成功", fileName);
            }
            return ReturnVO.success(new FileUploadPreprocessResponse(tempFileLength, tempFileSha256));
        }

        // 文件不存在
        return ReturnVO.success(new FileUploadPreprocessResponse(0, null));
    }

    /**
     * 将文件移动到正式目录，并保存文件元数据
     *
     * @param sha256     文件sha256
     * @param totalBytes 文件总字节数
     * @param tempFile   临时文件
     * @param fileName   文件名
     * @param fucName    方法名
     * @throws IOException IO异常
     */
    private void moveFileToUploadFileWithSaveMetadata(String sha256, long totalBytes, File tempFile, String fileName, String fucName) throws Exception {
        File fileInUploadDir;
        UploadFileUtil.moveFileToUploadDir(tempFile, fileName, fileMetadataProperties.getUploadDir());
        try {
            // 保存文件元数据
            fileMetadataStorage.addFileMetadata(new FileMetadata(fileName, sha256, totalBytes));
        } catch (Exception e) {
            // 清理正式文件，防止数据不一致
            fileInUploadDir = new File(fileMetadataProperties.getUploadDir(), fileName);
            if (fileInUploadDir.exists()) {
                boolean deleteResult = fileInUploadDir.delete();
                log.warn("【{}】文件 {} 元数据保存过程中出现错误，清理正式文件结果为 {}", fucName, fileName, deleteResult);
            }
            throw e;
        }
    }

    @Override
    public ReturnVO<String> uploadFile(String sha256, InputStream file, long startByte, long totalBytes) {
        String fileName = sha256 + FILE_NAME_SPLIT + totalBytes;
        log.info("【上传文件】开始上传文件 {}，开始字节-{}，总字节-{}", fileName, startByte, totalBytes);

        try {
            // 检查正式目录，如果文件已经存在，则直接返回成功
            File fileInUploadDir = new File(fileMetadataProperties.getUploadDir(), fileName);
            if (fileInUploadDir.exists() && fileInUploadDir.length() == totalBytes) {
                log.info("【上传文件】文件 {} 已经存在，直接返回成功", fileName);
                return ReturnVO.success("文件已经存在");
            }

            File tempFile = UploadFileUtil.getFile(fileName + TEMP_FILE_SUFFIX, fileMetadataProperties.getTempDir());
            // 父类目录不存在则创建
            if (!tempFile.getParentFile().exists()) {
                boolean mkdirsResult = tempFile.getParentFile().mkdirs();
                log.info("【上传文件】文件 {} 临时目录不存在，创建临时目录结果为 {}", tempFile.getParentFile().getAbsoluteFile(), mkdirsResult);
            }

            // 判断临时文件是否存在
            if (!tempFile.exists()) {
                // 不存在则创建
                boolean createResult = tempFile.createNewFile();
                log.info("【上传文件】文件 {} 不存在，创建临时文件结果为 {}", tempFile.getAbsolutePath(), createResult);
            }
            UploadFileUtil.writeToFile(file, startByte, tempFile.getPath());

            // 完整性校验和移动文件
            if (UploadFileUtil.isFileComplete(sha256, tempFile) && tempFile.length() == totalBytes) {
                // 完整，则移动文件到正式目录，并保存文件元数据
                moveFileToUploadFileWithSaveMetadata(sha256, totalBytes, tempFile, fileName, "上传文件");
                log.info("【上传文件】文件 {} 的文件上传成功", fileName);
                return ReturnVO.success("文件上传成功");
            } else {
                // 不一致，则删除临时文件，返回失败
                boolean deleteTmpResult = tempFile.delete();
                log.info("【上传文件】文件 {} 上传失败，删除临时文件结果为 {}", fileName, deleteTmpResult);
                return ReturnVO.fail(UploadErrorCodeEnum.SHA256_CHECK_FAIL, "文件sha256校验结果不一致，已删除临时文件，上传失败");
            }

        } catch (Exception e) {
            log.error("【上传文件】文件 {} 上传过程中出现错误: ", fileName, e);
            return ReturnVO.fail("文件上传过程中出现错误: " + e.getMessage());
        }
    }


}

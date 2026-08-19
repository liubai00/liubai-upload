package ch.liubai.upload.controller;

import ch.liubai.upload.entity.FileUploadPreprocessResponse;
import ch.liubai.upload.entity.ReturnVO;
import ch.liubai.upload.service.FileService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.PositiveOrZero;
import javax.validation.constraints.Pattern;
import java.io.IOException;
import java.io.InputStream;

/**
 * 文件控制器
 *
 * @author ljh
 * @version 1.0
 * @since 2024/1/5 15:02
 */
@RestController
@RequestMapping("/file")
@Validated
public class FileController {

    private final FileService fileService;

    public FileController(FileService fileService) {
        this.fileService = fileService;
    }


    @GetMapping("/preprocess")
    public ReturnVO<FileUploadPreprocessResponse> preprocessFileUpload(
            @RequestParam("sha256")
            @NotBlank(message = "sha256不能为空")
            @Pattern(regexp = "^[0-9a-fA-F]{64}$", message = "sha256必须是64位十六进制字符串") String sha256,
            @RequestParam("totalBytes") @PositiveOrZero(message = "totalBytes不能小于0") long totalBytes) throws Exception {
        return fileService.preprocessFileUpload(sha256, totalBytes);
    }

    @PostMapping("/upload")
    public ReturnVO<String> uploadFile(@RequestParam("sha256")
                                       @NotBlank(message = "sha256不能为空")
                                       @Pattern(regexp = "^[0-9a-fA-F]{64}$", message = "sha256必须是64位十六进制字符串") String sha256,
                                       @RequestParam("file") MultipartFile file,
                                       @RequestParam("startByte") @PositiveOrZero(message = "startByte不能小于0") long startByte,
                                       @RequestParam("totalBytes") @PositiveOrZero(message = "totalBytes不能小于0") long totalBytes) throws IOException {
        try (InputStream inputStream = file.getInputStream()) {
            return fileService.uploadFile(sha256, inputStream, file.getSize(), startByte, totalBytes);
        }
    }
}

package ch.liubai.upload.controller;

import ch.liubai.upload.entity.FileUploadPreprocessResponse;
import ch.liubai.upload.entity.ReturnVO;
import ch.liubai.upload.service.FileService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.constraints.NotBlank;
import java.io.IOException;

/**
 * 文件控制器
 *
 * @author ljh
 * @version 1.0
 * @since 2024/1/5 15:02
 */
@RestController
@RequestMapping("/file")
public class FileController {

    private final FileService fileService;

    public FileController(FileService fileService) {
        this.fileService = fileService;
    }


    @GetMapping("/preprocess")
    public ReturnVO<FileUploadPreprocessResponse> preprocessFileUpload(
            @RequestParam("sha256") @NotBlank(message = "sha256不能为空") String sha256,
            @RequestParam("totalBytes") long totalBytes) throws Exception {
        return fileService.preprocessFileUpload(sha256, totalBytes);
    }

    @PostMapping("/upload")
    public ReturnVO<String> uploadFile(@RequestParam("sha256") @NotBlank(message = "sha256不能为空") String sha256,
                                       @RequestParam("file") MultipartFile file,
                                       @RequestParam("startByte") long startByte,
                                       @RequestParam("totalBytes") long totalBytes) throws IOException {
        return fileService.uploadFile(sha256, file.getInputStream(), startByte, totalBytes);
    }
}
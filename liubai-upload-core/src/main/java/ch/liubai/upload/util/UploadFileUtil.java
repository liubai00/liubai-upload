package ch.liubai.upload.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * 上传文件工具类
 *
 * @author ljh
 * @version 1.0
 * @since 2024/1/5 14:43
 */
public class UploadFileUtil {

    private static final int BUFFER_SIZE = 64 * 1024;

    private static final Logger log = LoggerFactory.getLogger(UploadFileUtil.class);

    /**
     * 将文件写入到指定位置
     *
     * @param file      文件
     * @param startByte 开始字节
     * @param filePath  文件路径
     * @throws IOException IO异常
     */
    public static void writeToFile(MultipartFile file, long startByte, String filePath) throws IOException {
        try (InputStream inputStream = file.getInputStream()) {
            writeToFile(inputStream, startByte, Paths.get(filePath), Long.MAX_VALUE);
        }
    }

    /**
     * 将文件写入到指定位置
     *
     * @param inputStream 文件输入流
     * @param startByte   开始字节
     * @param filePath    文件路径
     */
    public static void writeToFile(InputStream inputStream, long startByte, String filePath) {
        try {
            writeToFile(inputStream, startByte, Paths.get(filePath), Long.MAX_VALUE);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Streams one upload chunk into a temporary file. A failed write is rolled back to
     * {@code startByte}, so callers never observe a partially committed chunk.
     *
     * @return number of bytes written
     */
    public static long writeToFile(InputStream inputStream, long startByte, Path filePath, long maxBytes)
            throws IOException {
        if (inputStream == null) {
            throw new IllegalArgumentException("上传内容不能为空");
        }
        if (startByte < 0 || maxBytes < 0) {
            throw new IllegalArgumentException("上传偏移量或分片大小非法");
        }

        try (RandomAccessFile randomAccessFile = new RandomAccessFile(filePath.toFile(), "rw")) {
            if (startByte == 0) {
                randomAccessFile.setLength(0);
            }
            randomAccessFile.seek(startByte);
            byte[] buffer = new byte[BUFFER_SIZE];
            long written = 0;
            try {
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    if (bytesRead == 0) {
                        continue;
                    }
                    if (written > maxBytes - bytesRead) {
                        throw new IllegalArgumentException("上传分片超过文件声明大小");
                    }
                    randomAccessFile.write(buffer, 0, bytesRead);
                    written += bytesRead;
                }
                return written;
            } catch (IOException | RuntimeException e) {
                try {
                    randomAccessFile.setLength(startByte);
                } catch (IOException rollbackError) {
                    e.addSuppressed(rollbackError);
                }
                throw e;
            }
        }
    }

    /**
     * 获取文件
     *
     * @param fileName 文件的名字
     * @param dirUrl   文件所在目录
     * @return 文件
     */
    public static File getFile(String fileName, String dirUrl) {
        return new File(dirUrl, fileName);
    }

    /**
     * 对比文件是否完整
     *
     * @param sha256 文件的SHA256
     * @param file   文件
     * @return true如果文件完整，false如果不完整
     * @throws IOException IO异常
     */
    public static boolean isFileComplete(String sha256, File file) throws IOException, NoSuchAlgorithmException {
        // 实现文件SHA256校验
        // 返回true如果文件完整，false如果不完整
        return calculateSHA256(file).equals(sha256);
    }

    /**
     * 将临时文件移动到上传目录
     *
     * @param tempFile  临时文件
     * @param fileName  文件名
     * @param uploadDir 上传目录
     * @throws IOException IO异常
     */
    public static void moveFileToUploadDir(File tempFile, String fileName, String uploadDir) throws IOException {
        moveFile(tempFile.toPath(), Paths.get(uploadDir).resolve(fileName));
    }

    public static void moveFile(Path source, Path target) throws IOException {
        Files.createDirectories(target.toAbsolutePath().normalize().getParent());
        try {
            Files.move(source, target, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            log.debug("文件系统不支持原子移动，回退到普通移动：{} -> {}", source, target);
            Files.move(source, target);
        }
    }

    /**
     * 计算文件的SHA256
     *
     * @param file 文件
     * @return 文件的SHA256
     * @throws IOException              IO异常
     * @throws NoSuchAlgorithmException 没有这个算法异常
     */
    public static String calculateSHA256(File file) throws IOException, NoSuchAlgorithmException {
        return calculateSHA256(file.toPath());
    }

    public static String calculateSHA256(Path file) throws IOException, NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (InputStream inputStream = new BufferedInputStream(Files.newInputStream(file))) {
            byte[] buffer = new byte[BUFFER_SIZE];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                if (bytesRead > 0) {
                    digest.update(buffer, 0, bytesRead);
                }
            }
        }

        StringBuilder result = new StringBuilder(64);
        for (byte value : digest.digest()) {
            result.append(String.format("%02x", value & 0xff));
        }
        return result.toString();
    }

}

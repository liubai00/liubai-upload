package ch.liubai.upload.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
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

    private static final int BUFFER_SIZE = 4096;

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
        RandomAccessFile raf = new RandomAccessFile(filePath, "rw");
        raf.seek(startByte);
        raf.write(file.getBytes());
        raf.close();
    }

    /**
     * 将文件写入到指定位置
     *
     * @param inputStream 文件输入流
     * @param startByte   开始字节
     * @param filePath    文件路径
     */
    public static void writeToFile(InputStream inputStream, long startByte, String filePath) {
        try (RandomAccessFile raf = new RandomAccessFile(filePath, "rw");
             FileOutputStream fos = new FileOutputStream(raf.getFD());
             BufferedOutputStream bos = new BufferedOutputStream(fos)) {
            raf.seek(startByte);
            byte[] bytes = new byte[1024];
            int len;
            while ((len = inputStream.read(bytes)) != -1) {
                bos.write(bytes, 0, len);
                bos.flush();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
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
        return new File(dirUrl + "/" + fileName);
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
        // 判断上传目录是否存在，不存在则创建
        File uploadDirFile = new File(uploadDir);
        if (!uploadDirFile.exists()) {
            boolean mkdirsResult = uploadDirFile.mkdirs();
            log.info("上传目录不存在，创建上传目录结果为 {}", mkdirsResult);
        }
        File uploadFile = new File(uploadDir + "/" + fileName);
        Files.move(tempFile.toPath(), uploadFile.toPath());

        // 判断文件是否移动成功
        if (!uploadFile.exists() || tempFile.exists()) {
            throw new RuntimeException("文件移动失败");
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
        MessageDigest digest = MessageDigest.getInstance("SHA-256");

        try (FileInputStream fis = new FileInputStream(file); FileChannel channel = fis.getChannel()) {
            ByteBuffer buffer = ByteBuffer.allocate(BUFFER_SIZE);
            long fileSize = channel.size();
            long position = 0;

            while (position < fileSize) {
                buffer.clear();
                int bytesRead = channel.read(buffer);
                if (bytesRead == -1) {
                    break;
                }
                buffer.flip();
                digest.update(buffer);
                position += bytesRead;
            }
        }

        byte[] bytes = digest.digest();

        StringBuilder sb = new StringBuilder();

        for (byte aByte : bytes) {
            sb.append(String.format("%02x", aByte));
        }

        return sb.toString();
    }

    public static void main(String[] args) {
        try {
            System.out.println(calculateSHA256(new File("D:\\data\\temp\\d4c616ca3af8ee66861e9ac774b68d8ec1c13cb588aa3ddbe7f4bb4ec376991f_181320585.tmp")));
        } catch (IOException | NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }

}

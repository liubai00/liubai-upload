package ch.liubai.upload.service.impl;

import ch.liubai.upload.entity.FileMetadata;
import ch.liubai.upload.entity.FileUploadPreprocessResponse;
import ch.liubai.upload.entity.ReturnVO;
import ch.liubai.upload.enums.UploadErrorCodeEnum;
import ch.liubai.upload.metadata.FileMetadataProperties;
import ch.liubai.upload.metadata.FileMetadataStorage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class FileServiceImplTest {

    @TempDir
    Path root;

    private Path tempDirectory;
    private Path uploadDirectory;
    private InMemoryMetadataStorage metadataStorage;
    private FileServiceImpl service;

    @BeforeEach
    void setUp() {
        tempDirectory = root.resolve("temp");
        uploadDirectory = root.resolve("files");
        FileMetadataProperties properties = new FileMetadataProperties();
        properties.setTempDir(tempDirectory.toString());
        properties.setUploadDir(uploadDirectory.toString());
        properties.setMetadataDir(root.resolve("metadata").toString());
        metadataStorage = new InMemoryMetadataStorage();
        service = new FileServiceImpl(properties, metadataStorage);
    }

    @Test
    void uploadsSequentialChunksAndFinalizesOnlyWhenComplete() throws Exception {
        byte[] content = "hello resumable upload".getBytes(StandardCharsets.UTF_8);
        String sha256 = sha256(content);
        int firstChunkLength = 5;

        ReturnVO<String> first = service.uploadFile(
                sha256,
                new ByteArrayInputStream(content, 0, firstChunkLength),
                firstChunkLength,
                0,
                content.length);

        assertTrue(first.isSuccess());
        assertFalse(Files.exists(uploadFile(sha256, content.length)));

        ReturnVO<FileUploadPreprocessResponse> state = service.preprocessFileUpload(sha256, content.length);
        assertTrue(state.isSuccess());
        assertEquals(firstChunkLength, state.getData().getUploadedBytes());
        assertEquals(sha256(slice(content, 0, firstChunkLength)), state.getData().getCurrentSha256());

        ReturnVO<String> second = service.uploadFile(
                sha256,
                new ByteArrayInputStream(content, firstChunkLength, content.length - firstChunkLength),
                content.length - firstChunkLength,
                firstChunkLength,
                content.length);

        assertTrue(second.isSuccess());
        assertArrayEquals(content, Files.readAllBytes(uploadFile(sha256, content.length)));
        assertNotNull(metadataStorage.loadFileMetadata(sha256));
        assertFalse(Files.exists(tempFile(sha256, content.length)));
    }

    @Test
    void rejectsAndResetsACompleteButCorruptedTemporaryFile() throws Exception {
        byte[] expected = "expected".getBytes(StandardCharsets.UTF_8);
        byte[] corrupted = "corrupt!".getBytes(StandardCharsets.UTF_8);
        String sha256 = sha256(expected);
        Files.createDirectories(tempDirectory);
        Files.write(tempFile(sha256, expected.length), corrupted);

        ReturnVO<FileUploadPreprocessResponse> result = service.preprocessFileUpload(sha256, expected.length);

        assertEquals(UploadErrorCodeEnum.SHA256_CHECK_FAIL.getCode().intValue(), result.getCode());
        assertFalse(Files.exists(tempFile(sha256, expected.length)));
        assertFalse(Files.exists(uploadFile(sha256, expected.length)));
    }

    @Test
    void rejectsPathLikeSha256BeforeCreatingAnyFile() throws Exception {
        ReturnVO<String> result = service.uploadFile(
                "../../outside",
                new ByteArrayInputStream(new byte[]{1}),
                1,
                0,
                1);

        assertEquals(UploadErrorCodeEnum.PARAM_EXCEPTION.getCode().intValue(), result.getCode());
        try (Stream<Path> tempFiles = Files.list(tempDirectory);
             Stream<Path> uploadedFiles = Files.list(uploadDirectory)) {
            assertEquals(0, tempFiles.count());
            assertEquals(0, uploadedFiles.count());
        }
    }

    @Test
    void rejectsAnOffsetDifferentFromTheCommittedLength() throws Exception {
        byte[] content = "abcdef".getBytes(StandardCharsets.UTF_8);
        String sha256 = sha256(content);
        service.uploadFile(sha256, new ByteArrayInputStream(content, 0, 3), 3, 0, content.length);

        ReturnVO<String> result = service.uploadFile(
                sha256,
                new ByteArrayInputStream(content, 3, 3),
                3,
                2,
                content.length);

        assertEquals(UploadErrorCodeEnum.UPLOAD_OFFSET_MISMATCH.getCode().intValue(), result.getCode());
        assertArrayEquals(slice(content, 0, 3), Files.readAllBytes(tempFile(sha256, content.length)));
    }

    @Test
    void rejectsAChunkLargerThanTheRemainingFileWithoutWritingIt() throws Exception {
        byte[] content = "four".getBytes(StandardCharsets.UTF_8);
        String declaredSha256 = sha256("abc".getBytes(StandardCharsets.UTF_8));

        ReturnVO<String> result = service.uploadFile(
                declaredSha256,
                new ByteArrayInputStream(content),
                content.length,
                0,
                3);

        assertEquals(UploadErrorCodeEnum.PARAM_EXCEPTION.getCode().intValue(), result.getCode());
        assertFalse(Files.exists(tempFile(declaredSha256, 3)));
    }

    @Test
    void doesNotTrustAnExistingFinalFileByLengthAlone() throws Exception {
        byte[] expected = "right".getBytes(StandardCharsets.UTF_8);
        byte[] corrupted = "wrong".getBytes(StandardCharsets.UTF_8);
        String sha256 = sha256(expected);
        Files.createDirectories(uploadDirectory);
        Files.write(uploadFile(sha256, expected.length), corrupted);

        ReturnVO<FileUploadPreprocessResponse> result = service.preprocessFileUpload(sha256, expected.length);

        assertEquals(UploadErrorCodeEnum.SHA256_CHECK_FAIL.getCode().intValue(), result.getCode());
    }

    @Test
    void restoresTheCompletedFileToTemporaryStorageWhenMetadataSaveFails() throws Exception {
        byte[] content = "metadata rollback".getBytes(StandardCharsets.UTF_8);
        String sha256 = sha256(content);
        FileMetadataStorage failingStorage = new FileMetadataStorage() {
            @Override
            public void addFileMetadata(FileMetadata metadata) throws Exception {
                throw new Exception("database unavailable");
            }

            @Override
            public FileMetadata loadFileMetadata(String ignored) {
                return null;
            }
        };
        FileMetadataProperties properties = new FileMetadataProperties();
        properties.setTempDir(tempDirectory.toString());
        properties.setUploadDir(uploadDirectory.toString());
        properties.setMetadataDir(root.resolve("metadata").toString());
        FileServiceImpl failingService = new FileServiceImpl(properties, failingStorage);

        ReturnVO<String> result = failingService.uploadFile(
                sha256, new ByteArrayInputStream(content), content.length, 0, content.length);

        assertEquals(UploadErrorCodeEnum.SYSTEM_EXCEPTION.getCode().intValue(), result.getCode());
        assertArrayEquals(content, Files.readAllBytes(tempFile(sha256, content.length)));
        assertFalse(Files.exists(uploadFile(sha256, content.length)));
    }

    private Path tempFile(String sha256, long totalBytes) {
        return tempDirectory.resolve(sha256 + "_" + totalBytes + ".tmp");
    }

    private Path uploadFile(String sha256, long totalBytes) {
        return uploadDirectory.resolve(sha256 + "_" + totalBytes);
    }

    private static byte[] slice(byte[] source, int start, int end) {
        byte[] result = new byte[end - start];
        System.arraycopy(source, start, result, 0, result.length);
        return result;
    }

    private static String sha256(byte[] bytes) throws Exception {
        StringBuilder result = new StringBuilder();
        for (byte value : MessageDigest.getInstance("SHA-256").digest(bytes)) {
            result.append(String.format("%02x", value & 0xff));
        }
        return result.toString();
    }

    private static final class InMemoryMetadataStorage implements FileMetadataStorage {
        private final Map<String, FileMetadata> values = new HashMap<>();

        @Override
        public void addFileMetadata(FileMetadata metadata) {
            values.put(metadata.getSha256(), metadata);
        }

        @Override
        public FileMetadata loadFileMetadata(String sha256) {
            return values.get(sha256);
        }
    }
}

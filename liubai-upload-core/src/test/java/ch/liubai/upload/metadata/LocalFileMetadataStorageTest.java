package ch.liubai.upload.metadata;

import ch.liubai.upload.entity.FileMetadata;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class LocalFileMetadataStorageTest {

    @TempDir
    Path directory;

    @Test
    void roundTripsJavaTimeMetadataAsUtf8Json() throws Exception {
        String sha256 = repeat('a', 64);
        LocalFileMetadataStorage storage = new LocalFileMetadataStorage(directory.toString());

        storage.addFileMetadata(new FileMetadata("file", sha256, 12L));
        FileMetadata loaded = storage.loadFileMetadata(sha256);

        assertEquals("file", loaded.getFileName());
        assertEquals(sha256, loaded.getSha256());
        assertEquals(12L, loaded.getFileSize());
        assertNotNull(loaded.getCreateTime());
        assertNotNull(loaded.getUpdateTime());
    }

    private static String repeat(char value, int count) {
        char[] values = new char[count];
        java.util.Arrays.fill(values, value);
        return new String(values);
    }
}

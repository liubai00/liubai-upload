package ch.liubai.upload.metadata;

import ch.liubai.upload.entity.FileMetadata;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class DatabaseFileMetadataStorageTest {

    @Test
    void returnsTheConnectionToThePoolAfterWriting() throws Exception {
        DataSource dataSource = mock(DataSource.class);
        Connection connection = mock(Connection.class);
        PreparedStatement statement = mock(PreparedStatement.class);
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(anyString())).thenReturn(statement);

        DatabaseFileMetadataStorage storage = new DatabaseFileMetadataStorage(dataSource);
        char[] hash = new char[64];
        java.util.Arrays.fill(hash, 'b');
        storage.addFileMetadata(new FileMetadata("file", new String(hash), 1L));

        verify(statement).close();
        verify(connection).close();
    }
}

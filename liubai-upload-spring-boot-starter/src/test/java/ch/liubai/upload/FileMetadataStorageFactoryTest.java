package ch.liubai.upload;

import ch.liubai.upload.metadata.DatabaseFileMetadataStorage;
import ch.liubai.upload.metadata.FileMetadataProperties;
import ch.liubai.upload.metadata.FileMetadataStorage;
import ch.liubai.upload.metadata.LocalFileMetadataStorage;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.sql.DataSource;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.*;

class FileMetadataStorageFactoryTest {

    @TempDir
    Path root;

    @Test
    void explicitMysqlInitializesTheTableAndClosesTheConnection() throws Exception {
        DataSource dataSource = mock(DataSource.class);
        Connection connection = mock(Connection.class);
        Statement statement = mock(Statement.class);
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.createStatement()).thenReturn(statement);

        FileMetadataProperties properties = new FileMetadataProperties();
        properties.setStorageType("mysql");
        FileMetadataStorageFactory factory = new FileMetadataStorageFactory(dataSource, new ObjectMapper());

        FileMetadataStorage storage = factory.create(properties);

        assertTrue(storage instanceof DatabaseFileMetadataStorage);
        verify(statement).execute(contains("CREATE TABLE IF NOT EXISTS file_metadata"));
        verify(statement).close();
        verify(connection).close();
    }

    @Test
    void automaticSelectionDoesNotTreatAnUnrelatedDataSourceAsMysql() throws Exception {
        DataSource dataSource = mock(DataSource.class);
        Connection connection = mock(Connection.class);
        DatabaseMetaData metadata = mock(DatabaseMetaData.class);
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.getMetaData()).thenReturn(metadata);
        when(metadata.getDatabaseProductName()).thenReturn("PostgreSQL");

        FileMetadataProperties properties = new FileMetadataProperties();
        properties.setMetadataDir(root.resolve("metadata").toString());
        FileMetadataStorageFactory factory = new FileMetadataStorageFactory(dataSource, new ObjectMapper());

        FileMetadataStorage storage = factory.create(properties);

        assertTrue(storage instanceof LocalFileMetadataStorage);
        verify(connection).close();
        verify(connection, never()).createStatement();
    }

    @Test
    void automaticSelectionUsesAnAvailableMysqlDataSource() throws Exception {
        DataSource dataSource = mock(DataSource.class);
        Connection detectionConnection = mock(Connection.class);
        Connection schemaConnection = mock(Connection.class);
        DatabaseMetaData metadata = mock(DatabaseMetaData.class);
        Statement statement = mock(Statement.class);
        when(dataSource.getConnection()).thenReturn(detectionConnection, schemaConnection);
        when(detectionConnection.getMetaData()).thenReturn(metadata);
        when(metadata.getDatabaseProductName()).thenReturn("MySQL");
        when(schemaConnection.createStatement()).thenReturn(statement);

        FileMetadataProperties properties = new FileMetadataProperties();
        FileMetadataStorageFactory factory = new FileMetadataStorageFactory(dataSource, new ObjectMapper());

        FileMetadataStorage storage = factory.create(properties);

        assertTrue(storage instanceof DatabaseFileMetadataStorage);
        verify(detectionConnection).close();
        verify(statement).execute(contains("CREATE TABLE IF NOT EXISTS file_metadata"));
        verify(schemaConnection).close();
    }
}

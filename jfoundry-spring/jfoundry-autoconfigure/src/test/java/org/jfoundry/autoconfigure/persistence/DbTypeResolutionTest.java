package org.jfoundry.autoconfigure.persistence;

import com.baomidou.mybatisplus.annotation.DbType;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/// P2-3: DbTypeResolver must (a) honor explicit config, (b) auto-detect from
/// DataSource metadata when not configured.
class DbTypeResolutionTest {

    @Test
    void explicitConfigOverridesAutoDetection() {
        JfoundryPersistenceProperties props = new JfoundryPersistenceProperties();
        props.setDbType(DbType.DM);

        DbType resolved = new DbTypeResolver().resolve(props, stubDataSource("H2", "H2"));

        assertThat(resolved).isEqualTo(DbType.DM);
    }

    @Test
    void autoDetectFromH2WhenNotConfigured() {
        JfoundryPersistenceProperties props = new JfoundryPersistenceProperties();  // dbType = null
        DataSource ds = stubDataSource("H2", "H2");

        DbType resolved = new DbTypeResolver().resolve(props, ds);

        assertThat(resolved).isEqualTo(DbType.H2);
    }

    @Test
    void autoDetectFromDmProduct() {
        JfoundryPersistenceProperties props = new JfoundryPersistenceProperties();
        DataSource ds = stubDataSource("DM DBMS", "8.1");

        DbType resolved = new DbTypeResolver().resolve(props, ds);

        assertThat(resolved).isEqualTo(DbType.DM);
    }

    private DataSource stubDataSource(String productName, String productVersion) {
        DatabaseMetaData meta = mock(DatabaseMetaData.class);
        Connection conn = mock(Connection.class);
        DataSource ds = mock(DataSource.class);
        try {
            when(meta.getDatabaseProductName()).thenReturn(productName);
            when(meta.getDatabaseProductVersion()).thenReturn(productVersion);
            when(conn.getMetaData()).thenReturn(meta);
            when(ds.getConnection()).thenReturn(conn);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return ds;
    }
}

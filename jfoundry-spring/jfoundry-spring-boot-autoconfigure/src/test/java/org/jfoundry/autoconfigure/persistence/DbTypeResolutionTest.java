package org.jfoundry.autoconfigure.persistence;

import com.baomidou.mybatisplus.annotation.DbType;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.logging.Logger;

import static org.assertj.core.api.Assertions.assertThat;

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
        return new StubDataSource(productName, productVersion);
    }

    private record StubDataSource(String productName, String productVersion) implements DataSource {
        @Override
        public Connection getConnection() {
            return (Connection) java.lang.reflect.Proxy.newProxyInstance(
                    Connection.class.getClassLoader(),
                    new Class<?>[]{Connection.class},
                    (proxy, method, args) -> switch (method.getName()) {
                        case "getMetaData" -> metadata();
                        case "close" -> null;
                        case "isClosed" -> false;
                        case "unwrap" -> proxy;
                        case "isWrapperFor" -> false;
                        default -> throw new SQLFeatureNotSupportedException(method.getName());
                    });
        }

        @Override
        public Connection getConnection(String username, String password) {
            return getConnection();
        }

        private DatabaseMetaData metadata() {
            return (DatabaseMetaData) java.lang.reflect.Proxy.newProxyInstance(
                    DatabaseMetaData.class.getClassLoader(),
                    new Class<?>[]{DatabaseMetaData.class},
                    (proxy, method, args) -> switch (method.getName()) {
                        case "getDatabaseProductName" -> productName;
                        case "getDatabaseProductVersion" -> productVersion;
                        case "unwrap" -> proxy;
                        case "isWrapperFor" -> false;
                        default -> throw new SQLFeatureNotSupportedException(method.getName());
                    });
        }

        @Override
        public PrintWriter getLogWriter() {
            return null;
        }

        @Override
        public void setLogWriter(PrintWriter out) {
        }

        @Override
        public void setLoginTimeout(int seconds) {
        }

        @Override
        public int getLoginTimeout() {
            return 0;
        }

        @Override
        public Logger getParentLogger() {
            return Logger.getGlobal();
        }

        @Override
        public <T> T unwrap(Class<T> iface) throws SQLException {
            if (iface.isInstance(this)) {
                return iface.cast(this);
            }
            throw new SQLException("Not a wrapper for " + iface.getName());
        }

        @Override
        public boolean isWrapperFor(Class<?> iface) {
            return iface.isInstance(this);
        }
    }
}

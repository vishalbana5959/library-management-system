package com.library.utils;

import java.sql.*;
import java.util.Properties;
import java.io.InputStream;
import org.apache.commons.dbcp2.BasicDataSource;

public class DBConnection {
    private static BasicDataSource dataSource;
    private static final int INITIAL_POOL_SIZE = 5;
    private static final int MAX_POOL_SIZE = 20;
    private static final long MAX_WAIT_MILLIS = 5000; // 5 seconds

    static {
        initializeDataSource();
    }

    private static void initializeDataSource() {
        try (InputStream input = DBConnection.class.getClassLoader()
                .getResourceAsStream("config.properties")) {

            if (input == null) {
                throw new RuntimeException("config.properties not found in classpath");
            }

            Properties prop = new Properties();
            prop.load(input);

            // Validate required properties
            String url = prop.getProperty("db.url");
            String username = prop.getProperty("db.user");
            String password = prop.getProperty("db.password");

            if (url == null || username == null || password == null) {
                throw new RuntimeException("Missing required database configuration in config.properties");
            }

            // Set up connection pool
            dataSource = new BasicDataSource();
            dataSource.setUrl(url);
            dataSource.setUsername(username);
            dataSource.setPassword(password);
            dataSource.setDriverClassName("com.mysql.cj.jdbc.Driver");

            // Connection pool configuration
            dataSource.setInitialSize(INITIAL_POOL_SIZE);
            dataSource.setMaxTotal(MAX_POOL_SIZE);
            dataSource.setMaxWaitMillis(MAX_WAIT_MILLIS);
            dataSource.setValidationQuery("SELECT 1");
            dataSource.setTestOnBorrow(true);
            dataSource.setTestWhileIdle(true);
            dataSource.setTimeBetweenEvictionRunsMillis(30000); // 30 seconds
            dataSource.setMinEvictableIdleTimeMillis(60000); // 1 minute

            // Test the initial connection
            try (Connection testConn = dataSource.getConnection()) {
                if (!testConn.isValid(5)) {
                    throw new SQLException("Failed to establish valid database connection");
                }
            }

        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize database connection pool: " + e.getMessage(), e);
        }
    }

    /**
     * Gets a database connection from the pool
     * @return Connection object
     * @throws SQLException if a database access error occurs
     */
    public static Connection getConnection() throws SQLException {
        try {
            Connection conn = dataSource.getConnection();
            // Set reasonable defaults for library operations
            conn.setAutoCommit(true);
            conn.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
            return conn;
        } catch (SQLException e) {
            throw new SQLException("Failed to get database connection: " + e.getMessage(), e);
        }
    }

    /**
     * Returns a connection to the pool
     * @param conn Connection to close
     */
    public static void closeConnection(Connection conn) {
        if (conn != null) {
            try {
                if (!conn.isClosed()) {
                    conn.close(); // Returns to pool
                }
            } catch (SQLException e) {
                System.err.println("Error returning connection to pool: " + e.getMessage());
            }
        }
    }

    /**
     * Closes other JDBC resources (Statement, ResultSet)
     * @param resources Varargs of AutoCloseable resources
     */
    public static void closeResources(AutoCloseable... resources) {
        for (AutoCloseable resource : resources) {
            if (resource != null) {
                try {
                    resource.close();
                } catch (Exception e) {
                    System.err.println("Error closing resource: " + e.getMessage());
                }
            }
        }
    }

    /**
     * Rolls back a transaction and logs any errors
     * @param conn Connection to rollback
     */
    public static void rollbackTransaction(Connection conn) {
        if (conn != null) {
            try {
                if (!conn.getAutoCommit() && !conn.isClosed()) {
                    conn.rollback();
                }
            } catch (SQLException e) {
                System.err.println("Error rolling back transaction: " + e.getMessage());
            }
        }
    }

    /**
     * Shuts down the connection pool
     */
    public static void shutdown() {
        if (dataSource != null) {
            try {
                dataSource.close();
            } catch (SQLException e) {
                System.err.println("Error shutting down connection pool: " + e.getMessage());
            }
        }
    }
}
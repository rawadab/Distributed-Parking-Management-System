package com.example.shared.utils;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Utility class for managing database connections using HikariCP.
 *
 * This class provides methods for establishing, managing, and closing connections to the database.
 * It includes load balancing between multiple database servers, optimizing query execution using
 * prepared statements, and handling connection pooling for better performance and reliability.
 *
 * **Key Features:**
 * - Load balancing between multiple database servers.
 * - Connection pooling for enhanced performance.
 * - Automatic handling of connection failures and reconnections.
 * - Optimized query execution using cached prepared statements.
 *
 * @author
 * @version 1.0
 * @since 2024
 */
public class DatabaseUtil {

    /**
     * The JDBC URL for the database, configured with load balancing and additional parameters for optimization.
     */
    private static final String DB_URL = "jdbc:mysql:loadbalance://100.85.154.51:3306,100.76.110.20:3306/muligansystem"
            + "?autoReconnect=true"                 // Automatically reconnect in case of connection loss.
            + "&failOverReadOnly=false"             // Allows write operations after failover.
            + "&connectTimeout=1000"                // Maximum time to wait for a new connection (in milliseconds).
            + "&socketTimeout=3000"                 // Maximum time to wait for data read/write (in milliseconds).
            + "&loadBalanceBlacklistTimeout=10000"  // Time (in milliseconds) to blacklist a failed server for future connections.
            + "&loadBalanceStrategy=leastConnections"     // Load balancing strategy to evenly distribute traffic across servers.
            + "&cachePrepStmts=true"                // Enables caching of prepared statements in the client.
            + "&prepStmtCacheSize=250"              // Number of prepared statements that can be cached.
            + "&prepStmtCacheSqlLimit=2048"         // Maximum size of a SQL statement (in bytes) that can be cached.
            + "&useServerPrepStmts=true";           // Enables use of server-side prepared statements.

    /**
     * The username for authenticating with the database.
     */
    private static final String USERNAME = "root";

    /**
     * The password for authenticating with the database.
     */
    private static final String PASSWORD = "root";

    /**
     * HikariCP connection pool for managing database connections.
     */
    private static HikariDataSource dataSource;

    // Static block to initialize the connection pool.
    static {
        try {
            HikariConfig config = new HikariConfig();
            config.setJdbcUrl(DB_URL);
            config.setUsername(USERNAME);
            config.setPassword(PASSWORD);

            // Configure connection pool settings.
            config.setMaximumPoolSize(20); // Maximum number of connections in the pool.
            config.setMinimumIdle(5);     // Minimum number of idle connections.
            config.setIdleTimeout(10000); // Time to close an idle connection (in milliseconds).
            config.setConnectionTimeout(3000); // Maximum wait time for a connection (in milliseconds).
            config.setMaxLifetime(1800000); // Maximum lifetime of a connection (30 minutes).

            dataSource = new HikariDataSource(config); // Create the connection pool.
        } catch (Exception e) {
            throw new RuntimeException("Error initializing HikariDataSource", e);
        }
    }

    /**
     * Provides an active database connection from the connection pool.
     *
     * @return A {@link Connection} object for interacting with the database.
     * @throws SQLException If the connection pool is unavailable or closed, or if a connection cannot be retrieved.
     */
    public static Connection connect() throws SQLException {
        if (dataSource == null || dataSource.isClosed()) {
            throw new SQLException("Connection pool is not available or has been closed.");
        }
        return dataSource.getConnection();
    }

    /**
     * Closes database resources (Connection, PreparedStatement, ResultSet).
     * Ensures resources are released in the correct order and handles exceptions gracefully.
     *
     * @param conn The {@link Connection} object to be closed (can be null).
     * @param stmt The {@link PreparedStatement} object to be closed (can be null).
     * @param rs   The {@link ResultSet} object to be closed (can be null).
     */
    public static void closeResources(Connection
                                              conn, PreparedStatement stmt, ResultSet rs) {
        try {
            if (rs != null) rs.close(); // Close the ResultSet if it exists.
            if (stmt != null) stmt.close(); // Close the PreparedStatement if it exists.
            if (conn != null && !conn.isClosed()) conn.close(); // Close the Connection if it exists and is open.
        } catch (SQLException e) {
            e.printStackTrace(); // Print stack trace if an error occurs while closing resources.
        }
    }

    /**
     * Resets the connection pool by closing the existing pool and reinitializing it.
     * This can be useful in scenarios where the connection pool configuration needs to be updated.
     *
     * @throws SQLException If an error occurs while resetting the connection pool.
     */
    public static void resetConnectionPool() throws SQLException {
        close(); // Ensure the old connection pool is closed.
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(DB_URL); // Ensure the JDBC URL is correctly configured.
        config.setUsername(USERNAME);
        config.setPassword(PASSWORD);
        config.setMaximumPoolSize(20); // Maximum number of connections in the new pool.
        config.setConnectionTimeout(5000); // Timeout for obtaining connections.
        dataSource = new HikariDataSource(config); // Reinitialize the connection pool.
    }

    /**
     * Closes the connection pool and releases all resources.
     * This method should be called when the application shuts down to prevent resource leaks.
     */
    public static void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close(); // Close the connection pool if it exists and is open.
        }
    }
}

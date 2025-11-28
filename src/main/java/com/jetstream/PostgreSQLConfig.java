package com.jetstream.database;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.logging.Logger;

/**
 * PostgreSQL Configuration with simple connection pooling.
 * Manages PostgreSQL connections for dual-write functionality.
 */
public class PostgreSQLConfig {

    private static final Logger logger = Logger.getLogger(PostgreSQLConfig.class.getName());
    private static BlockingQueue<Connection> connectionPool;
    private static boolean enabled = false;
    private static boolean initialized = false;
    private static int maxRetries = 3;
    private static long retryDelayMs = 1000;
    private static int poolSize = 5;

    // Connection settings
    private static String dbUrl;
    private static String dbUser;
    private static String dbPassword;

    // Configuration keys
    private static final String PROP_ENABLED = "postgres.dualwrite.enabled";
    private static final String PROP_URL = "postgres.url";
    private static final String PROP_USER = "postgres.user";
    private static final String PROP_PASSWORD = "postgres.password";
    private static final String PROP_POOL_SIZE = "postgres.pool.size";
    private static final String PROP_MAX_RETRIES = "postgres.max.retries";
    private static final String PROP_RETRY_DELAY = "postgres.retry.delay.ms";
    private static final String PROP_ASYNC_WRITES = "postgres.async.writes";

    private static boolean asyncWrites = true;

    /**
     * Initialize PostgreSQL connection pool from config.properties
     */
    public static void init() {
        Properties prop = new Properties();
        try (InputStream input = PostgreSQLConfig.class.getClassLoader().getResourceAsStream("config.properties")) {
            if (input == null) {
                logger.warning("config.properties not found, PostgreSQL disabled");
                return;
            }
            prop.load(input);

            // Check if dual-write is enabled
            enabled = Boolean.parseBoolean(prop.getProperty(PROP_ENABLED, "false"));
            if (!enabled) {
                logger.info("PostgreSQL dual-write is disabled in config");
                return;
            }

            dbUrl = prop.getProperty(PROP_URL);
            dbUser = prop.getProperty(PROP_USER);
            dbPassword = prop.getProperty(PROP_PASSWORD);
            poolSize = Integer.parseInt(prop.getProperty(PROP_POOL_SIZE, "5"));
            maxRetries = Integer.parseInt(prop.getProperty(PROP_MAX_RETRIES, "3"));
            retryDelayMs = Long.parseLong(prop.getProperty(PROP_RETRY_DELAY, "1000"));
            asyncWrites = Boolean.parseBoolean(prop.getProperty(PROP_ASYNC_WRITES, "true"));

            if (dbUrl == null || dbUser == null || dbPassword == null) {
                logger.warning("PostgreSQL configuration incomplete, dual-write disabled");
                enabled = false;
                return;
            }

            // Load PostgreSQL driver
            try {
                Class.forName("org.postgresql.Driver");
            } catch (ClassNotFoundException e) {
                logger.severe("PostgreSQL JDBC driver not found");
                enabled = false;
                return;
            }

            // Initialize simple connection pool
            connectionPool = new ArrayBlockingQueue<>(poolSize);
            int successfulConnections = 0;
            for (int i = 0; i < poolSize; i++) {
                try {
                    Connection conn = createConnection();
                    if (conn != null) {
                        connectionPool.offer(conn);
                        successfulConnections++;
                    }
                } catch (SQLException e) {
                    logger.warning("Failed to create pool connection " + (i+1) + ": " + e.getMessage());
                }
            }

            if (successfulConnections > 0) {
                initialized = true;
                logger.info("✓ PostgreSQL connection pool initialized: " + successfulConnections + "/" + poolSize + " connections");
                logger.info("  Async Writes: " + asyncWrites);
            } else {
                logger.severe("Failed to create any PostgreSQL connections, dual-write disabled");
                enabled = false;
            }

        } catch (Exception e) {
            logger.severe("Error loading PostgreSQL config: " + e.getMessage());
            enabled = false;
        }
    }

    /**
     * Create a new database connection
     */
    private static Connection createConnection() throws SQLException {
        return DriverManager.getConnection(dbUrl, dbUser, dbPassword);
    }

    /**
     * Get a connection from the pool
     */
    public static Connection getConnection() throws SQLException {
        if (!enabled || connectionPool == null) {
            return null;
        }

        Connection conn = connectionPool.poll();
        if (conn != null) {
            try {
                if (conn.isClosed() || !conn.isValid(2)) {
                    conn = createConnection();
                }
            } catch (SQLException e) {
                conn = createConnection();
            }
        } else {
            // Pool exhausted, create new connection
            conn = createConnection();
        }
        return conn;
    }

    /**
     * Return a connection to the pool
     */
    public static void releaseConnection(Connection conn) {
        if (conn != null && connectionPool != null) {
            try {
                if (!conn.isClosed() && conn.isValid(1)) {
                    if (!connectionPool.offer(conn)) {
                        conn.close(); // Pool full, close connection
                    }
                } else {
                    conn.close();
                }
            } catch (SQLException e) {
                try { conn.close(); } catch (SQLException ignored) {}
            }
        }
    }

    /**
     * Check if PostgreSQL dual-write is enabled
     */
    public static boolean isEnabled() {
        return enabled && initialized;
    }

    /**
     * Check if async writes are enabled
     */
    public static boolean isAsyncWritesEnabled() {
        return asyncWrites;
    }

    /**
     * Get max retry count
     */
    public static int getMaxRetries() {
        return maxRetries;
    }

    /**
     * Get retry delay in milliseconds
     */
    public static long getRetryDelayMs() {
        return retryDelayMs;
    }

    /**
     * Enable or disable dual-write at runtime
     */
    public static void setEnabled(boolean enable) {
        enabled = enable;
        logger.info("PostgreSQL dual-write " + (enable ? "enabled" : "disabled"));
    }

    /**
     * Close the connection pool
     */
    public static void close() {
        if (connectionPool != null) {
            Connection conn;
            while ((conn = connectionPool.poll()) != null) {
                try {
                    conn.close();
                } catch (SQLException ignored) {}
            }
            logger.info("PostgreSQL connection pool closed");
        }
        initialized = false;
    }
}


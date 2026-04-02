package com.orangehrm.db;

import com.orangehrm.config.ConfigManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * JDBC-based database utility for OrangeHRM test validations.
 *
 * <h3>Flow name: DatabaseValidationFlow</h3>
 * <p>Single entrypoint for all direct database operations during test execution.
 * Reads connection parameters from {@link ConfigManager}, which supports
 * environment-switching (dev/qa/staging/production) via properties files and
 * environment variables. No values are hardcoded.</p>
 *
 * <h3>Contract</h3>
 * <ul>
 *   <li><b>Inputs:</b> SQL statements (String), optional bind parameters (Object[]).</li>
 *   <li><b>Outputs:</b> Query results as {@code List<Map<String,Object>>};
 *       row counts for DML operations; boolean for existence checks.</li>
 *   <li><b>Errors:</b> {@link DatabaseException} wrapping {@link SQLException}
 *       with added context (operation name, SQL statement).</li>
 *   <li><b>Side-effects:</b> Opens/closes JDBC connections. DML statements
 *       modify the target database.</li>
 * </ul>
 *
 * <h3>Configuration keys (resolved by ConfigManager)</h3>
 * <ul>
 *   <li>{@code db.url} — JDBC URL, e.g. {@code jdbc:mysql://host:3306/orangehrm}</li>
 *   <li>{@code db.username} — database username</li>
 *   <li>{@code db.password} — database password</li>
 *   <li>{@code db.driver} — JDBC driver class (e.g. {@code com.mysql.cj.jdbc.Driver})</li>
 * </ul>
 *
 * <h3>Observability</h3>
 * <p>All operations log start, result summary, and failure context at
 * appropriate levels (INFO for lifecycle, DEBUG for queries, ERROR for failures).</p>
 *
 * <h3>Failure modes</h3>
 * <ol>
 *   <li>DB not configured — {@link DatabaseException} with clear message.</li>
 *   <li>Connection failure — wraps underlying SQLException with context.</li>
 *   <li>SQL execution failure — wraps with the SQL statement for debugging.</li>
 * </ol>
 */
// PUBLIC_INTERFACE
public final class DatabaseUtil {

    private static final Logger LOG = LoggerFactory.getLogger(DatabaseUtil.class);

    private DatabaseUtil() {
        // Utility class — no instantiation
    }

    // ─── Connection Management ───

    /**
     * Open a new JDBC connection using the active environment's configuration.
     * The caller is responsible for closing the returned connection.
     *
     * @return a live JDBC {@link Connection}
     * @throws DatabaseException if the database is not configured or the connection fails
     */
    // PUBLIC_INTERFACE
    public static Connection getConnection() {
        LOG.debug("DatabaseUtil.getConnection — environment: {}", ConfigManager.getActiveEnvironment());

        if (!ConfigManager.isDatabaseConfigured()) {
            throw new DatabaseException(
                    "Database is not configured. Set db.url, db.username, db.password, "
                            + "and db.driver via properties files or environment variables. "
                            + "Active environment: " + ConfigManager.getActiveEnvironment());
        }

        String url = ConfigManager.getDbUrl();
        String username = ConfigManager.getDbUsername();
        String password = ConfigManager.getDbPassword();
        String driver = ConfigManager.getDbDriver();

        try {
            if (!driver.isEmpty()) {
                Class.forName(driver);
                LOG.debug("JDBC driver loaded: {}", driver);
            }
            Connection conn = DriverManager.getConnection(url, username, password);
            LOG.info("Database connection established — URL: {}", url);
            return conn;
        } catch (ClassNotFoundException e) {
            throw new DatabaseException("JDBC driver class not found: " + driver, e);
        } catch (SQLException e) {
            throw new DatabaseException(
                    "Failed to connect to database — URL: " + url + ", user: " + username, e);
        }
    }

    /**
     * Close a JDBC connection quietly, logging any errors without throwing.
     *
     * @param conn the connection to close (may be {@code null})
     */
    // PUBLIC_INTERFACE
    public static void closeQuietly(Connection conn) {
        if (conn != null) {
            try {
                conn.close();
                LOG.debug("Database connection closed");
            } catch (SQLException e) {
                LOG.warn("Error closing database connection: {}", e.getMessage());
            }
        }
    }

    // ─── Query Operations ───

    /**
     * Execute a SELECT query and return all rows as a list of ordered maps.
     * Each map key is the column name (lower-cased), each value is the column value.
     *
     * @param sql    the SELECT statement (may contain {@code ?} placeholders)
     * @param params bind parameters (positional, matching {@code ?} placeholders)
     * @return list of row maps (empty list when no rows match)
     * @throws DatabaseException on any SQL error
     */
    // PUBLIC_INTERFACE
    public static List<Map<String, Object>> executeQuery(String sql, Object... params) {
        LOG.info("DatabaseUtil.executeQuery — SQL: {}", sql);
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            conn = getConnection();
            stmt = conn.prepareStatement(sql);
            bindParameters(stmt, params);
            rs = stmt.executeQuery();

            List<Map<String, Object>> results = mapResultSet(rs);
            LOG.info("Query returned {} row(s)", results.size());
            return results;
        } catch (SQLException e) {
            throw new DatabaseException("Query execution failed — SQL: " + sql, e);
        } finally {
            closeQuietly(rs);
            closeQuietly(stmt);
            closeQuietly(conn);
        }
    }

    /**
     * Execute a DML statement (INSERT, UPDATE, DELETE) and return the number
     * of affected rows.
     *
     * @param sql    the DML statement (may contain {@code ?} placeholders)
     * @param params bind parameters
     * @return number of rows affected
     * @throws DatabaseException on any SQL error
     */
    // PUBLIC_INTERFACE
    public static int executeUpdate(String sql, Object... params) {
        LOG.info("DatabaseUtil.executeUpdate — SQL: {}", sql);
        Connection conn = null;
        PreparedStatement stmt = null;
        try {
            conn = getConnection();
            stmt = conn.prepareStatement(sql);
            bindParameters(stmt, params);
            int affected = stmt.executeUpdate();
            LOG.info("Update affected {} row(s)", affected);
            return affected;
        } catch (SQLException e) {
            throw new DatabaseException("Update execution failed — SQL: " + sql, e);
        } finally {
            closeQuietly(stmt);
            closeQuietly(conn);
        }
    }

    // ─── Convenience Methods for Test Validations ───

    /**
     * Check whether at least one row exists that matches the given query.
     *
     * @param sql    a SELECT statement
     * @param params bind parameters
     * @return {@code true} if the query returns one or more rows
     */
    // PUBLIC_INTERFACE
    public static boolean recordExists(String sql, Object... params) {
        List<Map<String, Object>> rows = executeQuery(sql, params);
        boolean exists = !rows.isEmpty();
        LOG.info("recordExists — result: {}", exists);
        return exists;
    }

    /**
     * Return the count of rows matching a query. The query must return a single
     * numeric column (e.g. {@code SELECT COUNT(*) FROM ...}).
     *
     * @param sql    a COUNT query
     * @param params bind parameters
     * @return the count value
     * @throws DatabaseException if the query fails or returns unexpected results
     */
    // PUBLIC_INTERFACE
    public static long getRowCount(String sql, Object... params) {
        List<Map<String, Object>> rows = executeQuery(sql, params);
        if (rows.isEmpty()) {
            return 0;
        }
        Object value = rows.get(0).values().iterator().next();
        long count = ((Number) value).longValue();
        LOG.info("getRowCount — result: {}", count);
        return count;
    }

    /**
     * Retrieve a single column value from the first row of a query result.
     *
     * @param sql        a SELECT statement expected to return at least one row
     * @param columnName the column to retrieve (case-insensitive)
     * @param params     bind parameters
     * @return the column value, or {@code null} if no rows are returned
     */
    // PUBLIC_INTERFACE
    public static Object getSingleValue(String sql, String columnName, Object... params) {
        List<Map<String, Object>> rows = executeQuery(sql, params);
        if (rows.isEmpty()) {
            LOG.warn("getSingleValue — no rows returned for column '{}'", columnName);
            return null;
        }
        Object value = rows.get(0).get(columnName.toLowerCase());
        LOG.debug("getSingleValue — column '{}' = {}", columnName, value);
        return value;
    }

    /**
     * Insert a row into the specified table using the provided column-value map.
     *
     * @param tableName the target table name
     * @param data      map of column names to values
     * @return number of rows inserted (typically 1)
     */
    // PUBLIC_INTERFACE
    public static int insertRow(String tableName, Map<String, Object> data) {
        if (data == null || data.isEmpty()) {
            throw new DatabaseException("Cannot insert empty data into table: " + tableName);
        }

        StringBuilder columns = new StringBuilder();
        StringBuilder placeholders = new StringBuilder();
        List<Object> values = new ArrayList<>();

        for (Map.Entry<String, Object> entry : data.entrySet()) {
            if (columns.length() > 0) {
                columns.append(", ");
                placeholders.append(", ");
            }
            columns.append(entry.getKey());
            placeholders.append("?");
            values.add(entry.getValue());
        }

        String sql = String.format("INSERT INTO %s (%s) VALUES (%s)",
                tableName, columns, placeholders);
        LOG.info("insertRow — table: {}, columns: {}", tableName, columns);
        return executeUpdate(sql, values.toArray());
    }

    /**
     * Update rows in the specified table matching the given WHERE clause.
     *
     * @param tableName   the target table name
     * @param setData     map of column names to new values
     * @param whereClause the WHERE condition (without the keyword {@code WHERE})
     * @param whereParams bind parameters for the WHERE clause
     * @return number of rows updated
     */
    // PUBLIC_INTERFACE
    public static int updateRows(String tableName, Map<String, Object> setData,
                                 String whereClause, Object... whereParams) {
        if (setData == null || setData.isEmpty()) {
            throw new DatabaseException("Cannot update with empty data on table: " + tableName);
        }

        StringBuilder setClause = new StringBuilder();
        List<Object> allParams = new ArrayList<>();

        for (Map.Entry<String, Object> entry : setData.entrySet()) {
            if (setClause.length() > 0) {
                setClause.append(", ");
            }
            setClause.append(entry.getKey()).append(" = ?");
            allParams.add(entry.getValue());
        }

        // Append WHERE params after SET params
        for (Object wp : whereParams) {
            allParams.add(wp);
        }

        String sql = String.format("UPDATE %s SET %s WHERE %s",
                tableName, setClause, whereClause);
        LOG.info("updateRows — table: {}, SET: {}, WHERE: {}", tableName, setClause, whereClause);
        return executeUpdate(sql, allParams.toArray());
    }

    /**
     * Delete rows from the specified table matching the given WHERE clause.
     *
     * @param tableName   the target table name
     * @param whereClause the WHERE condition (without the keyword {@code WHERE})
     * @param whereParams bind parameters for the WHERE clause
     * @return number of rows deleted
     */
    // PUBLIC_INTERFACE
    public static int deleteRows(String tableName, String whereClause, Object... whereParams) {
        String sql = String.format("DELETE FROM %s WHERE %s", tableName, whereClause);
        LOG.info("deleteRows — table: {}, WHERE: {}", tableName, whereClause);
        return executeUpdate(sql, whereParams);
    }

    // ─── Internal Helpers ───

    /**
     * Bind positional parameters to a prepared statement.
     */
    private static void bindParameters(PreparedStatement stmt, Object[] params) throws SQLException {
        if (params != null) {
            for (int i = 0; i < params.length; i++) {
                stmt.setObject(i + 1, params[i]);
            }
        }
    }

    /**
     * Convert a {@link ResultSet} into a list of ordered maps.
     * Column names are lower-cased for consistent access.
     */
    private static List<Map<String, Object>> mapResultSet(ResultSet rs) throws SQLException {
        List<Map<String, Object>> results = new ArrayList<>();
        ResultSetMetaData meta = rs.getMetaData();
        int columnCount = meta.getColumnCount();

        while (rs.next()) {
            Map<String, Object> row = new LinkedHashMap<>();
            for (int i = 1; i <= columnCount; i++) {
                String colName = meta.getColumnLabel(i).toLowerCase();
                row.put(colName, rs.getObject(i));
            }
            results.add(row);
        }
        return results;
    }

    /**
     * Close a {@link ResultSet} quietly.
     */
    private static void closeQuietly(ResultSet rs) {
        if (rs != null) {
            try {
                rs.close();
            } catch (SQLException e) {
                LOG.warn("Error closing ResultSet: {}", e.getMessage());
            }
        }
    }

    /**
     * Close a {@link Statement} quietly.
     */
    private static void closeQuietly(Statement stmt) {
        if (stmt != null) {
            try {
                stmt.close();
            } catch (SQLException e) {
                LOG.warn("Error closing Statement: {}", e.getMessage());
            }
        }
    }
}

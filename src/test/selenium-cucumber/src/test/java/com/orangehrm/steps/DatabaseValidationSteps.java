package com.orangehrm.steps;

import com.orangehrm.config.ConfigManager;
import com.orangehrm.db.DatabaseException;
import com.orangehrm.db.DatabaseUtil;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;
import static org.junit.Assume.assumeTrue;

/**
 * Cucumber step definitions for database validation scenarios.
 *
 * <h3>Flow name: DatabaseValidationFlow</h3>
 * <p>Provides Gherkin steps to validate data insertion, updates, and deletions
 * directly against the OrangeHRM MySQL database. All database connectivity is
 * resolved through {@link ConfigManager}, enabling environment-switching
 * (dev/qa/staging/production) with no hardcoded values.</p>
 *
 * <h3>Contract</h3>
 * <ul>
 *   <li><b>Inputs:</b> table names, column names/values, WHERE clauses from
 *       Gherkin steps.</li>
 *   <li><b>Outputs:</b> JUnit assertions that pass or fail the scenario.</li>
 *   <li><b>Errors:</b> {@link DatabaseException} propagated as scenario failures
 *       with full context logging.</li>
 *   <li><b>Side-effects:</b> DML operations (insert/update/delete) modify the
 *       target database. Cleanup steps are provided to revert changes.</li>
 * </ul>
 *
 * <h3>Observability</h3>
 * <p>Every step logs its intent, parameters, and outcome at INFO level.
 * Failures include the SQL context at ERROR level.</p>
 */
// PUBLIC_INTERFACE
public class DatabaseValidationSteps {

    private static final Logger LOG = LoggerFactory.getLogger(DatabaseValidationSteps.class);

    /** Stores the last affected row count from a DML operation. */
    private int lastAffectedRows;

    /** Stores the last query result set for multi-step assertions. */
    private List<Map<String, Object>> lastQueryResults;

    /** Stores dynamic data for the current scenario (table, columns, where clause). */
    private String currentTable;
    private final Map<String, Object> currentRowData = new HashMap<>();
    private String currentWhereClause;
    private Object[] currentWhereParams;

    // ─── Given Steps (Preconditions) ───

    /**
     * Verify that the database connection is available for the active environment.
     *
     * <p>Uses JUnit's {@code assumeTrue} so that scenarios are <b>skipped</b>
     * (not failed) when the database is not configured. This is the correct
     * semantic because DB configuration is optional — the CI environment may
     * not have a MySQL/MariaDB instance available.</p>
     */
    @Given("the database connection is configured for the current environment")
    public void theDatabaseConnectionIsConfiguredForTheCurrentEnvironment() {
        LOG.info("Step: Verifying database connection for environment '{}'",
                ConfigManager.getActiveEnvironment());

        boolean dbConfigured = ConfigManager.isDatabaseConfigured();
        if (!dbConfigured) {
            LOG.warn("Database is NOT configured (db.url is empty). "
                    + "Skipping database scenario. To enable, set db.url, db.username, "
                    + "db.password, db.driver in properties or environment variables.");
        }
        // assumeTrue will throw AssumptionViolatedException when false,
        // which Cucumber/JUnit treats as SKIPPED rather than FAILED.
        assumeTrue(
                "Database is not configured — skipping. Set db.url, db.username, "
                        + "db.password, db.driver in properties or environment variables.",
                dbConfigured);

        // Verify we can actually connect
        try {
            java.sql.Connection conn = DatabaseUtil.getConnection();
            DatabaseUtil.closeQuietly(conn);
            LOG.info("Database connection verified successfully");
        } catch (DatabaseException e) {
            LOG.error("Database connection failed: {}", e.getMessage());
            fail("Cannot connect to database: " + e.getMessage());
        }
    }

    /**
     * Ensure a specific table exists in the database.
     */
    @Given("the table {string} exists in the database")
    public void theTableExistsInTheDatabase(String tableName) {
        LOG.info("Step: Verifying table '{}' exists", tableName);
        currentTable = tableName;
        boolean exists = DatabaseUtil.recordExists(
                "SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES "
                        + "WHERE TABLE_NAME = ? AND TABLE_SCHEMA = DATABASE()",
                tableName);
        assertTrue("Table '" + tableName + "' does not exist in the database", exists);
        LOG.info("Table '{}' exists — confirmed", tableName);
    }

    /**
     * Ensure a record matching the given column/value pair exists before the test.
     */
    @Given("a record exists in {string} where {string} = {string}")
    public void aRecordExistsInTableWhereColumnEqualsValue(String tableName, String column, String value) {
        LOG.info("Step: Verifying record exists in '{}' where {} = '{}'", tableName, column, value);
        currentTable = tableName;
        currentWhereClause = column + " = ?";
        currentWhereParams = new Object[]{value};

        boolean exists = DatabaseUtil.recordExists(
                String.format("SELECT * FROM %s WHERE %s = ?", tableName, column), value);
        assertTrue(
                String.format("Expected record in '%s' where %s = '%s' but none found",
                        tableName, column, value),
                exists);
        LOG.info("Record confirmed in '{}' where {} = '{}'", tableName, column, value);
    }

    /**
     * Ensure no record matching the given column/value pair exists (clean slate).
     */
    @Given("no record exists in {string} where {string} = {string}")
    public void noRecordExistsInTableWhereColumnEqualsValue(String tableName, String column, String value) {
        LOG.info("Step: Ensuring no record in '{}' where {} = '{}'", tableName, column, value);
        currentTable = tableName;

        boolean exists = DatabaseUtil.recordExists(
                String.format("SELECT * FROM %s WHERE %s = ?", tableName, column), value);
        if (exists) {
            LOG.info("Pre-existing record found — deleting for clean state");
            int deleted = DatabaseUtil.deleteRows(tableName, column + " = ?", value);
            LOG.info("Deleted {} pre-existing record(s)", deleted);
        }
        LOG.info("Clean state ensured — no record in '{}' where {} = '{}'", tableName, column, value);
    }

    // ─── When Steps (Actions) ───

    /**
     * Insert a record with a single column/value pair into a table.
     */
    @When("I insert a record into {string} with {string} = {string}")
    public void iInsertARecordIntoTableWithColumnValue(String tableName, String column, String value) {
        LOG.info("Step: Inserting record into '{}' — {} = '{}'", tableName, column, value);
        currentTable = tableName;
        currentRowData.clear();
        currentRowData.put(column, value);

        lastAffectedRows = DatabaseUtil.insertRow(tableName, currentRowData);
        LOG.info("Insert completed — {} row(s) affected", lastAffectedRows);
    }

    /**
     * Insert a record with multiple column/value pairs (comma-separated).
     * Format: "col1=val1, col2=val2, col3=val3"
     */
    @When("I insert a record into {string} with values {string}")
    public void iInsertARecordIntoTableWithValues(String tableName, String columnValues) {
        LOG.info("Step: Inserting record into '{}' with values: {}", tableName, columnValues);
        currentTable = tableName;
        currentRowData.clear();

        Map<String, Object> data = parseColumnValues(columnValues);
        currentRowData.putAll(data);

        lastAffectedRows = DatabaseUtil.insertRow(tableName, data);
        LOG.info("Insert completed — {} row(s) affected", lastAffectedRows);
    }

    /**
     * Update a record in a table, setting a column to a new value where a condition matches.
     */
    @When("I update {string} set {string} = {string} where {string} = {string}")
    public void iUpdateTableSetColumnWhereCondition(String tableName, String setColumn,
                                                     String setValue, String whereColumn,
                                                     String whereValue) {
        LOG.info("Step: Updating '{}' — SET {} = '{}' WHERE {} = '{}'",
                tableName, setColumn, setValue, whereColumn, whereValue);
        currentTable = tableName;
        currentWhereClause = whereColumn + " = ?";
        currentWhereParams = new Object[]{whereValue};

        Map<String, Object> setData = new HashMap<>();
        setData.put(setColumn, setValue);

        lastAffectedRows = DatabaseUtil.updateRows(tableName, setData,
                whereColumn + " = ?", whereValue);
        LOG.info("Update completed — {} row(s) affected", lastAffectedRows);
    }

    /**
     * Update multiple columns in a table (comma-separated set values).
     * Format for set values: "col1=val1, col2=val2"
     */
    @When("I update {string} set values {string} where {string} = {string}")
    public void iUpdateTableSetValuesWhereCondition(String tableName, String setValues,
                                                     String whereColumn, String whereValue) {
        LOG.info("Step: Updating '{}' — SET [{}] WHERE {} = '{}'",
                tableName, setValues, whereColumn, whereValue);
        currentTable = tableName;

        Map<String, Object> setData = parseColumnValues(setValues);

        lastAffectedRows = DatabaseUtil.updateRows(tableName, setData,
                whereColumn + " = ?", whereValue);
        LOG.info("Update completed — {} row(s) affected", lastAffectedRows);
    }

    /**
     * Delete a record from a table where a condition matches.
     */
    @When("I delete from {string} where {string} = {string}")
    public void iDeleteFromTableWhereCondition(String tableName, String column, String value) {
        LOG.info("Step: Deleting from '{}' WHERE {} = '{}'", tableName, column, value);
        currentTable = tableName;
        currentWhereClause = column + " = ?";
        currentWhereParams = new Object[]{value};

        lastAffectedRows = DatabaseUtil.deleteRows(tableName, column + " = ?", value);
        LOG.info("Delete completed — {} row(s) affected", lastAffectedRows);
    }

    /**
     * Execute a custom SQL query and store the result.
     */
    @When("I execute the query {string}")
    public void iExecuteTheQuery(String sql) {
        LOG.info("Step: Executing query: {}", sql);
        lastQueryResults = DatabaseUtil.executeQuery(sql);
        LOG.info("Query returned {} row(s)", lastQueryResults.size());
    }

    /**
     * Execute a custom SQL query with a single parameter.
     */
    @When("I execute the query {string} with parameter {string}")
    public void iExecuteTheQueryWithParameter(String sql, String param) {
        LOG.info("Step: Executing query: {} with param: {}", sql, param);
        lastQueryResults = DatabaseUtil.executeQuery(sql, param);
        LOG.info("Query returned {} row(s)", lastQueryResults.size());
    }

    // ─── Then Steps (Assertions) ───

    /**
     * Verify that the last DML operation affected exactly N rows.
     */
    @Then("the operation should affect {int} row(s)")
    public void theOperationShouldAffectNRows(int expectedRows) {
        LOG.info("Step: Verifying affected rows — expected: {}, actual: {}",
                expectedRows, lastAffectedRows);
        assertEquals("Expected " + expectedRows + " affected row(s) but got " + lastAffectedRows,
                expectedRows, lastAffectedRows);
        LOG.info("Affected rows assertion passed");
    }

    /**
     * Verify that a record exists in a table matching a column/value condition.
     */
    @Then("a record should exist in {string} where {string} = {string}")
    public void aRecordShouldExistInTableWhereColumnEquals(String tableName, String column, String value) {
        LOG.info("Step: Asserting record exists in '{}' where {} = '{}'", tableName, column, value);
        boolean exists = DatabaseUtil.recordExists(
                String.format("SELECT * FROM %s WHERE %s = ?", tableName, column), value);
        assertTrue(
                String.format("Expected record in '%s' where %s = '%s' but none found",
                        tableName, column, value),
                exists);
        LOG.info("Record existence confirmed");
    }

    /**
     * Verify that no record exists in a table matching a column/value condition.
     */
    @Then("no record should exist in {string} where {string} = {string}")
    public void noRecordShouldExistInTableWhereColumnEquals(String tableName, String column, String value) {
        LOG.info("Step: Asserting no record in '{}' where {} = '{}'", tableName, column, value);
        boolean exists = DatabaseUtil.recordExists(
                String.format("SELECT * FROM %s WHERE %s = ?", tableName, column), value);
        assertFalse(
                String.format("Expected no record in '%s' where %s = '%s' but found one",
                        tableName, column, value),
                exists);
        LOG.info("No-record assertion passed");
    }

    /**
     * Verify that a specific column in a table has the expected value for a matching row.
     */
    @Then("the value of {string} in {string} where {string} = {string} should be {string}")
    public void theValueOfColumnInTableShouldBe(String targetColumn, String tableName,
                                                 String whereColumn, String whereValue,
                                                 String expectedValue) {
        LOG.info("Step: Asserting {}.{} = '{}' where {} = '{}'",
                tableName, targetColumn, expectedValue, whereColumn, whereValue);
        Object actual = DatabaseUtil.getSingleValue(
                String.format("SELECT %s FROM %s WHERE %s = ?", targetColumn, tableName, whereColumn),
                targetColumn, whereValue);
        assertNotNull(
                String.format("No row found in '%s' where %s = '%s'", tableName, whereColumn, whereValue),
                actual);
        assertEquals(
                String.format("Expected %s.%s = '%s' but got '%s'",
                        tableName, targetColumn, expectedValue, actual),
                expectedValue, String.valueOf(actual));
        LOG.info("Value assertion passed — {} = '{}'", targetColumn, actual);
    }

    /**
     * Verify the row count in a table matching a condition.
     */
    @Then("the count of records in {string} where {string} = {string} should be {int}")
    public void theCountOfRecordsShouldBe(String tableName, String column, String value,
                                           int expectedCount) {
        LOG.info("Step: Asserting count in '{}' where {} = '{}' is {}",
                tableName, column, value, expectedCount);
        long actualCount = DatabaseUtil.getRowCount(
                String.format("SELECT COUNT(*) FROM %s WHERE %s = ?", tableName, column), value);
        assertEquals(
                String.format("Expected %d record(s) in '%s' where %s = '%s' but found %d",
                        expectedCount, tableName, column, value, actualCount),
                expectedCount, (int) actualCount);
        LOG.info("Count assertion passed — {} record(s)", actualCount);
    }

    /**
     * Verify the total row count from the last executed query.
     */
    @Then("the query result should contain {int} row(s)")
    public void theQueryResultShouldContainNRows(int expectedRows) {
        LOG.info("Step: Asserting query result contains {} row(s)", expectedRows);
        assertNotNull("No query has been executed yet", lastQueryResults);
        assertEquals("Expected " + expectedRows + " row(s) in query result",
                expectedRows, lastQueryResults.size());
        LOG.info("Query result row count assertion passed");
    }

    /**
     * Verify that the query result is not empty.
     */
    @Then("the query result should not be empty")
    public void theQueryResultShouldNotBeEmpty() {
        LOG.info("Step: Asserting query result is not empty");
        assertNotNull("No query has been executed yet", lastQueryResults);
        assertFalse("Expected non-empty query result", lastQueryResults.isEmpty());
        LOG.info("Query result is not empty — {} row(s)", lastQueryResults.size());
    }

    /**
     * Verify that the query result is empty.
     */
    @Then("the query result should be empty")
    public void theQueryResultShouldBeEmpty() {
        LOG.info("Step: Asserting query result is empty");
        assertNotNull("No query has been executed yet", lastQueryResults);
        assertTrue("Expected empty query result but got " + lastQueryResults.size() + " row(s)",
                lastQueryResults.isEmpty());
        LOG.info("Query result is empty — confirmed");
    }

    /**
     * Verify that the database is accessible (connectivity check).
     */
    @Then("the database should be accessible")
    public void theDatabaseShouldBeAccessible() {
        LOG.info("Step: Verifying database accessibility");
        try {
            long count = DatabaseUtil.getRowCount("SELECT 1");
            LOG.info("Database is accessible — SELECT 1 returned successfully");
        } catch (DatabaseException e) {
            LOG.error("Database is NOT accessible: {}", e.getMessage());
            fail("Database is not accessible: " + e.getMessage());
        }
    }

    // ─── Cleanup Steps ───

    /**
     * Delete test data inserted during the scenario to restore clean state.
     */
    @And("I clean up the record in {string} where {string} = {string}")
    public void iCleanUpTheRecordInTableWhereColumnEquals(String tableName, String column, String value) {
        LOG.info("Step: Cleaning up record in '{}' where {} = '{}'", tableName, column, value);
        try {
            int deleted = DatabaseUtil.deleteRows(tableName, column + " = ?", value);
            LOG.info("Cleanup completed — {} row(s) deleted", deleted);
        } catch (DatabaseException e) {
            LOG.warn("Cleanup failed (non-fatal): {}", e.getMessage());
        }
    }

    // ─── Internal Helpers ───

    /**
     * Parse a comma-separated string of "column=value" pairs into a map.
     * Example: "name=John, age=30, status=Active" → {name=John, age=30, status=Active}
     *
     * @param columnValues comma-separated key=value pairs
     * @return ordered map of column to value
     */
    private Map<String, Object> parseColumnValues(String columnValues) {
        Map<String, Object> result = new HashMap<>();
        String[] pairs = columnValues.split(",");
        for (String pair : pairs) {
            String trimmed = pair.trim();
            int eqIndex = trimmed.indexOf('=');
            if (eqIndex > 0 && eqIndex < trimmed.length() - 1) {
                String key = trimmed.substring(0, eqIndex).trim();
                String val = trimmed.substring(eqIndex + 1).trim();
                result.put(key, val);
            } else {
                LOG.warn("Skipping malformed column=value pair: '{}'", trimmed);
            }
        }
        return result;
    }
}

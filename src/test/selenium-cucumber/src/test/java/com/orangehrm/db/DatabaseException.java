package com.orangehrm.db;

/**
 * Runtime exception wrapper for database-related errors in test utilities.
 *
 * <p>Wraps {@link java.sql.SQLException} and other database errors with
 * additional context (operation name, SQL statement) to facilitate debugging.
 * By extending {@link RuntimeException}, callers are not forced to catch
 * checked exceptions but can still inspect the full causal chain.</p>
 */
// PUBLIC_INTERFACE
public class DatabaseException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * Construct a DatabaseException with a descriptive message.
     *
     * @param message context about the failed operation
     */
    public DatabaseException(String message) {
        super(message);
    }

    /**
     * Construct a DatabaseException with a descriptive message and root cause.
     *
     * @param message context about the failed operation
     * @param cause   the underlying exception (e.g. SQLException)
     */
    public DatabaseException(String message, Throwable cause) {
        super(message, cause);
    }
}

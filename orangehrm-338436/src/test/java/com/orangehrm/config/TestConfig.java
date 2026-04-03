package com.orangehrm.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * PUBLIC_INTERFACE
 * TestConfig provides centralized configuration management for the test suite.
 *
 * <p>Configuration values are resolved in the following priority order:
 * <ol>
 *   <li>System properties (e.g., {@code -Dbrowser=firefox})</li>
 *   <li>Environment variables (e.g., {@code ORANGEHRM_BASE_URL})</li>
 *   <li>Properties file values ({@code config.properties})</li>
 *   <li>Default values defined in this class</li>
 * </ol>
 *
 * <p>Key configuration properties:
 * <ul>
 *   <li>{@code ORANGEHRM_BASE_URL} - Base URL of the OrangeHRM application</li>
 *   <li>{@code ORANGEHRM_USERNAME} - Default username for login tests</li>
 *   <li>{@code ORANGEHRM_PASSWORD} - Default password for login tests (should be set via env var)</li>
 *   <li>{@code browser} - Browser type: chrome, firefox (default: chrome)</li>
 *   <li>{@code headless} - Run in headless mode: true/false (default: false)</li>
 * </ul>
 *
 * <p><strong>Security:</strong> Credentials should be provided via environment variables
 * ({@code ORANGEHRM_USERNAME}, {@code ORANGEHRM_PASSWORD}) rather than hardcoded in
 * config files or source code. Use {@link #mask(String)} when logging any sensitive value.</p>
 */
public final class TestConfig {

    // Default configuration values
    private static final String DEFAULT_BASE_URL = "http://localhost:8080";
    private static final String DEFAULT_USERNAME = "Admin";
    private static final String DEFAULT_BROWSER = "chrome";
    private static final boolean DEFAULT_HEADLESS = false;

    /**
     * Minimum number of characters to reveal when masking a value.
     * Values shorter than or equal to this length are fully masked.
     */
    private static final int MASK_REVEAL_LENGTH = 2;

    private static final Properties properties = new Properties();

    static {
        // Attempt to load optional config.properties from classpath
        try (InputStream input = TestConfig.class.getClassLoader()
                .getResourceAsStream("config.properties")) {
            if (input != null) {
                properties.load(input);
            }
        } catch (IOException e) {
            // Silently continue with defaults if config file is not available
            System.err.println("Warning: Could not load config.properties, using defaults.");
        }
    }

    // Prevent instantiation
    private TestConfig() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * PUBLIC_INTERFACE
     * Returns the base URL of the OrangeHRM application under test.
     *
     * @return the base URL string (e.g., "http://localhost:8080")
     */
    public static String getBaseUrl() {
        return resolveProperty("ORANGEHRM_BASE_URL", DEFAULT_BASE_URL);
    }

    /**
     * PUBLIC_INTERFACE
     * Returns the default username for authentication tests.
     *
     * @return the username string
     */
    public static String getUsername() {
        return resolveProperty("ORANGEHRM_USERNAME", DEFAULT_USERNAME);
    }

    /**
     * PUBLIC_INTERFACE
     * Returns the default password for authentication tests.
     *
     * <p><strong>Security note:</strong> The password should be provided via
     * the {@code ORANGEHRM_PASSWORD} environment variable or system property.
     * If not set, a well-known default is used and a warning is emitted.
     * Never log the return value directly; use {@link #mask(String)} instead.</p>
     *
     * @return the password string
     */
    public static String getPassword() {
        String password = resolveProperty("ORANGEHRM_PASSWORD", null);
        if (password == null || password.isEmpty()) {
            System.err.println("WARNING: ORANGEHRM_PASSWORD is not set via environment variable "
                + "or system property. Using well-known default. "
                + "Set ORANGEHRM_PASSWORD for secure test execution.");
            return "admin123";
        }
        return password;
    }

    /**
     * PUBLIC_INTERFACE
     * Returns the browser type to use for test execution.
     *
     * @return the browser name string (e.g., "chrome", "firefox")
     */
    public static String getBrowser() {
        return resolveProperty("browser", DEFAULT_BROWSER);
    }

    /**
     * PUBLIC_INTERFACE
     * Returns whether the browser should run in headless mode.
     *
     * @return true if headless mode is enabled, false otherwise
     */
    public static boolean isHeadless() {
        String value = resolveProperty("headless", String.valueOf(DEFAULT_HEADLESS));
        return Boolean.parseBoolean(value);
    }

    /**
     * PUBLIC_INTERFACE
     * Masks a sensitive value for safe inclusion in log output.
     *
     * <p>If the value is {@code null} or empty, returns {@code "****"}.
     * If the value is short (≤ {@value MASK_REVEAL_LENGTH} characters), it is fully masked.
     * Otherwise, the first character and last character are shown with asterisks in between.</p>
     *
     * <p>Examples:
     * <ul>
     *   <li>{@code mask("Admin")} → {@code "A***n"}</li>
     *   <li>{@code mask("admin123")} → {@code "a******3"}</li>
     *   <li>{@code mask("ab")} → {@code "****"}</li>
     *   <li>{@code mask(null)} → {@code "****"}</li>
     * </ul>
     *
     * @param value the sensitive string to mask
     * @return a masked representation safe for logging
     */
    public static String mask(String value) {
        if (value == null || value.isEmpty()) {
            return "****";
        }
        if (value.length() <= MASK_REVEAL_LENGTH) {
            return "****";
        }
        // Show first and last character, mask everything in between
        return value.charAt(0)
            + "*".repeat(value.length() - 2)
            + value.charAt(value.length() - 1);
    }

    /**
     * Resolves a property value by checking system properties, environment variables,
     * loaded properties file, and finally falling back to the default value.
     *
     * @param key          the property/environment variable name
     * @param defaultValue the fallback value if not found elsewhere (may be null)
     * @return the resolved property value, or defaultValue if not found
     */
    private static String resolveProperty(String key, String defaultValue) {
        // Priority 1: System properties (command line -D flags)
        String value = System.getProperty(key);
        if (value != null && !value.isEmpty()) {
            return value;
        }

        // Priority 2: Environment variables
        value = System.getenv(key);
        if (value != null && !value.isEmpty()) {
            return value;
        }

        // Priority 3: Properties file
        value = properties.getProperty(key);
        if (value != null && !value.isEmpty()) {
            return value;
        }

        // Priority 4: Default value
        return defaultValue;
    }
}

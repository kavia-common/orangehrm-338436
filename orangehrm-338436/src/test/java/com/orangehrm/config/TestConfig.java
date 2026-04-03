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
 *   <li>Default values defined in this class</li>
 * </ol>
 *
 * <p>Key configuration properties:
 * <ul>
 *   <li>{@code ORANGEHRM_BASE_URL} - Base URL of the OrangeHRM application</li>
 *   <li>{@code ORANGEHRM_USERNAME} - Default username for login tests</li>
 *   <li>{@code ORANGEHRM_PASSWORD} - Default password for login tests</li>
 *   <li>{@code browser} - Browser type: chrome, firefox (default: chrome)</li>
 *   <li>{@code headless} - Run in headless mode: true/false (default: false)</li>
 * </ul>
 */
public final class TestConfig {

    // Default configuration values
    private static final String DEFAULT_BASE_URL = "http://localhost:8080";
    private static final String DEFAULT_USERNAME = "Admin";
    private static final String DEFAULT_PASSWORD = "admin123";
    private static final String DEFAULT_BROWSER = "chrome";
    private static final boolean DEFAULT_HEADLESS = false;

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
     * @return the password string
     */
    public static String getPassword() {
        return resolveProperty("ORANGEHRM_PASSWORD", DEFAULT_PASSWORD);
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
     * Resolves a property value by checking system properties, environment variables,
     * loaded properties file, and finally falling back to the default value.
     *
     * @param key          the property/environment variable name
     * @param defaultValue the fallback value if not found elsewhere
     * @return the resolved property value
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

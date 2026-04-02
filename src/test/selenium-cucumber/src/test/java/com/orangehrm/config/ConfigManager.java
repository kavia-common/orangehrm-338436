package com.orangehrm.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Centralized configuration manager for the Selenium test suite.
 * Loads properties from config.properties and supports overrides via
 * system properties and environment variables.
 */
// PUBLIC_INTERFACE
public final class ConfigManager {

    private static final Logger LOG = LoggerFactory.getLogger(ConfigManager.class);
    private static final String CONFIG_FILE = "config.properties";
    private static final Properties PROPS = new Properties();

    static {
        try (InputStream is = ConfigManager.class.getClassLoader().getResourceAsStream(CONFIG_FILE)) {
            if (is != null) {
                PROPS.load(is);
                LOG.info("Configuration loaded from {}", CONFIG_FILE);
            } else {
                LOG.warn("Configuration file {} not found on classpath; using defaults/system properties", CONFIG_FILE);
            }
        } catch (IOException e) {
            LOG.error("Failed to load configuration file: {}", CONFIG_FILE, e);
        }
    }

    private ConfigManager() {
        // Utility class – no instantiation
    }

    /**
     * Retrieve a configuration value with the following precedence:
     * 1. System property
     * 2. Environment variable (dots replaced with underscores, uppercased)
     * 3. config.properties file
     * 4. Provided default value
     *
     * @param key          the property key
     * @param defaultValue fallback value if key is not found anywhere
     * @return resolved value
     */
    // PUBLIC_INTERFACE
    public static String get(String key, String defaultValue) {
        // 1. System property
        String value = System.getProperty(key);
        if (value != null && !value.isBlank()) {
            LOG.debug("Config [{}] resolved from system property: {}", key, value);
            return value;
        }

        // 2. Environment variable (e.g., base.url -> BASE_URL)
        String envKey = key.replace('.', '_').toUpperCase();
        value = System.getenv(envKey);
        if (value != null && !value.isBlank()) {
            LOG.debug("Config [{}] resolved from env variable {}: {}", key, envKey, value);
            return value;
        }

        // 3. Properties file
        value = PROPS.getProperty(key);
        if (value != null && !value.isBlank()) {
            LOG.debug("Config [{}] resolved from properties file: {}", key, value);
            return value;
        }

        // 4. Default
        LOG.debug("Config [{}] using default: {}", key, defaultValue);
        return defaultValue;
    }

    /**
     * Retrieve a configuration value; returns empty string if not found.
     *
     * @param key the property key
     * @return resolved value or empty string
     */
    // PUBLIC_INTERFACE
    public static String get(String key) {
        return get(key, "");
    }

    /** @return the configured base URL for the application under test */
    // PUBLIC_INTERFACE
    public static String getBaseUrl() {
        // Also check the baseUrl system property (passed by Maven)
        String baseUrl = System.getProperty("baseUrl");
        if (baseUrl != null && !baseUrl.isBlank() && !baseUrl.equals("${baseUrl}")) {
            return baseUrl;
        }
        return get("base.url", "https://opensource-demo.orangehrmlive.com");
    }

    /** @return true if headless mode is enabled */
    // PUBLIC_INTERFACE
    public static boolean isHeadless() {
        String headless = System.getProperty("headless");
        if (headless != null && !headless.isBlank() && !headless.equals("${headless}")) {
            return Boolean.parseBoolean(headless);
        }
        return Boolean.parseBoolean(get("browser.headless", "true"));
    }

    /** @return explicit wait timeout in seconds */
    // PUBLIC_INTERFACE
    public static int getExplicitWait() {
        return Integer.parseInt(get("wait.explicit", "15"));
    }

    /** @return page load timeout in seconds */
    // PUBLIC_INTERFACE
    public static int getPageLoadTimeout() {
        return Integer.parseInt(get("wait.pageLoad", "30"));
    }

    /** @return admin username */
    // PUBLIC_INTERFACE
    public static String getAdminUsername() {
        return get("admin.username", "Admin");
    }

    /** @return admin password */
    // PUBLIC_INTERFACE
    public static String getAdminPassword() {
        return get("admin.password", "Jacqueline@OHRM123");
    }

    /** @return ESS username */
    // PUBLIC_INTERFACE
    public static String getEssUsername() {
        return get("ess.username", "John");
    }

    /** @return ESS password */
    // PUBLIC_INTERFACE
    public static String getEssPassword() {
        return get("ess.password", "John@OHRM123");
    }
}

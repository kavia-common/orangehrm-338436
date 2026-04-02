package com.orangehrm.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Centralized, environment-switchable configuration manager for the Selenium
 * test suite.
 *
 * <h3>Flow name: ConfigResolutionFlow</h3>
 * <p>Single entrypoint for all test configuration. Loads a base
 * {@code config.properties} and then overlays an environment-specific file
 * (e.g.&nbsp;{@code config-qa.properties}) based on the active environment.</p>
 *
 * <h3>Resolution precedence (highest to lowest):</h3>
 * <ol>
 *   <li>JVM system property ({@code -Dkey=value})</li>
 *   <li>OS environment variable (dots to underscores, upper-cased)</li>
 *   <li>Environment-specific properties file
 *       ({@code config-&lt;env&gt;.properties})</li>
 *   <li>Base {@code config.properties} file</li>
 * </ol>
 *
 * <h3>Contract</h3>
 * <ul>
 *   <li><b>Inputs:</b> property key (String); optional default value.</li>
 *   <li><b>Outputs:</b> resolved String value, or empty string / supplied
 *       default when key is absent everywhere.</li>
 *   <li><b>Errors:</b> missing files are logged as warnings; missing required
 *       values are logged as errors. No exceptions are thrown for missing
 *       config -- callers receive empty strings and should validate.</li>
 *   <li><b>Side-effects:</b> none (read-only after class loading).</li>
 * </ul>
 *
 * <h3>Environment selection</h3>
 * Set the active environment via any of:
 * <ul>
 *   <li>{@code -Dtest.environment=qa}</li>
 *   <li>{@code TEST_ENVIRONMENT=staging} (env var)</li>
 *   <li>{@code test.environment=production} in {@code config.properties}</li>
 * </ul>
 * Valid values: {@code dev}, {@code qa}, {@code staging}, {@code production}.
 * Default: {@code dev} (uses base {@code config.properties} only).
 */
// PUBLIC_INTERFACE
public final class ConfigManager {

    private static final Logger LOG = LoggerFactory.getLogger(ConfigManager.class);

    /** Base configuration file (always loaded first). */
    private static final String BASE_CONFIG_FILE = "config.properties";

    /** Pattern for environment-specific overlay files. */
    private static final String ENV_CONFIG_PATTERN = "config-%s.properties";

    /** Key that selects the active environment profile. */
    static final String ENV_KEY = "test.environment";

    /** Base properties (loaded from {@code config.properties}). */
    private static final Properties BASE_PROPS = new Properties();

    /** Environment-specific overlay properties. */
    private static final Properties ENV_PROPS = new Properties();

    /** The resolved active environment name. */
    private static final String ACTIVE_ENVIRONMENT;

    /*
     * Static initializer: load base config, resolve environment, load overlay.
     * This block runs exactly once when the class is first referenced.
     */
    static {
        loadProperties(BASE_PROPS, BASE_CONFIG_FILE);
        ACTIVE_ENVIRONMENT = resolveEnvironment();
        if (!"dev".equalsIgnoreCase(ACTIVE_ENVIRONMENT)) {
            String envFile = String.format(ENV_CONFIG_PATTERN, ACTIVE_ENVIRONMENT.toLowerCase());
            loadProperties(ENV_PROPS, envFile);
        }
        LOG.info("ConfigManager initialised -- active environment: [{}]", ACTIVE_ENVIRONMENT);
    }

    private ConfigManager() {
        // Utility class -- no instantiation
    }

    // --- Core Resolution ---

    /**
     * Retrieve a configuration value using the four-level precedence chain.
     *
     * @param key          the property key (e.g. {@code "base.url"})
     * @param defaultValue fallback when the key is absent everywhere
     * @return resolved value (never {@code null})
     */
    // PUBLIC_INTERFACE
    public static String get(String key, String defaultValue) {
        // 1. JVM system property
        String value = System.getProperty(key);
        if (isPresent(value)) {
            LOG.debug("Config [{}] resolved from system property", key);
            return value;
        }

        // 2. OS environment variable (base.url -> BASE_URL)
        String envKey = key.replace('.', '_').toUpperCase();
        value = System.getenv(envKey);
        if (isPresent(value)) {
            LOG.debug("Config [{}] resolved from env var {}", key, envKey);
            return value;
        }

        // 3. Environment-specific properties file
        value = ENV_PROPS.getProperty(key);
        if (isPresent(value)) {
            LOG.debug("Config [{}] resolved from environment overlay [{}]", key, ACTIVE_ENVIRONMENT);
            return value;
        }

        // 4. Base properties file
        value = BASE_PROPS.getProperty(key);
        if (isPresent(value)) {
            LOG.debug("Config [{}] resolved from base config", key);
            return value;
        }

        // 5. Fallback default
        LOG.debug("Config [{}] using default: {}", key, defaultValue);
        return defaultValue;
    }

    /**
     * Retrieve a configuration value; returns empty string when absent.
     *
     * @param key the property key
     * @return resolved value or empty string
     */
    // PUBLIC_INTERFACE
    public static String get(String key) {
        return get(key, "");
    }

    /**
     * Retrieve a required configuration value.
     * Logs an error when the value is missing but does not throw.
     *
     * @param key the property key
     * @return resolved value or empty string (with an ERROR log)
     */
    // PUBLIC_INTERFACE
    public static String getRequired(String key) {
        String value = get(key);
        if (value.isEmpty()) {
            LOG.error("Required configuration key [{}] is not set in any source", key);
        }
        return value;
    }

    // --- Convenience Accessors ---

    /**
     * The active environment name.
     *
     * @return one of {@code dev}, {@code qa}, {@code staging}, {@code production}
     */
    // PUBLIC_INTERFACE
    public static String getActiveEnvironment() {
        return ACTIVE_ENVIRONMENT;
    }

    /**
     * Base URL for the application under test.
     * Checks the Maven {@code baseUrl} system property first, then the
     * standard resolution chain for {@code base.url}.
     *
     * @return application base URL
     */
    // PUBLIC_INTERFACE
    public static String getBaseUrl() {
        // Maven surefire passes baseUrl as a system property
        String mavenBaseUrl = System.getProperty("baseUrl");
        if (isPresent(mavenBaseUrl) && !mavenBaseUrl.startsWith("${")) {
            return mavenBaseUrl;
        }
        return get("base.url");
    }

    /**
     * Configured browser type (e.g. {@code chrome}, {@code firefox}).
     *
     * @return browser identifier in lower-case
     */
    // PUBLIC_INTERFACE
    public static String getBrowser() {
        // Maven surefire passes browser as a system property
        String mavenBrowser = System.getProperty("browser");
        if (isPresent(mavenBrowser) && !mavenBrowser.startsWith("${")) {
            return mavenBrowser.toLowerCase();
        }
        return get("browser", "chrome").toLowerCase();
    }

    /**
     * Whether headless mode is enabled.
     *
     * @return {@code true} if the browser should run headless
     */
    // PUBLIC_INTERFACE
    public static boolean isHeadless() {
        String headless = System.getProperty("headless");
        if (isPresent(headless) && !headless.startsWith("${")) {
            return Boolean.parseBoolean(headless);
        }
        return Boolean.parseBoolean(get("browser.headless", "true"));
    }

    /** @return explicit wait timeout in seconds */
    // PUBLIC_INTERFACE
    public static int getExplicitWait() {
        return parseIntSafe(get("wait.explicit", "15"));
    }

    /** @return page-load timeout in seconds */
    // PUBLIC_INTERFACE
    public static int getPageLoadTimeout() {
        return parseIntSafe(get("wait.pageLoad", "30"));
    }

    // --- Credential Accessors (no hardcoded defaults) ---

    /**
     * Admin username from the active configuration.
     * No hardcoded default -- must be set in a properties file or env var.
     *
     * @return admin username or empty string
     */
    // PUBLIC_INTERFACE
    public static String getAdminUsername() {
        return getRequired("admin.username");
    }

    /**
     * Admin password from the active configuration.
     * No hardcoded default -- must be set in a properties file or env var.
     *
     * @return admin password or empty string
     */
    // PUBLIC_INTERFACE
    public static String getAdminPassword() {
        return getRequired("admin.password");
    }

    /**
     * ESS (Employee Self-Service) username.
     *
     * @return ESS username or empty string
     */
    // PUBLIC_INTERFACE
    public static String getEssUsername() {
        return getRequired("ess.username");
    }

    /**
     * ESS (Employee Self-Service) password.
     *
     * @return ESS password or empty string
     */
    // PUBLIC_INTERFACE
    public static String getEssPassword() {
        return getRequired("ess.password");
    }

    // --- Database Configuration (optional) ---

    /**
     * JDBC URL for the application database.
     *
     * @return JDBC URL or empty string when DB testing is not configured
     */
    // PUBLIC_INTERFACE
    public static String getDbUrl() {
        return get("db.url");
    }

    /**
     * Database username.
     *
     * @return database username or empty string
     */
    // PUBLIC_INTERFACE
    public static String getDbUsername() {
        return get("db.username");
    }

    /**
     * Database password.
     *
     * @return database password or empty string
     */
    // PUBLIC_INTERFACE
    public static String getDbPassword() {
        return get("db.password");
    }

    /**
     * JDBC driver class name (e.g. {@code com.mysql.cj.jdbc.Driver}).
     *
     * @return driver class name or empty string
     */
    // PUBLIC_INTERFACE
    public static String getDbDriver() {
        return get("db.driver");
    }

    /**
     * Whether database configuration is present and usable.
     *
     * @return {@code true} when at least {@code db.url} is configured
     */
    // PUBLIC_INTERFACE
    public static boolean isDatabaseConfigured() {
        return !getDbUrl().isEmpty();
    }

    // --- Internal Helpers ---

    /**
     * Resolve the active environment name from the standard precedence chain.
     * Falls back to {@code dev} when nothing is set.
     */
    private static String resolveEnvironment() {
        // System property first
        String env = System.getProperty(ENV_KEY);
        if (isPresent(env)) {
            LOG.info("Environment resolved from system property: {}", env);
            return env.toLowerCase();
        }
        // Environment variable (TEST_ENVIRONMENT)
        env = System.getenv(ENV_KEY.replace('.', '_').toUpperCase());
        if (isPresent(env)) {
            LOG.info("Environment resolved from env var: {}", env);
            return env.toLowerCase();
        }
        // Base properties file
        env = BASE_PROPS.getProperty(ENV_KEY);
        if (isPresent(env)) {
            LOG.info("Environment resolved from base config: {}", env);
            return env.toLowerCase();
        }
        LOG.info("No environment specified -- defaulting to 'dev'");
        return "dev";
    }

    /**
     * Load a properties file from the classpath into the given
     * {@link Properties} object. Logs a warning when the file is missing.
     */
    private static void loadProperties(Properties target, String filename) {
        try (InputStream is = ConfigManager.class.getClassLoader().getResourceAsStream(filename)) {
            if (is != null) {
                target.load(is);
                LOG.info("Loaded configuration from {}", filename);
            } else {
                LOG.warn("Configuration file {} not found on classpath -- skipping", filename);
            }
        } catch (IOException e) {
            LOG.error("Failed to load configuration file {}: {}", filename, e.getMessage());
        }
    }

    /** @return {@code true} when the value is non-null and non-blank. */
    private static boolean isPresent(String value) {
        return value != null && !value.isBlank();
    }

    /** Parse an integer safely, returning 0 on failure. */
    private static int parseIntSafe(String value) {
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            LOG.warn("Cannot parse integer from '{}' -- returning 0", value);
            return 0;
        }
    }
}

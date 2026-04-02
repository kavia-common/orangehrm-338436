package com.orangehrm.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Validates and describes the available test-environment profiles.
 *
 * <p>Each profile maps to a classpath properties file
 * ({@code config-&lt;env&gt;.properties}). The {@code dev} profile uses only the
 * base {@code config.properties}.</p>
 *
 * <h3>Contract</h3>
 * <ul>
 *   <li><b>Inputs:</b> environment name (String).</li>
 *   <li><b>Outputs:</b> boolean validity, overlay filename, summary map.</li>
 *   <li><b>Errors:</b> none thrown -- invalid names simply return {@code false}
 *       from {@link #isValid(String)}.</li>
 * </ul>
 */
// PUBLIC_INTERFACE
public final class EnvironmentConfig {

    private static final Logger LOG = LoggerFactory.getLogger(EnvironmentConfig.class);

    /** Canonical set of recognised environment names. */
    private static final Set<String> VALID_ENVIRONMENTS =
            Set.of("dev", "qa", "staging", "production");

    private EnvironmentConfig() {
        // Utility class
    }

    /**
     * Check whether the given environment name is recognised.
     *
     * @param env the environment name to check
     * @return {@code true} when the name is a known profile
     */
    // PUBLIC_INTERFACE
    public static boolean isValid(String env) {
        if (env == null) {
            return false;
        }
        return VALID_ENVIRONMENTS.contains(env.toLowerCase());
    }

    /**
     * Return the overlay properties filename for the given environment.
     * The {@code dev} profile has no overlay (returns empty string).
     *
     * @param env the environment name
     * @return overlay filename, e.g. {@code "config-qa.properties"}, or empty
     */
    // PUBLIC_INTERFACE
    public static String getOverlayFilename(String env) {
        if (env == null || "dev".equalsIgnoreCase(env)) {
            return "";
        }
        return String.format("config-%s.properties", env.toLowerCase());
    }

    /**
     * Return a read-only set of all recognised environment names.
     *
     * @return unmodifiable set of environment names
     */
    // PUBLIC_INTERFACE
    public static Set<String> getValidEnvironments() {
        return VALID_ENVIRONMENTS;
    }

    /**
     * Build a summary map of the current runtime configuration for
     * diagnostics and logging. Sensitive values (passwords) are masked.
     *
     * @return ordered map of key to value pairs
     */
    // PUBLIC_INTERFACE
    public static Map<String, String> buildConfigSummary() {
        Map<String, String> summary = new LinkedHashMap<>();
        summary.put("environment", ConfigManager.getActiveEnvironment());
        summary.put("base.url", ConfigManager.getBaseUrl());
        summary.put("browser", ConfigManager.getBrowser());
        summary.put("browser.headless", String.valueOf(ConfigManager.isHeadless()));
        summary.put("admin.username", ConfigManager.getAdminUsername());
        summary.put("admin.password", mask(ConfigManager.getAdminPassword()));
        summary.put("ess.username", ConfigManager.getEssUsername());
        summary.put("ess.password", mask(ConfigManager.getEssPassword()));
        summary.put("db.configured", String.valueOf(ConfigManager.isDatabaseConfigured()));
        if (ConfigManager.isDatabaseConfigured()) {
            summary.put("db.url", ConfigManager.getDbUrl());
            summary.put("db.username", ConfigManager.getDbUsername());
            summary.put("db.password", mask(ConfigManager.getDbPassword()));
            summary.put("db.driver", ConfigManager.getDbDriver());
        }
        summary.put("wait.explicit", String.valueOf(ConfigManager.getExplicitWait()));
        summary.put("wait.pageLoad", String.valueOf(ConfigManager.getPageLoadTimeout()));
        return Collections.unmodifiableMap(summary);
    }

    /**
     * Log the full configuration summary at INFO level.
     */
    // PUBLIC_INTERFACE
    public static void logConfigSummary() {
        LOG.info("========================================================");
        LOG.info("        TEST CONFIGURATION SUMMARY                      ");
        LOG.info("========================================================");
        buildConfigSummary().forEach((k, v) ->
                LOG.info("  {}: {}", padRight(k, 20), v));
        LOG.info("========================================================");
    }

    /** Mask a sensitive value, showing only the first and last character. */
    private static String mask(String value) {
        if (value == null || value.length() <= 2) {
            return "***";
        }
        return value.charAt(0) + "***" + value.charAt(value.length() - 1);
    }

    /** Right-pad a string to the given width. */
    private static String padRight(String text, int width) {
        if (text.length() >= width) {
            return text;
        }
        return text + " ".repeat(width - text.length());
    }
}

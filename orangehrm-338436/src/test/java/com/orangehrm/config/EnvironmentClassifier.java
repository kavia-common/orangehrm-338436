package com.orangehrm.config;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * PUBLIC_INTERFACE
 * EnvironmentClassifier probes the OrangeHRM application and classifies
 * the runtime environment into one of several states for fail-fast diagnostics.
 *
 * <p>This class performs lightweight HTTP health checks against the configured
 * base URL to determine whether the application is reachable, whether the
 * database appears healthy, or whether the environment is completely down.</p>
 *
 * <p><strong>Environment States:</strong></p>
 * <ul>
 *   <li>{@link EnvironmentState#APP_REACHABLE_DB_OK} - App is up, DB appears healthy</li>
 *   <li>{@link EnvironmentState#APP_REACHABLE_DB_DOWN} - App is up but DB/backend has errors</li>
 *   <li>{@link EnvironmentState#APP_UNREACHABLE} - App is not reachable at the configured URL</li>
 *   <li>{@link EnvironmentState#SIMULATED_DB_ON} - Simulated healthy environment</li>
 *   <li>{@link EnvironmentState#SIMULATED_DB_OFF} - Simulated unhealthy DB</li>
 * </ul>
 *
 * <p><strong>Simulation Mode:</strong> Set the system property or env var
 * {@code SIMULATE_DB_STATUS} to {@code ON} or {@code OFF} to bypass real
 * probing and simulate a specific environment state.</p>
 */
public final class EnvironmentClassifier {

    /** HTTP connection timeout in milliseconds */
    private static final int CONNECT_TIMEOUT_MS = 5000;

    /** HTTP read timeout in milliseconds */
    private static final int READ_TIMEOUT_MS = 10000;

    /** Maximum number of probe retries before declaring unreachable */
    private static final int MAX_RETRIES = 2;

    /** Delay between retries in milliseconds */
    private static final long RETRY_DELAY_MS = 2000;

    /**
     * Configuration key for simulating DB status.
     * Set to "ON" for simulated healthy DB, "OFF" for simulated unhealthy DB.
     */
    public static final String SIMULATE_DB_STATUS_KEY = "SIMULATE_DB_STATUS";

    // Keywords that indicate DB/backend errors in HTTP response pages
    private static final String[] DB_ERROR_INDICATORS = {
        "database", "connection refused", "mysql", "mariadb", "pdo",
        "sqlstate", "unable to connect", "db error", "migration",
        "not installed", "installer", "setup", "uninitialized"
    };

    // Keywords that indicate the login page loaded successfully
    private static final String[] LOGIN_PAGE_INDICATORS = {
        "login", "orangehrm", "username", "password"
    };

    // Prevent instantiation
    private EnvironmentClassifier() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * PUBLIC_INTERFACE
     * Classifies the current environment state by probing the application URL.
     *
     * @return a {@link ClassificationResult} with the environment state and diagnostics
     */
    public static ClassificationResult classify() {
        Instant start = Instant.now();
        List<String> diagnostics = new ArrayList<>();
        String baseUrl = TestConfig.getBaseUrl();

        diagnostics.add("Base URL: " + baseUrl);
        diagnostics.add("Classification started at: " + start);

        // Check for simulation mode first
        String simulateFlag = resolveSimulationFlag();
        if (simulateFlag != null) {
            return handleSimulationMode(simulateFlag, diagnostics, start);
        }

        // Real probing mode
        diagnostics.add("Mode: LIVE PROBE (no simulation flag set)");
        return performLiveClassification(baseUrl, diagnostics, start);
    }

    /**
     * PUBLIC_INTERFACE
     * Returns true if the environment is in simulation mode.
     *
     * @return true if SIMULATE_DB_STATUS is set to a non-empty value
     */
    public static boolean isSimulationMode() {
        return resolveSimulationFlag() != null;
    }

    /**
     * PUBLIC_INTERFACE
     * Returns a human-readable summary of the classification result.
     *
     * @param result the classification result to summarize
     * @return formatted summary string
     */
    public static String formatSummary(ClassificationResult result) {
        StringBuilder sb = new StringBuilder();
        sb.append("\n======================================================\n");
        sb.append("         ENVIRONMENT CLASSIFICATION RESULT\n");
        sb.append("======================================================\n");
        sb.append(String.format("  State     : %s%n", result.getState()));
        sb.append(String.format("  Can Run   : %s%n", result.canRunTests() ? "YES" : "NO - Tests will be skipped"));
        sb.append(String.format("  Duration  : %s ms%n", result.getDurationMs()));
        sb.append("------------------------------------------------------\n");
        sb.append("  Diagnostics:\n");
        for (String diag : result.getDiagnostics()) {
            String truncated = diag.length() > 70 ? diag.substring(0, 67) + "..." : diag;
            sb.append("    - ").append(truncated).append("\n");
        }
        sb.append("======================================================\n");
        return sb.toString();
    }

    /**
     * Resolves the simulation flag from system properties or environment variables.
     *
     * @return "ON", "OFF", or null if not set
     */
    private static String resolveSimulationFlag() {
        String value = System.getProperty(SIMULATE_DB_STATUS_KEY);
        if (value == null || value.isEmpty()) {
            value = System.getenv(SIMULATE_DB_STATUS_KEY);
        }
        if (value != null && !value.isEmpty()) {
            return value.trim().toUpperCase();
        }
        return null;
    }

    /**
     * Handles simulation mode by returning a pre-determined state without probing.
     */
    private static ClassificationResult handleSimulationMode(
            String flag, List<String> diagnostics, Instant start) {

        diagnostics.add("Mode: SIMULATION (SIMULATE_DB_STATUS=" + flag + ")");

        if ("ON".equals(flag)) {
            diagnostics.add("Simulating: Application reachable, DB healthy");
            diagnostics.add("All scenarios should execute normally (mocked environment)");
            return new ClassificationResult(
                EnvironmentState.SIMULATED_DB_ON, diagnostics, start);
        } else if ("OFF".equals(flag)) {
            diagnostics.add("Simulating: Application reachable but DB is down/uninitialized");
            diagnostics.add("Scenarios requiring DB will fail with expected errors");
            diagnostics.add("Login page may show error or redirect to installer");
            return new ClassificationResult(
                EnvironmentState.SIMULATED_DB_OFF, diagnostics, start);
        } else {
            diagnostics.add("WARNING: Unknown SIMULATE_DB_STATUS value: " + flag);
            diagnostics.add("Valid values: ON, OFF. Falling back to live probe.");
            String baseUrl = TestConfig.getBaseUrl();
            return performLiveClassification(baseUrl, diagnostics, start);
        }
    }

    /**
     * Performs actual HTTP probing to classify the environment.
     */
    private static ClassificationResult performLiveClassification(
            String baseUrl, List<String> diagnostics, Instant start) {

        String loginUrl = baseUrl + "/web/index.php/auth/login";
        diagnostics.add("Probing: " + loginUrl);

        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            diagnostics.add("Attempt " + attempt + "/" + MAX_RETRIES);

            try {
                ProbeResult probe = httpProbe(loginUrl);
                diagnostics.add("HTTP status: " + probe.statusCode);

                if (probe.statusCode >= 200 && probe.statusCode < 400) {
                    String bodyLower = probe.responseBody.toLowerCase();
                    boolean hasLoginIndicators = containsAny(bodyLower, LOGIN_PAGE_INDICATORS);
                    boolean hasDbErrors = containsAny(bodyLower, DB_ERROR_INDICATORS);

                    if (hasLoginIndicators && !hasDbErrors) {
                        diagnostics.add("Login page loaded successfully");
                        diagnostics.add("No DB error indicators found");
                        return new ClassificationResult(
                            EnvironmentState.APP_REACHABLE_DB_OK, diagnostics, start);
                    } else if (hasDbErrors) {
                        diagnostics.add("DB error indicators detected in response");
                        return new ClassificationResult(
                            EnvironmentState.APP_REACHABLE_DB_DOWN, diagnostics, start);
                    } else {
                        diagnostics.add("Page loaded but content is unexpected (no login form found)");
                        return new ClassificationResult(
                            EnvironmentState.APP_REACHABLE_DB_DOWN, diagnostics, start);
                    }
                } else if (probe.statusCode >= 500) {
                    diagnostics.add("Server error response - likely DB or backend issue");
                    return new ClassificationResult(
                        EnvironmentState.APP_REACHABLE_DB_DOWN, diagnostics, start);
                } else if (probe.statusCode >= 400) {
                    diagnostics.add("Client error response (may still indicate app is reachable)");
                    return new ClassificationResult(
                        EnvironmentState.APP_REACHABLE_DB_DOWN, diagnostics, start);
                }

            } catch (IOException e) {
                diagnostics.add("Connection failed: " + e.getClass().getSimpleName()
                    + " - " + e.getMessage());

                if (attempt < MAX_RETRIES) {
                    diagnostics.add("Retrying in " + RETRY_DELAY_MS + "ms...");
                    try {
                        Thread.sleep(RETRY_DELAY_MS);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        diagnostics.add("Retry interrupted");
                        break;
                    }
                }
            }
        }

        diagnostics.add("All probe attempts failed - app is unreachable");
        return new ClassificationResult(
            EnvironmentState.APP_UNREACHABLE, diagnostics, start);
    }

    /**
     * Performs a single HTTP GET probe against the given URL.
     */
    private static ProbeResult httpProbe(String urlString) throws IOException {
        URL url = URI.create(urlString).toURL();
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        try {
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(CONNECT_TIMEOUT_MS);
            conn.setReadTimeout(READ_TIMEOUT_MS);
            conn.setInstanceFollowRedirects(true);
            conn.setRequestProperty("User-Agent", "OrangeHRM-TestSuite-HealthCheck/1.0");

            int status = conn.getResponseCode();

            String body = "";
            try {
                InputStream is = (status < 400) ? conn.getInputStream() : conn.getErrorStream();
                if (is != null) {
                    byte[] buffer = new byte[4096];
                    int bytesRead = is.read(buffer);
                    if (bytesRead > 0) {
                        body = new String(buffer, 0, bytesRead, StandardCharsets.UTF_8);
                    }
                }
            } catch (IOException readErr) {
                body = "(body read failed: " + readErr.getMessage() + ")";
            }

            return new ProbeResult(status, body);
        } finally {
            conn.disconnect();
        }
    }

    /**
     * Checks if a string contains any of the given keywords.
     */
    private static boolean containsAny(String text, String[] keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    // ==================== Inner Types ====================

    /**
     * PUBLIC_INTERFACE
     * Enumeration of possible environment states detected by the classifier.
     */
    public enum EnvironmentState {
        /** Application is reachable and database appears healthy */
        APP_REACHABLE_DB_OK("App Reachable, DB OK", true),
        /** Application is reachable but database is down or uninitialized */
        APP_REACHABLE_DB_DOWN("App Reachable, DB Down/Uninitialized", false),
        /** Application is not reachable at the configured URL */
        APP_UNREACHABLE("App Unreachable", false),
        /** Simulated: DB is healthy (no real probe performed) */
        SIMULATED_DB_ON("Simulated DB-ON", true),
        /** Simulated: DB is down (no real probe performed) */
        SIMULATED_DB_OFF("Simulated DB-OFF", false);

        private final String description;
        private final boolean testsCanRun;

        EnvironmentState(String description, boolean testsCanRun) {
            this.description = description;
            this.testsCanRun = testsCanRun;
        }

        /** @return human-readable description */
        public String getDescription() { return description; }

        /** @return true if tests can meaningfully run in this state */
        public boolean canRunTests() { return testsCanRun; }

        @Override
        public String toString() { return description; }
    }

    /**
     * PUBLIC_INTERFACE
     * Immutable result of an environment classification.
     */
    public static final class ClassificationResult {
        private final EnvironmentState state;
        private final List<String> diagnostics;
        private final long durationMs;

        ClassificationResult(EnvironmentState state, List<String> diagnostics, Instant start) {
            this.state = state;
            this.diagnostics = Collections.unmodifiableList(new ArrayList<>(diagnostics));
            this.durationMs = Duration.between(start, Instant.now()).toMillis();
        }

        /** @return the classified environment state */
        public EnvironmentState getState() { return state; }

        /** @return whether tests can run given this classification */
        public boolean canRunTests() { return state.canRunTests(); }

        /** @return unmodifiable list of diagnostic strings */
        public List<String> getDiagnostics() { return diagnostics; }

        /** @return classification duration in ms */
        public long getDurationMs() { return durationMs; }
    }

    /** Internal result of a single HTTP probe. */
    private static final class ProbeResult {
        final int statusCode;
        final String responseBody;

        ProbeResult(int statusCode, String responseBody) {
            this.statusCode = statusCode;
            this.responseBody = responseBody;
        }
    }
}

package com.orangehrm.stepdefinitions;

import com.orangehrm.config.EnvironmentClassifier;
import com.orangehrm.config.EnvironmentClassifier.ClassificationResult;
import com.orangehrm.config.EnvironmentClassifier.EnvironmentState;
import com.orangehrm.config.TestConfig;

import io.cucumber.java.Before;
import io.cucumber.java.Scenario;

/**
 * PUBLIC_INTERFACE
 * HealthCheckHook provides a Cucumber @Before hook (order=0) that runs
 * environment classification before any scenario executes.
 *
 * <p>This hook implements a fail-fast gate: if the application or database
 * is not in a healthy state, the scenario is immediately failed with clear
 * diagnostic output rather than letting Selenium timeout with opaque errors.</p>
 */
public class HealthCheckHook {

    /** Cached classification result. Computed once per JVM lifetime. */
    private static volatile ClassificationResult cachedResult = null;

    /** Lock object for thread-safe lazy initialization */
    private static final Object LOCK = new Object();

    /** Counter for scenarios skipped due to environment issues */
    private static int skippedScenarioCount = 0;

    /**
     * PUBLIC_INTERFACE
     * Cucumber @Before hook that gates scenario execution on environment health.
     * Runs at order=0, before Hooks.setUp() which runs at default order.
     *
     * @param scenario the current Cucumber scenario
     * @throws EnvironmentNotReadyException if the environment cannot support test execution
     */
    @Before(order = 0)
    public void checkEnvironmentHealth(Scenario scenario) {
        ClassificationResult result = getOrComputeClassification();

        System.out.println("[HealthCheck] Scenario: " + scenario.getName()
            + " | Environment: " + result.getState());

        if (!result.canRunTests()) {
            skippedScenarioCount++;
            String message = buildFailFastMessage(result, scenario);
            System.err.println(message);

            throw new EnvironmentNotReadyException(
                "Environment not ready: " + result.getState().getDescription()
                + ". See diagnostics above. Scenario: " + scenario.getName());
        }

        if (result.getState() == EnvironmentState.SIMULATED_DB_ON) {
            System.out.println("[HealthCheck] Running in SIMULATED DB-ON mode. "
                + "Scenarios will execute but may fail at Selenium level "
                + "if no real app is present.");
        }
    }

    /**
     * Lazily computes and caches the environment classification.
     */
    private ClassificationResult getOrComputeClassification() {
        if (cachedResult == null) {
            synchronized (LOCK) {
                if (cachedResult == null) {
                    System.out.println("\n[HealthCheck] Performing environment classification...");
                    System.out.println("[HealthCheck] Base URL: " + TestConfig.getBaseUrl());
                    cachedResult = EnvironmentClassifier.classify();
                    System.out.println(EnvironmentClassifier.formatSummary(cachedResult));
                }
            }
        }
        return cachedResult;
    }

    /**
     * Builds a detailed fail-fast message for console output.
     */
    private String buildFailFastMessage(ClassificationResult result, Scenario scenario) {
        StringBuilder sb = new StringBuilder();
        sb.append("\n");
        sb.append("======================================================\n");
        sb.append("        FAIL-FAST: ENVIRONMENT NOT READY\n");
        sb.append("======================================================\n");
        sb.append("  Scenario : ").append(scenario.getName()).append("\n");
        sb.append("  State    : ").append(result.getState().getDescription()).append("\n");
        sb.append("  Skipped# : ").append(skippedScenarioCount).append("\n");
        sb.append("------------------------------------------------------\n");
        sb.append("  RECOMMENDATION:\n");

        switch (result.getState()) {
            case APP_UNREACHABLE:
                sb.append("  1. Verify OrangeHRM is running at the base URL\n");
                sb.append("  2. Check network connectivity and firewall rules\n");
                sb.append("  3. Verify ORANGEHRM_BASE_URL configuration\n");
                break;
            case APP_REACHABLE_DB_DOWN:
                sb.append("  1. Check MySQL/MariaDB service is running\n");
                sb.append("  2. Verify DB connection settings in OrangeHRM config\n");
                sb.append("  3. Run OrangeHRM installer if DB is not initialized\n");
                break;
            case SIMULATED_DB_OFF:
                sb.append("  1. DB-OFF simulation active (SIMULATE_DB_STATUS=OFF)\n");
                sb.append("  2. Tests are intentionally skipped in this mode\n");
                sb.append("  3. This is expected behavior for DB-OFF analysis\n");
                break;
            default:
                sb.append("  Check the diagnostics above for details\n");
                break;
        }

        sb.append("======================================================\n");
        return sb.toString();
    }

    /** @return the number of scenarios skipped due to environment issues */
    public static int getSkippedScenarioCount() { return skippedScenarioCount; }

    /** @return the cached classification result, or null if not yet computed */
    public static ClassificationResult getCachedResult() { return cachedResult; }

    /**
     * Resets the cached state. Used by simulation runners between runs.
     */
    public static void resetCache() {
        synchronized (LOCK) {
            cachedResult = null;
            skippedScenarioCount = 0;
        }
    }

    /**
     * PUBLIC_INTERFACE
     * Exception thrown when the environment is not ready for test execution.
     */
    public static class EnvironmentNotReadyException extends RuntimeException {
        /** @param message the detail message including state and diagnostics */
        public EnvironmentNotReadyException(String message) {
            super(message);
        }
    }
}

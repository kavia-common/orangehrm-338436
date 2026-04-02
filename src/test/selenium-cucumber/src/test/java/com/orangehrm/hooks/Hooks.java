package com.orangehrm.hooks;

import com.orangehrm.config.ConfigManager;
import com.orangehrm.config.EnvironmentConfig;
import com.orangehrm.driver.DriverFactory;
import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.Scenario;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;

/**
 * Cucumber hooks for managing WebDriver lifecycle per scenario.
 * Handles driver initialisation, configuration logging, screenshot capture
 * on failure, and teardown.
 *
 * <h3>Observability</h3>
 * <p>On the first scenario of a run, the full configuration summary is logged
 * so that CI logs always contain the runtime settings used.</p>
 *
 * <h3>Database-only scenario optimisation</h3>
 * <p>For scenarios tagged {@code @database} where the database is not
 * configured, WebDriver initialization is skipped to avoid wasting
 * resources on a browser that won't be used (the scenario will be
 * skipped by the {@code assumeTrue} check in the Given step).</p>
 */
// PUBLIC_INTERFACE
public class Hooks {

    private static final Logger LOG = LoggerFactory.getLogger(Hooks.class);

    /** Guard flag so that the config summary is logged only once per JVM. */
    private static volatile boolean configLogged = false;

    /**
     * Before each scenario: log config (once), then initialise a fresh
     * WebDriver instance.
     *
     * <p>For scenarios tagged {@code @database} where the database is not
     * configured, WebDriver initialization is skipped to avoid wasting
     * resources on a browser that won't be used (the scenario will be
     * skipped by the {@code assumeTrue} check in the Given step).</p>
     *
     * @param scenario the current Cucumber scenario
     */
    @Before(order = 0)
    public void setUp(Scenario scenario) {
        logConfigOnce();

        LOG.info("===================================================");
        LOG.info("STARTING SCENARIO: {}", scenario.getName());
        LOG.info("Tags: {}", scenario.getSourceTagNames());
        LOG.info("Environment: {}", ConfigManager.getActiveEnvironment());
        LOG.info("===================================================");

        // Skip WebDriver init for database-only scenarios when DB is not configured.
        // These scenarios will be skipped by assumeTrue in the step definition anyway.
        if (isDatabaseOnlyScenario(scenario) && !ConfigManager.isDatabaseConfigured()) {
            LOG.info("Database not configured — skipping WebDriver init for database-only scenario");
            return;
        }

        DriverFactory.initDriver();
    }

    /**
     * After each scenario: capture screenshot on failure, then quit the driver.
     *
     * <p>Handles the case where WebDriver was never initialized (e.g. for
     * database-only scenarios that were skipped because DB was not configured).</p>
     *
     * @param scenario the current Cucumber scenario
     */
    @After(order = 0)
    public void tearDown(Scenario scenario) {
        try {
            if (scenario.isFailed()) {
                LOG.error("SCENARIO FAILED: {}", scenario.getName());
                captureScreenshot(scenario);
            } else {
                LOG.info("SCENARIO PASSED/SKIPPED: {}", scenario.getName());
            }
        } finally {
            // quitDriver() already handles null driver gracefully (no-op if not init'd)
            DriverFactory.quitDriver();
            LOG.info("===================================================");
            LOG.info("FINISHED SCENARIO: {} [{}]", scenario.getName(),
                    scenario.isFailed() ? "FAILED" : "PASSED/SKIPPED");
            LOG.info("===================================================");
        }
    }

    /**
     * Log the full configuration summary exactly once per test run.
     */
    private void logConfigOnce() {
        if (!configLogged) {
            synchronized (Hooks.class) {
                if (!configLogged) {
                    EnvironmentConfig.logConfigSummary();
                    configLogged = true;
                }
            }
        }
    }

    /**
     * Determine if a scenario is a database-only scenario (tagged @database
     * or @db-validation but not tagged with any UI-related tags).
     *
     * @param scenario the Cucumber scenario to inspect
     * @return {@code true} if the scenario is exclusively a database scenario
     */
    private boolean isDatabaseOnlyScenario(Scenario scenario) {
        Collection<String> tags = scenario.getSourceTagNames();
        boolean hasDbTag = tags.stream().anyMatch(t ->
                t.equalsIgnoreCase("@database") || t.equalsIgnoreCase("@db-validation"));
        boolean hasUiTag = tags.stream().anyMatch(t ->
                t.equalsIgnoreCase("@ui") || t.equalsIgnoreCase("@login")
                        || t.equalsIgnoreCase("@regression") || t.equalsIgnoreCase("@critical"));
        return hasDbTag && !hasUiTag;
    }

    /**
     * Capture a screenshot and attach it to the Cucumber report.
     * Gracefully handles the case where no WebDriver is available
     * (e.g. database-only scenarios that were skipped).
     */
    private void captureScreenshot(Scenario scenario) {
        try {
            WebDriver driver = DriverFactory.getDriver();
            byte[] screenshot = ((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES);
            scenario.attach(screenshot, "image/png", "failure-screenshot-" + scenario.getName());
            LOG.info("Screenshot captured for failed scenario: {}", scenario.getName());
        } catch (IllegalStateException e) {
            // WebDriver was never initialized (e.g. skipped DB scenario) — no screenshot possible
            LOG.debug("WebDriver not available for screenshot (scenario may have been skipped): {}",
                    e.getMessage());
        } catch (Exception e) {
            LOG.warn("Could not capture screenshot for scenario {}: {}", scenario.getName(), e.getMessage());
        }
    }
}

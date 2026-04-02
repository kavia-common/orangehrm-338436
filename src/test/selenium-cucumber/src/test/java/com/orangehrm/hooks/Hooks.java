package com.orangehrm.hooks;

import com.orangehrm.config.ConfigManager;
import com.orangehrm.config.EnvironmentConfig;
import com.orangehrm.driver.DriverFactory;
import com.orangehrm.utils.ScenarioTags;
import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.Scenario;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Cucumber hooks for managing WebDriver lifecycle per scenario.
 * Handles driver initialisation, configuration logging, screenshot capture
 * on failure, and teardown.
 *
 * <h3>Flow name: ScenarioLifecycleFlow</h3>
 * <p>Single canonical lifecycle management for every Cucumber scenario.
 * Tag-based classification is delegated to {@link ScenarioTags} to avoid
 * scattered conditional logic.</p>
 *
 * <h3>Thread safety</h3>
 * <p>Cucumber PicoContainer creates a new instance of this class for each
 * scenario. The {@code configLogged} guard uses double-checked locking
 * (volatile + synchronized) which is safe for multi-threaded execution.
 * WebDriver lifecycle is managed via {@link DriverFactory}'s ThreadLocal,
 * so parallel scenarios never share a browser instance.</p>
 *
 * <h3>Observability</h3>
 * <p>On the first scenario of a run, the full configuration summary is logged
 * so that CI logs always contain the runtime settings used. Thread names are
 * included in log messages to aid parallel execution debugging.</p>
 *
 * <h3>Database-only scenario optimisation</h3>
 * <p>For scenarios tagged {@code @database} where the database is not
 * configured, WebDriver initialization is skipped to avoid wasting
 * resources on a browser that won't be used.</p>
 *
 * <h3>Flaky scenario awareness</h3>
 * <p>Scenarios tagged {@code @flaky} are logged with a warning at start
 * and their outcomes are explicitly noted for CI triage.</p>
 */
// PUBLIC_INTERFACE
public class Hooks {

    private static final Logger LOG = LoggerFactory.getLogger(Hooks.class);

    /** Guard flag so that the config summary is logged only once per JVM. */
    private static volatile boolean configLogged = false;

    /** Track whether WebDriver was initialised for this scenario. */
    private boolean driverInitialised = false;

    /**
     * Before each scenario: log config (once), classify scenario by tags,
     * then initialise a fresh WebDriver instance when needed.
     *
     * @param scenario the current Cucumber scenario
     */
    @Before(order = 0)
    public void setUp(Scenario scenario) {
        logConfigOnce();

        String threadName = Thread.currentThread().getName();
        LOG.info("===================================================");
        LOG.info("STARTING SCENARIO: {} [thread={}]", scenario.getName(), threadName);
        LOG.info("Tags: {}", scenario.getSourceTagNames());
        LOG.info("Environment: {}", ConfigManager.getActiveEnvironment());

        // Log flaky scenario warning for CI triage
        if (ScenarioTags.isFlaky(scenario)) {
            LOG.warn("FLAKY SCENARIO: '{}' — tagged @flaky, results may be non-deterministic",
                    scenario.getName());
        }

        // Log WIP scenario info
        if (ScenarioTags.isWip(scenario)) {
            LOG.info("WIP SCENARIO: '{}' — tagged @wip, may be incomplete", scenario.getName());
        }

        LOG.info("===================================================");

        // Skip WebDriver init for database-only scenarios when DB is not configured.
        // These scenarios will be skipped by assumeTrue in the step definition anyway.
        if (ScenarioTags.isDatabaseOnly(scenario) && !ConfigManager.isDatabaseConfigured()) {
            LOG.info("Database not configured — skipping WebDriver init for database-only scenario");
            driverInitialised = false;
            return;
        }

        // Skip WebDriver init for database-only scenarios even when DB is configured
        // (no browser needed for pure DB validation)
        if (ScenarioTags.isDatabaseOnly(scenario)) {
            LOG.info("Database-only scenario — skipping WebDriver init (no browser needed)");
            driverInitialised = false;
            return;
        }

        DriverFactory.initDriver();
        driverInitialised = true;
    }

    /**
     * After each scenario: capture screenshot on failure, then quit the driver.
     *
     * @param scenario the current Cucumber scenario
     */
    @After(order = 0)
    public void tearDown(Scenario scenario) {
        String threadName = Thread.currentThread().getName();
        try {
            if (scenario.isFailed()) {
                String flakyNote = ScenarioTags.isFlaky(scenario) ? " [FLAKY]" : "";
                LOG.error("SCENARIO FAILED{}: {} [thread={}]",
                        flakyNote, scenario.getName(), threadName);
                if (driverInitialised) {
                    captureScreenshot(scenario);
                }
            } else {
                LOG.info("SCENARIO PASSED/SKIPPED: {} [thread={}]", scenario.getName(), threadName);
            }
        } finally {
            // quitDriver() already handles null driver gracefully (no-op if not init'd)
            DriverFactory.quitDriver();
            driverInitialised = false;
            LOG.info("===================================================");
            LOG.info("FINISHED SCENARIO: {} [{}] [thread={}]", scenario.getName(),
                    scenario.isFailed() ? "FAILED" : "PASSED/SKIPPED", threadName);
            LOG.info("===================================================");
        }
    }

    /**
     * Log the full configuration summary exactly once per test run.
     * Uses double-checked locking with volatile for thread safety.
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
     * Capture a screenshot and attach it to the Cucumber report.
     * Gracefully handles the case where no WebDriver is available.
     */
    private void captureScreenshot(Scenario scenario) {
        try {
            WebDriver driver = DriverFactory.getDriver();
            byte[] screenshot = ((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES);
            scenario.attach(screenshot, "image/png", "failure-screenshot-" + scenario.getName());
            LOG.info("Screenshot captured for failed scenario: {}", scenario.getName());
        } catch (IllegalStateException e) {
            // WebDriver was never initialized — no screenshot possible
            LOG.debug("WebDriver not available for screenshot (scenario may have been skipped): {}",
                    e.getMessage());
        } catch (Exception e) {
            LOG.warn("Could not capture screenshot for scenario {}: {}", scenario.getName(), e.getMessage());
        }
    }
}

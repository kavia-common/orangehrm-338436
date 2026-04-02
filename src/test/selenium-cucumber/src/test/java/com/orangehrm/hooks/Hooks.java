package com.orangehrm.hooks;

import com.orangehrm.driver.DriverFactory;
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
 * Handles driver initialization, screenshot capture on failure, and teardown.
 */
// PUBLIC_INTERFACE
public class Hooks {

    private static final Logger LOG = LoggerFactory.getLogger(Hooks.class);

    /**
     * Before each scenario: initialize a fresh WebDriver instance.
     *
     * @param scenario the current Cucumber scenario
     */
    @Before(order = 0)
    public void setUp(Scenario scenario) {
        LOG.info("═══════════════════════════════════════════════════");
        LOG.info("STARTING SCENARIO: {}", scenario.getName());
        LOG.info("Tags: {}", scenario.getSourceTagNames());
        LOG.info("═══════════════════════════════════════════════════");
        DriverFactory.initDriver();
    }

    /**
     * After each scenario: capture screenshot on failure, then quit the driver.
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
                LOG.info("SCENARIO PASSED: {}", scenario.getName());
            }
        } finally {
            DriverFactory.quitDriver();
            LOG.info("═══════════════════════════════════════════════════");
            LOG.info("FINISHED SCENARIO: {} [{}]", scenario.getName(),
                    scenario.isFailed() ? "FAILED" : "PASSED");
            LOG.info("═══════════════════════════════════════════════════\n");
        }
    }

    /**
     * Capture a screenshot and attach it to the Cucumber report.
     */
    private void captureScreenshot(Scenario scenario) {
        try {
            WebDriver driver = DriverFactory.getDriver();
            byte[] screenshot = ((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES);
            scenario.attach(screenshot, "image/png", "failure-screenshot-" + scenario.getName());
            LOG.info("Screenshot captured for failed scenario: {}", scenario.getName());
        } catch (Exception e) {
            LOG.warn("Could not capture screenshot for scenario {}: {}", scenario.getName(), e.getMessage());
        }
    }
}

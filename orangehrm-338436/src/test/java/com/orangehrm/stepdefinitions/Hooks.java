package com.orangehrm.stepdefinitions;

import com.orangehrm.driver.DriverFactory;
import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.Scenario;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;

/**
 * PUBLIC_INTERFACE
 * Hooks provides Cucumber lifecycle callbacks for managing WebDriver setup and teardown.
 *
 * <p>This class contains {@code @Before} and {@code @After} hooks that:
 * <ul>
 *   <li>Initialize the WebDriver before each scenario</li>
 *   <li>Capture a screenshot on test failure</li>
 *   <li>Quit the WebDriver after each scenario to free resources</li>
 * </ul>
 *
 * <p><strong>Note:</strong> The {@link HealthCheckHook} runs at {@code order=0}
 * before this hook, ensuring the environment is validated before any WebDriver
 * initialization occurs. If the environment check fails, this hook's setUp
 * will still run but tearDown safely handles the case where the driver
 * was never fully initialized.</p>
 */
public class Hooks {

    /**
     * PUBLIC_INTERFACE
     * Initializes the WebDriver instance before each Cucumber scenario.
     *
     * <p>This ensures a fresh browser session is available for every scenario,
     * providing test isolation.</p>
     *
     * @param scenario the current Cucumber scenario (injected by Cucumber)
     */
    @Before
    public void setUp(Scenario scenario) {
        System.out.println("Starting scenario: " + scenario.getName());
        // Initialize the WebDriver (creates a new browser instance)
        DriverFactory.getDriver();
    }

    /**
     * PUBLIC_INTERFACE
     * Cleans up the WebDriver instance after each Cucumber scenario.
     *
     * <p>If the scenario has failed, a screenshot is captured and attached
     * to the Cucumber report for debugging purposes. If the driver was never
     * initialized (e.g., because HealthCheckHook failed fast), cleanup is
     * safely skipped.</p>
     *
     * @param scenario the current Cucumber scenario (injected by Cucumber)
     */
    @After
    public void tearDown(Scenario scenario) {
        try {
            WebDriver driver = DriverFactory.getDriver();

            // Capture screenshot on failure for debugging
            if (scenario.isFailed() && driver instanceof TakesScreenshot) {
                try {
                    byte[] screenshot = ((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES);
                    scenario.attach(screenshot, "image/png", "failure-screenshot");
                    System.out.println("Screenshot captured for failed scenario: " + scenario.getName());
                } catch (Exception e) {
                    System.err.println("Warning: Failed to capture screenshot: " + e.getMessage());
                }
            }

            // Quit the browser and clean up resources
            DriverFactory.quitDriver();
        } catch (Exception e) {
            // Driver may not have been initialized if HealthCheckHook failed fast
            System.err.println("Warning: tearDown skipped driver cleanup "
                + "(driver not initialized or already closed): " + e.getMessage());
        }
        System.out.println("Completed scenario: " + scenario.getName());
    }
}

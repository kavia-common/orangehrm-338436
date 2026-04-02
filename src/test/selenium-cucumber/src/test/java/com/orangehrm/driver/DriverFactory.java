package com.orangehrm.driver;

import com.orangehrm.config.ConfigManager;
import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;

/**
 * Factory for creating and managing Selenium WebDriver instances.
 * Supports Chromium/Chrome in both headless and headed modes.
 * Uses ThreadLocal to support parallel execution.
 */
// PUBLIC_INTERFACE
public final class DriverFactory {

    private static final Logger LOG = LoggerFactory.getLogger(DriverFactory.class);
    private static final ThreadLocal<WebDriver> DRIVER_THREAD_LOCAL = new ThreadLocal<>();

    private DriverFactory() {
        // Utility class – no instantiation
    }

    /**
     * Initialize and return a new WebDriver instance based on configuration.
     * The driver is stored in a ThreadLocal for safe parallel access.
     *
     * @return the initialized WebDriver
     */
    // PUBLIC_INTERFACE
    public static WebDriver initDriver() {
        LOG.info("Initializing WebDriver...");

        // Setup ChromeDriver via WebDriverManager
        WebDriverManager.chromedriver().setup();

        ChromeOptions options = buildChromeOptions();
        WebDriver driver = new ChromeDriver(options);

        // Configure timeouts from config
        driver.manage().timeouts().pageLoadTimeout(
                Duration.ofSeconds(ConfigManager.getPageLoadTimeout()));
        driver.manage().timeouts().implicitlyWait(
                Duration.ofSeconds(Integer.parseInt(ConfigManager.get("wait.implicit", "5"))));
        driver.manage().window().maximize();

        DRIVER_THREAD_LOCAL.set(driver);
        LOG.info("WebDriver initialized successfully (headless={})", ConfigManager.isHeadless());
        return driver;
    }

    /**
     * Retrieve the current thread's WebDriver instance.
     *
     * @return the WebDriver for the current thread
     * @throws IllegalStateException if driver has not been initialized
     */
    // PUBLIC_INTERFACE
    public static WebDriver getDriver() {
        WebDriver driver = DRIVER_THREAD_LOCAL.get();
        if (driver == null) {
            throw new IllegalStateException(
                    "WebDriver not initialized. Call DriverFactory.initDriver() first.");
        }
        return driver;
    }

    /**
     * Quit the current thread's WebDriver and clean up.
     */
    // PUBLIC_INTERFACE
    public static void quitDriver() {
        WebDriver driver = DRIVER_THREAD_LOCAL.get();
        if (driver != null) {
            LOG.info("Quitting WebDriver...");
            try {
                driver.quit();
            } catch (Exception e) {
                LOG.warn("Error while quitting WebDriver: {}", e.getMessage());
            } finally {
                DRIVER_THREAD_LOCAL.remove();
                LOG.info("WebDriver instance removed from ThreadLocal");
            }
        }
    }

    /**
     * Build ChromeOptions with headless/headed configuration and
     * stability flags for CI environments.
     */
    private static ChromeOptions buildChromeOptions() {
        ChromeOptions options = new ChromeOptions();
        boolean headless = ConfigManager.isHeadless();

        if (headless) {
            // Modern headless mode (Chrome 109+)
            options.addArguments("--headless=new");
            LOG.info("Chrome configured in HEADLESS mode");
        } else {
            LOG.info("Chrome configured in HEADED (non-headless) mode");
        }

        // Stability and CI-friendly arguments
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--disable-gpu");
        options.addArguments("--window-size=1920,1080");
        options.addArguments("--disable-extensions");
        options.addArguments("--disable-infobars");
        options.addArguments("--remote-allow-origins=*");
        options.addArguments("--disable-web-security");
        options.addArguments("--allow-insecure-localhost");

        // Accept insecure certs (useful for test environments)
        options.setAcceptInsecureCerts(true);

        return options;
    }
}

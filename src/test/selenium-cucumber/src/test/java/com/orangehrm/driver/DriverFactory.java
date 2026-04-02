package com.orangehrm.driver;

import com.orangehrm.config.ConfigManager;
import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;

/**
 * Factory for creating and managing Selenium WebDriver instances.
 *
 * <h3>Flow name: DriverInitFlow</h3>
 * <p>Supports Chrome and Firefox browsers in both headless and headed modes.
 * Browser selection is driven by {@link ConfigManager#getBrowser()}. Uses
 * ThreadLocal storage to support parallel test execution.</p>
 *
 * <h3>Thread safety</h3>
 * <p>Each thread gets its own WebDriver instance stored in a
 * {@link ThreadLocal}. This ensures complete isolation when running
 * parallel scenarios via Maven Surefire forks or JUnit parallel runners.
 * Callers must invoke {@link #quitDriver()} in an {@code @After} hook
 * to prevent browser process leaks.</p>
 *
 * <h3>Resilience</h3>
 * <p>Driver initialization includes a configurable retry mechanism
 * ({@code driver.init.retries}, default 2) to handle transient CI
 * failures such as browser crashes on startup or driver download
 * timeouts.</p>
 *
 * <h3>Contract</h3>
 * <ul>
 *   <li><b>Inputs:</b> browser type and headless flag from ConfigManager.</li>
 *   <li><b>Outputs:</b> initialised {@link WebDriver} stored in ThreadLocal.</li>
 *   <li><b>Errors:</b> {@link IllegalStateException} when accessing an
 *       uninitialised driver; {@link UnsupportedOperationException} for
 *       unknown browser types.</li>
 *   <li><b>Side-effects:</b> WebDriverManager downloads browser drivers as
 *       needed; browser process is launched.</li>
 * </ul>
 */
// PUBLIC_INTERFACE
public final class DriverFactory {

    private static final Logger LOG = LoggerFactory.getLogger(DriverFactory.class);
    private static final ThreadLocal<WebDriver> DRIVER_THREAD_LOCAL = new ThreadLocal<>();

    /** Number of retry attempts for driver initialization (handles transient CI failures). */
    private static final int INIT_RETRIES = parseIntSafe(ConfigManager.get("driver.init.retries", "2"));

    /** Delay between retry attempts in milliseconds. */
    private static final long RETRY_DELAY_MS = 2000L;

    private DriverFactory() {
        // Utility class -- no instantiation
    }

    /**
     * Initialise a new WebDriver instance based on configuration and store
     * it in a ThreadLocal for safe parallel access. Retries on failure
     * up to the configured retry count.
     *
     * @return the initialised WebDriver
     * @throws UnsupportedOperationException when an unknown browser is configured
     * @throws RuntimeException if all retry attempts fail
     */
    // PUBLIC_INTERFACE
    public static WebDriver initDriver() {
        String browser = ConfigManager.getBrowser();
        boolean headless = ConfigManager.isHeadless();

        LOG.info("Initialising WebDriver — browser={}, headless={}, thread={}",
                browser, headless, Thread.currentThread().getName());

        WebDriver driver = null;
        Exception lastException = null;

        for (int attempt = 1; attempt <= INIT_RETRIES; attempt++) {
            try {
                driver = createDriver(browser, headless);
                break; // Success — exit retry loop
            } catch (Exception e) {
                lastException = e;
                LOG.warn("WebDriver init attempt {}/{} failed: {}", attempt, INIT_RETRIES, e.getMessage());
                if (attempt < INIT_RETRIES) {
                    try {
                        Thread.sleep(RETRY_DELAY_MS);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("Interrupted during WebDriver init retry", ie);
                    }
                }
            }
        }

        if (driver == null) {
            throw new RuntimeException(
                    "Failed to initialise WebDriver after " + INIT_RETRIES + " attempts", lastException);
        }

        // Configure timeouts from config
        driver.manage().timeouts().pageLoadTimeout(
                Duration.ofSeconds(ConfigManager.getPageLoadTimeout()));
        // NOTE: Implicit waits are intentionally set to 0 to avoid interference
        // with explicit waits. All element lookups use explicit waits via BasePage.
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(0));
        driver.manage().window().maximize();

        DRIVER_THREAD_LOCAL.set(driver);
        LOG.info("WebDriver initialised successfully (browser={}, headless={}, thread={})",
                browser, headless, Thread.currentThread().getName());
        return driver;
    }

    /**
     * Retrieve the current thread's WebDriver instance.
     *
     * @return the WebDriver for the current thread
     * @throws IllegalStateException if the driver has not been initialised
     */
    // PUBLIC_INTERFACE
    public static WebDriver getDriver() {
        WebDriver driver = DRIVER_THREAD_LOCAL.get();
        if (driver == null) {
            throw new IllegalStateException(
                    "WebDriver not initialised. Call DriverFactory.initDriver() first.");
        }
        return driver;
    }

    /**
     * Quit the current thread's WebDriver and clean up the ThreadLocal.
     * Safe to call even if the driver was never initialised (no-op).
     */
    // PUBLIC_INTERFACE
    public static void quitDriver() {
        WebDriver driver = DRIVER_THREAD_LOCAL.get();
        if (driver != null) {
            LOG.info("Quitting WebDriver (thread={})...", Thread.currentThread().getName());
            try {
                driver.quit();
            } catch (Exception e) {
                LOG.warn("Error while quitting WebDriver: {}", e.getMessage());
            } finally {
                DRIVER_THREAD_LOCAL.remove();
                LOG.info("WebDriver instance removed from ThreadLocal (thread={})",
                        Thread.currentThread().getName());
            }
        }
    }

    // --- Browser Strategy Dispatch ---

    /**
     * Create a WebDriver for the requested browser type.
     *
     * @param browser  browser identifier ({@code "chrome"} or {@code "firefox"})
     * @param headless whether to run headless
     * @return new WebDriver instance
     */
    private static WebDriver createDriver(String browser, boolean headless) {
        switch (browser) {
            case "chrome":
            case "chromium":
                return createChromeDriver(headless);
            case "firefox":
                return createFirefoxDriver(headless);
            default:
                LOG.error("Unsupported browser type: {}", browser);
                throw new UnsupportedOperationException(
                        "Browser '" + browser + "' is not supported. Use 'chrome' or 'firefox'.");
        }
    }

    /**
     * Create a Chrome/Chromium WebDriver with stability flags for CI.
     */
    private static WebDriver createChromeDriver(boolean headless) {
        WebDriverManager.chromedriver().setup();

        ChromeOptions options = new ChromeOptions();
        if (headless) {
            options.addArguments("--headless=new"); // Modern headless (Chrome 109+)
            LOG.info("Chrome configured in HEADLESS mode");
        } else {
            LOG.info("Chrome configured in HEADED mode");
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
        options.setAcceptInsecureCerts(true);

        return new ChromeDriver(options);
    }

    /**
     * Create a Firefox WebDriver with headless support.
     */
    private static WebDriver createFirefoxDriver(boolean headless) {
        WebDriverManager.firefoxdriver().setup();

        FirefoxOptions options = new FirefoxOptions();
        if (headless) {
            options.addArguments("-headless");
            LOG.info("Firefox configured in HEADLESS mode");
        } else {
            LOG.info("Firefox configured in HEADED mode");
        }

        options.addArguments("--width=1920");
        options.addArguments("--height=1080");
        options.setAcceptInsecureCerts(true);

        return new FirefoxDriver(options);
    }

    /**
     * Parse an integer safely, returning a default on failure.
     *
     * @param value the string to parse
     * @return parsed integer or 2 as fallback
     */
    private static int parseIntSafe(String value) {
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return 2;
        }
    }
}

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

    private DriverFactory() {
        // Utility class -- no instantiation
    }

    /**
     * Initialise a new WebDriver instance based on configuration and store
     * it in a ThreadLocal for safe parallel access.
     *
     * @return the initialised WebDriver
     * @throws UnsupportedOperationException when an unknown browser is configured
     */
    // PUBLIC_INTERFACE
    public static WebDriver initDriver() {
        String browser = ConfigManager.getBrowser();
        boolean headless = ConfigManager.isHeadless();

        LOG.info("Initialising WebDriver -- browser={}, headless={}", browser, headless);

        WebDriver driver = createDriver(browser, headless);

        // Configure timeouts from config
        driver.manage().timeouts().pageLoadTimeout(
                Duration.ofSeconds(ConfigManager.getPageLoadTimeout()));
        driver.manage().timeouts().implicitlyWait(
                Duration.ofSeconds(Integer.parseInt(ConfigManager.get("wait.implicit", "5"))));
        driver.manage().window().maximize();

        DRIVER_THREAD_LOCAL.set(driver);
        LOG.info("WebDriver initialised successfully (browser={}, headless={})", browser, headless);
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
}

package com.orangehrm.driver;

import com.orangehrm.config.TestConfig;
import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;

import java.time.Duration;
import org.openqa.selenium.Dimension;

/**
 * PUBLIC_INTERFACE
 * DriverFactory manages the lifecycle of Selenium WebDriver instances.
 *
 * <p>This factory uses ThreadLocal storage to ensure thread-safe WebDriver
 * management, enabling parallel test execution. It supports Chrome and Firefox
 * browsers with optional headless mode.</p>
 *
 * <p>WebDriverManager is used to automatically download and configure the
 * appropriate browser driver binaries.</p>
 *
 * <p>Usage in step definitions:
 * <pre>
 *   WebDriver driver = DriverFactory.getDriver();
 *   // ... perform actions ...
 *   DriverFactory.quitDriver();
 * </pre>
 */
public final class DriverFactory {

    /** Thread-local WebDriver instance for parallel test execution safety */
    private static final ThreadLocal<WebDriver> driverThreadLocal = new ThreadLocal<>();

    /** Default implicit wait timeout in seconds */
    private static final int DEFAULT_IMPLICIT_WAIT_SECONDS = 10;

    /** Default page load timeout in seconds */
    private static final int DEFAULT_PAGE_LOAD_TIMEOUT_SECONDS = 30;

    // Prevent instantiation
    private DriverFactory() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * PUBLIC_INTERFACE
     * Returns the WebDriver instance for the current thread, creating one if it does not exist.
     *
     * <p>The browser type and headless mode are determined by {@link TestConfig}.</p>
     *
     * @return the WebDriver instance for the current thread
     * @throws IllegalArgumentException if the configured browser type is not supported
     */
    public static WebDriver getDriver() {
        if (driverThreadLocal.get() == null) {
            WebDriver driver = createDriver();
            configureDriver(driver);
            driverThreadLocal.set(driver);
        }
        return driverThreadLocal.get();
    }

    /**
     * PUBLIC_INTERFACE
     * Quits the WebDriver instance for the current thread and removes it from ThreadLocal storage.
     *
     * <p>This method should be called in an {@code @After} hook or cleanup method
     * to ensure browser resources are properly released.</p>
     */
    public static void quitDriver() {
        WebDriver driver = driverThreadLocal.get();
        if (driver != null) {
            try {
                driver.quit();
            } catch (Exception e) {
                System.err.println("Warning: Error while quitting WebDriver: " + e.getMessage());
            } finally {
                driverThreadLocal.remove();
            }
        }
    }

    /**
     * Creates a new WebDriver instance based on the configured browser type.
     *
     * @return a new WebDriver instance
     * @throws IllegalArgumentException if the browser type is not supported
     */
    private static WebDriver createDriver() {
        String browser = TestConfig.getBrowser().toLowerCase();
        boolean headless = TestConfig.isHeadless();

        switch (browser) {
            case "chrome":
                return createChromeDriver(headless);
            case "firefox":
                return createFirefoxDriver(headless);
            default:
                throw new IllegalArgumentException(
                    "Unsupported browser type: " + browser + ". Supported: chrome, firefox"
                );
        }
    }

    /**
     * Creates a Chrome WebDriver instance with the specified options.
     *
     * @param headless whether to run in headless mode
     * @return a configured ChromeDriver instance
     */
    private static WebDriver createChromeDriver(boolean headless) {
        WebDriverManager.chromedriver().setup();
        ChromeOptions options = new ChromeOptions();

        if (headless) {
            options.addArguments("--headless=new");
        }

        // Common Chrome options for stability
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--disable-gpu");
        options.addArguments("--window-size=1920,1080");
        options.addArguments("--remote-allow-origins=*");

        return new ChromeDriver(options);
    }

    /**
     * Creates a Firefox WebDriver instance with the specified options.
     *
     * @param headless whether to run in headless mode
     * @return a configured FirefoxDriver instance
     */
    private static WebDriver createFirefoxDriver(boolean headless) {
        WebDriverManager.firefoxdriver().setup();
        FirefoxOptions options = new FirefoxOptions();

        if (headless) {
            options.addArguments("--headless");
        }

        options.addArguments("--width=1920");
        options.addArguments("--height=1080");

        return new FirefoxDriver(options);
    }

    /**
     * Configures common driver settings such as timeouts and window management.
     *
     * @param driver the WebDriver instance to configure
     */
    private static void configureDriver(WebDriver driver) {
        driver.manage().timeouts().implicitlyWait(
            Duration.ofSeconds(DEFAULT_IMPLICIT_WAIT_SECONDS)
        );
        driver.manage().timeouts().pageLoadTimeout(
            Duration.ofSeconds(DEFAULT_PAGE_LOAD_TIMEOUT_SECONDS)
        );
        try { driver.manage().window().setSize(new Dimension(1920, 1080)); } catch (Exception e) { System.err.println("Warning: Could not set window size: " + e.getMessage()); }
    }
}

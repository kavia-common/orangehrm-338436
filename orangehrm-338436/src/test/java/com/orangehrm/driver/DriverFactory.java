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
 * browsers with configurable headless mode.</p>
 *
 * <p><strong>Execution Modes:</strong></p>
 * <ul>
 *   <li><strong>Visible (non-headless) mode</strong> – The browser window opens
 *       on screen so you can watch tests run in real time. Requires a display
 *       (e.g., a desktop session or X11 DISPLAY). This is the <em>default</em> mode.</li>
 *   <li><strong>Headless mode</strong> – The browser runs without a visible window,
 *       suitable for CI/CD pipelines. Enable via {@code -Dheadless=true} or
 *       {@code -Pheadless} Maven profile.</li>
 * </ul>
 *
 * <p><strong>Run commands:</strong></p>
 * <pre>
 *   # Non-headless (visible browser) – default:
 *   mvn test
 *   mvn test -Dheadless=false
 *
 *   # Headless (for CI):
 *   mvn test -Dheadless=true
 *   mvn test -Pheadless
 * </pre>
 *
 * <p>WebDriverManager is used to automatically download and configure the
 * appropriate browser driver binaries.</p>
 *
 * <p>Usage in step definitions:</p>
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
     * <p>The browser type and headless mode are determined by {@link TestConfig}.
     * By default, the browser runs in visible (non-headless) mode so you can see
     * the browser UI during test execution.</p>
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
     * <p>Logs the execution mode (headless vs. visible) and any display environment
     * information to help with debugging.</p>
     *
     * @return a new WebDriver instance
     * @throws IllegalArgumentException if the browser type is not supported
     */
    private static WebDriver createDriver() {
        String browser = TestConfig.getBrowser().toLowerCase();
        boolean headless = TestConfig.isHeadless();

        // Log the execution mode for clarity
        logExecutionMode(browser, headless);

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
     * Logs the current execution mode and display environment information.
     *
     * @param browser  the browser type being used
     * @param headless whether headless mode is enabled
     */
    private static void logExecutionMode(String browser, boolean headless) {
        String displayEnv = System.getenv("DISPLAY");
        System.out.println("=== WebDriver Configuration ===");
        System.out.println("  Browser : " + browser);
        System.out.println("  Headless: " + headless);
        System.out.println("  DISPLAY : " + (displayEnv != null ? displayEnv : "(not set)"));

        if (!headless) {
            // Visible mode: check if a display is available
            if (displayEnv == null || displayEnv.isEmpty()) {
                System.out.println("  WARNING: Running in visible (non-headless) mode but no DISPLAY "
                    + "environment variable is set. The browser may fail to launch.");
                System.out.println("  TIP: If you are on a headless server or CI, run with "
                    + "-Dheadless=true or use the -Pheadless Maven profile.");
            } else {
                System.out.println("  Mode    : VISIBLE – Chrome will open on screen at DISPLAY=" + displayEnv);
            }
        } else {
            System.out.println("  Mode    : HEADLESS – no browser window will be shown");
        }
        System.out.println("================================");
    }

    /**
     * Creates a Chrome WebDriver instance configured for either visible or headless execution.
     *
     * <p>In <strong>visible (non-headless) mode</strong>:
     * <ul>
     *   <li>No {@code --headless} argument is added</li>
     *   <li>Chrome opens a real browser window on the screen</li>
     *   <li>You can watch login and other actions happen in real time</li>
     *   <li>Requires a display (DISPLAY env var or desktop session)</li>
     * </ul>
     *
     * <p>In <strong>headless mode</strong>:
     * <ul>
     *   <li>{@code --headless=new} argument is added for Chrome's new headless mode</li>
     *   <li>No display or xvfb-run is required</li>
     *   <li>Suitable for CI/CD environments</li>
     * </ul>
     *
     * @param headless whether to run in headless mode
     * @return a configured ChromeDriver instance
     */
    private static WebDriver createChromeDriver(boolean headless) {
        WebDriverManager.chromedriver().setup();
        ChromeOptions options = new ChromeOptions();

        if (headless) {
            // Headless mode: run without a visible browser window (for CI)
            options.addArguments("--headless=new");
            // disable-gpu is recommended in headless mode on some platforms
            options.addArguments("--disable-gpu");
        }
        // Note: --headless is NOT added when headless=false (the default),
        // so Chrome will launch a real visible window on the screen.

        // Common Chrome stability options
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--window-size=1920,1080");
        options.addArguments("--remote-allow-origins=*");

        // Additional options to improve visible mode stability
        if (!headless) {
            // Disable Chrome's "Chrome is being controlled by automated test software" infobar
            options.setExperimentalOption("excludeSwitches", new String[]{"enable-automation"});
            // Disable automation extension to reduce popups/infobars
            options.addArguments("--disable-infobars");
            // Start maximized so the browser is fully visible
            options.addArguments("--start-maximized");
        }

        return new ChromeDriver(options);
    }

    /**
     * Creates a Firefox WebDriver instance with the specified options.
     *
     * <p>In visible mode, Firefox will open a real browser window.
     * In headless mode, {@code --headless} is passed to run without a window.</p>
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
        // Set window size; may fail in some headless environments
        try {
            driver.manage().window().setSize(new Dimension(1920, 1080));
        } catch (Exception e) {
            System.err.println("Warning: Could not set window size: " + e.getMessage());
        }
    }
}

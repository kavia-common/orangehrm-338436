package com.orangehrm.utils;

import com.orangehrm.config.ConfigManager;
import com.orangehrm.driver.DriverFactory;
import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.FluentWait;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

/**
 * Centralized wait utilities for the Selenium test suite.
 *
 * <h3>Flow name: WaitFlow</h3>
 * <p>Single reusable entrypoint for all wait operations in the test suite.
 * Eliminates {@code Thread.sleep} calls by providing polling-based alternatives
 * for every common wait condition.</p>
 *
 * <h3>Contract</h3>
 * <ul>
 *   <li><b>Inputs:</b> locators, timeout durations, expected conditions, or
 *       custom boolean predicates.</li>
 *   <li><b>Outputs:</b> WebElements (for element waits), boolean (for condition
 *       checks), or void (for pure wait operations).</li>
 *   <li><b>Errors:</b> {@link TimeoutException} when a wait condition is not
 *       met within the specified timeout. Callers decide whether to propagate
 *       or handle gracefully.</li>
 *   <li><b>Side-effects:</b> none — read-only browser polling.</li>
 * </ul>
 *
 * <h3>Thread safety</h3>
 * <p>All driver access is via {@link DriverFactory#getDriver()} (ThreadLocal).
 * No mutable shared state. Safe for parallel execution.</p>
 *
 * <h3>Observability</h3>
 * <p>All waits log their intent at DEBUG level and timeouts at WARN level.</p>
 */
// PUBLIC_INTERFACE
public final class WaitUtils {

    private static final Logger LOG = LoggerFactory.getLogger(WaitUtils.class);

    /** Default polling interval for fluent waits. */
    private static final Duration POLL_INTERVAL = Duration.ofMillis(300);

    /** Maximum stale-element retries. */
    private static final int STALE_RETRIES = 3;

    private WaitUtils() {
        // Utility class — no instantiation
    }

    // --- Factory methods ---

    /**
     * Create a WebDriverWait with the configured default explicit timeout.
     *
     * @return a fresh WebDriverWait instance
     */
    // PUBLIC_INTERFACE
    public static WebDriverWait defaultWait() {
        return new WebDriverWait(
                DriverFactory.getDriver(),
                Duration.ofSeconds(ConfigManager.getExplicitWait()));
    }

    /**
     * Create a WebDriverWait with a custom timeout.
     *
     * @param timeoutSeconds custom timeout in seconds
     * @return a fresh WebDriverWait instance
     */
    // PUBLIC_INTERFACE
    public static WebDriverWait waitFor(int timeoutSeconds) {
        return new WebDriverWait(
                DriverFactory.getDriver(),
                Duration.ofSeconds(timeoutSeconds));
    }

    /**
     * Create a FluentWait that ignores stale-element and no-such-element
     * exceptions during polling.
     *
     * @param timeoutSeconds total timeout in seconds
     * @return configured FluentWait
     */
    // PUBLIC_INTERFACE
    public static FluentWait<WebDriver> fluentWait(int timeoutSeconds) {
        return new FluentWait<>(DriverFactory.getDriver())
                .withTimeout(Duration.ofSeconds(timeoutSeconds))
                .pollingEvery(POLL_INTERVAL)
                .ignoring(NoSuchElementException.class)
                .ignoring(StaleElementReferenceException.class);
    }

    // --- Element waits ---

    /**
     * Wait for an element to become visible using the default timeout.
     *
     * @param locator the By locator
     * @return the visible WebElement
     */
    // PUBLIC_INTERFACE
    public static WebElement forVisible(By locator) {
        LOG.debug("WaitUtils.forVisible — locator: {}", locator);
        return defaultWait().until(ExpectedConditions.visibilityOfElementLocated(locator));
    }

    /**
     * Wait for an element to become visible using a custom timeout.
     *
     * @param locator        the By locator
     * @param timeoutSeconds custom timeout
     * @return the visible WebElement
     */
    // PUBLIC_INTERFACE
    public static WebElement forVisible(By locator, int timeoutSeconds) {
        LOG.debug("WaitUtils.forVisible — locator: {}, timeout: {}s", locator, timeoutSeconds);
        return waitFor(timeoutSeconds).until(ExpectedConditions.visibilityOfElementLocated(locator));
    }

    /**
     * Wait for an element to become clickable using the default timeout.
     *
     * @param locator the By locator
     * @return the clickable WebElement
     */
    // PUBLIC_INTERFACE
    public static WebElement forClickable(By locator) {
        LOG.debug("WaitUtils.forClickable — locator: {}", locator);
        return defaultWait().until(ExpectedConditions.elementToBeClickable(locator));
    }

    /**
     * Wait for an element to become clickable using a custom timeout.
     *
     * @param locator        the By locator
     * @param timeoutSeconds custom timeout
     * @return the clickable WebElement
     */
    // PUBLIC_INTERFACE
    public static WebElement forClickable(By locator, int timeoutSeconds) {
        LOG.debug("WaitUtils.forClickable — locator: {}, timeout: {}s", locator, timeoutSeconds);
        return waitFor(timeoutSeconds).until(ExpectedConditions.elementToBeClickable(locator));
    }

    /**
     * Wait for an element to be present in the DOM (not necessarily visible).
     *
     * @param locator the By locator
     * @return the WebElement
     */
    // PUBLIC_INTERFACE
    public static WebElement forPresence(By locator) {
        LOG.debug("WaitUtils.forPresence — locator: {}", locator);
        return defaultWait().until(ExpectedConditions.presenceOfElementLocated(locator));
    }

    /**
     * Wait for an element to become invisible or removed from DOM.
     *
     * @param locator        the By locator
     * @param timeoutSeconds custom timeout
     * @return true if the element became invisible
     */
    // PUBLIC_INTERFACE
    public static boolean forInvisible(By locator, int timeoutSeconds) {
        LOG.debug("WaitUtils.forInvisible — locator: {}, timeout: {}s", locator, timeoutSeconds);
        try {
            return waitFor(timeoutSeconds).until(ExpectedConditions.invisibilityOfElementLocated(locator));
        } catch (TimeoutException e) {
            LOG.warn("Element did not become invisible within {}s: {}", timeoutSeconds, locator);
            return false;
        }
    }

    // --- URL waits ---

    /**
     * Wait for the current URL to contain a specific fragment.
     *
     * @param urlFragment the expected URL fragment
     */
    // PUBLIC_INTERFACE
    public static void forUrlContains(String urlFragment) {
        LOG.debug("WaitUtils.forUrlContains — fragment: {}", urlFragment);
        defaultWait().until(ExpectedConditions.urlContains(urlFragment));
    }

    /**
     * Wait for the current URL to contain a specific fragment with custom timeout.
     *
     * @param urlFragment    the expected URL fragment
     * @param timeoutSeconds custom timeout
     */
    // PUBLIC_INTERFACE
    public static void forUrlContains(String urlFragment, int timeoutSeconds) {
        LOG.debug("WaitUtils.forUrlContains — fragment: {}, timeout: {}s", urlFragment, timeoutSeconds);
        waitFor(timeoutSeconds).until(ExpectedConditions.urlContains(urlFragment));
    }

    // --- Page state waits ---

    /**
     * Wait for the page document.readyState to be "complete".
     * Uses polling instead of Thread.sleep.
     *
     * @param timeoutSeconds maximum time to wait
     */
    // PUBLIC_INTERFACE
    public static void forPageReady(int timeoutSeconds) {
        LOG.debug("WaitUtils.forPageReady — timeout: {}s", timeoutSeconds);
        waitFor(timeoutSeconds).until(
                (ExpectedCondition<Boolean>) driver -> {
                    String readyState = ((JavascriptExecutor) driver)
                            .executeScript("return document.readyState").toString();
                    return "complete".equals(readyState);
                });
    }

    /**
     * Wait for the page document.readyState to be "complete" using default timeout.
     */
    // PUBLIC_INTERFACE
    public static void forPageReady() {
        forPageReady(ConfigManager.getPageLoadTimeout());
    }

    /**
     * Wait for any OXD loading spinner to disappear. OrangeHRM uses
     * {@code .oxd-loading-spinner} during async operations.
     *
     * @param timeoutSeconds maximum time to wait for spinner to disappear
     */
    // PUBLIC_INTERFACE
    public static void forSpinnerToDisappear(int timeoutSeconds) {
        LOG.debug("WaitUtils.forSpinnerToDisappear — timeout: {}s", timeoutSeconds);
        forInvisible(By.cssSelector(".oxd-loading-spinner, .oxd-loading-spinner-container"), timeoutSeconds);
    }

    /**
     * Wait for any OXD loading spinner to disappear using default timeout.
     */
    // PUBLIC_INTERFACE
    public static void forSpinnerToDisappear() {
        forSpinnerToDisappear(ConfigManager.getExplicitWait());
    }

    // --- Custom condition waits ---

    /**
     * Wait for a custom boolean condition to become true, using polling
     * instead of Thread.sleep. This is the preferred replacement for any
     * {@code Thread.sleep} + check pattern.
     *
     * @param description    human-readable description for logging
     * @param condition      the condition to poll
     * @param timeoutSeconds maximum wait time
     * @return true if the condition was met
     * @throws TimeoutException if the condition is not met within timeout
     */
    // PUBLIC_INTERFACE
    public static boolean forCondition(String description, BooleanSupplier condition, int timeoutSeconds) {
        LOG.debug("WaitUtils.forCondition — '{}', timeout: {}s", description, timeoutSeconds);
        return fluentWait(timeoutSeconds).until(driver -> condition.getAsBoolean());
    }

    /**
     * Wait for a custom boolean condition, returning false on timeout
     * instead of throwing.
     *
     * @param description    human-readable description for logging
     * @param condition      the condition to poll
     * @param timeoutSeconds maximum wait time
     * @return true if the condition was met, false on timeout
     */
    // PUBLIC_INTERFACE
    public static boolean forConditionSafe(String description, BooleanSupplier condition, int timeoutSeconds) {
        try {
            return forCondition(description, condition, timeoutSeconds);
        } catch (TimeoutException e) {
            LOG.warn("Condition not met within {}s: '{}'", timeoutSeconds, description);
            return false;
        }
    }

    // --- Retry helpers ---

    /**
     * Retry an action up to {@code maxRetries} times on
     * {@link StaleElementReferenceException}. Essential for dynamic Vue.js
     * pages where the DOM re-renders between locate and act.
     *
     * @param action     the action to execute
     * @param maxRetries maximum retry attempts
     * @param <T>        return type
     * @return the action's result
     * @throws StaleElementReferenceException if all retries are exhausted
     */
    // PUBLIC_INTERFACE
    public static <T> T retryOnStale(Supplier<T> action, int maxRetries) {
        StaleElementReferenceException lastException = null;
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                return action.get();
            } catch (StaleElementReferenceException e) {
                lastException = e;
                LOG.debug("StaleElementReferenceException on attempt {}/{} — retrying",
                        attempt, maxRetries);
            }
        }
        throw lastException;
    }

    /**
     * Retry an action with default retry count (3).
     *
     * @param action the action to execute
     * @param <T>    return type
     * @return the action's result
     */
    // PUBLIC_INTERFACE
    public static <T> T retryOnStale(Supplier<T> action) {
        return retryOnStale(action, STALE_RETRIES);
    }
}

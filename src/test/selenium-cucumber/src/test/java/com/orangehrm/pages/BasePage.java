package com.orangehrm.pages;

import com.orangehrm.config.ConfigManager;
import com.orangehrm.driver.DriverFactory;
import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.FluentWait;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.List;
import java.util.function.Supplier;

/**
 * Base page object providing reusable WebDriver utilities:
 * explicit waits, robust locators for OXD components, common interactions,
 * retry-safe element operations, and exception-safe helpers.
 *
 * <h3>Stability improvements</h3>
 * <ul>
 *   <li>All waits are explicit (no reliance on implicit waits).</li>
 *   <li>{@link #retryOnStale(Supplier, int)} wraps actions that may encounter
 *       {@link StaleElementReferenceException} in a retry loop.</li>
 *   <li>Configuration values are resolved lazily to avoid class-load ordering
 *       issues with {@link ConfigManager}.</li>
 * </ul>
 *
 * <h3>Thread safety</h3>
 * <p>All driver access goes through {@link DriverFactory#getDriver()} which is
 * {@code ThreadLocal}-backed. Each step definition class should create its own
 * {@code BasePage} instance (or receive one via DI) so there is no shared
 * mutable state across threads.</p>
 */
// PUBLIC_INTERFACE
public class BasePage {

    protected static final Logger LOG = LoggerFactory.getLogger(BasePage.class);

    /** Maximum number of retries for stale-element recovery. */
    private static final int STALE_RETRY_COUNT = 3;

    /** Polling interval for fluent waits. */
    private static final Duration POLL_INTERVAL = Duration.ofMillis(300);

    // ─── OXD CSS Selectors (aligned with the Vue OXD component library) ───

    /** OXD form wrapper */
    public static final String OXD_FORM = ".oxd-form";
    /** OXD generic button */
    public static final String OXD_BUTTON = ".oxd-button";
    /** OXD text input */
    public static final String OXD_INPUT = ".oxd-input";
    /** OXD input group (label + input + error) */
    public static final String OXD_INPUT_GROUP = ".oxd-input-group";
    /** OXD toast notification wrapper */
    public static final String OXD_TOAST = ".oxd-toast";
    /** OXD toast success variant */
    public static final String OXD_TOAST_SUCCESS = ".oxd-toast--success";
    /** OXD toast error variant */
    public static final String OXD_TOAST_ERROR = ".oxd-toast--error";
    /** OXD toast info variant */
    public static final String OXD_TOAST_INFO = ".oxd-toast--info";
    /** OXD page title */
    public static final String OXD_PAGE_TITLE = ".orangehrm-main-title";
    /** OXD page context area */
    public static final String OXD_PAGE_CONTEXT = ".oxd-layout-context";
    /** OXD table body */
    public static final String OXD_TABLE_BODY = ".oxd-table-body";
    /** OXD table row */
    public static final String OXD_TABLE_ROW = ".oxd-table-card";
    /** OXD select/dropdown input */
    public static final String OXD_SELECT = ".oxd-select-text-input";
    /** OXD autocomplete input */
    public static final String OXD_AUTOCOMPLETE = ".oxd-autocomplete-text-input";
    /** OXD checkbox input */
    public static final String OXD_CHECKBOX = ".oxd-checkbox-input";
    /** OXD switch toggle */
    public static final String OXD_SWITCH = ".oxd-switch-input";
    /** OXD textarea */
    public static final String OXD_TEXTAREA = ".oxd-textarea";
    /** OXD sheet/dialog */
    public static final String OXD_DIALOG = ".oxd-dialog-sheet";
    /** OXD sidebar/left menu */
    public static final String OXD_SIDEBAR = ".oxd-sidepanel";
    /** OXD topbar navigation */
    public static final String OXD_TOPBAR = ".oxd-topbar";
    /** OXD user dropdown */
    public static final String OXD_USERDROPDOWN = ".oxd-userdropdown";
    /** OXD dropdown menu */
    public static final String OXD_DROPDOWN_MENU = ".oxd-dropdown-menu";
    /** Validation error span within input group */
    public static final String OXD_INPUT_ERROR = ".oxd-input-group > .oxd-input-field-error-message";
    /** General alert banner on pages like login */
    public static final String OXD_ALERT = ".oxd-alert";
    /** OXD sheet container for confirmation dialogs */
    public static final String OXD_SHEET = ".orangehrm-dialog-popup";

    // ─── Core Utility Methods ───

    /**
     * Resolve the configured explicit-wait timeout lazily.
     * Avoids class-load ordering issues with ConfigManager static init.
     *
     * @return explicit wait timeout in seconds
     */
    // PUBLIC_INTERFACE
    protected int getExplicitWaitSeconds() {
        return ConfigManager.getExplicitWait();
    }

    /**
     * Get a fresh WebDriverWait instance.
     *
     * @param timeoutSeconds timeout in seconds
     * @return WebDriverWait
     */
    // PUBLIC_INTERFACE
    protected WebDriverWait getWait(int timeoutSeconds) {
        return new WebDriverWait(DriverFactory.getDriver(), Duration.ofSeconds(timeoutSeconds));
    }

    /**
     * Get a default WebDriverWait using configured explicit timeout.
     *
     * @return WebDriverWait
     */
    // PUBLIC_INTERFACE
    protected WebDriverWait getWait() {
        return getWait(getExplicitWaitSeconds());
    }

    /**
     * Get a FluentWait that ignores stale-element and no-such-element
     * exceptions during polling, reducing flakiness on dynamic pages.
     *
     * @param timeoutSeconds total timeout in seconds
     * @return configured FluentWait
     */
    // PUBLIC_INTERFACE
    protected FluentWait<WebDriver> getFluentWait(int timeoutSeconds) {
        return new FluentWait<>(DriverFactory.getDriver())
                .withTimeout(Duration.ofSeconds(timeoutSeconds))
                .pollingEvery(POLL_INTERVAL)
                .ignoring(NoSuchElementException.class)
                .ignoring(StaleElementReferenceException.class);
    }

    /**
     * Retry an action up to {@code maxRetries} times when it throws
     * {@link StaleElementReferenceException}. This is essential for
     * interacting with elements on dynamic Vue.js pages where the DOM
     * may be re-rendered between locating and acting on an element.
     *
     * @param action     the action to execute
     * @param maxRetries maximum retry attempts
     * @param <T>        return type
     * @return the action's result
     */
    // PUBLIC_INTERFACE
    protected <T> T retryOnStale(Supplier<T> action, int maxRetries) {
        StaleElementReferenceException lastException = null;
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                return action.get();
            } catch (StaleElementReferenceException e) {
                lastException = e;
                LOG.debug("StaleElementReferenceException on attempt {}/{} — retrying", attempt, maxRetries);
            }
        }
        throw lastException;
    }

    /**
     * Navigate to a path relative to the configured base URL.
     *
     * @param path the relative path (e.g., "/auth/login")
     */
    // PUBLIC_INTERFACE
    public void navigateTo(String path) {
        String url = ConfigManager.getBaseUrl() + "/web/index.php" + path;
        LOG.info("Navigating to: {}", url);
        DriverFactory.getDriver().get(url);
    }

    /**
     * Navigate to an absolute URL.
     *
     * @param url the full URL
     */
    // PUBLIC_INTERFACE
    public void navigateToAbsolute(String url) {
        LOG.info("Navigating to absolute URL: {}", url);
        DriverFactory.getDriver().get(url);
    }

    /**
     * Wait for an element to be visible and return it.
     *
     * @param locator the By locator
     * @return the visible WebElement
     */
    // PUBLIC_INTERFACE
    public WebElement waitForVisible(By locator) {
        LOG.debug("Waiting for element to be visible: {}", locator);
        return getWait().until(ExpectedConditions.visibilityOfElementLocated(locator));
    }

    /**
     * Wait for an element to be visible with a custom timeout.
     *
     * @param locator        the By locator
     * @param timeoutSeconds custom timeout
     * @return the visible WebElement
     */
    // PUBLIC_INTERFACE
    public WebElement waitForVisible(By locator, int timeoutSeconds) {
        LOG.debug("Waiting ({}s) for element to be visible: {}", timeoutSeconds, locator);
        return getWait(timeoutSeconds).until(ExpectedConditions.visibilityOfElementLocated(locator));
    }

    /**
     * Wait for an element to be clickable and return it.
     *
     * @param locator the By locator
     * @return the clickable WebElement
     */
    // PUBLIC_INTERFACE
    public WebElement waitForClickable(By locator) {
        LOG.debug("Waiting for element to be clickable: {}", locator);
        return getWait().until(ExpectedConditions.elementToBeClickable(locator));
    }

    /**
     * Wait for all matching elements to be visible.
     *
     * @param locator the By locator
     * @return list of visible WebElements
     */
    // PUBLIC_INTERFACE
    public List<WebElement> waitForAllVisible(By locator) {
        LOG.debug("Waiting for all elements to be visible: {}", locator);
        return getWait().until(ExpectedConditions.visibilityOfAllElementsLocatedBy(locator));
    }

    /**
     * Wait for the presence of an element in the DOM (not necessarily visible).
     *
     * @param locator the By locator
     * @return the WebElement
     */
    // PUBLIC_INTERFACE
    public WebElement waitForPresence(By locator) {
        LOG.debug("Waiting for element presence: {}", locator);
        return getWait().until(ExpectedConditions.presenceOfElementLocated(locator));
    }

    /**
     * Check if an element is displayed on the page (non-throwing).
     *
     * @param locator the By locator
     * @return true if displayed
     */
    // PUBLIC_INTERFACE
    public boolean isElementDisplayed(By locator) {
        try {
            return DriverFactory.getDriver().findElement(locator).isDisplayed();
        } catch (NoSuchElementException | StaleElementReferenceException e) {
            return false;
        }
    }

    /**
     * Check if an element becomes visible within the given timeout.
     *
     * @param locator        the By locator
     * @param timeoutSeconds how long to wait
     * @return true if visible
     */
    // PUBLIC_INTERFACE
    public boolean isElementVisible(By locator, int timeoutSeconds) {
        try {
            getWait(timeoutSeconds).until(ExpectedConditions.visibilityOfElementLocated(locator));
            return true;
        } catch (TimeoutException e) {
            return false;
        }
    }

    /**
     * Clear an input field and type text into it with stale-element retry.
     *
     * @param locator the By locator for the input
     * @param text    the text to type
     */
    // PUBLIC_INTERFACE
    public void clearAndType(By locator, String text) {
        retryOnStale(() -> {
            WebElement element = waitForClickable(locator);
            element.clear();
            element.sendKeys(text);
            LOG.debug("Typed '{}' into {}", text, locator);
            return null;
        }, STALE_RETRY_COUNT);
    }

    /**
     * Click an element after waiting for it to be clickable, with
     * stale-element retry for dynamic DOM updates.
     *
     * @param locator the By locator
     */
    // PUBLIC_INTERFACE
    public void click(By locator) {
        retryOnStale(() -> {
            WebElement element = waitForClickable(locator);
            element.click();
            LOG.debug("Clicked element: {}", locator);
            return null;
        }, STALE_RETRY_COUNT);
    }

    /**
     * Get the text content of an element.
     *
     * @param locator the By locator
     * @return the trimmed text
     */
    // PUBLIC_INTERFACE
    public String getText(By locator) {
        return waitForVisible(locator).getText().trim();
    }

    /**
     * Get the current page URL.
     *
     * @return current URL
     */
    // PUBLIC_INTERFACE
    public String getCurrentUrl() {
        return DriverFactory.getDriver().getCurrentUrl();
    }

    /**
     * Wait for the current URL to contain a specific fragment.
     *
     * @param urlFragment the expected URL fragment
     */
    // PUBLIC_INTERFACE
    public void waitForUrlContains(String urlFragment) {
        LOG.debug("Waiting for URL to contain: {}", urlFragment);
        getWait().until(ExpectedConditions.urlContains(urlFragment));
    }

    /**
     * Find the OXD input group for a given label text, then return
     * the actual input element within it. Uses stale-element retry
     * for robustness on dynamic pages.
     *
     * @param labelText the visible label text (e.g. "Username")
     * @return the input WebElement inside that group
     */
    // PUBLIC_INTERFACE
    public WebElement findOxdInputByLabel(String labelText) {
        LOG.debug("Looking for OXD input with label: {}", labelText);
        return retryOnStale(() -> {
            // OXD input groups: label is in .oxd-input-group > .oxd-label
            List<WebElement> groups = waitForAllVisible(By.cssSelector(OXD_INPUT_GROUP));
            for (WebElement group : groups) {
                try {
                    WebElement label = group.findElement(By.cssSelector(".oxd-label"));
                    if (label.getText().trim().equalsIgnoreCase(labelText)) {
                        // Try standard input first, then textarea, then autocomplete
                        try {
                            return group.findElement(By.cssSelector(OXD_INPUT));
                        } catch (NoSuchElementException e1) {
                            try {
                                return group.findElement(By.cssSelector(OXD_TEXTAREA));
                            } catch (NoSuchElementException e2) {
                                return group.findElement(By.cssSelector(OXD_AUTOCOMPLETE + ", " + OXD_SELECT));
                            }
                        }
                    }
                } catch (NoSuchElementException ignored) {
                    // Group without label – skip
                }
            }
            throw new NoSuchElementException("Could not find OXD input with label: " + labelText);
        }, STALE_RETRY_COUNT);
    }

    /**
     * Get the validation error message for an OXD input group with the given label.
     * Uses an explicit wait (up to 3 seconds) for the error message to appear,
     * which avoids the flaky {@code Thread.sleep} pattern.
     *
     * @param labelText the label text of the input group
     * @return the error message text, or empty string if none appears
     */
    // PUBLIC_INTERFACE
    public String getInputValidationError(String labelText) {
        return retryOnStale(() -> {
            List<WebElement> groups = DriverFactory.getDriver().findElements(By.cssSelector(OXD_INPUT_GROUP));
            for (WebElement group : groups) {
                try {
                    WebElement label = group.findElement(By.cssSelector(".oxd-label"));
                    if (label.getText().trim().equalsIgnoreCase(labelText)) {
                        // Use a short fluent wait for validation messages to render
                        try {
                            new FluentWait<>(group)
                                    .withTimeout(Duration.ofSeconds(3))
                                    .pollingEvery(Duration.ofMillis(200))
                                    .ignoring(NoSuchElementException.class)
                                    .until(g -> g.findElement(By.cssSelector(".oxd-input-field-error-message")));
                            WebElement error = group.findElement(By.cssSelector(".oxd-input-field-error-message"));
                            return error.getText().trim();
                        } catch (TimeoutException e) {
                            return "";
                        }
                    }
                } catch (NoSuchElementException ignored) {
                    // no label in this group
                }
            }
            return "";
        }, STALE_RETRY_COUNT);
    }

    /**
     * Wait for a validation error to appear on the specified field.
     * Uses polling via {@link FluentWait} instead of {@code Thread.sleep}
     * to avoid hardcoded delays and improve reliability.
     *
     * @param labelText    the label text of the input group
     * @param expectedText the expected error text fragment
     * @param timeoutSec   how long to wait for the error to appear
     * @return the actual error message text
     * @throws TimeoutException if the expected error does not appear in time
     */
    // PUBLIC_INTERFACE
    public String waitForValidationError(String labelText, String expectedText, int timeoutSec) {
        LOG.debug("Waiting for validation error on '{}' containing '{}'", labelText, expectedText);
        final String[] lastError = {""};
        try {
            new FluentWait<>(DriverFactory.getDriver())
                    .withTimeout(Duration.ofSeconds(timeoutSec))
                    .pollingEvery(POLL_INTERVAL)
                    .ignoring(NoSuchElementException.class)
                    .ignoring(StaleElementReferenceException.class)
                    .until(driver -> {
                        lastError[0] = getInputValidationError(labelText);
                        return !lastError[0].isEmpty() && lastError[0].contains(expectedText);
                    });
            LOG.debug("Validation error found: '{}'", lastError[0]);
        } catch (TimeoutException e) {
            LOG.debug("Validation error '{}' not found on '{}' within {}s — returning last seen: '{}'",
                    expectedText, labelText, timeoutSec, lastError[0]);
        }
        // Return whatever was last seen (may be empty) — let the caller assert
        return lastError[0];
    }

    /**
     * Click a button by its visible text, with stale-element retry.
     *
     * @param buttonText the button text (e.g., "Login", "Save")
     */
    // PUBLIC_INTERFACE
    public void clickButtonByText(String buttonText) {
        LOG.debug("Clicking button with text: {}", buttonText);
        retryOnStale(() -> {
            List<WebElement> buttons = waitForAllVisible(By.cssSelector(OXD_BUTTON));
            for (WebElement btn : buttons) {
                if (btn.getText().trim().equalsIgnoreCase(buttonText)) {
                    btn.click();
                    LOG.info("Clicked button: {}", buttonText);
                    return null;
                }
            }
            // Fallback: try XPath contains
            WebElement fallback = waitForClickable(
                    By.xpath("//button[contains(normalize-space(),'" + buttonText + "')]"));
            fallback.click();
            LOG.info("Clicked button (fallback XPath): {}", buttonText);
            return null;
        }, STALE_RETRY_COUNT);
    }

    /**
     * Wait for a toast notification with the specified type and message.
     *
     * @param toastType    e.g., "success", "error", "info"
     * @param expectedText the expected message fragment
     * @return true if the toast appeared with matching text
     */
    // PUBLIC_INTERFACE
    public boolean waitForToast(String toastType, String expectedText) {
        String selector = ".oxd-toast--" + toastType;
        LOG.debug("Waiting for toast [{}] with text containing: {}", toastType, expectedText);
        try {
            WebElement toast = waitForVisible(By.cssSelector(selector), 10);
            String toastText = toast.getText();
            LOG.info("Toast appeared: [{}] {}", toastType, toastText);
            return toastText.contains(expectedText);
        } catch (TimeoutException e) {
            LOG.warn("Toast [{}] with text '{}' did not appear within timeout", toastType, expectedText);
            return false;
        }
    }

    /**
     * Wait for the page content area to finish loading (OXD layout context visible).
     */
    // PUBLIC_INTERFACE
    public void waitForPageLoad() {
        waitForVisible(By.cssSelector(OXD_PAGE_CONTEXT));
        LOG.debug("Page content area loaded");
    }

    /**
     * Take a screenshot and return it as bytes (for attaching to reports).
     *
     * @return screenshot bytes, or null on failure
     */
    // PUBLIC_INTERFACE
    public byte[] takeScreenshot() {
        try {
            return ((TakesScreenshot) DriverFactory.getDriver()).getScreenshotAs(OutputType.BYTES);
        } catch (Exception e) {
            LOG.error("Failed to capture screenshot: {}", e.getMessage());
            return null;
        }
    }
}

package com.orangehrm.pages;

import com.orangehrm.config.ConfigManager;
import com.orangehrm.driver.DriverFactory;
import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.List;

/**
 * Base page object providing reusable WebDriver utilities:
 * explicit waits, robust locators for OXD components, common interactions,
 * and exception-safe element operations.
 */
// PUBLIC_INTERFACE
public class BasePage {

    protected static final Logger LOG = LoggerFactory.getLogger(BasePage.class);

    /** Default explicit wait timeout sourced from configuration */
    protected static final int EXPLICIT_WAIT_SECONDS = ConfigManager.getExplicitWait();

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
     * Get a fresh WebDriverWait instance.
     *
     * @param timeoutSeconds timeout in seconds
     * @return WebDriverWait
     */
    // PUBLIC_INTERFACE
    protected WebDriverWait getWait(int timeoutSeconds) {
        return new WebDriverWait(DriverFactory.getDriver(), Duration.ofSeconds(timeoutSeconds));
    }

    /** @return default WebDriverWait using configured explicit timeout */
    // PUBLIC_INTERFACE
    protected WebDriverWait getWait() {
        return getWait(EXPLICIT_WAIT_SECONDS);
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
     * Clear an input field and type text into it.
     *
     * @param locator the By locator for the input
     * @param text    the text to type
     */
    // PUBLIC_INTERFACE
    public void clearAndType(By locator, String text) {
        WebElement element = waitForClickable(locator);
        element.clear();
        element.sendKeys(text);
        LOG.debug("Typed '{}' into {}", text, locator);
    }

    /**
     * Click an element after waiting for it to be clickable.
     *
     * @param locator the By locator
     */
    // PUBLIC_INTERFACE
    public void click(By locator) {
        WebElement element = waitForClickable(locator);
        element.click();
        LOG.debug("Clicked element: {}", locator);
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
     * the actual input element within it.
     *
     * @param labelText the visible label text (e.g. "Username")
     * @return the input WebElement inside that group
     */
    // PUBLIC_INTERFACE
    public WebElement findOxdInputByLabel(String labelText) {
        LOG.debug("Looking for OXD input with label: {}", labelText);
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
    }

    /**
     * Get the validation error message for an OXD input group with the given label.
     *
     * @param labelText the label text of the input group
     * @return the error message text, or empty string if none
     */
    // PUBLIC_INTERFACE
    public String getInputValidationError(String labelText) {
        List<WebElement> groups = DriverFactory.getDriver().findElements(By.cssSelector(OXD_INPUT_GROUP));
        for (WebElement group : groups) {
            try {
                WebElement label = group.findElement(By.cssSelector(".oxd-label"));
                if (label.getText().trim().equalsIgnoreCase(labelText)) {
                    try {
                        WebElement error = group.findElement(By.cssSelector(".oxd-input-field-error-message"));
                        return error.getText().trim();
                    } catch (NoSuchElementException e) {
                        return "";
                    }
                }
            } catch (NoSuchElementException ignored) {
                // no label in this group
            }
        }
        return "";
    }

    /**
     * Click a button by its visible text.
     *
     * @param buttonText the button text (e.g., "Login", "Save")
     */
    // PUBLIC_INTERFACE
    public void clickButtonByText(String buttonText) {
        LOG.debug("Clicking button with text: {}", buttonText);
        List<WebElement> buttons = waitForAllVisible(By.cssSelector(OXD_BUTTON));
        for (WebElement btn : buttons) {
            if (btn.getText().trim().equalsIgnoreCase(buttonText)) {
                btn.click();
                LOG.info("Clicked button: {}", buttonText);
                return;
            }
        }
        // Fallback: try XPath contains
        WebElement fallback = waitForClickable(
                By.xpath("//button[contains(normalize-space(),'" + buttonText + "')]"));
        fallback.click();
        LOG.info("Clicked button (fallback XPath): {}", buttonText);
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

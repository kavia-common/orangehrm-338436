package com.orangehrm.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.List;

/**
 * PUBLIC_INTERFACE
 * LoginPage represents the OrangeHRM login page using the Page Object Model pattern.
 *
 * <p>This class encapsulates all interactions with the login page elements,
 * providing a clean API for step definitions to use without exposing
 * Selenium implementation details.</p>
 *
 * <p>Locators are based on the OrangeHRM 5.x UI structure which uses
 * Vue 3 components with specific CSS class names and attributes.</p>
 */
public class LoginPage {

    private final WebDriver driver;
    private final WebDriverWait wait;

    // ==================== Locators ====================

    /** Username input field locator */
    private static final By USERNAME_INPUT = By.name("username");

    /** Password input field locator */
    private static final By PASSWORD_INPUT = By.name("password");

    /** Login submit button locator */
    private static final By LOGIN_BUTTON = By.cssSelector("button[type='submit']");

    /** Error alert message locator */
    private static final By ERROR_MESSAGE = By.cssSelector(".oxd-alert-content--error");

    /** Required field validation message locator */
    private static final By REQUIRED_FIELD_MESSAGE = By.cssSelector(".oxd-input-field-error-message");

    /** Dashboard header element locator */
    private static final By DASHBOARD_HEADER = By.cssSelector(".oxd-topbar-header-breadcrumb");

    /** Default explicit wait timeout in seconds */
    private static final int EXPLICIT_WAIT_SECONDS = 10;

    /**
     * PUBLIC_INTERFACE
     * Constructs a LoginPage instance with the given WebDriver.
     *
     * @param driver the Selenium WebDriver instance to use for page interactions
     */
    public LoginPage(WebDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(EXPLICIT_WAIT_SECONDS));
    }

    /**
     * PUBLIC_INTERFACE
     * Enters the specified username into the username input field.
     *
     * @param username the username text to enter
     */
    public void enterUsername(String username) {
        WebElement usernameField = wait.until(
            ExpectedConditions.visibilityOfElementLocated(USERNAME_INPUT)
        );
        usernameField.clear();
        usernameField.sendKeys(username);
    }

    /**
     * PUBLIC_INTERFACE
     * Enters the specified password into the password input field.
     *
     * @param password the password text to enter
     */
    public void enterPassword(String password) {
        WebElement passwordField = wait.until(
            ExpectedConditions.visibilityOfElementLocated(PASSWORD_INPUT)
        );
        passwordField.clear();
        passwordField.sendKeys(password);
    }

    /**
     * PUBLIC_INTERFACE
     * Clicks the login button to submit the credentials.
     */
    public void clickLoginButton() {
        WebElement loginBtn = wait.until(
            ExpectedConditions.elementToBeClickable(LOGIN_BUTTON)
        );
        loginBtn.click();
    }

    /**
     * PUBLIC_INTERFACE
     * Checks whether the dashboard page is displayed after login.
     *
     * <p>Verifies by checking if the current URL contains the dashboard path.</p>
     *
     * @return true if the dashboard page URL is detected, false otherwise
     */
    public boolean isDashboardPageDisplayed() {
        try {
            wait.until(ExpectedConditions.urlContains("/dashboard"));
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * PUBLIC_INTERFACE
     * Checks whether the dashboard header breadcrumb is visible.
     *
     * @return true if the dashboard header element is visible, false otherwise
     */
    public boolean isDashboardHeaderVisible() {
        try {
            wait.until(ExpectedConditions.visibilityOfElementLocated(DASHBOARD_HEADER));
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * PUBLIC_INTERFACE
     * Checks whether an error message matching the expected text is displayed.
     *
     * @param expectedMessage the expected error message text (or partial text)
     * @return true if a matching error message is visible, false otherwise
     */
    public boolean isErrorMessageDisplayed(String expectedMessage) {
        try {
            WebElement errorElement = wait.until(
                ExpectedConditions.visibilityOfElementLocated(ERROR_MESSAGE)
            );
            return errorElement.getText().contains(expectedMessage);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * PUBLIC_INTERFACE
     * Checks whether the current page URL contains the expected path fragment.
     *
     * <p>Uses an explicit wait to allow time for page navigation to complete
     * before checking the URL.</p>
     *
     * @param expectedPath the URL fragment to check for (e.g., "/dashboard")
     * @return true if the current URL contains the expected path, false otherwise
     */
    public boolean isUrlContaining(String expectedPath) {
        try {
            wait.until(ExpectedConditions.urlContains(expectedPath));
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * PUBLIC_INTERFACE
     * Checks whether required field validation messages are displayed.
     *
     * <p>This is typically shown when the form is submitted with empty required fields.</p>
     *
     * @return true if at least one required field message is visible, false otherwise
     */
    public boolean isRequiredFieldMessageDisplayed() {
        try {
            List<WebElement> messages = wait.until(
                ExpectedConditions.visibilityOfAllElementsLocatedBy(REQUIRED_FIELD_MESSAGE)
            );
            return !messages.isEmpty();
        } catch (Exception e) {
            return false;
        }
    }
}

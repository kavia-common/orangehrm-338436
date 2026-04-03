package com.orangehrm.stepdefinitions;

import com.orangehrm.config.TestConfig;
import com.orangehrm.driver.DriverFactory;
import com.orangehrm.pages.LoginPage;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.When;
import io.cucumber.java.en.Then;

import org.openqa.selenium.WebDriver;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PUBLIC_INTERFACE
 * LoginStepDefinitions provides Cucumber step implementations for the login feature.
 *
 * <p>This class maps Gherkin steps from {@code login.feature} to Java methods
 * that interact with the OrangeHRM login page via Selenium WebDriver using
 * the Page Object pattern.</p>
 *
 * <p>All credentials and URLs are sourced from {@link TestConfig}, which resolves
 * values from system properties, environment variables, or default configuration —
 * no hardcoded URLs or credentials are used.</p>
 *
 * <p>Supported step groups:
 * <ul>
 *   <li><strong>Navigation:</strong> Navigating to the login page</li>
 *   <li><strong>Input:</strong> Entering valid/invalid usernames and passwords</li>
 *   <li><strong>Action:</strong> Clicking the login button</li>
 *   <li><strong>Verification:</strong> Asserting dashboard redirect, URL content,
 *       error messages, and required field validation messages</li>
 * </ul>
 */
public class LoginStepDefinitions {

    /** Selenium WebDriver instance for browser interaction */
    private final WebDriver driver;

    /** Page object encapsulating login page elements and actions */
    private final LoginPage loginPage;

    /**
     * Constructor initializes the WebDriver and LoginPage instances.
     * The WebDriver is obtained from the thread-safe {@link DriverFactory}.
     */
    public LoginStepDefinitions() {
        this.driver = DriverFactory.getDriver();
        this.loginPage = new LoginPage(driver);
    }

    // ==================== Navigation Steps ====================

    /**
     * Navigates the browser to the OrangeHRM login page.
     * The base URL is retrieved from {@link TestConfig#getBaseUrl()}, ensuring
     * no hardcoded URL is used.
     */
    @Given("I navigate to the OrangeHRM login page")
    public void iNavigateToTheOrangeHRMLoginPage() {
        String loginUrl = TestConfig.getBaseUrl() + "/web/index.php/auth/login";
        System.out.println("Navigating to login page: " + loginUrl);
        driver.get(loginUrl);
    }

    // ==================== Input Steps (Valid Credentials) ====================

    /**
     * Enters the default valid username from the test configuration.
     * The username is retrieved from {@link TestConfig#getUsername()}.
     */
    @When("I enter a valid username")
    public void iEnterAValidUsername() {
        String username = TestConfig.getUsername();
        System.out.println("Entering valid username: " + TestConfig.mask(username));
        loginPage.enterUsername(username);
    }

    /**
     * Enters the default valid password from the test configuration.
     * The password is retrieved from {@link TestConfig#getPassword()}.
     */
    @When("I enter a valid password")
    public void iEnterAValidPassword() {
        String password = TestConfig.getPassword();
        System.out.println("Entering valid password (masked)");
        loginPage.enterPassword(password);
    }

    // ==================== Input Steps (Invalid Credentials) ====================

    /**
     * Enters a specified invalid username into the username field.
     *
     * @param username the invalid username string to type into the field
     */
    @When("I enter an invalid username {string}")
    public void iEnterAnInvalidUsername(String username) {
        System.out.println("Entering invalid username: " + TestConfig.mask(username));
        loginPage.enterUsername(username);
    }

    /**
     * Enters a specified invalid password into the password field.
     *
     * @param password the invalid password string to type into the field
     */
    @When("I enter an invalid password {string}")
    public void iEnterAnInvalidPassword(String password) {
        System.out.println("Entering invalid password (masked)");
        loginPage.enterPassword(password);
    }

    // ==================== Action Steps ====================

    /**
     * Clicks the login/submit button on the login page to submit the credentials form.
     */
    @When("I click the login button")
    public void iClickTheLoginButton() {
        System.out.println("Clicking the login button");
        loginPage.clickLoginButton();
    }

    // ==================== Verification Steps (Positive Outcomes) ====================

    /**
     * Verifies that the user has been redirected to the dashboard page
     * by checking if the current URL contains "/dashboard".
     */
    @Then("I should be redirected to the dashboard page")
    public void iShouldBeRedirectedToTheDashboardPage() {
        System.out.println("Verifying redirect to dashboard page");
        assertTrue(
            loginPage.isDashboardPageDisplayed(),
            "Expected to be redirected to the dashboard page after successful login, "
                + "but current URL is: " + driver.getCurrentUrl()
        );
    }

    /**
     * Verifies that the dashboard header breadcrumb is visible on the page,
     * indicating the user has successfully logged in and the dashboard loaded.
     */
    @Then("I should see the dashboard header")
    public void iShouldSeeTheDashboardHeader() {
        System.out.println("Verifying dashboard header is visible");
        assertTrue(
            loginPage.isDashboardHeaderVisible(),
            "Expected the dashboard header to be visible after login, "
                + "but it was not found on the page"
        );
    }

    /**
     * Verifies that the current page URL contains the expected path fragment.
     * Useful for checking post-login redirect destinations.
     *
     * @param expectedPath the URL fragment expected in the current page URL
     */
    @Then("the page URL should contain {string}")
    public void thePageUrlShouldContain(String expectedPath) {
        System.out.println("Verifying page URL contains: " + expectedPath);
        assertTrue(
            loginPage.isUrlContaining(expectedPath),
            "Expected page URL to contain '" + expectedPath + "', "
                + "but actual URL is: " + driver.getCurrentUrl()
        );
    }

    // ==================== Verification Steps (Negative Outcomes) ====================

    /**
     * Verifies that the specified error message is displayed on the login page.
     * This is used for failed login attempts where the application shows an
     * error alert (e.g., "Invalid credentials").
     *
     * @param expectedMessage the expected error message text (or partial text)
     */
    @Then("I should see an error message {string}")
    public void iShouldSeeAnErrorMessage(String expectedMessage) {
        System.out.println("Verifying error message is displayed: " + expectedMessage);
        assertTrue(
            loginPage.isErrorMessageDisplayed(expectedMessage),
            "Expected error message '" + expectedMessage + "' to be displayed on the login page"
        );
    }

    /**
     * Verifies that required field validation messages are shown when the login
     * form is submitted with one or more empty required fields (username/password).
     */
    @Then("I should see a required field validation message")
    public void iShouldSeeARequiredFieldValidationMessage() {
        System.out.println("Verifying required field validation message is displayed");
        assertTrue(
            loginPage.isRequiredFieldMessageDisplayed(),
            "Expected required field validation message to be displayed "
                + "when submitting the form with empty fields"
        );
    }
}

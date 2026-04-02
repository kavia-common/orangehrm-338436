package com.orangehrm.utils;

import com.orangehrm.config.ConfigManager;
import com.orangehrm.driver.DriverFactory;
import com.orangehrm.pages.BasePage;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Reusable navigation helper for common OrangeHRM UI flows.
 *
 * <h3>Flow name: NavigationFlow</h3>
 * <p>Consolidates navigation operations (login, logout, page navigation)
 * into a single canonical helper. All step definitions should delegate
 * to this class rather than duplicating navigation logic inline.</p>
 *
 * <h3>Contract</h3>
 * <ul>
 *   <li><b>Inputs:</b> URL paths (relative to base URL), credentials.</li>
 *   <li><b>Outputs:</b> void (navigation side-effect); boolean for checks.</li>
 *   <li><b>Errors:</b> {@link org.openqa.selenium.TimeoutException} if page
 *       elements do not appear within configured timeouts.</li>
 *   <li><b>Side-effects:</b> Browser navigation, form submission.</li>
 * </ul>
 *
 * <h3>Thread safety</h3>
 * <p>Stateless utility class. All driver access is via ThreadLocal.
 * Each scenario should create its own instance or use static methods.</p>
 */
// PUBLIC_INTERFACE
public final class NavigationHelper {

    private static final Logger LOG = LoggerFactory.getLogger(NavigationHelper.class);

    /** Shared BasePage instance for element interactions. */
    private static final BasePage PAGE = new BasePage();

    /** Login page relative path. */
    private static final String LOGIN_PATH = "/auth/login";

    /** CSS selector for the login form. */
    private static final By LOGIN_FORM = By.cssSelector(BasePage.OXD_FORM);

    /** CSS selector for the user dropdown (indicates logged-in state). */
    private static final By USER_DROPDOWN = By.cssSelector(BasePage.OXD_USERDROPDOWN);

    private NavigationHelper() {
        // Utility class — no instantiation
    }

    /**
     * Perform a complete login flow: navigate to login page, enter credentials,
     * click Login, and wait for dashboard.
     *
     * <p>This is the single canonical login implementation. All step definitions
     * that need to log in should call this method.</p>
     *
     * @param username the username to enter
     * @param password the password to enter
     */
    // PUBLIC_INTERFACE
    public static void login(String username, String password) {
        LOG.info("NavigationHelper.login — user: '{}', thread: {}",
                username, Thread.currentThread().getName());

        navigateToLoginPage();

        WebElement usernameInput = PAGE.findOxdInputByLabel("Username");
        usernameInput.clear();
        usernameInput.sendKeys(username);

        WebElement passwordInput = PAGE.findOxdInputByLabel("Password");
        passwordInput.clear();
        passwordInput.sendKeys(password);

        PAGE.clickButtonByText("Login");
        LOG.info("Login form submitted for user: '{}'", username);
    }

    /**
     * Login as the configured admin user and wait for the dashboard.
     */
    // PUBLIC_INTERFACE
    public static void loginAsAdmin() {
        LOG.info("NavigationHelper.loginAsAdmin — starting");
        login(ConfigManager.getAdminUsername(), ConfigManager.getAdminPassword());
        WaitUtils.forUrlContains("dashboard");
        LOG.info("NavigationHelper.loginAsAdmin — dashboard reached");
    }

    /**
     * Login as the configured ESS user and wait for the dashboard.
     */
    // PUBLIC_INTERFACE
    public static void loginAsEss() {
        LOG.info("NavigationHelper.loginAsEss — starting");
        login(ConfigManager.getEssUsername(), ConfigManager.getEssPassword());
        WaitUtils.forUrlContains("dashboard");
        LOG.info("NavigationHelper.loginAsEss — dashboard reached");
    }

    /**
     * Navigate to the OrangeHRM login page and wait for the form to appear.
     */
    // PUBLIC_INTERFACE
    public static void navigateToLoginPage() {
        LOG.info("NavigationHelper.navigateToLoginPage — navigating");
        PAGE.navigateTo(LOGIN_PATH);
        PAGE.waitForVisible(LOGIN_FORM);
        LOG.info("NavigationHelper.navigateToLoginPage — login form visible");
    }

    /**
     * Navigate to a page relative to the OrangeHRM web root and wait for
     * the page content area to load.
     *
     * @param path relative path (e.g., "/dashboard/index")
     */
    // PUBLIC_INTERFACE
    public static void navigateToPage(String path) {
        LOG.info("NavigationHelper.navigateToPage — path: '{}'", path);
        PAGE.navigateTo(path);
        WaitUtils.forPageReady();
        LOG.info("NavigationHelper.navigateToPage — page ready");
    }

    /**
     * Logout the current user by clicking the user dropdown and selecting Logout.
     * Safe to call even if not currently logged in.
     */
    // PUBLIC_INTERFACE
    public static void logout() {
        LOG.info("NavigationHelper.logout — starting");
        try {
            PAGE.click(USER_DROPDOWN);
            WebElement logoutLink = WaitUtils.forClickable(
                    By.xpath("//a[contains(normalize-space(),'Logout')]"), 5);
            logoutLink.click();
            WaitUtils.forUrlContains("login");
            LOG.info("NavigationHelper.logout — complete, redirected to login");
        } catch (Exception e) {
            LOG.warn("NavigationHelper.logout — could not logout (may not be logged in): {}",
                    e.getMessage());
        }
    }

    /**
     * Clear all cookies to simulate a session timeout or ensure no user is logged in.
     */
    // PUBLIC_INTERFACE
    public static void clearSession() {
        LOG.info("NavigationHelper.clearSession — clearing cookies");
        WebDriver driver = DriverFactory.getDriver();
        driver.manage().deleteAllCookies();
        LOG.info("NavigationHelper.clearSession — cookies cleared");
    }

    /**
     * Check if a user is currently logged in by looking for the user dropdown.
     *
     * @return true if the user dropdown is visible
     */
    // PUBLIC_INTERFACE
    public static boolean isLoggedIn() {
        return PAGE.isElementVisible(USER_DROPDOWN, 3);
    }
}

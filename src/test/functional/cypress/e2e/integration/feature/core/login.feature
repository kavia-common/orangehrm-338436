# OrangeHRM Login Feature
# Based on the existing login flow:
#   - LoginController (GET /auth/login) renders the Vue Login.vue component
#   - ValidateController (POST /auth/validate) handles credential authentication
#   - AuthProviderChain authenticates UserCredential, redirects to dashboard/index on success
#   - Login form enforces "Required" validation on Username and Password fields
#   - AuthenticationException defines error types: invalid_credentials, invalid_csrf_token,
#     user_disabled, employee_terminated, employee_not_assigned, session_expired,
#     no_user_found, unexpected_error, password_not_strong
#   - Login.vue has a 20-minute client-side auto-reload timeout (setTimeout → reloadPage)
#   - AuthenticationSubscriber checks session validity, user status, and employee assignment
#
# Conventions aligned with: login.cy.js, commands.js (cy.login, cy.getOXDInput),
# and the user.json fixture (Admin / Jacqueline@OHRM123).

Feature: Core - Login Page
  As an OrangeHRM user
  I want to log in to the application using my credentials
  So that I can access the HRM dashboard and manage HR operations

  Background:
    Given the database has been reset to its initial state
    And the login page is loaded at "/auth/login"

  # ------------------------------------------------------------------
  # Successful login
  # ------------------------------------------------------------------

  Scenario: Admin user logs in with valid credentials
    When the user enters "Admin" in the "Username" field
    And the user enters "Jacqueline@OHRM123" in the "Password" field
    And the user clicks the "Login" button
    Then the authentication request is sent to "/auth/validate"
    And the response redirects to a URL matching "/dashboard/index"
    And the user should see the dashboard page

  # ------------------------------------------------------------------
  # Form validation – empty fields
  # ------------------------------------------------------------------

  Scenario: Login form shows validation errors when submitted empty
    When the user clicks the "Login" button without filling in any fields
    Then the "Username" field should display the validation error "Required"
    And the "Password" field should display the validation error "Required"
    And the user should remain on the login page

  Scenario: Login form shows validation error for empty username
    When the user enters "Jacqueline@OHRM123" in the "Password" field
    And the user clicks the "Login" button
    Then the "Username" field should display the validation error "Required"
    And the user should remain on the login page

  Scenario: Login form shows validation error for empty password
    When the user enters "Admin" in the "Username" field
    And the user clicks the "Login" button
    Then the "Password" field should display the validation error "Required"
    And the user should remain on the login page

  # ------------------------------------------------------------------
  # Invalid credentials – failed login attempts
  # ------------------------------------------------------------------

  Scenario: Login fails with incorrect password
    When the user enters "Admin" in the "Username" field
    And the user enters "WrongPassword123" in the "Password" field
    And the user clicks the "Login" button
    Then the authentication request is sent to "/auth/validate"
    And an error alert should be displayed with the message "Invalid credentials"
    And the user should remain on the login page

  Scenario: Login fails with a non-existent username
    When the user enters "NonExistentUser" in the "Username" field
    And the user enters "SomePassword123" in the "Password" field
    And the user clicks the "Login" button
    Then the authentication request is sent to "/auth/validate"
    And an error alert should be displayed with the message "Invalid credentials"
    And the user should remain on the login page

  Scenario: Login fails with correct username and empty-looking password after trim
    When the user enters "Admin" in the "Username" field
    And the user enters "   " in the "Password" field
    And the user clicks the "Login" button
    Then the authentication request is sent to "/auth/validate"
    And an error alert should be displayed with the message "Invalid credentials"
    And the user should remain on the login page

  Scenario: Login fails with swapped username and password values
    When the user enters "Jacqueline@OHRM123" in the "Username" field
    And the user enters "Admin" in the "Password" field
    And the user clicks the "Login" button
    Then the authentication request is sent to "/auth/validate"
    And an error alert should be displayed with the message "Invalid credentials"
    And the user should remain on the login page

  Scenario: Login fails with case-altered password
    When the user enters "Admin" in the "Username" field
    And the user enters "jacqueline@ohrm123" in the "Password" field
    And the user clicks the "Login" button
    Then the authentication request is sent to "/auth/validate"
    And an error alert should be displayed with the message "Invalid credentials"
    And the user should remain on the login page

  # ------------------------------------------------------------------
  # Invalid input – special characters and boundary values
  # ------------------------------------------------------------------

  Scenario: Login form handles special characters in username gracefully
    When the user enters "<script>alert('xss')</script>" in the "Username" field
    And the user enters "SomePassword123" in the "Password" field
    And the user clicks the "Login" button
    Then the authentication request is sent to "/auth/validate"
    And an error alert should be displayed with the message "Invalid credentials"
    And the user should remain on the login page

  Scenario: Login form handles SQL injection attempt in username gracefully
    When the user enters "' OR 1=1 --" in the "Username" field
    And the user enters "anything" in the "Password" field
    And the user clicks the "Login" button
    Then the authentication request is sent to "/auth/validate"
    And an error alert should be displayed with the message "Invalid credentials"
    And the user should remain on the login page

  Scenario: Login form handles special characters in password gracefully
    When the user enters "Admin" in the "Username" field
    And the user enters "<script>alert('xss')</script>" in the "Password" field
    And the user clicks the "Login" button
    Then the authentication request is sent to "/auth/validate"
    And an error alert should be displayed with the message "Invalid credentials"
    And the user should remain on the login page

  Scenario: Login form handles excessively long username input
    When the user enters a string of 500 characters in the "Username" field
    And the user enters "SomePassword123" in the "Password" field
    And the user clicks the "Login" button
    Then the authentication request is sent to "/auth/validate"
    And an error alert should be displayed with the message "Invalid credentials"
    And the user should remain on the login page

  Scenario: Login form handles excessively long password input
    When the user enters "Admin" in the "Username" field
    And the user enters a string of 500 characters in the "Password" field
    And the user clicks the "Login" button
    Then the authentication request is sent to "/auth/validate"
    And an error alert should be displayed with the message "Invalid credentials"
    And the user should remain on the login page

  Scenario: Login form handles unicode and emoji characters in credentials
    When the user enters "Ädmin✓" in the "Username" field
    And the user enters "Pässwörd🔑" in the "Password" field
    And the user clicks the "Login" button
    Then the authentication request is sent to "/auth/validate"
    And an error alert should be displayed with the message "Invalid credentials"
    And the user should remain on the login page

  # ------------------------------------------------------------------
  # CSRF token validation
  # ------------------------------------------------------------------

  Scenario: Login fails when CSRF token is missing or invalid
    Given the login form CSRF token has been tampered with
    When the user enters "Admin" in the "Username" field
    And the user enters "Jacqueline@OHRM123" in the "Password" field
    And the user clicks the "Login" button
    Then the authentication request is sent to "/auth/validate"
    And an error alert should be displayed with the message "CSRF token validation failed"
    And the user should remain on the login page

  # ------------------------------------------------------------------
  # Disabled user account
  # ------------------------------------------------------------------

  Scenario: Login fails for a disabled user account
    Given a user account "DisabledUser" exists but is disabled
    When the user enters "DisabledUser" in the "Username" field
    And the user enters the corresponding password in the "Password" field
    And the user clicks the "Login" button
    Then the authentication request is sent to "/auth/validate"
    And an error alert should be displayed with the message "Account disabled"
    And the user should remain on the login page

  # ------------------------------------------------------------------
  # Employee not assigned
  # ------------------------------------------------------------------

  Scenario: Login fails when the employee record is not assigned to the user
    Given a user account "UnassignedUser" exists but has no assigned employee
    When the user enters "UnassignedUser" in the "Username" field
    And the user enters the corresponding password in the "Password" field
    And the user clicks the "Login" button
    Then the authentication request is sent to "/auth/validate"
    And an error alert should be displayed with the message "Employee not assigned"
    And the user should remain on the login page

  # ------------------------------------------------------------------
  # Terminated employee
  # ------------------------------------------------------------------

  Scenario: Login fails when the associated employee has been terminated
    Given a user account "TerminatedEmpUser" exists with a terminated employee
    When the user enters "TerminatedEmpUser" in the "Username" field
    And the user enters the corresponding password in the "Password" field
    And the user clicks the "Login" button
    Then the authentication request is sent to "/auth/validate"
    And an error alert should be displayed with the message "Employee is terminated"
    And the user should remain on the login page

  # ------------------------------------------------------------------
  # Timeout and session handling
  # ------------------------------------------------------------------

  Scenario: Login page auto-reloads after the client-side idle timeout
    Given the login page has been open for longer than the 20-minute idle threshold
    When the idle timeout elapses
    Then the login page should automatically reload
    And the user should see a fresh login form with empty fields

  Scenario: User is redirected to login page when session expires during navigation
    Given the user is logged in as "Admin"
    And the user session has expired on the server
    When the user attempts to navigate to a protected page "/pim/viewEmployeeList"
    Then the user should be redirected to "/auth/login"

  Scenario: Session timeout preserves the original requested URL for redirect after re-login
    Given the user is logged in as "Admin"
    And the user session has expired on the server
    When the user attempts to navigate to a protected page "/pim/viewEmployeeList"
    Then the user should be redirected to "/auth/login"
    When the user enters "Admin" in the "Username" field
    And the user enters "Jacqueline@OHRM123" in the "Password" field
    And the user clicks the "Login" button
    Then the user should be redirected to the originally requested page "/pim/viewEmployeeList"

  Scenario: Expired session returns HTTP 401 for REST API requests
    Given the user is logged in as "Admin"
    And the user session has expired on the server
    When an API request is sent to a protected REST endpoint
    Then the API response status code should be 401
    And the API response body should contain the message "Session expired"

  # ------------------------------------------------------------------
  # Authenticated user redirect
  # ------------------------------------------------------------------

  Scenario: Already authenticated user is redirected away from login page
    Given the user is already logged in as "Admin"
    When the user navigates to "/auth/login"
    Then the user should be redirected to the dashboard page

  # ------------------------------------------------------------------
  # Forgot Password link
  # ------------------------------------------------------------------

  Scenario: User navigates to the forgot password page
    When the user clicks the "Forgot your password?" link
    Then the user should be navigated to "/auth/requestPasswordResetCode"

  # ------------------------------------------------------------------
  # Weak password enforcement redirect
  # ------------------------------------------------------------------

  Scenario: User with a weak password is redirected to change password page
    Given password strength enforcement is enabled in system configuration
    And a user account "WeakPwdUser" exists with a password that does not meet strength requirements
    When the user enters "WeakPwdUser" in the "Username" field
    And the user enters the weak password in the "Password" field
    And the user clicks the "Login" button
    Then the authentication request is sent to "/auth/validate"
    And the user should be redirected to the change weak password page matching "/auth/changeWeakPassword/resetCode/"

  # ------------------------------------------------------------------
  # Unexpected server error during authentication
  # ------------------------------------------------------------------

  Scenario: Login displays a generic error when an unexpected server error occurs
    Given the authentication service encounters an unexpected error
    When the user enters "Admin" in the "Username" field
    And the user enters "Jacqueline@OHRM123" in the "Password" field
    And the user clicks the "Login" button
    Then the authentication request is sent to "/auth/validate"
    And an error alert should be displayed with the message "Unexpected error occurred"
    And the user should remain on the login page

  # ------------------------------------------------------------------
  # Repeated failed login attempts
  # ------------------------------------------------------------------

  Scenario: Multiple consecutive failed login attempts still show the error message
    When the user enters "Admin" in the "Username" field
    And the user enters "WrongPassword1" in the "Password" field
    And the user clicks the "Login" button
    Then an error alert should be displayed with the message "Invalid credentials"
    When the login page is reloaded
    And the user enters "Admin" in the "Username" field
    And the user enters "WrongPassword2" in the "Password" field
    And the user clicks the "Login" button
    Then an error alert should be displayed with the message "Invalid credentials"
    When the login page is reloaded
    And the user enters "Admin" in the "Username" field
    And the user enters "WrongPassword3" in the "Password" field
    And the user clicks the "Login" button
    Then an error alert should be displayed with the message "Invalid credentials"
    And the user should remain on the login page

  # ------------------------------------------------------------------
  # Double-submit prevention
  # ------------------------------------------------------------------

  Scenario: Clicking the Login button multiple times does not submit the form more than once
    When the user enters "Admin" in the "Username" field
    And the user enters "Jacqueline@OHRM123" in the "Password" field
    And the user clicks the "Login" button rapidly multiple times
    Then only a single authentication request should be sent to "/auth/validate"

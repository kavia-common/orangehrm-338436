@regression @top-priority
Feature: OrangeHRM Top-Priority Regression Pack
  As a QA tester
  I want to run a comprehensive regression pack
  So that I can verify no regressions in core functionality

  # ─── Login Regression Tests ───

  @login
  Scenario: Successful admin login with configured credentials
    Given the OrangeHRM application is accessible
    And the user is on the login page "/auth/login"
    When the user enters valid credentials and submits the form
    Then the user should be redirected to the dashboard "/dashboard/index"
    And the POST to "/auth/validate" should return a redirect response
    And the user should be redirected to the home page via HomePageService

  @login
  Scenario: Login form contains CSRF protection
    Given the user is on the login page "/auth/login"
    Then the login form should contain a hidden CSRF token field

  @login
  Scenario: CSRF token is submitted with login request
    Given the user is on the login page "/auth/login"
    When the user enters valid credentials and submits the form
    Then the CSRF token should be included in the POST request to "/auth/validate"

  @login
  Scenario: Login page displays social media section
    Given the user is on the login page "/auth/login"
    Then the social media section should be displayed as configured

  @login
  Scenario: XSS prevention in login fields
    Given the user is on the login page "/auth/login"
    When the user enters username "<script>alert('XSS')</script>" and password "test"
    And the user clicks the "Login" button
    Then no script execution should occur
    And the user should remain on the login page

  @login
  Scenario: Disabled user cannot log in
    Given a user account exists that has been disabled
    When the disabled user attempts to log in with valid credentials
    Then the login page should display an error message
    And the user should remain on the login page

  @login
  Scenario: Terminated employee cannot log in
    Given an employee has been terminated but the user account still exists
    When the terminated employee attempts to log in
    Then the login page should display an error message
    And the user should remain on the login page

  # ─── Session Management Regression ───

  @login
  Scenario: Session timeout redirects to login
    Given the admin user is logged in
    When the session times out
    And the user is redirected to the login page
    Then the user should remain on the login page

  @login
  Scenario: Re-login after session timeout
    Given the admin user is logged in
    When the session times out
    And the user is redirected to the login page
    And the user logs in again with valid credentials
    Then the user should be redirected to the dashboard "/dashboard/index"

  @login
  Scenario: Unauthenticated access redirects to login
    Given no user is logged in
    When the user attempts to navigate directly to "/dashboard/index"
    Then the user should remain on the login page

  # ─── Dashboard Regression Tests ───

  @ui @login
  Scenario: Dashboard page loads all core widgets
    Given the admin user is logged in
    When the user navigates to the dashboard "/dashboard/index"
    Then the dashboard page should load successfully
    And the "Quick Launch" widget should be visible
    And the Quick Launch widget should display shortcut icons

  @ui @login
  Scenario: Dashboard charts render
    Given the admin user is logged in
    When the user navigates to the dashboard "/dashboard/index"
    Then the dashboard page should load successfully
    And the "Employees" chart should be visible

  @ui @login
  Scenario: Unauthenticated dashboard access redirects to login
    Given no user is logged in
    When the user navigates to the dashboard "/dashboard/index"
    Then the user should be redirected to the login page "/auth/login"

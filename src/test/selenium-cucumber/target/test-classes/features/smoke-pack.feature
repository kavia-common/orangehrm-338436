@smoke @critical
Feature: OrangeHRM Smoke Pack
  As a QA tester
  I want to verify core OrangeHRM functionality is working
  So that I can confirm the application is in a deployable state

  # ─── Login Smoke Tests ───

  @login
  Scenario: Admin user can log in successfully
    Given the user is on the login page "/auth/login"
    When the user enters username "Admin" and password "admin123"
    And the user clicks the "Login" button
    Then the user should be redirected to the dashboard "/dashboard/index"

  @login
  Scenario: Login page displays branding elements
    Given the user is on the login page "/auth/login"
    Then the login logo image should be visible

  @login
  Scenario: Empty credentials show validation errors
    Given the user is on the login page "/auth/login"
    When the user clicks the "Login" button without entering credentials
    Then the "Username" field should display "Required" validation error
    And the "Password" field should display "Required" validation error

  @login
  Scenario: Invalid credentials show error message
    Given the user is on the login page "/auth/login"
    When the user enters username "InvalidUser" and password "WrongPass123"
    And the user clicks the "Login" button
    Then the login page should display an "Invalid credentials" error message
    And the user should remain on the login page

  # ─── Dashboard Smoke Tests ───

  @ui @login
  Scenario: Dashboard loads after admin login
    Given the admin user is logged in
    Then the dashboard page should load successfully

  @ui @login
  Scenario: Dashboard displays Quick Launch widget
    Given the admin user is logged in
    When the user navigates to the dashboard "/dashboard/index"
    Then the dashboard page should load successfully
    And the "Quick Launch" widget should be visible

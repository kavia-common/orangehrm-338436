@smoke @critical
Feature: OrangeHRM Smoke Pack
  As a QA engineer
  I want to run a concise set of high-signal tests
  So that I can quickly verify the core OrangeHRM functionality is working

  Background:
    Given the OrangeHRM application is accessible
    And the database is in a known clean state

  # ─────────────────────────────────────────────
  # LOGIN SCENARIOS (Priority: P0)
  # ─────────────────────────────────────────────

  @smoke @login @P0 @order-1
  Scenario: SMOKE-01 – Admin can log in with valid credentials
    Given the user is on the login page "/auth/login"
    When the user enters username "Admin" and password "Jacqueline@OHRM123"
    And the user clicks the "Login" button
    Then the user should be redirected to the dashboard "/dashboard/index"

  @smoke @login @P0 @order-2
  Scenario: SMOKE-02 – Login form shows validation for empty fields
    Given the user is on the login page "/auth/login"
    When the user clicks the "Login" button without entering credentials
    Then the "Username" field should display "Required" validation error
    And the "Password" field should display "Required" validation error

  @smoke @login @P0 @order-3
  Scenario: SMOKE-03 – Login fails with invalid credentials
    Given the user is on the login page "/auth/login"
    When the user enters username "InvalidUser" and password "WrongPass"
    And the user clicks the "Login" button
    Then the login page should display an "Invalid credentials" error message
    And the user should remain on the login page

  # ─────────────────────────────────────────────
  # DASHBOARD SCENARIOS (Priority: P0)
  # ─────────────────────────────────────────────

  @smoke @dashboard @P0 @order-4
  Scenario: SMOKE-04 – Dashboard loads with all expected widgets
    Given the admin user is logged in
    When the user navigates to the dashboard "/dashboard/index"
    Then the dashboard page should load successfully
    And the "Time at Work" widget should be visible
    And the "My Actions" widget should be visible
    And the "Quick Launch" widget should be visible
    And the "Employees on Leave Today" widget should be visible

  @smoke @dashboard @P0 @order-5
  Scenario: SMOKE-05 – Dashboard Quick Launch shortcuts are functional
    Given the admin user is logged in
    When the user navigates to the dashboard "/dashboard/index"
    Then the Quick Launch widget should display shortcut icons
    And each shortcut should have a valid navigation target

  @smoke @dashboard @P1 @order-6
  Scenario: SMOKE-06 – Dashboard employee distribution charts render
    Given the admin user is logged in
    When the user navigates to the dashboard "/dashboard/index"
    Then the "Employee Distribution by Sub Unit" chart should be visible
    And the "Employee Distribution by Location" chart should be visible

  # ─────────────────────────────────────────────
  # EMPLOYEE MANAGEMENT (PIM) SCENARIOS (Priority: P0)
  # ─────────────────────────────────────────────

  @smoke @pim @employee @P0 @order-7
  Scenario: SMOKE-07 – Employee list page loads successfully
    Given the admin user is logged in
    When the user navigates to the employee list "/pim/viewEmployeeList"
    Then the employee list page should load successfully
    And the page title should contain "Employee Information"

  @smoke @pim @employee @P0 @order-8
  Scenario: SMOKE-08 – Admin can add a new employee
    Given the admin user is logged in
    When the user navigates to the add employee page "/pim/addEmployee"
    And the user enters the first name "Smoke" and last name "TestEmployee"
    And the user enters the employee ID "SMOKE001"
    And the user clicks the "Save" button
    Then a success notification should be displayed
    And the employee personal details page should load for the new employee

  @smoke @pim @employee @P0 @order-9
  Scenario: SMOKE-09 – Admin can search for an employee
    Given the admin user is logged in
    And an employee named "Smoke TestEmployee" exists in the system
    When the user navigates to the employee list "/pim/viewEmployeeList"
    And the user searches for employee name "Smoke"
    And the user clicks the "Search" button
    Then the search results should display the employee "Smoke TestEmployee"

  @smoke @pim @employee @P0 @order-10
  Scenario: SMOKE-10 – Admin can view employee personal details
    Given the admin user is logged in
    And an employee exists in the system
    When the user navigates to the employee personal details page
    Then the personal details form should be displayed
    And the first name and last name fields should be populated

  @smoke @pim @employee @P1 @order-11
  Scenario: SMOKE-11 – Admin can delete an employee
    Given the admin user is logged in
    And an employee named "Smoke TestEmployee" exists in the system
    When the user navigates to the employee list "/pim/viewEmployeeList"
    And the user selects the employee "Smoke TestEmployee"
    And the user clicks the "Delete Selected" button
    And the user confirms the deletion
    Then a success notification "Successfully Deleted" should be displayed
    And the employee "Smoke TestEmployee" should no longer appear in the list

  # ─────────────────────────────────────────────
  # LOGOUT SCENARIOS (Priority: P0)
  # ─────────────────────────────────────────────

  @smoke @logout @P0 @order-12
  Scenario: SMOKE-12 – User can log out and session is invalidated
    Given the admin user is logged in
    When the user clicks the user dropdown in the top-right corner
    And the user clicks the "Logout" option
    Then the user should be redirected to the login page "/auth/login"
    And attempting to access the dashboard "/dashboard/index" should redirect to login

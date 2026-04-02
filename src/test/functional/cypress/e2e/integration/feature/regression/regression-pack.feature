@regression @top-priority
Feature: OrangeHRM Top-Priority Regression Pack
  As a QA engineer
  I want to run a comprehensive regression test suite derived from all core features
  So that I can verify no existing functionality is broken after code changes

  Background:
    Given the OrangeHRM application is accessible
    And the database is in a known clean state

  # ═══════════════════════════════════════════════════
  # LOGIN REGRESSION SCENARIOS
  # ═══════════════════════════════════════════════════

  @regression @login @P0 @order-1
  Scenario: REG-01 – Admin can log in with valid credentials
    Given the user is on the login page "/auth/login"
    When the user enters username "Admin" and password "Jacqueline@OHRM123"
    And the user clicks the "Login" button
    Then the POST to "/auth/validate" should return a redirect response
    And the user should be redirected to the dashboard "/dashboard/index"

  @regression @login @P0 @order-2
  Scenario: REG-02 – Login form shows validation for empty fields
    Given the user is on the login page "/auth/login"
    When the user clicks the "Login" button without entering credentials
    Then the "Username" field should display "Required" validation error
    And the "Password" field should display "Required" validation error

  @regression @login @P0 @order-3
  Scenario: REG-03 – Login fails with invalid credentials
    Given the user is on the login page "/auth/login"
    When the user enters username "InvalidUser" and password "WrongPass"
    And the user clicks the "Login" button
    Then the login page should display an "Invalid credentials" error message
    And the user should remain on the login page

  @regression @login @P0 @order-4
  Scenario: REG-04 – Login page renders branding elements correctly
    Given the user is on the login page "/auth/login"
    Then the login logo image should be visible
    And the login banner image should be visible
    And the social media section should be displayed as configured

  @regression @login @P0 @order-5
  Scenario: REG-05 – Login includes CSRF token protection
    Given the user is on the login page "/auth/login"
    Then the login form should contain a hidden CSRF token field
    When the user enters valid credentials and submits the form
    Then the CSRF token should be included in the POST request to "/auth/validate"

  @regression @login @P1 @order-6
  Scenario: REG-06 – Already authenticated user is redirected from login page
    Given the admin user is logged in
    When the user navigates to the login page "/auth/login"
    Then the user should be redirected to the home page via HomePageService

  @regression @login @P1 @order-7
  Scenario Outline: REG-07 – Login rejects various invalid credential combinations
    Given the user is on the login page "/auth/login"
    When the user enters username "<username>" and password "<password>"
    And the user clicks the "Login" button
    Then the login page should display an error message
    And the user should remain on the login page

    Examples:
      | username  | password             |
      | Admin     | WrongPassword        |
      | Unknown   | Jacqueline@OHRM123  |
      | admin     | Jacqueline@OHRM123  |
      |           | Jacqueline@OHRM123  |
      | Admin     |                      |

  @regression @login @P1 @order-8
  Scenario: REG-08 – Login with disabled user account shows appropriate error
    Given a user account exists that has been disabled
    When the disabled user attempts to log in with valid credentials
    Then the login page should display an "Account disabled" error message
    And the user should remain on the login page

  @regression @login @P1 @order-9
  Scenario: REG-09 – Login with terminated employee shows appropriate error
    Given an employee has been terminated but the user account still exists
    When the terminated employee attempts to log in
    Then the login page should display an "Employee is terminated" error message
    And the user should remain on the login page

  @regression @login @P2 @order-10
  Scenario: REG-10 – Login form handles special characters in inputs
    Given the user is on the login page "/auth/login"
    When the user enters username "<script>alert(1)</script>" and password "test"
    And the user clicks the "Login" button
    Then no script execution should occur
    And the login page should display an error message

  @regression @login @P2 @order-11
  Scenario: REG-11 – Session timeout redirect is restored after re-login
    Given the admin user is logged in
    And the user is on the page "/pim/viewEmployeeList"
    When the session times out
    And the user is redirected to the login page
    And the user logs in again with valid credentials
    Then the user should be redirected back to "/pim/viewEmployeeList"

  # ═══════════════════════════════════════════════════
  # DASHBOARD REGRESSION SCENARIOS
  # ═══════════════════════════════════════════════════

  @regression @dashboard @P0 @order-12
  Scenario: REG-12 – Dashboard loads with all expected widgets for admin
    Given the admin user is logged in
    When the user navigates to the dashboard "/dashboard/index"
    Then the dashboard page should load successfully
    And the "Time at Work" widget should be visible
    And the "My Actions" widget should be visible
    And the "Quick Launch" widget should be visible
    And the "Employees on Leave Today" widget should be visible
    And the "Employee Distribution by Sub Unit" chart should be visible
    And the "Employee Distribution by Location" chart should be visible

  @regression @dashboard @P0 @order-13
  Scenario: REG-13 – Dashboard Quick Launch shortcuts are functional
    Given the admin user is logged in
    When the user navigates to the dashboard "/dashboard/index"
    Then the Quick Launch widget should display shortcut icons
    And clicking each shortcut should navigate to the correct page

  @regression @dashboard @P0 @order-14
  Scenario: REG-14 – Dashboard My Actions widget displays pending action counts
    Given the admin user is logged in
    When the user navigates to the dashboard "/dashboard/index"
    Then the "My Actions" widget should display pending action counts
    And the action summary API "/api/v2/dashboard/employees/action-summary" should respond with 200

  @regression @dashboard @P1 @order-15
  Scenario: REG-15 – Dashboard Time at Work widget displays current day data
    Given the admin user is logged in
    When the user navigates to the dashboard "/dashboard/index"
    Then the "Time at Work" widget should display the current date
    And the time at work API "/api/v2/dashboard/employees/time-at-work" should respond with 200

  @regression @dashboard @P1 @order-16
  Scenario: REG-16 – Dashboard Employees on Leave Today widget renders
    Given the admin user is logged in
    When the user navigates to the dashboard "/dashboard/index"
    Then the "Employees on Leave Today" widget should be visible
    And the leave API "/api/v2/dashboard/employees/leaves" should respond with 200

  @regression @dashboard @P1 @order-17
  Scenario: REG-17 – Employee Distribution by Sub Unit chart renders with data
    Given the admin user is logged in
    When the user navigates to the dashboard "/dashboard/index"
    Then the "Employee Distribution by Sub Unit" chart should render
    And the subunit API "/api/v2/dashboard/employees/subunit" should respond with valid data

  @regression @dashboard @P1 @order-18
  Scenario: REG-18 – Employee Distribution by Location chart renders with data
    Given the admin user is logged in
    When the user navigates to the dashboard "/dashboard/index"
    Then the "Employee Distribution by Location" chart should render
    And the location API "/api/v2/dashboard/employees/locations" should respond with valid data

  @regression @dashboard @P1 @order-19
  Scenario: REG-19 – Dashboard permission-based widget visibility for ESS user
    Given an ESS user is logged in
    When the user navigates to the dashboard "/dashboard/index"
    Then the dashboard should only display widgets permitted for the ESS role
    And admin-only widgets should not be visible

  @regression @dashboard @P2 @order-20
  Scenario: REG-20 – Dashboard is not accessible without authentication
    Given no user is logged in
    When the user attempts to navigate directly to "/dashboard/index"
    Then the user should be redirected to the login page "/auth/login"

  # ═══════════════════════════════════════════════════
  # EMPLOYEE MANAGEMENT (PIM) REGRESSION SCENARIOS
  # ═══════════════════════════════════════════════════

  @regression @pim @employee @P0 @order-21
  Scenario: REG-21 – Employee list page loads successfully
    Given the admin user is logged in
    When the user navigates to the employee list "/pim/viewEmployeeList"
    Then the employee list page should load successfully
    And the page title should contain "Employee Information"
    And the employee records table should be displayed

  @regression @pim @employee @P0 @order-22
  Scenario: REG-22 – Admin can add a new employee with required fields only
    Given the admin user is logged in
    When the user navigates to the add employee page "/pim/addEmployee"
    And the user enters the first name "Regression" and last name "TestEmployee"
    And the user clicks the "Save" button
    Then a success notification should be displayed
    And the employee personal details page should load for the new employee

  @regression @pim @employee @P0 @order-23
  Scenario: REG-23 – Admin can add a new employee with all fields
    Given the admin user is logged in
    When the user navigates to the add employee page "/pim/addEmployee"
    And the user enters the first name "Regression" and last name "FullFields"
    And the user enters the middle name "M"
    And the user enters the employee ID "REG001"
    And the user clicks the "Save" button
    Then a success notification should be displayed
    And the employee personal details page should load for the new employee

  @regression @pim @employee @P0 @order-24
  Scenario: REG-24 – Admin can search for an employee by name
    Given the admin user is logged in
    And an employee named "Regression TestEmployee" exists in the system
    When the user navigates to the employee list "/pim/viewEmployeeList"
    And the user searches for employee name "Regression"
    And the user clicks the "Search" button
    Then the search results should display the employee "Regression TestEmployee"

  @regression @pim @employee @P0 @order-25
  Scenario: REG-25 – Admin can search for an employee by employee ID
    Given the admin user is logged in
    And an employee with ID "REG001" exists in the system
    When the user navigates to the employee list "/pim/viewEmployeeList"
    And the user searches for employee ID "REG001"
    And the user clicks the "Search" button
    Then the search results should display the matching employee

  @regression @pim @employee @P0 @order-26
  Scenario: REG-26 – Admin can view employee personal details
    Given the admin user is logged in
    And an employee exists in the system
    When the user navigates to the employee personal details page
    Then the personal details form should be displayed
    And the first name and last name fields should be populated
    And the PIM left menu tabs should be visible

  @regression @pim @employee @P0 @order-27
  Scenario: REG-27 – Admin can edit employee personal details
    Given the admin user is logged in
    And an employee named "Regression TestEmployee" exists in the system
    When the user navigates to the employee personal details page for that employee
    And the user changes the last name to "UpdatedEmployee"
    And the user clicks the "Save" button on the personal details form
    Then a success notification "Successfully Updated" should be displayed
    And the last name field should now show "UpdatedEmployee"

  @regression @pim @employee @P0 @order-28
  Scenario: REG-28 – Admin can delete an employee
    Given the admin user is logged in
    And an employee named "Regression FullFields" exists in the system
    When the user navigates to the employee list "/pim/viewEmployeeList"
    And the user selects the employee "Regression FullFields"
    And the user clicks the "Delete Selected" button
    And the user confirms the deletion
    Then a success notification "Successfully Deleted" should be displayed
    And the employee "Regression FullFields" should no longer appear in the list

  @regression @pim @employee @P1 @order-29
  Scenario: REG-29 – Add employee form validations for required fields
    Given the admin user is logged in
    When the user navigates to the add employee page "/pim/addEmployee"
    And the user clicks the "Save" button without entering any fields
    Then the "First Name" field should display "Required" validation error
    And the "Last Name" field should display "Required" validation error

  @regression @pim @employee @P1 @order-30
  Scenario: REG-30 – Add employee with duplicate employee ID is rejected
    Given the admin user is logged in
    And an employee with ID "REG001" exists in the system
    When the user navigates to the add employee page "/pim/addEmployee"
    And the user enters the first name "Duplicate" and last name "IDTest"
    And the user enters the employee ID "REG001"
    Then the employee ID field should display an "Already exists" validation error

  @regression @pim @employee @P1 @order-31
  Scenario: REG-31 – Admin can view employee contact details tab
    Given the admin user is logged in
    And an employee exists in the system
    When the user navigates to the employee contact details page
    Then the contact details form should be displayed
    And the form should contain fields for street, city, state, zip, country, and phone

  @regression @pim @employee @P1 @order-32
  Scenario: REG-32 – Admin can view employee job details tab
    Given the admin user is logged in
    And an employee exists in the system
    When the user navigates to the employee job details page
    Then the job details form should be displayed
    And the form should contain fields for job title, employment status, and joined date

  @regression @pim @employee @P1 @order-33
  Scenario: REG-33 – Admin can add employee emergency contact
    Given the admin user is logged in
    And an employee exists in the system
    When the user navigates to the employee emergency contacts page
    And the user adds an emergency contact with name "Jane Doe" and phone "555-0100"
    And the user clicks the "Save" button
    Then a success notification should be displayed
    And the emergency contact "Jane Doe" should appear in the contacts list

  @regression @pim @employee @P1 @order-34
  Scenario: REG-34 – Admin can add employee dependent
    Given the admin user is logged in
    And an employee exists in the system
    When the user navigates to the employee dependents page
    And the user adds a dependent with name "John Jr" and relationship "Child"
    And the user clicks the "Save" button
    Then a success notification should be displayed
    And the dependent "John Jr" should appear in the dependents list

  @regression @pim @employee @P1 @order-35
  Scenario: REG-35 – Admin can create login details for an employee
    Given the admin user is logged in
    When the user navigates to the add employee page "/pim/addEmployee"
    And the user enters the first name "LoginTest" and last name "Employee"
    And the user enables the "Create Login Details" toggle
    And the user enters the username "logintest" and password "LoginTest@123"
    And the user confirms the password "LoginTest@123"
    And the user clicks the "Save" button
    Then a success notification should be displayed
    And the employee should be created with an associated user account

  @regression @pim @employee @P1 @order-36
  Scenario: REG-36 – Employee list search returns empty results with appropriate message
    Given the admin user is logged in
    When the user navigates to the employee list "/pim/viewEmployeeList"
    And the user searches for employee name "NonExistentXYZ99"
    And the user clicks the "Search" button
    Then the results should display "No Records Found"

  @regression @pim @employee @P1 @order-37
  Scenario: REG-37 – Employee list supports pagination
    Given the admin user is logged in
    And multiple employees exist in the system
    When the user navigates to the employee list "/pim/viewEmployeeList"
    Then the employee list should display pagination controls if records exceed the page limit

  @regression @pim @employee @P2 @order-38
  Scenario: REG-38 – ESS user can view own personal details
    Given an ESS user is logged in
    When the ESS user navigates to their own personal details page
    Then the personal details form should be displayed with their information

  @regression @pim @employee @P2 @order-39
  Scenario: REG-39 – ESS user cannot access another employee's details
    Given an ESS user is logged in
    When the ESS user attempts to navigate to another employee's personal details page
    Then the user should see an access denied or not found error

  @regression @pim @employee @P2 @order-40
  Scenario: REG-40 – Admin can navigate through all PIM left menu tabs
    Given the admin user is logged in
    And an employee exists in the system
    When the user navigates to the employee personal details page
    Then the PIM left menu should display all available tabs
    And the user should be able to navigate to each tab without errors

  # ═══════════════════════════════════════════════════
  # LOGOUT REGRESSION SCENARIOS
  # ═══════════════════════════════════════════════════

  @regression @logout @P0 @order-41
  Scenario: REG-41 – User can log out and is redirected to login page
    Given the admin user is logged in
    When the user clicks the user dropdown in the top-right corner
    And the user clicks the "Logout" option
    Then the user should be redirected to the login page "/auth/login"

  @regression @logout @P0 @order-42
  Scenario: REG-42 – Session is invalidated after logout
    Given the admin user is logged in
    When the user logs out via "/auth/logout"
    Then the session should be invalidated
    And attempting to access the dashboard "/dashboard/index" should redirect to login

  @regression @logout @P0 @order-43
  Scenario: REG-43 – API endpoints return 401 after logout
    Given the admin user is logged in
    When the user logs out via "/auth/logout"
    And a GET request is made to "/api/v2/pim/employees"
    Then the API should return HTTP status 401
    And the response body should contain "Session expired"

  @regression @logout @P1 @order-44
  Scenario: REG-44 – User can log in again after logout
    Given the admin user is logged in
    When the user logs out via "/auth/logout"
    And the user logs in again with username "Admin" and password "Jacqueline@OHRM123"
    Then the user should be redirected to the dashboard "/dashboard/index"

  @regression @logout @P1 @order-45
  Scenario: REG-45 – Multiple protected pages redirect to login after logout
    Given the admin user is logged in
    When the user logs out via "/auth/logout"
    Then attempting to access "/dashboard/index" should redirect to login
    And attempting to access "/pim/viewEmployeeList" should redirect to login
    And attempting to access "/admin/viewSystemUsers" should redirect to login
    And attempting to access "/leave/viewLeaveList" should redirect to login

  @regression @logout @P1 @order-46
  Scenario: REG-46 – Logout via direct URL access
    Given the admin user is logged in
    When the user directly navigates to "/auth/logout"
    Then the user should be redirected to the login page "/auth/login"
    And the session should be fully invalidated

  @regression @logout @P2 @order-47
  Scenario: REG-47 – Browser back button after logout does not restore session
    Given the admin user is logged in
    And the user is on the dashboard page
    When the user logs out via "/auth/logout"
    And the user presses the browser back button
    Then the user should not see the dashboard with active session
    And any page interaction should redirect to login

  @regression @logout @P2 @order-48
  Scenario: REG-48 – Session invalidation due to user account modification
    Given the admin user is logged in
    When the admin user's account is modified externally in the database
    And the user attempts to access a protected page
    Then the session should be automatically invalidated
    And the user should be redirected to the login page

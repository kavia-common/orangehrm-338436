@login
Feature: OrangeHRM Login
  As an OrangeHRM user
  I want to be able to log in to the application
  So that I can access the HR management dashboard

  Background:
    Given I navigate to the OrangeHRM login page

  # ==================== Successful Login Scenarios ====================

  @smoke @positive
  Scenario: Successful login with valid credentials
    When I enter a valid username
    And I enter a valid password
    And I click the login button
    Then I should be redirected to the dashboard page
    And I should see the dashboard header

  @positive
  Scenario: Successful login redirects to expected URL
    When I enter a valid username
    And I enter a valid password
    And I click the login button
    Then the page URL should contain "/dashboard"

  # ==================== Unsuccessful Login Scenarios ====================

  @negative
  Scenario: Failed login with invalid username and invalid password
    When I enter an invalid username "InvalidUser"
    And I enter an invalid password "InvalidPass123"
    And I click the login button
    Then I should see an error message "Invalid credentials"

  @negative
  Scenario: Failed login with valid username and wrong password
    When I enter a valid username
    And I enter an invalid password "WrongPassword!"
    And I click the login button
    Then I should see an error message "Invalid credentials"

  @negative
  Scenario: Failed login with invalid username and valid password
    When I enter an invalid username "NonExistentUser"
    And I enter a valid password
    And I click the login button
    Then I should see an error message "Invalid credentials"

  @negative
  Scenario: Login attempt with empty credentials
    When I click the login button
    Then I should see a required field validation message

  @negative
  Scenario: Login attempt with empty username and valid password
    When I enter a valid password
    And I click the login button
    Then I should see a required field validation message

  @negative
  Scenario: Login attempt with valid username and empty password
    When I enter a valid username
    And I click the login button
    Then I should see a required field validation message

  @negative
  Scenario Outline: Failed login with various invalid credential combinations
    When I enter an invalid username "<username>"
    And I enter an invalid password "<password>"
    And I click the login button
    Then I should see an error message "Invalid credentials"

    Examples:
      | username        | password         |
      | admin           | admin            |
      | Admin           | wrongpassword    |
      | test@user.com   | test123          |
      | ' OR '1'='1    | ' OR '1'='1     |
      | ADMIN           | ADMIN123         |

  @negative
  Scenario: Login with spaces-only username and password
    When I enter an invalid username "   "
    And I enter an invalid password "   "
    And I click the login button
    Then I should see an error message "Invalid credentials"

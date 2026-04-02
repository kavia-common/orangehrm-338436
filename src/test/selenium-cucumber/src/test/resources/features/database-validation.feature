@database @db-validation
Feature: Database Validation
  As a QA tester
  I want to validate data operations directly against the OrangeHRM MySQL database
  So that I can verify data integrity for insertions, updates, and deletions

  Background:
    Given the database connection is configured for the current environment

  # ─── Data Insertion Validation ───

  @db-insert @smoke
  Scenario: Verify database connectivity
    Then the database should be accessible

  @db-insert
  Scenario: Validate data insertion into a table
    Given the table "ohrm_nationality" exists in the database
    And no record exists in "ohrm_nationality" where "name" = "TestNationality_Auto"
    When I insert a record into "ohrm_nationality" with values "id=999, name=TestNationality_Auto"
    Then the operation should affect 1 row(s)
    And a record should exist in "ohrm_nationality" where "name" = "TestNationality_Auto"
    And the value of "name" in "ohrm_nationality" where "id" = "999" should be "TestNationality_Auto"
    # Cleanup
    And I clean up the record in "ohrm_nationality" where "id" = "999"

  @db-insert
  Scenario: Validate inserted record count
    Given the table "ohrm_nationality" exists in the database
    And no record exists in "ohrm_nationality" where "name" = "TestCount_Auto"
    When I insert a record into "ohrm_nationality" with values "id=998, name=TestCount_Auto"
    Then the count of records in "ohrm_nationality" where "name" = "TestCount_Auto" should be 1
    # Cleanup
    And I clean up the record in "ohrm_nationality" where "id" = "998"

  @db-insert
  Scenario: Validate insertion with query verification
    Given the table "ohrm_nationality" exists in the database
    And no record exists in "ohrm_nationality" where "name" = "TestQuery_Auto"
    When I insert a record into "ohrm_nationality" with values "id=997, name=TestQuery_Auto"
    And I execute the query "SELECT * FROM ohrm_nationality WHERE name = ?" with parameter "TestQuery_Auto"
    Then the query result should not be empty
    And the query result should contain 1 row(s)
    # Cleanup
    And I clean up the record in "ohrm_nationality" where "id" = "997"

  # ─── Data Update Validation ───

  @db-update
  Scenario: Validate data update in a table
    Given the table "ohrm_nationality" exists in the database
    And no record exists in "ohrm_nationality" where "name" = "UpdateTest_Auto"
    And no record exists in "ohrm_nationality" where "name" = "UpdatedName_Auto"
    # Setup: insert a record to update
    When I insert a record into "ohrm_nationality" with values "id=996, name=UpdateTest_Auto"
    Then a record should exist in "ohrm_nationality" where "name" = "UpdateTest_Auto"
    # Perform update
    When I update "ohrm_nationality" set "name" = "UpdatedName_Auto" where "id" = "996"
    Then the operation should affect 1 row(s)
    And a record should exist in "ohrm_nationality" where "name" = "UpdatedName_Auto"
    And no record should exist in "ohrm_nationality" where "name" = "UpdateTest_Auto"
    And the value of "name" in "ohrm_nationality" where "id" = "996" should be "UpdatedName_Auto"
    # Cleanup
    And I clean up the record in "ohrm_nationality" where "id" = "996"

  @db-update
  Scenario: Validate update does not affect unrelated records
    Given the table "ohrm_nationality" exists in the database
    And no record exists in "ohrm_nationality" where "name" = "KeepMe_Auto"
    And no record exists in "ohrm_nationality" where "name" = "ChangeMe_Auto"
    # Setup: insert two records
    When I insert a record into "ohrm_nationality" with values "id=994, name=KeepMe_Auto"
    And I insert a record into "ohrm_nationality" with values "id=995, name=ChangeMe_Auto"
    # Update only one record
    When I update "ohrm_nationality" set "name" = "Changed_Auto" where "id" = "995"
    Then a record should exist in "ohrm_nationality" where "name" = "KeepMe_Auto"
    And a record should exist in "ohrm_nationality" where "name" = "Changed_Auto"
    And no record should exist in "ohrm_nationality" where "name" = "ChangeMe_Auto"
    # Cleanup
    And I clean up the record in "ohrm_nationality" where "id" = "994"
    And I clean up the record in "ohrm_nationality" where "id" = "995"

  # ─── Data Deletion Validation ───

  @db-delete
  Scenario: Validate data deletion from a table
    Given the table "ohrm_nationality" exists in the database
    And no record exists in "ohrm_nationality" where "name" = "DeleteMe_Auto"
    # Setup: insert a record to delete
    When I insert a record into "ohrm_nationality" with values "id=993, name=DeleteMe_Auto"
    Then a record should exist in "ohrm_nationality" where "name" = "DeleteMe_Auto"
    # Perform delete
    When I delete from "ohrm_nationality" where "id" = "993"
    Then the operation should affect 1 row(s)
    And no record should exist in "ohrm_nationality" where "name" = "DeleteMe_Auto"
    And the count of records in "ohrm_nationality" where "id" = "993" should be 0

  @db-delete
  Scenario: Validate deletion with query verification
    Given the table "ohrm_nationality" exists in the database
    And no record exists in "ohrm_nationality" where "name" = "DeleteQuery_Auto"
    # Setup
    When I insert a record into "ohrm_nationality" with values "id=992, name=DeleteQuery_Auto"
    And I execute the query "SELECT * FROM ohrm_nationality WHERE id = ?" with parameter "992"
    Then the query result should not be empty
    # Perform delete
    When I delete from "ohrm_nationality" where "id" = "992"
    And I execute the query "SELECT * FROM ohrm_nationality WHERE id = ?" with parameter "992"
    Then the query result should be empty

  @db-delete
  Scenario: Validate deletion does not affect unrelated records
    Given the table "ohrm_nationality" exists in the database
    And no record exists in "ohrm_nationality" where "name" = "Survivor_Auto"
    And no record exists in "ohrm_nationality" where "name" = "Victim_Auto"
    # Setup: insert two records
    When I insert a record into "ohrm_nationality" with values "id=990, name=Survivor_Auto"
    And I insert a record into "ohrm_nationality" with values "id=991, name=Victim_Auto"
    # Delete only one
    When I delete from "ohrm_nationality" where "id" = "991"
    Then a record should exist in "ohrm_nationality" where "name" = "Survivor_Auto"
    And no record should exist in "ohrm_nationality" where "name" = "Victim_Auto"
    # Cleanup
    And I clean up the record in "ohrm_nationality" where "id" = "990"

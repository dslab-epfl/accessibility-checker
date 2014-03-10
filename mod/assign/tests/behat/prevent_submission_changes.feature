@mod @mod_assign
Feature: Prevent or allow assignment submission changes
  In order to control when a student can change his/her submission
  As a teacher
  I need to prevent or allow student submission at any time

  @javascript
  Scenario: Preventing changes and allowing them again
    Given the following "courses" exists:
      | fullname | shortname | category | groupmode |
      | Course 1 | C1 | 0 | 1 |
    And the following "users" exists:
      | username | firstname | lastname | email |
      | teacher1 | Teacher | 1 | teacher1@asd.com |
      | student1 | Student | 1 | student1@asd.com |
    And the following "course enrolments" exists:
      | user | course | role |
      | teacher1 | C1 | editingteacher |
      | student1 | C1 | student |
    And I log in as "teacher1"
    And I follow "Course 1"
    And I turn editing mode on
    And I add a "Assignment" to section "1" and I fill the form with:
      | Assignment name | Test assignment name |
      | Description | Submit your online text |
      | assignsubmission_onlinetext_enabled | 1 |
      | assignsubmission_file_enabled | 0 |
    And I log out
    And I log in as "student1"
    And I follow "Course 1"
    And I follow "Test assignment name"
    And I press "Add submission"
    And I fill the moodle form with:
      | Online text | I'm the student submission |
    And I press "Save changes"
    And I press "Edit submission"
    And I fill the moodle form with:
      | Online text | I'm the student submission and he/she edited me |
    And I press "Save changes"
    And I log out
    And I log in as "teacher1"
    And I follow "Course 1"
    And I follow "Test assignment name"
    When I follow "View/grade all submissions"
    And I click on "//tr[contains(., 'Student 1')]/descendant::td/descendant::img[@alt='Actions']/parent::a" "xpath_element"
    And I follow "Prevent submission changes"
    Then I should see "Submission changes not allowed"
    And I log out
    And I log in as "student1"
    And I follow "Course 1"
    And I follow "Test assignment name"
    And "Edit submission" "button" should not exists
    And I should see "This assignment is not accepting submissions"
    And I log out
    And I log in as "teacher1"
    And I follow "Course 1"
    And I follow "Test assignment name"
    And I follow "View/grade all submissions"
    And I click on "//tr[contains(., 'Student 1')]/descendant::td/descendant::img[@alt='Actions']/parent::a" "xpath_element"
    And I follow "Allow submission changes"
    And I log out
    And I log in as "student1"
    And I follow "Course 1"
    And I follow "Test assignment name"
    And I should not see "This assignment is not accepting submissions"
    And I press "Edit submission"
    And I fill the moodle form with:
      | Online text | I'm the student submission edited again |
    And I press "Save changes"
    And I should see "I'm the student submission edited again"

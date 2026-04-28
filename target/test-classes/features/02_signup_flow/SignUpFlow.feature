@signupFlow
Feature: Sign Up Flow — Direct Manual Account Creation

  Scenario: Case 1 — Direct manual signup activates account and blocks duplicate registration
    Given I navigate to sign up via "direct_signup"
    When I fill in the sign up form:
      | company   | Aybee Test |
      | firstName | Test       |
      | lastName  | User       |
      | password  | Test@1234  |
    And I click Sign Up
    And I enter the OTP received on my email
    And I click Next to activate my account
    And I complete the onboarding questions if displayed
    Then I should be on the dashboard

    When I log out
    And I navigate to the sign up page
    And I attempt to sign up with the same email and:
      | company   | Aybee Test |
      | firstName | Test       |
      | lastName  | User       |
      | password  | Test@1234  |
    And I click Sign Up
    Then I should see a notification matching "EMAIL_ALREADY_REGISTERED"

@signinFlow
Feature: Sign In Flow — Positive and Negative Authentication Cases

  Scenario: Registered user signs in successfully with valid credentials
    Given a verified user account exists
    And I am on the sign in page
    When I sign in with my registered credentials
    And I complete the onboarding questions if displayed
    Then I should be on the dashboard

  Scenario: Sign in fails with an incorrect password
    Given a verified user account exists
    And I am on the sign in page
    When I sign in with my registered email and password "WrongPass999"
    Then I should see a notification matching "INVALID_SIGNIN_CREDENTIALS"

  Scenario: Sign in fails with an unregistered email address
    Given I am on the sign in page
    When I sign in with email "nobody@ibyoxwxn.mailosaur.net" and password "SomePass123"
    Then I should see a notification matching "INVALID_SIGNIN_CREDENTIALS"

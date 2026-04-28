@copyLinkInvite
Feature: Copy Link Invite — Join Toast, Company Lock, and Signup via Shared Link

  Scenario: Opening a copied invite link shows join toast and locked company name
    Given I am logged in as a verified user
    When I copy the team invite link
    And I open the copied invite link in a fresh session
    Then I should see a join team notification for the inviter's company
    And the company name field should be pre-filled and locked

  Scenario: New user signs up successfully via a copied invite link
    Given I am logged in as a verified user
    When I copy the team invite link
    And I open the copied invite link in a fresh session
    And I fill in the sign up form:
      | firstName | Test      |
      | lastName  | User      |
      | password  | Test@1234 |
    And I click Sign Up
    And I complete the onboarding questions if displayed
    Then I should be on the dashboard

  Scenario: Already registered user is blocked from signing up via a copied invite link
    Given I am logged in as a verified user
    When I copy the team invite link
    And I open the copied invite link in a fresh session
    And I attempt to sign up with the same email and:
      | firstName | Test      |
      | lastName  | User      |
      | password  | Test@1234 |
    And I dismiss the active toast
    And I click Sign Up
    Then I should see a notification matching "EMAIL_ALREADY_REGISTERED"

  Scenario: Already registered user signs in directly via a copied invite link
    Given I am logged in as a verified user
    When I copy the team invite link
    And I open the copied invite link in a fresh session
    When I click the Sign In link
    And I sign in with my registered credentials
    And I complete the onboarding questions if displayed
    Then I should be on the dashboard

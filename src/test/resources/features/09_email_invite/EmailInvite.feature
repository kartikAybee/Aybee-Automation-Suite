@emailInvite
Feature: Email Invite — Accept Invitation Page for New and Existing Users

  Scenario: New user accepts an email invitation and lands on the dashboard
    Given I am logged in as a verified user
    When I send an email invite to a new user with role "user"
    And I open the accept invitation link for the new user from email
    Then the invitation email field should be pre-filled and locked
    When I enter "Password123" as the invitation password
    And I click Accept Invitation
    And I complete the onboarding questions if displayed
    Then I should be on the dashboard

  # Alert text is locale-dependent: "This email is already in use: <email>"
  Scenario: Already registered user sees a browser alert when accepting an email invitation
    Given I am logged in as a verified user
    When I send an email invite to my own email address
    And I open the accept invitation link from email
    And I enter my own password as the invitation password
    And I click Accept Invitation
    Then I should see an already registered alert containing my email

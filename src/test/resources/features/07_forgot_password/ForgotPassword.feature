@forgotPassword
Feature: Forgot Password — Reset Link and Password Update Flow

  Scenario: Registered user receives a password reset link by email
    Given a verified user account exists
    And I am on the forgot password page
    When I enter my registered email in the reset field
    And I click Send Reset Link
    Then I should see a reset confirmation message

  Scenario: Unregistered email should not trigger a reset confirmation
    Given I am on the forgot password page
    When I enter "notregistered@ibyoxwxn.mailosaur.net" in the reset email field
    And I click Send Reset Link
    Then I should not see a reset confirmation message

  # Entering mismatched passwords first, then correcting them, verifies page recovery.
  Scenario: User recovers from a password mismatch and resets successfully
    Given a verified user account exists
    And I am on the forgot password page
    When I enter my registered email in the reset field
    And I click Send Reset Link
    Then I should see a reset confirmation message
    When I follow the password reset link from email
    And I enter "NewPass@5678" as the new password
    And I enter "DiffPass@9999" as the confirm password
    And I save the new password
    Then I should see a notification matching "PASSWORDS_DO_NOT_MATCH"
    And I dismiss the active toast
    When I clear the password reset fields
    And I enter "NewPass@5678" as the new password
    And I enter "NewPass@5678" as the confirm password
    And I save the new password
    Then I should see a password reset success notification

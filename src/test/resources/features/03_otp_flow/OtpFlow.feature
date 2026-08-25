@otpFlow
Feature: OTP Verification — Wrong Code, Cancellation, and Abandonment Cases

  Scenario: Entering a wrong OTP shows invalid code error
    Given I navigate to sign up via "direct_signup"
    When I fill in the sign up form:
      | firstName | Test       |
      | lastName  | User       |
      | password  | Test@1234  |
    And I click Sign Up
    And I enter OTP "000000" on the activation page
    Then I should see a notification matching "INVALID_OTP"

  # Reuses OTP page from previous scenario if still loaded — skips fresh sign-up.
  Scenario: Cancelling OTP activation prevents sign-in with those credentials
    Given I am on the OTP activation page
    When I click Cancel on the OTP activation page
    And I navigate to the sign in page
    And I sign in with my registered credentials
    Then I should see a notification matching "INVALID_SIGNIN_CREDENTIALS"

  # Reuses OTP page from previous scenario if still loaded — skips fresh sign-up.
  Scenario: Abandoning the OTP page without cancelling resumes activation on next sign-in
    Given I am on the OTP activation page
    When I abandon the activation page and navigate to the home page
    And I navigate to the sign in page
    And I sign in with my registered credentials
    Then I should be on the OTP activation page

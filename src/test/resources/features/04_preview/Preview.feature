@questionnaire @preview
Feature: Guest participant journey through the Questionnaire preview

  # Continues in the same reused browser. The preview URL captured in case3 persists via
  # GlobalTestState, so this case re-opens it as a fresh cleared-cache/cookies guest and completes
  # the full non-logged-in participant journey: demographics → consent → every form question.

  @case4 @guest
  Scenario: A non-logged-in guest answers demographics, consent, and every form question
    When I open the questionnaire preview as a guest
    And I answer all guest demographic questions
    And I agree to the consent statement
    And I answer all the questionnaire form questions
    Then the participant journey should redirect to sign in on completion

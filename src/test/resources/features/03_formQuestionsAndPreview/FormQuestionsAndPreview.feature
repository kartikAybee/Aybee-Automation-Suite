@formQuestionsAndPreview
Feature: Validate Form Questions and Preview CTR Experiment

  # No Background — browser is on the form questions page from ShopSetup.
  # CTR experiments have 2 pre-added long-text questions; no questions are added manually.

  Scenario: Validate form questions page load, preview as logged-in user, answer demographics, verify scenario selection on consent, and reach product list
    When I wait for the form questions page to load
    And I preview the CTR experiment as a logged-in user
    And I answer the demographic questions as a logged-in user
    And I agree to the CTR consent statement and proceed
    And I dismiss the marketplace help popup if present

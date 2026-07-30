@questionnaire @login
Feature: Sign In and open the Marketing & Ads new-experiment section

  # First feature of the Questionnaire suite. Login AND navigation are bundled into this single
  # case (case1) per the suite design — it establishes the signed-in browser session and leaves the
  # browser in the Marketing & Ads new-experiment section, ready for the creation case. The browser
  # is reused across the whole suite.

  @case1
  Scenario: Sign in and navigate to the Marketing & Ads new-experiment section
    Given I am signed in as a valid user
    When I navigate to the experiments page
    And I click add new experiment
    And I select the Marketing and Ads use case
    Then the Questionnaire test type option should be available

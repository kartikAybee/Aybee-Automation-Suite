@questionnaire @experimentCreation
Feature: Create a Marketing & Ads Questionnaire Experiment

  # No Background — continues in the same browser session left in the Marketing & Ads
  # new-experiment section by the Login & navigation feature (case1). This case is the creation
  # part: pick the (side-scroll hidden) Questionnaire test type and target the United States.

  @case2
  Scenario: Create a Questionnaire experiment for Marketing & Ads targeting the United States
    When I scroll to and select the Questionnaire test type
    And I select United States as the target market
    Then the study objective step should be loaded

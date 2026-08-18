@pdpSimulation @experimentCreation
Feature: Create a Marketing & Ads PDP Simulation Experiment

  # No Background — continues in the same browser session signed in during the Login feature (case1).

  @case2
  Scenario: Create a PDP Simulation experiment for Marketing & Ads targeting the United States
    When I navigate to the experiments page
    And I click add new experiment
    And I select the Marketing and Ads use case
    And I scroll to and select the PDP Simulation test type
    And I select United States as the target market
    Then the shop setup step should be loaded with the Add New Product button clickable

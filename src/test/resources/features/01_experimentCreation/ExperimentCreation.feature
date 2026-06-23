@experimentCreation
Feature: Create a New D2C Experiment on Aybee Platform

  Background:
    Given I am signed in as a valid user

  @case1
  Scenario: Create a D2C experiment for Product Development targeting the United States
    When I navigate to the experiments page
    And I click add new experiment
    And I select the Product Development use case
    And I select the D2C test type
    And I select United States as the target market
    Then I should be on the create project page

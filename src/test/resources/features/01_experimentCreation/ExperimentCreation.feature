@experimentCreation
Feature: Create a New CTR Optimization Experiment on Aybee Platform

  Background:
    Given I am signed in as a valid user

  Scenario: Create a Marketing Ads CTR Optimization experiment targeting the United States
    When I navigate to the experiments page
    And I click add new experiment
    And I select the Product Development use case
    And I select the Packaging Optimization section
    And I select the CTR Optimization test type
    And I select United States as the target market
    Then I should be on the create project page

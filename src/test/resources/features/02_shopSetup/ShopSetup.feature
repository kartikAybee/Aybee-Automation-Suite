@shopSetup
Feature: Add Products and Configure Scenarios for CTR Optimization

  # No Background — browser is on the shop setup page from ExperimentCreation.

  Scenario: Add product via ASIN, create CTR variation by deleting first picture only, and proceed to form questions
    When I add a new product via ASIN
    And I add a product variation for CTR optimization
    And I proceed to form questions

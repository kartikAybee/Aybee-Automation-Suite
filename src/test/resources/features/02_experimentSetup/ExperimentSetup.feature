@experimentSetup
Feature: Configure Shop Products and Verify Initial Form Questions

  # No Background — browser is on the experiment settings page from the previous scenario.

  @case2
  Scenario: Complete experiment settings, configure shop products, verify initial form questions, and capture the preview URL
    When I enter the study objective
    And I click continue to generate business questions
    And I add all business questions
    And I proceed to shop setup
    And I add a new product via ASIN
    And I add a product variation with updated price
    And I proceed to form questions
    And I verify the initial form questions are loaded
    And I add an A/B test product long text form question
    And I capture the experiment preview URL

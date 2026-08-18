@pdpSimulation @experimentSetup
Feature: Experiment Setup — Shop Setup and Form Questions (PDP Simulation Steps 1 & 2)

  # Shop Setup and Form Questions are the two steps of a single experiment setup, so they run as one
  # continuous case. No Background — continues in the same browser session on the Shop Setup step
  # reached in case2.

  @case3
  Scenario: Configure shop scenarios, set up form questions, and open the preview
    # ── Shop Setup (step 1) ──
    When I add a product to Scenario A via ASIN
    And I add a second scenario
    And I add a third scenario
    And I delete the third scenario
    And I edit the second scenario to trim its product name
    And I save the scenario changes
    And I capture both scenario product details
    And I proceed to the form questions step
    And the form questions step should be loaded with the Add Question button clickable
    # ── Form Questions (step 2) — PDP has no default questions; our 4 Split Tests are the only ones ──
    And I create four Split Test questions covering each What to Display option
    And I validate all inputs and open the preview as a logged-in user
    Then the preview URL should be captured

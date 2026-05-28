@finalQuestions
Feature: Participant Form Questions and Exit Scenarios

  # No Background — browser is on the post-checkout page from the previous scenario.

  @case5
  Scenario: Complete participant form for both participants and verify exit scenarios

    # Participant 1 — current participant from Feature 4's checkout.
    # Scenario already detected in Feature 4; all question types handled dynamically.
    When I answer the participant form questions

    # Not-interested exit from marketplace
    And I navigate to the preview URL as a guest again
    And I answer the demographic questions
    And I agree to the consent statement and proceed to the marketplace
    And I dismiss the marketplace help popup if present
    And I click not interested from the marketplace and verify I am filtered out

    # Not-interested exit from product detail
    And I navigate to the preview URL as a guest again
    And I answer the demographic questions
    And I agree to the consent statement and proceed to the marketplace
    And I dismiss the marketplace help popup if present
    And I detect and store the current scenario assignment
    And I select our product and answer the opener question
    And I click not interested from the product detail and verify I am filtered out

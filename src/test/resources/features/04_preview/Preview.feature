@pdpSimulation @preview
Feature: Preview Journeys — Not Interested and No Buying Intent (PDP Simulation)

  # Both owner preview journeys are bundled in this one feature but kept segregated as separate
  # scenarios. Each re-previews the URL captured in the experiment setup case (case3) and runs the
  # identical exclusive question flow; they differ only in the trigger button. No Background —
  # continues in the same browser session.

  @case4 @notInterested
  Scenario: Not Interested — close the rating overlay, then decline via button-not-interest
    When I open the captured preview URL as a logged-in user
    And I answer the gender and age demographics
    And I agree to the consent statement
    And I dismiss the help popup on the product page
    And I close the rating overlay on the product detail page
    And I click Not Interested
    Then I should answer all Not-Interested questions and be redirected to the Shop Setup page

  @case5 @noBuyingIntent
  Scenario: No Buying Intent — select product-not-interested on the rating overlay
    When I open the captured preview URL as a logged-in user
    And I answer the gender and age demographics
    And I agree to the consent statement
    And I dismiss the help popup on the product page
    And I click No Buying Intent
    Then I should answer all Not-Interested questions and be redirected to the Shop Setup page

  @case6 @guest
  Scenario: Guest — clear session, run full demographics, and verify product detail matches the scenario
    When I open the captured preview URL as a guest
    And I answer all guest demographic questions
    And I agree to the consent statement
    And I dismiss the help popup on the product page
    And I verify the product detail page matches the assigned scenario
    And I verify the buy-now popup appears, then closes and reopens the slider
    And I move the decision slider, submit Buy Now, and land on the question page
    Then I answer all the manually added split test questions as a guest

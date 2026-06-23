@notInterestedAndGuestParticipant
Feature: Not Interested Filter and Guest Participant Flow for CTR Experiment

  # No Background — browser state carries over from FormQuestionsAndPreview (product list visible).
  # Scenario 1 runs while the browser is still on the product list page (logged-in user).
  # Scenario 2 starts a fresh guest session using the preview URL stored in GlobalTestState.

  Scenario: Logged-in user clicks Not Interested and is redirected to shop setup page
    When I click not interested and wait for shop setup page

  Scenario: Guest participant completes the full CTR experiment flow and reaches the sign-up page
    When I navigate to the preview URL as a guest for CTR
    And I answer the demographic questions
    And I agree to the guest consent statement and wait for product list
    And I dismiss the marketplace help popup if present
    And I select our CTR product from the marketplace
    And I confirm the product selection
    And I answer the opener question and wait for participant form
    And I answer the first CTR participant long text question
    And I answer the second CTR participant long text question
    And I verify the test completion redirects to sign up

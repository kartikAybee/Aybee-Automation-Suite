@previewJourney
Feature: Preview Journey — Logged-In Not-Interested Flows and Guest D2C Shopping Flows

  # No Background — browser is on the experiment page from the previous scenario.
  # Preview URL was captured and stored in context during ExperimentSetup.

  @case3
  Scenario: Preview as logged-in user twice to verify Not Interested redirects, then run full D2C guest flows for our product and competitor
    # ── Pass 1 (logged-in): Not Interested from product list ─────────────────────
    When I navigate to the preview URL as a logged-in user
    And I answer the gender and age demographic questions
    And I agree to the consent statement and proceed to the D2C product page
    And I wait for the help popup and dismiss it
    And I click not interested and verify I am redirected to shop setup

    # ── Pass 2 (logged-in): Not Interested from product page ─────────────────────
    And I navigate to the preview URL as a logged-in user
    And I answer the gender and age demographic questions
    And I agree to the consent statement and proceed to the D2C product page
    And I wait for the help popup and dismiss it
    And I select any product from the D2C product list and answer the opener question
    And I click not interested and verify I am redirected to shop setup

    # ── Flow 1 (guest): our product — Q2 must be absent ──────────────────────────
    And I clear the session
    And I navigate to the preview URL as a guest
    And I answer all demographic questions
    And I agree to the consent statement and proceed to the D2C product page
    And I wait for the help popup and dismiss it
    And I verify the D2C product list contains our product with matching price
    And I select our product from the D2C product list and answer the opener question
    And I verify the D2C product page name and price match the selection
    And I add the product to the D2C cart
    And I verify the D2C cart contains the product with correct price and total
    And I remove the product from the D2C cart and verify it is empty
    And I close the D2C cart sidebar
    And I add the product to the D2C cart again
    And I proceed to D2C checkout
    And I answer the D2C form questions for our product and verify Q2 is absent
    And I verify the guest is redirected to the sign-up page

    # ── Flow 2 (guest): competitor product — Q2 must appear ──────────────────────
    And I clear the session
    And I navigate to the preview URL as a guest
    And I answer all demographic questions
    And I agree to the consent statement and proceed to the D2C product page
    And I wait for the help popup and dismiss it
    And I select a competitor product from the D2C product list and answer the opener question
    And I verify the D2C product page name and price match the selection
    And I add the product to the D2C cart
    And I verify the D2C cart contains the product with correct price and total
    And I proceed to D2C checkout
    And I answer the D2C form questions for competitor product and verify Q2 is shown
    And I verify the guest is redirected to the sign-up page

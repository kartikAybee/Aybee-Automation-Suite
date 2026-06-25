@experimentSetup
Feature: Configure Shop Products and Form Questions

  # No Background — browser is on the shop setup page from the previous scenario.

  @case2
  Scenario: Complete experiment settings, configure shop products, add all form question types, and capture the preview URL
    When I enter the study objective
    And I click continue to generate business questions
    And I add all business questions
    And I proceed to shop setup
    And I add a new product via ASIN
    And I add a product variation with updated price
    And I proceed to form questions
    And I verify no unexpected questions are pre-added
    And I add a long text form question "Describe your overall impression of the product in your own words."
    And I add a limited choice form question "Which product attributes matter most to your purchase decision?"
    And I add a single choice form question "How would you rate the value for money of this product?"
    And I add a multiple choice form question "Which factors would most influence your decision to buy this product again?"
    And I add a horizontal likert form question "How likely are you to recommend this product to someone you know?"
    And I add a vertical likert form question "Rate your satisfaction with the following aspects of this product."
    And I clean up empty options on the limited choice question
    And I preview the experiment journey as a logged-in user
    And I answer the gender and age demographic questions
    And I agree to the consent statement and proceed to the marketplace
    And I dismiss the marketplace help popup if present
    And I click not interested from the marketplace and verify I am redirected to shop setup
    And I navigate to the preview URL as a logged-in user
    And I answer the gender and age demographic questions
    And I agree to the consent statement and proceed to the marketplace
    And I dismiss the marketplace help popup if present
    And I detect and store the current scenario assignment
    And I select our product and answer the opener question
    And I click not interested from the product detail and verify I am filtered out
    And I start the guest participant journey from the preview URL

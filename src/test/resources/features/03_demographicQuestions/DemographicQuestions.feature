@demographicQuestions
Feature: Demographic Questions Flow

  # No Background — preview URL is restored from GlobalTestState; navigateAsGuest handles auth.

  @case3
  Scenario: Answer demographic questions — verify decline-consent filter-out and proceed-with-consent flows
    When I answer the demographic questions
    And I decline the consent statement and verify I am redirected to the marketplace as a filtered out participant
    And I navigate to the preview URL as a guest again
    And I answer the demographic questions
    And I agree to the consent statement and proceed to the marketplace

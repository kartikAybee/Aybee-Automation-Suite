@qatCreation
Feature: Create a New QAT Experiment on Aybee Platform

  Background:
    Given I am signed in as a valid user

  @case1
  Scenario: Create a Quick Asset Testing experiment for Product Development targeting the United States
    When I navigate to the experiments page
    And I click add new experiment
    And I select the Product Development use case
    And I select Quick Asset Testing as the test type
    And I select United States as the target market
    Then I should be on the create project page
    When I upload the Scenario A asset image
    And I upload the Scenario B asset image
    Then both Scenario asset images should be uploaded
    When I proceed to set up form questions
    Then the form questions add-question button should be clickable
    # ── QAT-exclusive Show-to-Participants matrix (just_question / uploaded_image covered by msjourney) ──
    When I add a form question showing all creatives "How would you rate the overall appeal of the design?"
    And I add a form question showing the top choice creative "Did the top creative stand out to you?"
    And I add a form question showing a specific creative version "Which aspects of this version influenced you?"
    Then I preview the QAT experiment journey
    # Logged-in preview: accept gender + age + consent, click Not Interested, verify redirect to asset-upload step
    And I preview as a logged-in user and decline via Not Interested
    # Hand-off to the usual guest journeys (clear session + reopen as guest)
    And I clear the session and open the preview as a guest
    # As a guest every demographic question is shown — answer all in order with assertions
    And I answer all demographic questions in order as a guest
    # Agree to consent → QAT selection page should show every uploaded creative
    And I agree to the consent statement as a guest
    Then all uploaded creatives should be displayed on the QAT selection page
    # Pick a version, enter the >50-word reason, blur to enable Next, then advance to the questions
    And I select creative version "A" and continue to the questions
    # Questions appear in configured order — answer each with clicks (same as msjourney)
    Then I answer all QAT survey questions in order

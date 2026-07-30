@questionnaire @experimentSetup
Feature: Experiment Setup — Study Objective, Business Questions and Form Questions (Questionnaire)

  # The Questionnaire setup runs as one continuous case (case3): the Study Objective + Business
  # Questions step (step 1) flows straight into the Form Questions step (step 2). No Background —
  # continues in the same browser session on the Study Objective step reached in case2.

  @case3
  Scenario: Set the study objective, add a business question, build the form question matrix, and open the preview as a guest
    # ── Study Objective + Business Questions (step 1) ──
    When I enter a random study objective
    And I continue to generate business questions
    And I add one business question
    And I proceed to the form questions step
    And the form questions step should be loaded
    # ── Form Questions (step 2) — the question matrix (starts at question index 1) ──
    And I add a long text question shown as just question
    And I add a limited choice question shown as uploaded assets
    And I add a single choice question filtered by a prior response
    And I add a multiple choice question shown as uploaded assets filtered by prior responses
    And I add a horizontal likert question filtered by a prior response
    And I add a vertical likert question
    And I clean up empty options on the limited choice question
    # ── Validate + preview as a cleared-cache/cookies guest ──
    And I validate all questions and capture the preview URL
    And I open the preview URL as a cleared-cache guest
    Then the preview URL should be captured

@navigation
Feature: Page Navigation — Links Between Sign Up, Sign In and Forgot Password

  Scenario: Navigate from Sign Up to Sign In and back to Sign Up
    Given I am on the sign up page
    When I click the Sign In link
    Then I should be on the sign in page
    When I click the Sign Up link
    Then I should be on the sign up page

  Scenario: Navigate from Sign In to Forgot Password
    Given I am on the sign in page
    When I click the Forgot Password link
    Then I should be on the forgot password page

@pdpSimulation @login
Feature: Sign In to the Aybee Platform

  # First feature of the PDP Simulation suite — establishes the signed-in browser session
  # that the subsequent feature files continue to use (the browser is reused across the suite).

  @case1
  Scenario: Sign in as a valid user
    Given I am signed in as a valid user
    Then I should land on the Aybee dashboard

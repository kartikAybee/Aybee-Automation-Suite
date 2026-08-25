@joinRequest
Feature: Join Request — request / withdraw / approve / deny within the shared company

  # Two concurrent sessions: admin = shared account (approves/denies on the Team panel),
  # requester = a fresh same-domain user on the company-selection popup. Company C = the
  # shared account's company; approve/deny are within that same company.

  Background:
    Given the shared company admin is logged in on the Team panel

  Scenario: Requester-1 — request, withdraw lifecycle, then approved into the company
    Given a new same-domain user is on the company-selection popup
    When the requester requests to join the shared company
    Then the requester sees a pending status for the shared company
    And the requester's request button shows Withdraw
    When the admin opens the Team panel
    Then the admin sees approve and decline controls for the requester
    When the requester withdraws the request
    Then the requester's Withdraw span disappears
    And the requester no longer sees a pending status
    When the admin reloads the Team panel
    Then the admin no longer sees approve and decline controls for the requester
    When the requester requests to join the shared company again
    Then the requester sees a pending status for the shared company
    When the admin approves the requester
    Then the requester is admitted into the company

  Scenario: Requester-2 — request is denied and becomes terminal
    Given a new same-domain user is on the company-selection popup
    When the requester requests to join the shared company
    Then the requester sees a pending status for the shared company
    When the admin declines the requester
    Then the requester sees a rejected status for the shared company

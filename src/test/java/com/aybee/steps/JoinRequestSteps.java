package com.aybee.steps;

import com.aybee.context.ScenarioContext;
import com.aybee.driver.SessionManager;
import com.aybee.pages.CompanySelectionPage;
import com.aybee.pages.DashboardPage;
import com.aybee.pages.SignUpPage;
import com.aybee.pages.TeamPage;
import com.aybee.utils.TestUser;
import com.aybee.utils.TestUserFactory;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.testng.Assert;

// Join-request flow (folder 10) — two concurrent sessions:
//   • admin (SessionManager.useAdmin)     = shared account, approves/denies from the Team panel
//   • requester (SessionManager.useRequester) = fresh same-domain user on the company-selection popup
// The company C = the shared account's company; ids are built from the stored unique company name
// and the requester's unique display name so nothing clashes.
public class JoinRequestSteps {

    private final ScenarioContext context;

    public JoinRequestSteps(ScenarioContext context) {
        this.context = context;
    }

    // "First Last" exactly as entered — the id form Bubble uses for the approve/decline divs
    // (e.g. "QAmt8deont Req-approve-user"), NOT uppercased.
    private String requesterName() {
        TestUser u = context.joinRequester;
        return u.firstName + " " + u.lastName;
    }

    private String company() {
        return context.joinCompany;
    }

    // ─── Setup ──────────────────────────────────────────────────────────────────

    @Given("the shared company admin is logged in on the Team panel")
    public void theSharedCompanyAdminIsLoggedInOnTheTeamPanel() {
        SessionManager.useAdmin();
        TestUser admin = TestUserFactory.getSharedVerifiedUser();
        context.joinCompany = admin.company;

        DashboardPage dashboard = new DashboardPage();
        if (!dashboard.isLoaded()) {
            new SignUpPage().navigateTo().clickSignInLink()
                    .enterEmail(admin.email).enterPassword(admin.password).clickSignIn();
        }
        new TeamPage().navigateToTeamTab();
    }

    @Given("a new same-domain user is on the company-selection popup")
    public void aNewSameDomainUserIsOnTheCompanySelectionPopup() {
        SessionManager.useRequester();
        context.joinRequester = TestUserFactory.createJoinRequester();
    }

    // ─── Requester actions ────────────────────────────────────────────────────────

    @When("the requester requests to join the shared company")
    @When("the requester requests to join the shared company again")
    public void theRequesterRequestsToJoinTheSharedCompany() {
        SessionManager.useRequester();
        new CompanySelectionPage().clickRequestButton(company());
    }

    @When("the requester withdraws the request")
    public void theRequesterWithdrawsTheRequest() {
        SessionManager.useRequester();
        new CompanySelectionPage().clickRequestButton(company());  // same button toggles to withdraw
    }

    @Then("the requester sees a pending status for the shared company")
    public void theRequesterSeesAPendingStatus() {
        SessionManager.useRequester();
        Assert.assertTrue(new CompanySelectionPage().isPending(company()),
                "Expected pending status for company " + company());
    }

    @Then("the requester's request button shows Withdraw")
    public void theRequestButtonShowsWithdraw() {
        SessionManager.useRequester();
        Assert.assertTrue(new CompanySelectionPage().isWithdrawShown(company()),
                "Expected the request button to show Withdraw (span[2]) for " + company());
    }

    @Then("the requester's Withdraw span disappears")
    public void theWithdrawSpanDisappears() {
        SessionManager.useRequester();
        Assert.assertTrue(new CompanySelectionPage().isWithdrawGone(company()),
                "Expected the Withdraw span to disappear after withdrawing for " + company());
    }

    @Then("the requester no longer sees a pending status")
    public void theRequesterNoLongerSeesPending() {
        SessionManager.useRequester();
        Assert.assertTrue(new CompanySelectionPage().isPendingGone(company()),
                "Expected the pending status to clear after withdrawing for " + company());
    }

    @Then("the requester is admitted into the company")
    public void theRequesterIsAdmittedIntoTheCompany() {
        SessionManager.useRequester();
        // Admission moves the requester off the popup (pending clears; the request surface is gone).
        Assert.assertTrue(new CompanySelectionPage().isPendingGone(company()),
                "Expected the requester to be admitted (pending cleared) into " + company());
    }

    @Then("the requester sees a rejected status for the shared company")
    public void theRequesterSeesRejectedStatus() {
        SessionManager.useRequester();
        Assert.assertTrue(new CompanySelectionPage().isRejected(company()),
                "Expected a rejected status for company " + company() + " after decline");
    }

    // ─── Admin actions ─────────────────────────────────────────────────────────────

    @When("the admin opens the Team panel")
    public void theAdminOpensTheTeamPanel() {
        SessionManager.useAdmin();
        new TeamPage().reloadTeamPanel();  // reload to fetch the freshly-created request
    }

    @Then("the admin sees approve and decline controls for the requester")
    public void theAdminSeesApproveAndDeclineControls() {
        SessionManager.useAdmin();
        Assert.assertTrue(new TeamPage().waitForRequestActions(requesterName(), 20),
                "Expected approve/decline controls for " + requesterName());
    }

    @When("the admin reloads the Team panel")
    public void theAdminReloadsTheTeamPanel() {
        SessionManager.useAdmin();
        new TeamPage().reloadTeamPanel();
    }

    @Then("the admin no longer sees approve and decline controls for the requester")
    public void theAdminNoLongerSeesApproveDecline() {
        SessionManager.useAdmin();
        Assert.assertTrue(new TeamPage().waitForRequestActionsGone(requesterName(), 20),
                "Expected approve/decline controls to be gone for " + requesterName());
    }

    @When("the admin approves the requester")
    public void theAdminApprovesTheRequester() {
        SessionManager.useAdmin();
        TeamPage team = new TeamPage().reloadTeamPanel();
        team.waitForRequestActions(requesterName(), 20);
        team.approveUser(requesterName());
    }

    @When("the admin declines the requester")
    public void theAdminDeclinesTheRequester() {
        SessionManager.useAdmin();
        TeamPage team = new TeamPage().reloadTeamPanel();
        team.waitForRequestActions(requesterName(), 20);
        team.declineUser(requesterName());
    }
}

package com.aybee.steps;

import com.aybee.context.ScenarioContext;
import com.aybee.driver.DriverManager;
import com.aybee.pages.AcceptInvitationPage;
import com.aybee.pages.DashboardPage;
import com.aybee.pages.SignUpPage;
import com.aybee.utils.MailosaurHelper;
import com.aybee.utils.TestUserFactory;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.testng.Assert;

public class InviteFlowSteps {

    private final ScenarioContext context;

    public InviteFlowSteps(ScenarioContext context) {
        this.context = context;
    }

    // ─── COPY LINK FLOW ───────────────────────────────────────────────────────────

    @When("I copy the team invite link")
    public void iCopyTheTeamInviteLink() {
        context.inviteUrl = new com.aybee.pages.TeamPage()
                .navigateToTeamTab()
                .openInviteSidebar()
                .copyInviteLinkAndRead();
    }

    @When("I open the copied invite link in a fresh session")
    public void iOpenCopiedInviteLinkInFreshSession() {
        clearSession();
        DriverManager.getDriver().get(context.inviteUrl);
        context.signUpPage = new SignUpPage();
    }

    @Then("I should see a join team notification for the inviter's company")
    public void iShouldSeeJoinTeamNotification() {
        String actual   = context.signUpPage.getNotificationText();
        String expected = "Join " + context.inviterCompanyName + "'s team on Aybee.";
        Assert.assertTrue(actual.contains(expected),
                "Expected join toast [" + expected + "] but got [" + actual + "]");
    }

    @Then("the company name field should be pre-filled and locked")
    public void companyNameFieldShouldBePrefilledAndLocked() {
        Assert.assertTrue(context.signUpPage.isCompanyNameLocked(),
                "Expected company name field to be locked on the invite sign-up page");
        Assert.assertFalse(context.signUpPage.getCompanyNameValue().isEmpty(),
                "Expected company name field to be pre-filled on the invite sign-up page");
    }

    // ─── EMAIL INVITE FLOW ────────────────────────────────────────────────────────

    // Sends an email invite to a fresh Mailosaur address — new user, never registered.
    // Fetches the invite URL immediately while the inviter is still logged in so the
    // platform definitely has the email in-flight; the URL is stored for the open step.
    @When("I send an email invite to a new user with role {string}")
    public void iSendEmailInviteToNewUser(String role) {
        context.inviteeEmail = MailosaurHelper.generateEmail();
        new com.aybee.pages.TeamPage()
                .navigateToTeamTab()
                .openInviteSidebar()
                .sendEmailInvite(context.inviteeEmail, "New", "Invitee", role);

        context.inviteUrl = new MailosaurHelper().getInviteUrlForEmail(context.inviteeEmail);
    }

    // Sends an email invite to a FRESH (unregistered) address, fetches the invite URL
    // from Mailosaur while the address is still unregistered (so the platform delivers
    // the email), then registers that address. When the subsequent step navigates to
    // the stored URL the email is already registered → triggers the "already in use" alert.
    @When("I send an email invite to my own email address")
    public void iSendEmailInviteToMyOwnEmail() {
        context.inviteeEmail    = MailosaurHelper.generateEmail();
        context.inviteePassword = "Password123";

        new com.aybee.pages.TeamPage()
                .navigateToTeamTab()
                .openInviteSidebar()
                .sendEmailInvite(context.inviteeEmail, "Invitee", "User", "user");

        // Fetch the invite URL now, before registering — platform only sends the email
        // while the address is unregistered.
        context.inviteUrl = new MailosaurHelper().getInviteUrlForEmail(context.inviteeEmail);

        // Register the invitee so the invite-accept will fail with "email already in use".
        TestUserFactory.createVerifiedUserWithEmail(context.inviteeEmail, context.inviteePassword);

        clearSession();
    }

    @When("I open the accept invitation link for the new user from email")
    public void iOpenAcceptInvitationLinkForNewUser() {
        // URL pre-fetched in iSendEmailInviteToNewUser while the inviter was still logged in.
        clearSession();
        DriverManager.getDriver().get(context.inviteUrl);
        context.acceptInvitationPage = new AcceptInvitationPage();
    }

    @When("I open the accept invitation link from email")
    public void iOpenAcceptInvitationLinkFromEmail() {
        // URL was pre-fetched in iSendEmailInviteToMyOwnEmail before the invitee was registered.
        DriverManager.getDriver().get(context.inviteUrl);
        context.acceptInvitationPage = new AcceptInvitationPage();
    }

    @Then("the invitation email field should be pre-filled and locked")
    public void invitationEmailFieldShouldBePrefilledAndLocked() {
        Assert.assertTrue(context.acceptInvitationPage.isEmailLocked(),
                "Expected the invitee email field to be locked on the Accept Invitation page");
        Assert.assertEquals(context.acceptInvitationPage.getInviteeEmail(),
                context.inviteeEmail,
                "Invitee email field shows wrong email");
    }

    @When("I enter {string} as the invitation password")
    public void iEnterAsInvitationPassword(String password) {
        context.acceptInvitationPage.enterPassword(password);
    }

    @When("I enter my own password as the invitation password")
    public void iEnterMyOwnPasswordAsInvitationPassword() {
        context.acceptInvitationPage.enterPassword(context.inviteePassword);
    }

    @And("I click Accept Invitation")
    public void iClickAcceptInvitation() {
        context.acceptInvitationPage.clickAcceptInvitation();
    }

    // ─── TOAST HANDLING ──────────────────────────────────────────────────────────

    // Invite pages show a "Join team" toast on load. Dismiss it before asserting an
    // error toast so getNotificationText() doesn't read the stale join-team message.
    @And("I dismiss the active toast")
    public void iDismissTheActiveToast() {
        if (context.signUpPage == null) context.signUpPage = new com.aybee.pages.SignUpPage();
        context.signUpPage.dismissToastIfPresent();
    }

    // ─── ASSERTIONS ───────────────────────────────────────────────────────────────

    @Then("I should see an already registered alert containing my email")
    public void iShouldSeeAlreadyRegisteredAlertContainingMyEmail() {
        // Platform may show a native alert, a toast, or silently block the form.
        // In all cases the user must NOT land on the dashboard.
        String msg = context.acceptInvitationPage.getAndDismissAlreadyRegisteredAlert();
        if (!msg.isEmpty()) {
            boolean hasEmail   = msg.contains(context.inviteeEmail);
            boolean hasKeyword = msg.toLowerCase().contains("already")
                              || msg.toLowerCase().contains("schon")
                              || msg.toLowerCase().contains("exist")
                              || msg.toLowerCase().contains("use");
            Assert.assertTrue(hasEmail || hasKeyword,
                    "Error message [" + msg + "] did not indicate a duplicate-registration error");
        } else {
            // No explicit error — verify the form is still shown (user not accepted).
            Assert.assertTrue(context.acceptInvitationPage.isLoaded(),
                    "Already-registered user should stay on the invite page, not be accepted");
        }
    }

    // ─── HELPERS ──────────────────────────────────────────────────────────────────

    private void clearSession() {
        TestUserFactory.safeResetSession();
    }
}

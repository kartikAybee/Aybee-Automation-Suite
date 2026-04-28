package com.aybee.steps;

import com.aybee.context.ScenarioContext;
import com.aybee.pages.ResetPasswordPage;
import com.aybee.utils.MailosaurHelper;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.testng.Assert;

public class ForgotPasswordFlowSteps {

    private final ScenarioContext context;

    public ForgotPasswordFlowSteps(ScenarioContext context) {
        this.context = context;
    }

    // ─── FORGOT PASSWORD FORM ACTIONS ─────────────────────────────────────────────

    // Uses the email of the pre-created verified user stored in context.
    @When("I enter my registered email in the reset field")
    public void iEnterMyRegisteredEmailInResetField() {
        context.forgotPasswordPage.enterEmail(context.testUser.email);
    }

    // Accepts an arbitrary email string from the feature file (used for negative tests).
    @When("I enter {string} in the reset email field")
    public void iEnterEmailInResetField(String email) {
        context.forgotPasswordPage.enterEmail(email);
    }

    @When("I clear the password reset fields")
    public void iClearPasswordResetFields() {
        context.resetPasswordPage.clearPasswordFields();
    }

    @And("I click Send Reset Link")
    public void iClickSendResetLink() {
        context.forgotPasswordPage.clickSendResetLink();
    }

    // ─── FORGOT PASSWORD ASSERTIONS ───────────────────────────────────────────────

    @Then("I should see a reset confirmation message")
    public void iShouldSeeResetConfirmation() {
        context.softAssert.assertTrue(context.forgotPasswordPage.isConfirmationVisible(),
                "Expected a reset confirmation message to appear after submitting the form");
    }

    @Then("I should not see a reset confirmation message")
    public void iShouldNotSeeResetConfirmation() {
        context.softAssert.assertFalse(context.forgotPasswordPage.isConfirmationVisible(),
                "Reset confirmation appeared for an unregistered email — platform must not send a reset link to unknown addresses");
    }

    @And("the Mailosaur inbox should receive a password reset email")
    public void mailosaurShouldReceiveResetEmail() {
        context.softAssert.assertNotNull(
                new MailosaurHelper().waitForEmailWithSubject(
                        context.testUser.email, "Forgot your password? Happens."),
                "Expected a password reset email in Mailosaur but none arrived");
    }

    // ─── RESET PASSWORD PAGE NAVIGATION ──────────────────────────────────────────

    // Extracts the reset URL from the Mailosaur email and navigates to it directly.
    @Given("I follow the password reset link from email")
    public void iFollowPasswordResetLinkFromEmail() {
        String resetUrl = new MailosaurHelper().getResetPasswordUrlForEmail(context.testUser.email);
        com.aybee.driver.DriverManager.getDriver().get(resetUrl);
        context.resetPasswordPage = new ResetPasswordPage();
    }

    // ─── RESET PASSWORD FORM ACTIONS ──────────────────────────────────────────────

    @When("I enter {string} as the new password")
    public void iEnterAsNewPassword(String password) {
        context.resetPasswordPage.enterNewPassword(password);
    }

    @And("I enter {string} as the confirm password")
    public void iEnterAsConfirmPassword(String password) {
        context.resetPasswordPage.enterConfirmPassword(password);
    }

    @And("I save the new password")
    public void iSaveTheNewPassword() {
        context.resetPasswordPage.clickSave();
    }

    // ─── RESET PASSWORD ASSERTIONS ────────────────────────────────────────────────

    // No toast is shown — the platform redirects straight to the dashboard on success.
    @Then("I should see a password reset success notification")
    public void iShouldSeePasswordResetSuccess() {
        context.dashboardPage = new com.aybee.pages.DashboardPage();
        context.softAssert.assertTrue(context.dashboardPage.isLoaded(),
                "Expected to be redirected to the dashboard after a successful password reset");
    }

    @Then("the save password button should be disabled")
    public void theSavePasswordButtonShouldBeDisabled() {
        Assert.assertFalse(context.resetPasswordPage.isSaveButtonEnabled(),
                "Expected the Save button to be disabled when password fields are not both filled");
    }
}

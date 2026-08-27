package com.aybee.steps;

import com.aybee.context.ScenarioContext;
import com.aybee.pages.DashboardPage;
import com.aybee.pages.ForgotPasswordPage;
import com.aybee.pages.OtpVerificationPage;
import com.aybee.pages.SignInPage;
import com.aybee.pages.SignUpPage;
import com.aybee.utils.TestUser;
import com.aybee.utils.TestUserFactory;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.testng.Assert;

public class AuthSteps {

    private final ScenarioContext context;

    public AuthSteps(ScenarioContext context) {
        this.context = context;
    }

    // ─── PAGE NAVIGATION SETUP ────────────────────────────────────────────────────

    @Given("I am on the sign up page")
    public void iAmOnTheSignUpPage() {
        context.signUpPage = new SignUpPage().navigateTo();
    }

    @Given("I am on the sign in page")
    public void iAmOnTheSignInPage() {
        context.signUpPage = new SignUpPage().navigateTo();
        context.signInPage = context.signUpPage.clickSignInLink();
    }

    @Given("I am on the forgot password page")
    public void iAmOnTheForgotPasswordPage() {
        context.signUpPage         = new SignUpPage().navigateTo();
        context.signInPage         = context.signUpPage.clickSignInLink();
        context.forgotPasswordPage = context.signInPage.clickForgotPassword();
    }

    // Reuses the run-shared verified account (created once) so we don't spawn a new company per
    // scenario, then clears the session so the scenario under test starts logged out.
    @Given("a verified user account exists")
    public void aVerifiedUserAccountExists() {
        context.testUser = TestUserFactory.getSharedVerifiedUser();
        // Creating the shared account (first call) leaves the browser logged in on the dashboard;
        // safeResetSession navigates away before clearing to avoid Bubble.io's alert.
        TestUserFactory.safeResetSession();
    }

    // Reuses the run-shared verified account and ensures the browser is logged in as it — for
    // scenarios that start from an active session (invite flows, etc.). On first use the shared
    // account was just created and is already on the dashboard; on reuse the session was cleared,
    // so sign in with the account's CURRENT password (reset scenario may have changed it).
    @Given("I am logged in as a verified user")
    public void iAmLoggedInAsAVerifiedUser() {
        TestUser user = TestUserFactory.getSharedVerifiedUser();
        context.testUser           = user;
        context.inviterCompanyName = user.company;

        // Always sign in as THE shared account. A prior scenario may have left a DIFFERENT user
        // (e.g. an invitee who just accepted an invitation) logged in on a dashboard, so merely
        // "is a dashboard shown?" is not enough — we'd otherwise drive the Team page as that user
        // and the invite sidebar/fields would never appear. Reset the session and log in cleanly.
        TestUserFactory.safeResetSession();
        context.signInPage = new SignUpPage().navigateTo().clickSignInLink();
        context.signInPage.enterEmail(user.email).enterPassword(user.password).clickSignIn();
        DashboardPage dashboard = new DashboardPage();
        dashboard.isLoaded();
        context.dashboardPage = dashboard;
    }


    // Reuses the OTP page from the previous scenario if it is still loaded — avoids
    // repeating a full sign-up flow when the browser is already on the activation page.
    @Given("I am on the OTP activation page")
    public void iAmOnTheOtpActivationPage() {
        // Always start from a clean, logged-out session so the sign-up page actually loads. A
        // leftover session from a prior scenario redirects BASE_URL to the company-selection popup
        // (or dashboard) instead of sign-up, and signup-companyname never appears. (The old
        // context.otpPage reuse never triggered — ScenarioContext is per-scenario — and couldn't
        // carry context.testUser across scenarios anyway, so a fresh sign-up is the correct path.)
        TestUserFactory.safeResetSession();
        TestUser user = TestUserFactory.generateUser();
        context.testUser = user;
        new SignUpPage()
                .navigateTo()
                .enterCompanyName(user.company)
                .enterFirstName(user.firstName)
                .enterLastName(user.lastName)
                .enterEmail(user.email)
                .enterPassword(user.password)
                .clickSignUp();
        context.otpPage = new OtpVerificationPage();
        Assert.assertTrue(context.otpPage.isLoaded(), "OTP activation page did not appear after sign-up");
    }

    // ─── PAGE LINK CLICKS ─────────────────────────────────────────────────────────

    @When("I click the Sign In link")
    public void iClickTheSignInLink() {
        context.signInPage = context.signUpPage.clickSignInLink();
    }

    @When("I click the Sign Up link")
    public void iClickTheSignUpLink() {
        context.signUpPage = context.signInPage.clickSignUpLink();
    }

    @When("I click the Forgot Password link")
    public void iClickTheForgotPasswordLink() {
        context.forgotPasswordPage = context.signInPage.clickForgotPassword();
    }

    // ─── PAGE PRESENCE ASSERTIONS ────────────────────────────────────────────────

    @Then("I should be on the sign up page")
    public void iShouldBeOnTheSignUpPage() {
        if (context.signUpPage == null) context.signUpPage = new SignUpPage();
        Assert.assertTrue(context.signUpPage.isLoaded(), "Expected to be on the Sign Up page");
    }

    @Then("I should be on the sign in page")
    public void iShouldBeOnTheSignInPage() {
        if (context.signInPage == null) context.signInPage = new SignInPage();
        Assert.assertTrue(context.signInPage.isLoaded(), "Expected to be on the Sign In page");
    }

    @Then("I should be on the forgot password page")
    public void iShouldBeOnTheForgotPasswordPage() {
        if (context.forgotPasswordPage == null) context.forgotPasswordPage = new ForgotPasswordPage();
        Assert.assertTrue(context.forgotPasswordPage.isLoaded(), "Expected to be on the Forgot Password page");
    }

    @Then("I should be on the OTP activation page")
    public void iShouldBeOnTheOtpActivationPage() {
        if (context.otpPage == null) context.otpPage = new com.aybee.pages.OtpVerificationPage();
        Assert.assertTrue(context.otpPage.isLoaded(),
                "Expected to be redirected to the OTP activation page but the security-code field was not found");
    }

    // ─── OTP BUTTON ASSERTION ─────────────────────────────────────────────────────

    @Then("the OTP next button should be disabled")
    public void theOtpNextButtonShouldBeDisabled() {
        if (context.otpPage == null) context.otpPage = new com.aybee.pages.OtpVerificationPage();
        Assert.assertFalse(context.otpPage.isNextButtonEnabled(),
                "Expected the OTP Next button to be disabled");
    }

    // ─── BUTTON STATE ASSERTIONS ──────────────────────────────────────────────────

    @Then("the sign up button should be disabled")
    public void theSignUpButtonShouldBeDisabled() {
        Assert.assertFalse(context.signUpPage.isSignUpButtonEnabled(),
                "Expected the Sign Up button to be disabled");
    }

    @Then("the sign in button should be disabled")
    public void theSignInButtonShouldBeDisabled() {
        Assert.assertFalse(context.signInPage.isSignInButtonEnabled(),
                "Expected the Sign In button to be disabled");
    }

    @Then("the send reset link button should be disabled")
    public void theSendResetLinkButtonShouldBeDisabled() {
        Assert.assertFalse(context.forgotPasswordPage.isSendResetButtonEnabled(),
                "Expected the Send Reset Link button to be disabled");
    }

    @Then("the Continue with Google button should be visible on the sign up page")
    public void googleButtonVisibleOnSignUpPage() {
        Assert.assertTrue(context.signUpPage.isContinueWithGoogleVisible(),
                "Continue with Google button was not visible on the Sign Up page");
    }

    @Then("the Continue with Google button should be visible on the sign in page")
    public void googleButtonVisibleOnSignInPage() {
        Assert.assertTrue(context.signInPage.isContinueWithGoogleVisible(),
                "Continue with Google button was not visible on the Sign In page");
    }
}

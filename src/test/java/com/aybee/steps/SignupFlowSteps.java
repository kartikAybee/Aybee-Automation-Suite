package com.aybee.steps;

import com.aybee.context.ScenarioContext;
import com.aybee.pages.DashboardPage;
import com.aybee.pages.OnboardingPage;
import com.aybee.pages.OtpVerificationPage;
import com.aybee.pages.SignUpPage;
import com.aybee.utils.ConfigReader;
import com.aybee.utils.GoogleAuthHelper;
import com.aybee.utils.MailosaurHelper;
import com.aybee.utils.Notifications;
import com.aybee.utils.TestUser;
import com.aybee.utils.TestUserFactory;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.testng.Assert;

import java.util.Map;

public class SignupFlowSteps {

    private final ScenarioContext context;

    public SignupFlowSteps(ScenarioContext context) {
        this.context = context;
    }

    // ─── ENTRY POINT NAVIGATION ───────────────────────────────────────────────────

    @Given("I navigate to sign up via {string}")
    public void iNavigateToSignUpVia(String method) {
        context.signUpPage = new SignUpPage().navigateTo();
    }

    // ─── SIGN UP FORM ACTIONS ─────────────────────────────────────────────────────

    // Accepts a DataTable with keys: firstName, lastName, password (required) and optionally email.
    // The company is never taken from the table: on a direct signup the field is editable, so we
    // generate a fresh MLSR company (DB-filterable, and reused as-is when create-company is clicked);
    // on an invite signup the field is pre-filled + locked, so we leave it alone.
    @When("I fill in the sign up form:")
    public void iFillInSignUpForm(DataTable dataTable) {
        Map<String, String> data = dataTable.asMap(String.class, String.class);
        String email = data.containsKey("email")
                ? data.get("email")                         // explicit (e.g. invalid email test)
                : MailosaurHelper.generateEmail();          // always fresh — never reuse a prior user

        String company = enterMlsrCompanyIfEditable();
        context.testUser = new TestUser(
                email, data.get("password"), company, data.get("firstName"), data.get("lastName"));
        context.signUpPage
                .enterFirstName(context.testUser.firstName)
                .enterLastName(context.testUser.lastName)
                .enterEmail(context.testUser.email)
                .enterPassword(context.testUser.password);
    }

    // Reuses the email from context.testUser; other fields come from the DataTable.
    @When("I attempt to sign up with the same email and:")
    public void iAttemptSignUpWithSameEmailAnd(DataTable dataTable) {
        Map<String, String> data = dataTable.asMap(String.class, String.class);
        enterMlsrCompanyIfEditable();
        context.signUpPage
                .enterFirstName(data.get("firstName"))
                .enterLastName(data.get("lastName"))
                .enterEmail(context.testUser.email)
                .enterPassword(data.get("password"));
    }

    // Enters a generated MLSR company only when the field is editable (direct signup); on invite
    // signups it is pre-filled + locked, so this is a no-op. Returns the entered name (or "").
    private String enterMlsrCompanyIfEditable() {
        if (context.signUpPage.isCompanyNameLocked()) return "";
        String company = TestUserFactory.mlsrCompanyName();
        context.signUpPage.enterCompanyName(company);
        return company;
    }

    // Fills the sign up form using the configured Google test account email/password.
    // company, firstName, lastName come from the DataTable.
    @When("I attempt manual sign up using the Google account email:")
    public void iAttemptManualSignUpUsingGoogleEmail(DataTable dataTable) {
        Map<String, String> data = dataTable.asMap(String.class, String.class);
        context.signUpPage
                .enterCompanyName(data.get("company"))
                .enterFirstName(data.get("firstName"))
                .enterLastName(data.get("lastName"))
                .enterEmail(ConfigReader.get("GOOGLE_TEST_EMAIL"))
                .enterPassword(ConfigReader.get("GOOGLE_TEST_PASSWORD"));
    }

    @And("I click Sign Up")
    public void iClickSignUp() {
        context.signUpPage.clickSignUp();
    }

    @And("I enter the OTP received on my email")
    public void iEnterOtpReceivedOnMyEmail() {
        context.otpPage = new OtpVerificationPage();
        Assert.assertTrue(context.otpPage.isLoaded(),
                "OTP verification screen did not appear");
        String otp = new MailosaurHelper().getOtpForEmail(context.testUser.email);
        context.otpPage.enterOtp(otp);
    }

    @And("I click Next to activate my account")
    public void iClickNextToActivate() {
        context.otpPage.clickActivate();
    }

    // Direct registrations land on the company-selection popup after activation, before onboarding.
    // The new company reuses the name entered at sign up (MLSR) — the popup only has a button, no
    // name input. Bubble then redirects to the onboarding questions.
    @And("I create a new company")
    public void iCreateANewCompany() {
        new com.aybee.pages.CompanySelectionPage().waitUntilLoaded().createCompany();
    }

    @And("I complete the onboarding questions if displayed")
    public void iCompleteOnboardingQuestionsIfDisplayed() {
        new OnboardingPage().completeIfPresent();
    }

    @When("I click Cancel on the OTP activation page")
    public void iClickCancelOnOtpPage() {
        if (context.otpPage == null) context.otpPage = new OtpVerificationPage();
        context.signUpPage = context.otpPage.clickCancel();
    }

    // Simulates closing the tab or navigating away without cancelling — the account
    // remains in a pending-activation state so the app can resume it on next sign-in.
    @When("I abandon the activation page and navigate to the home page")
    public void iAbandonActivationPageAndNavigateToHomePage() {
        context.signUpPage = new SignUpPage().navigateTo();
    }

    @And("I enter OTP {string} on the activation page")
    public void iEnterOtpOnActivationPage(String otp) {
        if (context.otpPage == null) context.otpPage = new OtpVerificationPage();
        context.otpPage.enterOtp(otp);
        context.otpPage.clickActivate();
    }

    // Types a value into the OTP field without clicking Next — for button-state assertions.
    // Pass an empty string to check the button state without touching the field.
    @When("I type {string} in the OTP field")
    public void iTypeInOtpField(String value) {
        if (context.otpPage == null) context.otpPage = new OtpVerificationPage();
        Assert.assertTrue(context.otpPage.isLoaded(),
                "Expected to be on the OTP activation page but the OTP field was not visible");
        if (!value.isEmpty()) {
            context.otpPage.typeInOtpField(value);
        }
    }

    // ─── NAVIGATION ───────────────────────────────────────────────────────────────

    @When("I navigate to the sign up page")
    public void iNavigateToSignUpPage() {
        context.signUpPage = new SignUpPage().navigateTo();
    }

    @When("I navigate to the sign in page")
    public void iNavigateToSignInPage() {
        context.signUpPage = new SignUpPage().navigateTo();
        context.signInPage = context.signUpPage.clickSignInLink();
    }

    // ─── SIGN IN ACTIONS ──────────────────────────────────────────────────────────

    // Enters email + password from context and clicks Sign In.
    @When("I sign in with my registered credentials")
    public void iSignInWithMyRegisteredCredentials() {
        context.signInPage
                .enterEmail(context.testUser.email)
                .enterPassword(context.testUser.password)
                .clickSignIn();
        context.dashboardPage = new DashboardPage();
    }

    // Enters email from context, the given password, and clicks Sign In.
    @When("I sign in with my registered email and password {string}")
    public void iSignInWithRegisteredEmailAndPassword(String password) {
        context.signInPage
                .enterEmail(context.testUser.email)
                .enterPassword(password)
                .clickSignIn();
    }

    // Enters arbitrary email + password from the feature file and clicks Sign In.
    @When("I sign in with email {string} and password {string}")
    public void iSignInWithEmailAndPassword(String email, String password) {
        context.signInPage
                .enterEmail(email)
                .enterPassword(password)
                .clickSignIn();
    }

    // Fills email and password on the sign in page without clicking Sign In.
    // Used for validation scenarios where the button is expected to remain disabled.
    @When("I fill in the sign in form with email {string} and password {string}")
    public void iFillInSignInForm(String email, String password) {
        context.signInPage
                .enterEmail(email)
                .enterPassword(password);
    }

    @And("I click Sign In")
    public void iClickSignIn() {
        context.signInPage.clickSignIn();
    }

    // Uses GOOGLE_TEST_EMAIL + GOOGLE_TEST_PASSWORD from config.
    @When("I attempt to sign in manually with the Google account credentials")
    public void iAttemptSignInManuallyWithGoogleCredentials() {
        context.signInPage
                .enterEmail(ConfigReader.get("GOOGLE_TEST_EMAIL"))
                .enterPassword(ConfigReader.get("GOOGLE_TEST_PASSWORD"))
                .clickSignIn();
    }

    // ─── LOGOUT ───────────────────────────────────────────────────────────────────

    @When("I log out")
    public void iLogOut() {
        context.dashboardPage = new DashboardPage();
        context.signUpPage = context.dashboardPage.logout();
    }

    // ─── GOOGLE AUTH ──────────────────────────────────────────────────────────────

    @When("I click Continue with Google on the sign up page")
    public void iClickContinueWithGoogleOnSignUpPage() {
        context.googleEmail = ConfigReader.get("GOOGLE_TEST_EMAIL");
        context.signUpPage.clickContinueWithGoogle();
    }

    @When("I click Continue with Google on the sign in page")
    public void iClickContinueWithGoogleOnSignInPage() {
        context.googleEmail = ConfigReader.get("GOOGLE_TEST_EMAIL");
        context.signInPage.clickContinueWithGoogle();
    }

    @And("I complete Google authentication as a new user")
    public void iCompleteGoogleAuthAsNewUser() {
        GoogleAuthHelper.handleGoogleAuthPopup();
        context.dashboardPage = new DashboardPage();
    }

    @When("I attempt to sign up with Google using the same email")
    public void iAttemptSignUpWithGoogleSameEmail() {
        context.signUpPage.clickContinueWithGoogle();
        GoogleAuthHelper.handleGoogleAuthPopup();
    }

    @And("I attempt to sign in with Google using the same email")
    public void iAttemptSignInWithGoogleSameEmail() {
        context.signInPage.clickContinueWithGoogle();
        GoogleAuthHelper.handleGoogleAuthPopup();
    }

    @When("I sign in with Google using the same Google account")
    public void iSignInWithGoogleSameAccount() {
        context.signInPage.clickContinueWithGoogle();
        GoogleAuthHelper.handleGoogleAuthPopup();
        context.dashboardPage = new DashboardPage();
    }

    // ─── ASSERTIONS ───────────────────────────────────────────────────────────────

    @Then("I should be on the dashboard")
    public void iShouldBeOnTheDashboard() {
        if (context.dashboardPage == null) context.dashboardPage = new DashboardPage();
        Assert.assertTrue(context.dashboardPage.isLoaded(),
                "Expected to be on the dashboard but the dashboard container was not visible");
    }

    @Then("I should see a notification matching {string}")
    public void iShouldSeeNotificationMatching(String constantName) {
        String expected = resolveNotification(constantName);
        String actual   = new DashboardPage().getNotificationText();
        context.softAssert.assertTrue(actual.contains(expected),
                "Expected notification containing [" + expected + "] but got [" + actual + "]");
    }

    private String resolveNotification(String constantName) {
        switch (constantName) {
            case "EMAIL_ALREADY_REGISTERED":             return Notifications.EMAIL_ALREADY_REGISTERED;
            case "INVALID_SIGNIN_CREDENTIALS":           return Notifications.INVALID_SIGNIN_CREDENTIALS;
            case "GOOGLE_SIGNUP_BLOCKED_EMAIL_ACCOUNT":  return Notifications.GOOGLE_SIGNUP_BLOCKED_EMAIL_ACCOUNT;
            case "GOOGLE_SIGNIN_BLOCKED_EMAIL_ACCOUNT":  return Notifications.GOOGLE_SIGNIN_BLOCKED_EMAIL_ACCOUNT;
            case "MANUAL_SIGNUP_BLOCKED_GOOGLE_ACCOUNT": return Notifications.MANUAL_SIGNUP_BLOCKED_GOOGLE_ACCOUNT;
            case "GOOGLE_SIGNUP_BLOCKED_GOOGLE_ACCOUNT": return Notifications.GOOGLE_SIGNUP_BLOCKED_GOOGLE_ACCOUNT;
            case "MANUAL_SIGNIN_BLOCKED_GOOGLE_ACCOUNT": return Notifications.MANUAL_SIGNIN_BLOCKED_GOOGLE_ACCOUNT;
            case "TEAM_INVITE_EMAIL_ALREADY_EXISTS":     return Notifications.TEAM_INVITE_EMAIL_ALREADY_EXISTS;
            case "PERSONAL_INVITE_GOOGLE_BLOCKED":       return Notifications.PERSONAL_INVITE_GOOGLE_BLOCKED;
            case "INVALID_OTP":                          return Notifications.INVALID_OTP;
            case "PASSWORDS_DO_NOT_MATCH":               return Notifications.PASSWORDS_DO_NOT_MATCH;
            default: throw new IllegalArgumentException("Unknown notification constant: " + constantName);
        }
    }
}

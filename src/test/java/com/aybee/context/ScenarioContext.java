package com.aybee.context;

import com.aybee.pages.AcceptInvitationPage;
import com.aybee.pages.DashboardPage;
import com.aybee.pages.ForgotPasswordPage;
import com.aybee.pages.OtpVerificationPage;
import com.aybee.pages.ResetPasswordPage;
import com.aybee.pages.SignInPage;
import com.aybee.pages.SignUpPage;
import com.aybee.utils.ScreenshotSoftAssert;
import com.aybee.utils.TestUser;
import org.testng.asserts.SoftAssert;

public class ScenarioContext {

    // Collects non-fatal assertion failures — flushed in @After so the scenario
    // continues through all steps even when a soft check fails. ScreenshotSoftAssert
    // attaches a screenshot inline at the failing body step the instant each check fails.
    public final SoftAssert softAssert = new ScreenshotSoftAssert();

    // ─── Test data ────────────────────────────────────────────────────────────────
    public TestUser testUser;
    public String   googleEmail;
    public String   inviteUrl;           // URL copied from the invite sidebar or pre-fetched from email
    public String   inviteeEmail;        // email used for the email invite
    public String   inviteePassword;     // password for the invitee account (email invite already-registered flow)
    public String   inviterCompanyName;  // company name read from the inviter's profile

    // ─── Join-request flow ──────────────────────────────────────────────────────
    public TestUser joinRequester;       // the new user requesting to join (unique name → clash-free ids)
    public String   joinCompany;         // the shared company C being requested/approved into

    // ─── Page objects (shared across step classes via PicoContainer injection) ───
    public SignUpPage            signUpPage;
    public SignInPage            signInPage;
    public ForgotPasswordPage    forgotPasswordPage;
    public ResetPasswordPage     resetPasswordPage;
    public AcceptInvitationPage  acceptInvitationPage;
    public OtpVerificationPage   otpPage;
    public DashboardPage         dashboardPage;
}

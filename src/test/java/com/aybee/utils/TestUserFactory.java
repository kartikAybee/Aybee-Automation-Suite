package com.aybee.utils;

import com.aybee.driver.DriverManager;
import com.aybee.pages.CompanySelectionPage;
import com.aybee.pages.OnboardingPage;
import com.aybee.pages.OtpVerificationPage;
import com.aybee.pages.SignUpPage;

public class TestUserFactory {

    // ── Company naming ──────────────────────────────────────────────────────────
    // Every company this suite creates is named MLSR<short-unique-token> so the DB can be
    // filtered/cleaned with a single `name LIKE 'MLSR%'`. The token is a base36 timestamp plus
    // random base36 chars — short (username-like), sortable, and effectively impossible to repeat.
    private static final java.security.SecureRandom RAND = new java.security.SecureRandom();
    private static final String B36 = "0123456789abcdefghijklmnopqrstuvwxyz";

    public static String mlsrCompanyName() {
        StringBuilder sb = new StringBuilder("MLSR-");
        sb.append(Long.toString(System.currentTimeMillis(), 36));
        for (int i = 0; i < 4; i++) sb.append(B36.charAt(RAND.nextInt(B36.length())));
        return sb.toString();
    }

    // ── Shared account cache ────────────────────────────────────────────────────
    // One verified account+company is created lazily and reused for the whole run by every
    // scenario that only needs "a verified user" (sign-in, forgot-password link request, invite
    // inviter) — instead of creating a new company each time. Its `password` field is the single
    // source of truth for the current password (the reset scenario updates it in place).
    private static TestUser sharedUser;

    public static synchronized TestUser getSharedVerifiedUser() {
        if (sharedUser == null) {
            sharedUser = createVerifiedUser();
        }
        return sharedUser;
    }

    // Creates a fully verified Aybee account via the sign-up UI + Mailosaur OTP.
    // Clears cookies/storage first so stale sessions from previous scenarios never
    // cause BASE_URL to redirect to the OTP activation page instead of sign-up.
    public static TestUser createVerifiedUser() {
        TestUser user = generateUser();

        safeResetSession();

        new SignUpPage()
                .navigateTo()
                .enterCompanyName(user.company)
                .enterFirstName(user.firstName)
                .enterLastName(user.lastName)
                .enterEmail(user.email)
                .enterPassword(user.password)
                .clickSignUp();

        MailosaurHelper mailosaur = new MailosaurHelper();
        String otp = mailosaur.getOtpForEmail(user.email);

        new OtpVerificationPage()
                .enterOtp(otp)
                .clickActivate();

        // New registrations now land on the company-selection popup before onboarding.
        // This is the FIRST company, so create it (C1); Bubble then redirects to onboarding.
        new CompanySelectionPage().waitUntilLoaded().createCompany();

        new OnboardingPage().completeRequired();

        return user;
    }

    // Registers an account through signup + OTP and STOPS on the company-selection popup — the
    // account is verified/registered but joins no company and never onboards. Used where a test
    // only needs a registered email (e.g. the already-registered invite check) or as the start
    // state for a join-request (the requester then clicks a company's request button).
    public static TestUser registerToCompanySelection(String email, String password) {
        return registerToCompanySelection(
                new TestUser(email, password, mlsrCompanyName(), "Test", "User"));
    }

    // A uniquely-named requester for the join-request flow: its first/last name feed the
    // "<NAME>-approve-user" / "<NAME>-decline-user" ids on the admin's Team panel, so the name
    // MUST be unique per run to avoid id clashes. Registers and stops on the company-selection popup.
    public static TestUser createJoinRequester() {
        String token = Long.toString(System.currentTimeMillis(), 36);
        TestUser user = new TestUser(
                MailosaurHelper.generateEmail(), "Test@1234", mlsrCompanyName(), "QA" + token, "Req");
        return registerToCompanySelection(user);
    }

    private static TestUser registerToCompanySelection(TestUser user) {
        safeResetSession();

        new SignUpPage()
                .navigateTo()
                .enterCompanyName(user.company)
                .enterFirstName(user.firstName)
                .enterLastName(user.lastName)
                .enterEmail(user.email)
                .enterPassword(user.password)
                .clickSignUp();

        String otp = new MailosaurHelper().getOtpForEmailWithSubject(user.email, "Your Aybee code");

        new OtpVerificationPage()
                .enterOtp(otp)
                .clickActivate();

        new CompanySelectionPage().waitUntilLoaded();  // stop here — registered, on the popup
        return user;
    }

    // Safely wipes session state without triggering Bubble.io's "session changed" alert.
    // deleteAllCookies() is domain-scoped and must run while on the Aybee domain.
    // Navigating away unloads Bubble.io's storage listener so subsequent localStorage
    // clears (done by the caller's next navigateTo / get call) don't fire a native alert.
    public static void safeResetSession() {
        DriverManager.getDriver().manage().deleteAllCookies();
        DriverManager.getDriver().get("about:blank");
    }

    public static TestUser generateUser() {
        return new TestUser(
                MailosaurHelper.generateEmail(),
                "Test@1234",
                mlsrCompanyName(),
                "Test",
                "User"
        );
    }
}

package com.aybee.utils;

import com.aybee.driver.DriverManager;
import com.aybee.pages.OnboardingPage;
import com.aybee.pages.OtpVerificationPage;
import com.aybee.pages.SignUpPage;

public class TestUserFactory {

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

        new OnboardingPage().completeRequired();

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

    public static TestUser createVerifiedUserWithEmail(String email, String password) {
        TestUser user = new TestUser(email, password, "Co" + (System.currentTimeMillis() % 1_000_000L), "Test", "Invitee");

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

        new OnboardingPage().completeRequired();

        return user;
    }

    public static TestUser generateUser() {
        return new TestUser(
                MailosaurHelper.generateEmail(),
                "Test@1234",
                "Co" + (System.currentTimeMillis() % 1_000_000L),
                "Test",
                "User"
        );
    }
}

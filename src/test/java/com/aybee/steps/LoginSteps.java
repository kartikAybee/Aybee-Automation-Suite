package com.aybee.steps;

import com.aybee.context.ScenarioContext;
import com.aybee.pages.LandingPage;
import com.aybee.pages.OnboardingPage;
import com.aybee.pages.OtpVerificationPage;
import com.aybee.pages.SignInPage;
import com.aybee.pages.SignUpPage;
import com.aybee.utils.ConfigReader;
import com.aybee.utils.MailosaurHelper;
import io.cucumber.java.en.Given;
import org.testng.Assert;

public class LoginSteps {

    private final ScenarioContext context;

    public LoginSteps(ScenarioContext context) {
        this.context = context;
    }

    // Branches on USE_NEW_USER (config.properties or env var):
    //   false (default) → sign in with VALID_EMAIL / VALID_PASSWORD (existing account)
    //   true            → generate a Mailosaur address, sign up, verify OTP, complete onboarding
    @Given("I am signed in as a valid user")
    public void iAmSignedInAsAValidUser() {
        if (ConfigReader.getBoolean("USE_NEW_USER", false)) {
            signUpNewUser();
        } else {
            signInExistingUser();
        }
    }

    private void signInExistingUser() {
        LandingPage landing = new LandingPage();
        landing.navigateTo();

        SignInPage signIn = landing.clickSignInToggle();
        signIn.enterEmail(ConfigReader.get("VALID_EMAIL"))
              .enterPassword(ConfigReader.get("VALID_PASSWORD"))
              .clickSignIn();

        Assert.assertTrue(signIn.isDashboardLoaded(),
                "Dashboard did not load after sign-in");
    }

    private void signUpNewUser() {
        String email     = MailosaurHelper.generateEmail();
        String password  = ConfigReader.get("VALID_PASSWORD");
        String company   = ConfigReader.get("SIGNUP_COMPANY", "Test Company");
        String firstName = ConfigReader.get("SIGNUP_FIRSTNAME", "Test");
        String lastName  = ConfigReader.get("SIGNUP_LASTNAME", "User");

        SignUpPage signUp = new SignUpPage();
        signUp.navigateTo();
        signUp.enterCompanyName(company)
              .enterFirstName(firstName)
              .enterLastName(lastName)
              .enterEmail(email)
              .enterPassword(password)
              .clickSignUp();

        OtpVerificationPage otpPage = new OtpVerificationPage();
        Assert.assertTrue(otpPage.isLoaded(),
                "OTP verification screen did not appear after sign-up for: " + email);

        String otp = new MailosaurHelper().getOtpForEmail(email);
        System.out.println("[SignUp] OTP retrieved for: " + email);

        otpPage.enterOtp(otp)
               .clickActivate();

        new OnboardingPage().completeRequired();

        SignInPage signIn = new SignInPage();
        Assert.assertTrue(signIn.isDashboardLoaded(),
                "Dashboard did not load after sign-up and onboarding for: " + email);
    }
}

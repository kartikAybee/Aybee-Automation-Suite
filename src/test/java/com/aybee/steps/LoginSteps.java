package com.aybee.steps;

import com.aybee.context.ScenarioContext;
import com.aybee.pages.LandingPage;
import com.aybee.pages.SignInPage;
import com.aybee.utils.ConfigReader;
import io.cucumber.java.en.Given;
import org.testng.Assert;

public class LoginSteps {

    private final ScenarioContext context;

    public LoginSteps(ScenarioContext context) {
        this.context = context;
    }

    @Given("I am signed in as a valid user")
    public void iAmSignedInAsAValidUser() {
        LandingPage landing = new LandingPage();
        landing.navigateTo();

        SignInPage signIn = landing.clickSignInToggle();
        signIn.enterEmail(ConfigReader.get("VALID_EMAIL"))
              .enterPassword(ConfigReader.get("VALID_PASSWORD"))
              .clickSignIn();

        Assert.assertTrue(signIn.isDashboardLoaded(),
                "Dashboard did not load after sign-in");
    }
}

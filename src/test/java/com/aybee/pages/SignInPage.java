package com.aybee.pages;

import io.qameta.allure.Step;
import org.openqa.selenium.By;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

public class SignInPage extends BasePage {

    private final By emailField              = By.id("login-email");
    private final By passwordField           = By.id("login-password");
    private final By signInButton            = By.id("btn-signin");
    // navigator_projects_button appears in the nav bar after a successful sign-in.
    // It is used both as the "dashboard loaded" indicator and as the experiments-page entry point.
    private final By navigatorProjectsButton = By.id("navigator_projects_button");

    @Step("Enter sign-in email: {email}")
    public SignInPage enterEmail(String email) {
        type(emailField, email);
        return this;
    }

    @Step("Enter sign-in password")
    public SignInPage enterPassword(String password) {
        type(passwordField, password);
        return this;
    }

    @Step("Click Sign In button")
    public void clickSignIn() {
        // blurActiveElement() fires Bubble.io's reactive validation on the password field
        // so the sign-in button transitions from ghost → enabled before clickWhenEnabled() polls.
        blurActiveElement();
        clickWhenEnabled(signInButton);
    }

    // Waits up to 30 s for navigator_projects_button — the post-login landmark.
    // Returns true when visible; returns false on timeout so callers can assert with a message.
    public boolean isDashboardLoaded() {
        try {
            new WebDriverWait(driver, 30)
                    .until(ExpectedConditions.visibilityOfElementLocated(navigatorProjectsButton));
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean isLoaded() {
        return isElementVisible(signInButton);
    }

    public boolean isSignInButtonEnabled() {
        return isButtonEnabled(signInButton);
    }

    // Returns true when sign-in page is still visible and the dashboard has NOT appeared.
    // Used to assert that a failed sign-in did not inadvertently proceed.
    public boolean isStillOnSignInPage() {
        return isElementVisible(signInButton) && !isElementPresent(navigatorProjectsButton);
    }
}

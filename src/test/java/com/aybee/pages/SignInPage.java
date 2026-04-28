package com.aybee.pages;

import io.qameta.allure.Step;
import org.openqa.selenium.By;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

public class SignInPage extends BasePage {

    private final By emailField         = By.id("login-email");
    private final By passwordField      = By.id("login-password");
    private final By signInButton       = By.id("btn-signin");
    private final By forgotPasswordLink = By.id("toggle-forgotpassword");
    private final By continueWithGoogle = By.id("btn-login-google");
    private final By signUpLink         = By.id("toggle-sign-up");
    private final By pageTitle          = By.id("login-title");

    @Step("Enter sign in email: {email}")
    public SignInPage enterEmail(String email) {
        type(emailField, email);
        return this;
    }

    @Step("Enter sign in password")
    public SignInPage enterPassword(String password) {
        type(passwordField, password);
        return this;
    }

    @Step("Click Sign In button")
    public void clickSignIn() {
        blurActiveElement();
        clickWhenEnabled(signInButton);
    }

    @Step("Click Forgot Password link")
    public ForgotPasswordPage clickForgotPassword() {
        jsClick(forgotPasswordLink);
        ForgotPasswordPage page = new ForgotPasswordPage();
        page.isLoaded();
        return page;
    }

    @Step("Click Continue with Google on sign in page")
    public void clickContinueWithGoogle() {
        click(continueWithGoogle);
    }

    @Step("Click Sign Up link on sign in page")
    public SignUpPage clickSignUpLink() {
        jsClick(signUpLink);
        SignUpPage page = new SignUpPage();
        page.isLoaded();
        return page;
    }

    public boolean isLoaded() {
        return isElementVisible(signInButton);
    }

    public boolean isSignInButtonEnabled() {
        return isButtonEnabled(signInButton);
    }

    public boolean isContinueWithGoogleVisible() {
        return isElementVisible(continueWithGoogle);
    }

    public boolean isSignedInSuccessfully() {
        try {
            new WebDriverWait(driver, 10)
                    .until(ExpectedConditions.invisibilityOfElementLocated(signInButton));
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}

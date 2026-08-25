package com.aybee.pages;

import com.aybee.utils.ConfigReader;
import io.qameta.allure.Step;
import org.openqa.selenium.By;

public class SignUpPage extends BasePage {

    private final By companyNameField   = By.id("signup-companyname");
    private final By firstNameField     = By.id("signup-firstname");
    private final By lastNameField      = By.id("signup-lastname");
    private final By emailField         = By.id("signup-email");
    private final By passwordField      = By.id("signup-password");
    private final By signUpButton       = By.id("btn-signup");
    private final By continueWithGoogle = By.id("btn-signup-google");
    private final By signInLink         = By.id("toggle-sign-in");
    private final By pageTitle          = By.id("create-account-title");

    @Step("Navigate to Sign Up page")
    public SignUpPage navigateTo() {
        driver.get(ConfigReader.get("BASE_URL"));
        wait.until(d -> isElementPresent(companyNameField));
        return this;
    }

    @Step("Enter company name: {companyName}")
    public SignUpPage enterCompanyName(String companyName) {
        type(companyNameField, companyName);
        return this;
    }

    @Step("Enter first name: {firstName}")
    public SignUpPage enterFirstName(String firstName) {
        type(firstNameField, firstName);
        return this;
    }

    @Step("Enter last name: {lastName}")
    public SignUpPage enterLastName(String lastName) {
        type(lastNameField, lastName);
        return this;
    }

    @Step("Enter email: {email}")
    public SignUpPage enterEmail(String email) {
        type(emailField, email);  // type() force-clears first (handles Chrome/Bubble prefill)
        return this;
    }

    @Step("Enter password")
    public SignUpPage enterPassword(String password) {
        type(passwordField, password);
        return this;
    }

    @Step("Click Sign Up button")
    public void clickSignUp() {
        blurActiveElement();
        clickWhenEnabled(signUpButton);
    }

    @Step("Click Continue with Google on sign up page")
    public void clickContinueWithGoogle() {
        click(continueWithGoogle);
    }

    @Step("Click Sign In link on sign up page")
    public SignInPage clickSignInLink() {
        jsClick(signInLink);
        SignInPage page = new SignInPage();
        page.isLoaded();
        return page;
    }

    public boolean isLoaded() {
        return isElementVisible(companyNameField);
    }

    public boolean isContinueWithGoogleVisible() {
        return isElementVisible(continueWithGoogle);
    }

    public boolean isSignUpButtonEnabled() {
        return isButtonEnabled(signUpButton);
    }

    // Used to verify the company name is locked (pre-filled by invite) on the invite sign-up page.
    public boolean isCompanyNameLocked() {
        try {
            var el = driver.findElement(companyNameField);
            return !el.isEnabled() || "true".equalsIgnoreCase(el.getAttribute("readonly"));
        } catch (Exception e) {
            return false;
        }
    }

    public String getCompanyNameValue() {
        try {
            return driver.findElement(companyNameField).getAttribute("value");
        } catch (Exception e) {
            return "";
        }
    }
}

package com.aybee.pages;

import com.aybee.utils.ConfigReader;
import io.qameta.allure.Step;
import org.openqa.selenium.By;

public class SignUpPage extends BasePage {

    private final By companyNameField = By.id("signup-companyname");
    private final By firstNameField   = By.id("signup-firstname");
    private final By lastNameField    = By.id("signup-lastname");
    private final By emailField       = By.id("signup-email");
    private final By passwordField    = By.id("signup-password");
    private final By signUpButton     = By.id("btn-signup");

    @Step("Navigate to Sign Up page")
    public SignUpPage navigateTo() {
        driver.get(ConfigReader.get("BASE_URL"));
        driver.manage().deleteAllCookies();
        driver.navigate().refresh();
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

    @Step("Enter sign-up email: {email}")
    public SignUpPage enterEmail(String email) {
        type(emailField, email);
        return this;
    }

    @Step("Enter sign-up password")
    public SignUpPage enterPassword(String password) {
        type(passwordField, password);
        return this;
    }

    @Step("Click Sign Up button")
    public void clickSignUp() {
        blurActiveElement();
        clickWhenEnabled(signUpButton);
    }

    public boolean isLoaded() {
        return isElementVisible(companyNameField);
    }
}

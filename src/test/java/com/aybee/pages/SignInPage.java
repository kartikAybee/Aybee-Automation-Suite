package com.aybee.pages;

import io.qameta.allure.Step;
import org.openqa.selenium.By;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

public class SignInPage extends BasePage {

    private final By emailField              = By.id("login-email");
    private final By passwordField           = By.id("login-password");
    private final By signInButton            = By.id("btn-signin");
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
        blurActiveElement();
        clickWhenEnabled(signInButton);
    }

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
}

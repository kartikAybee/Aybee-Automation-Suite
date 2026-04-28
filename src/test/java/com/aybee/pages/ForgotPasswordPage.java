package com.aybee.pages;

import io.qameta.allure.Step;
import org.openqa.selenium.By;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

public class ForgotPasswordPage extends BasePage {

    private final By emailField       = By.id("in-emailreset-password");
    private final By sendResetButton  = By.id("btn-reset-continue");
    private final By pageTitle        = By.id("login-title");
    // Title element whose text changes to confirm the reset email was sent
    private final By resetTitle       = By.id("reset-pw-title");

    @Step("Enter email in reset field: {email}")
    public ForgotPasswordPage enterEmail(String email) {
        type(emailField, email);
        return this;
    }

    @Step("Click Send Reset Link button")
    public void clickSendResetLink() {
        blurActiveElement();
        clickWhenEnabled(sendResetButton);
    }

    public boolean isLoaded() {
        return isElementVisible(emailField);
    }

    // Polls until the page title changes to the confirmation text — element is always present,
    // only the text changes after the reset email is dispatched.
    public boolean isConfirmationVisible() {
        try {
            new WebDriverWait(driver, 10).until(d ->
                    driver.findElement(resetTitle).getText().contains("Password reset email has been sent"));
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    // Returns the current text of the confirmation title without waiting.
    // Used for the negative assertion — no point waiting for text that should never appear.
    public String getResetTitleText() {
        try {
            return driver.findElement(resetTitle).getText();
        } catch (Exception e) {
            return "";
        }
    }

    public boolean isSendResetButtonEnabled() {
        return isButtonEnabled(sendResetButton);
    }
}

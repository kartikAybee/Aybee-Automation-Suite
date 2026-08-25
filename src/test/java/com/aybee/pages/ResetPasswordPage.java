package com.aybee.pages;

import io.qameta.allure.Step;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

public class ResetPasswordPage extends BasePage {

    private final By newPasswordField     = By.id("reset_pw_field");
    private final By confirmPasswordField = By.id("reset_pw_retype_field");
    private final By saveButton           = By.id("save-new-password");
    private final By pageTitle            = By.id("new-pass-title");

    @Step("Enter new password")
    public ResetPasswordPage enterNewPassword(String password) {
        type(newPasswordField, password);
        return this;
    }

    @Step("Enter confirm password")
    public ResetPasswordPage enterConfirmPassword(String password) {
        type(confirmPasswordField, password);
        return this;
    }

    @Step("Clear both password fields")
    public ResetPasswordPage clearPasswordFields() {
        type(newPasswordField, "");
        type(confirmPasswordField, "");
        return this;
    }

    @Step("Click Save New Password")
    public void clickSave() {
        // Blur the confirm field so Bubble's reactive validation enables the button. Then trigger
        // the button with a real mouse hover + click (Actions) — a plain click was leaving the save
        // un-fired; the hover primes Bubble's handler. JS click is the last-resort fallback.
        blurActiveElement();
        WebElement btn = new WebDriverWait(driver, 15)
                .until(ExpectedConditions.presenceOfElementLocated(saveButton));
        ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block:'center'});", btn);
        try {
            new Actions(driver).moveToElement(btn).click().perform();
        } catch (Exception e) {
            ((JavascriptExecutor) driver).executeScript("arguments[0].click();", btn);
        }
    }

    public boolean isLoaded() {
        return isElementVisible(newPasswordField);
    }

    public boolean isSaveButtonEnabled() {
        return isButtonEnabled(saveButton);
    }
}

package com.aybee.pages;

import io.qameta.allure.Step;
import org.openqa.selenium.By;

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
        blurActiveElement();
        clickWhenEnabled(saveButton);
    }

    public boolean isLoaded() {
        return isElementVisible(newPasswordField);
    }

    public boolean isSaveButtonEnabled() {
        return isButtonEnabled(saveButton);
    }
}

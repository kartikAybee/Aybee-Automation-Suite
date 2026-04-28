package com.aybee.pages;

import io.qameta.allure.Step;
import org.openqa.selenium.By;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

public class OtpVerificationPage extends BasePage {

    private final By otpField         = By.id("security-code-field");
    private final By nextButton       = By.id("next-btn");
    private final By resendCodeButton = By.id("btn-resendcode");
    private final By cancelButton     = By.id("btn-cancelcreation");
    private final By pageTitle        = By.id("activate-account-title");

    @Step("Enter OTP code")
    public OtpVerificationPage enterOtp(String otp) {
        type(otpField, otp);
        return this;
    }

    @Step("Click Next / Activate button")
    public void clickActivate() {
        blurActiveElement();
        clickWhenEnabled(nextButton);
    }

    @Step("Click Resend Code button")
    public void clickResendCode() {
        click(resendCodeButton);
    }

    @Step("Click Cancel account creation")
    public SignUpPage clickCancel() {
        jsClick(cancelButton);
        SignUpPage page = new SignUpPage();
        page.isLoaded();
        return page;
    }

    public boolean isLoaded() {
        // Give the OTP page up to 30s to appear — Bubble.io navigation can be slow.
        try {
            new WebDriverWait(driver, 30)
                    .until(ExpectedConditions.visibilityOfElementLocated(otpField));
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    // Enters text in the OTP field without clicking Next — for button-state assertions.
    public OtpVerificationPage typeInOtpField(String value) {
        type(otpField, value);
        return this;
    }

    public boolean isNextButtonEnabled() {
        return isButtonEnabled(nextButton);
    }

    public boolean isActivationSuccessful() {
        try {
            new WebDriverWait(driver, 15)
                    .until(ExpectedConditions.visibilityOfElementLocated(By.id("profile-btn")));
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}

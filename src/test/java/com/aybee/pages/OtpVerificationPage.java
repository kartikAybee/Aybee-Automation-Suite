package com.aybee.pages;

import io.qameta.allure.Step;
import org.openqa.selenium.By;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

public class OtpVerificationPage extends BasePage {

    private final By otpField   = By.id("security-code-field");
    private final By nextButton = By.id("next-btn");

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

    // Waits up to 30 s for the OTP page — Bubble.io navigation after sign-up can be slow.
    public boolean isLoaded() {
        try {
            new WebDriverWait(driver, 30)
                    .until(ExpectedConditions.visibilityOfElementLocated(otpField));
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}

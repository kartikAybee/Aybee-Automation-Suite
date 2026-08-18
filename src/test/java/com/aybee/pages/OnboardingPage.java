package com.aybee.pages;

import io.qameta.allure.Step;
import org.openqa.selenium.By;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

public class OnboardingPage extends BasePage {

    private final By continueButton = By.id("onboarding-continue-btn");
    // Q4 only — "how did you hear about us" free-text input
    private final By hearAboutField = By.id("multiline-input-field");

    // Short wait — onboarding appears after Bubble.io processes OTP activation.
    // Returns false immediately if it never shows (skipped for invited/returning users).
    public boolean isLoaded() {
        try {
            new WebDriverWait(driver, 5)
                    .until(ExpectedConditions.visibilityOfElementLocated(By.id("onboarding_question1")));
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    // Q1–Q3: MCQ — jsClick first [id^='option_'] element; Bubble.io auto-advances to next question.
    // Q4:    free-text — type answer, blur, click Continue.
    // After Q4 Bubble.io loads the dashboard; we wait for the last question to disappear.
    @Step("Complete all onboarding steps")
    public void completeAllSteps() {
        int totalQuestions = 4;
        for (int i = 1; i <= totalQuestions; i++) {
            wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("onboarding_question" + i)));
            if (i == totalQuestions) {
                type(hearAboutField, "Aybee Test");
                blurActiveElement();
                click(continueButton);
            } else {
                jsClick(By.cssSelector("[id^='option_']"));
            }
        }
        wait.until(ExpectedConditions.invisibilityOfElementLocated(
                By.id("onboarding_question" + totalQuestions)));
    }

    // Called after OTP activation — onboarding MUST appear and be completed for new accounts,
    // otherwise the next sign-in will re-show it before reaching the dashboard.
    @Step("Complete required onboarding after activation")
    public void completeRequired() {
        try {
            new WebDriverWait(driver, 15)
                    .until(ExpectedConditions.visibilityOfElementLocated(By.id("onboarding_question1")));
        } catch (Exception e) {
            throw new RuntimeException(
                    "Onboarding questions did not appear after account activation — cannot proceed", e);
        }
        completeAllSteps();
    }
}

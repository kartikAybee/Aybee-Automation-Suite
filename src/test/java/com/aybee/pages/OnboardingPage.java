package com.aybee.pages;

import io.qameta.allure.Step;
import org.openqa.selenium.By;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

public class OnboardingPage extends BasePage {

    private final By continueButton = By.id("onboarding-continue-btn");
    // Q4 only — the "how did you hear about us" free-text field
    private final By hearAboutField = By.id("multiline-input-field");

    public boolean isLoaded() {
        // Short wait — onboarding appears after Bubble.io processes activation.
        // Returns false if it never shows (invited/returning users skip onboarding).
        try {
            new WebDriverWait(driver, 5)
                    .until(ExpectedConditions.visibilityOfElementLocated(By.id("onboarding_question1")));
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Step("Select first available onboarding option")
    public void selectFirstOption() {
        // JS click — Bubble.io option elements can have width:0px, causing elementToBeClickable to time out.
        jsClick(By.cssSelector("[id^='option_']"));
    }

    // Waits for each question by its indexed ID, then answers it:
    //   Q1–Q3 — MCQ: jsClick first option, app auto-proceeds to next question
    //   Q4     — free-text: type answer + blur question container + clickWhenEnabled continue
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
                selectFirstOption();
            }
        }
        // After Q4 the app loads the dashboard — wait for the last question to disappear
        wait.until(ExpectedConditions.invisibilityOfElementLocated(By.id("onboarding_question" + totalQuestions)));
    }

    @Step("Complete onboarding if displayed")
    public void completeIfPresent() {
        if (isLoaded()) {
            completeAllSteps();
        }
    }

    // Used after initial account activation — onboarding MUST appear and be completed,
    // otherwise the next sign-in will re-show it instead of going to the dashboard.
    @Step("Complete required onboarding after activation")
    public void completeRequired() {
        try {
            new WebDriverWait(driver, 15)
                    .until(ExpectedConditions.visibilityOfElementLocated(By.id("onboarding_question1")));
        } catch (Exception e) {
            throw new RuntimeException("Onboarding questions did not appear after account activation — cannot proceed", e);
        }
        completeAllSteps();
    }
}

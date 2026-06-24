package com.aybee.pages;

import io.qameta.allure.Step;
import org.openqa.selenium.By;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

public class OnboardingPage extends BasePage {

    private final By continueButton = By.id("onboarding-continue-btn");
    private final By hearAboutField = By.id("multiline-input-field");

    // Q1–Q3: MCQ — jsClick the first option; Bubble.io auto-advances.
    // Q4: free-text — type, blur, click Continue.
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

    @Step("Complete required onboarding after activation")
    public void completeRequired() {
        try {
            new WebDriverWait(driver, 15)
                    .until(ExpectedConditions.visibilityOfElementLocated(By.id("onboarding_question1")));
        } catch (Exception e) {
            throw new RuntimeException(
                    "Onboarding questions did not appear after account activation", e);
        }
        completeAllSteps();
    }
}

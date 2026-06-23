package com.aybee.pages;

import io.qameta.allure.Step;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

public class ExperimentSettingsPage extends BasePage {

    // Stage 1 landmark — used by CreateProjectPage.waitUntilLoaded() and isLoaded().
    static final By STAGE_LANDMARK = By.id("marketplacesimulation_testsettings_studyobjective_section");

    // richtext-editor-0 is the ql-container div; the contenteditable area is
    // the .ql-editor child inside it. By.id() reaches the container; findElement()
    // scopes the ql-editor lookup within it — no global CSS selector needed.
    private final By quillContainer    = By.id("richtext-editor-0");
    // A real (native) click on qz-title-text moves focus off the Quill editor, firing its
    // blur event so Bubble's reactive validation runs and enables the continue button.
    private final By titleText         = By.id("qz-title-text");
    private final By continueButton    = By.id("marketplacesimulation_testsettings_continue_button");
    private final By addBqButton       = By.id("btn-add-bq-1");
    private final By nextProjectButton = By.id("btn-next-project");

    static final int MAX_BUSINESS_QUESTIONS = 1;

    public boolean isLoaded() {
        return isElementVisible(STAGE_LANDMARK);
    }

    // The Quill editor starts empty on a fresh experiment form — click to focus, then sendKeys().
    @Step("Enter study objective in the Quill rich text editor")
    public ExperimentSettingsPage enterStudyObjective(String text) {
        WebElement editor = wait.until(ExpectedConditions.presenceOfElementLocated(quillContainer))
                                .findElement(By.className("ql-editor"));
        // A Bubble.io overlay intercepts the regular click — JS click focuses the editor directly.
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", editor);
        editor.sendKeys(text);
        return this;
    }

    @Step("Click Continue to generate business questions")
    public ExperimentSettingsPage clickContinue() {
        // Real click on qz-title-text once it's clickable — this moves focus off the Quill
        // editor and fires its blur, which is what Bubble's reactive validation listens for to
        // enable the continue button. A JS click wouldn't move focus, so a native click is used.
        click(titleText);
        clickWhenEnabled(continueButton);
        // btn-add-bq-1 appearing confirms the AI generated the first business question.
        wait.until(ExpectedConditions.visibilityOfElementLocated(addBqButton));
        return this;
    }

    // Adds all business questions by clicking btn-add-bq-1 once per question.
    // Each click causes Bubble.io to pop the next question from the stack and re-render
    // the button with the new question text (DOM replacement, not in-place mutation).
    // Sync strategy: wait for StaleElementReferenceException on the pre-click reference
    // (confirms the DOM was updated = previous question was added), then re-wait for
    // btn-add-bq-1 before the next click.
    // Fallback: if stalenessOf times out (Bubble.io mutated in-place instead of replacing),
    // the TimeoutException is swallowed and visibilityOfElementLocated acts as the sync point.
    @Step("Add all business questions")
    public ExperimentSettingsPage addAllBusinessQuestions() {
        for (int i = 0; i < MAX_BUSINESS_QUESTIONS; i++) {
            WebElement addBtn = new WebDriverWait(driver, 10)
                    .until(ExpectedConditions.elementToBeClickable(addBqButton));
            ((JavascriptExecutor) driver).executeScript("arguments[0].click();", addBtn);

            try {
                new WebDriverWait(driver, 3).until(ExpectedConditions.stalenessOf(addBtn));
            } catch (TimeoutException ignored) {}

            if (i < MAX_BUSINESS_QUESTIONS - 1) {
                new WebDriverWait(driver, 10)
                        .until(ExpectedConditions.elementToBeClickable(addBqButton));
            }
        }
        return this;
    }

    // Scrolls btn-next-project into the viewport before clicking — the button is below
    // the fold after business questions are added.
    @Step("Scroll to Next button and proceed to shop setup stage")
    public void proceedToShopSetup() {
        WebElement nextBtn = wait.until(ExpectedConditions.presenceOfElementLocated(nextProjectButton));
        ((JavascriptExecutor) driver).executeScript(
                "arguments[0].scrollIntoView({behavior: 'instant', block: 'center'});", nextBtn);
        clickWhenEnabled(nextProjectButton);
    }
}

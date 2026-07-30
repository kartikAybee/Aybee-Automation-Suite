package com.aybee.pages;

import io.qameta.allure.Step;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

// Experiment Settings — step 1 of the Questionnaire flow.
//
// Ported from the msjourney suite (the study-objective + business-questions step is identical
// across Marketplace Simulation and Questionnaire experiments, using the same element IDs). The
// only divergence is the final Next click: in a Questionnaire, btn-next-project advances straight
// to the Form Questions step (there is no Shop Setup step in between).
public class ExperimentSettingsPage extends BasePage {

    // Study-objective section landmark — confirms the settings step loaded after country selection.
    static final By STAGE_LANDMARK =
            By.id("marketplacesimulation_testsettings_studyobjective_section");

    // richtext-editor-0 is the Quill ql-container div; the contenteditable area is the
    // .ql-editor child inside it.
    private final By quillContainer    = By.id("richtext-editor-0");
    // A real (native) click on qz-title-text moves focus off the Quill editor, firing its blur
    // event so Bubble's reactive validation runs and enables the continue button.
    private final By titleText         = By.id("qz-title-text");
    private final By continueButton    = By.id("marketplacesimulation_testsettings_continue_button");
    private final By addBqButton       = By.id("btn-add-bq-1");
    private final By nextProjectButton = By.id("btn-next-project");

    // Form Questions landmark — the Add Question button confirms we advanced to the next step.
    private final By addQuestionButton = By.id("newproject_formquestions_addquestion_button");

    // User asked for exactly ONE business question ("add one like msjourney").
    static final int BUSINESS_QUESTIONS_TO_ADD = 1;

    // The study-objective Quill editor is the first thing shown once the experiment is created —
    // waiting for it confirms the settings step (step 1) has loaded after selecting the country.
    public boolean isLoaded() {
        try {
            return new WebDriverWait(driver, 45)
                    .until(ExpectedConditions.presenceOfElementLocated(quillContainer)) != null;
        } catch (Exception e) {
            return false;
        }
    }

    // The Quill editor starts empty on a fresh experiment form — click to focus, then sendKeys().
    @Step("Enter study objective in the Quill rich text editor")
    public ExperimentSettingsPage enterStudyObjective(String text) {
        WebElement editor = new WebDriverWait(driver, 45)
                .until(ExpectedConditions.presenceOfElementLocated(quillContainer))
                .findElement(By.className("ql-editor"));
        // A Bubble.io overlay intercepts the regular click — JS click focuses the editor directly.
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", editor);
        editor.sendKeys(text);
        return this;
    }

    @Step("Click Continue to generate business questions")
    public ExperimentSettingsPage clickContinue() {
        // Real click on qz-title-text once it's clickable — this moves focus off the Quill editor
        // and fires its blur, which is what Bubble's reactive validation listens for to enable the
        // continue button. A JS click wouldn't move focus, so a native click is used here.
        click(titleText);
        clickWhenEnabled(continueButton);
        // btn-add-bq-1 appearing confirms the AI generated the first business question.
        new WebDriverWait(driver, 60)
                .until(ExpectedConditions.visibilityOfElementLocated(addBqButton));
        return this;
    }

    // Adds business questions by clicking btn-add-bq-1 once per question (here: exactly one).
    // Each click causes Bubble.io to pop the next question from the stack and re-render the button
    // with the new text (DOM replacement, not in-place mutation).
    // Sync strategy: wait for StaleElementReferenceException on the pre-click reference (confirms
    // the DOM updated = previous question added). Fallback: if stalenessOf times out (Bubble mutated
    // in-place), the TimeoutException is swallowed and the elementToBeClickable re-wait acts as the
    // sync point before the next click.
    @Step("Add one business question (like msjourney)")
    public ExperimentSettingsPage addBusinessQuestions() {
        for (int i = 0; i < BUSINESS_QUESTIONS_TO_ADD; i++) {
            WebElement addBtn = new WebDriverWait(driver, 10)
                    .until(ExpectedConditions.elementToBeClickable(addBqButton));
            ((JavascriptExecutor) driver).executeScript("arguments[0].click();", addBtn);

            try {
                new WebDriverWait(driver, 3).until(ExpectedConditions.stalenessOf(addBtn));
            } catch (TimeoutException ignored) {}

            if (i < BUSINESS_QUESTIONS_TO_ADD - 1) {
                new WebDriverWait(driver, 10)
                        .until(ExpectedConditions.elementToBeClickable(addBqButton));
            }
        }
        return this;
    }

    // Scrolls btn-next-project into the viewport before clicking — the button is below the fold
    // once a business question is added. behavior:'instant' avoids timing gaps that 'smooth'
    // scrolling introduces between scroll end and click dispatch. In a Questionnaire this Next
    // click advances directly to the Form Questions step (no Shop Setup step in between).
    @Step("Scroll to Next and proceed to the Form Questions step")
    public void proceedToFormQuestions() {
        WebElement nextBtn = wait.until(
                ExpectedConditions.presenceOfElementLocated(nextProjectButton));
        ((JavascriptExecutor) driver).executeScript(
                "arguments[0].scrollIntoView({behavior: 'instant', block: 'center'});", nextBtn);
        clickWhenEnabled(nextProjectButton);
        // The Add Question button (at the bottom of the Form Questions step) appearing confirms
        // we advanced to the next step.
        WebElement addQ = new WebDriverWait(driver, 45)
                .until(ExpectedConditions.presenceOfElementLocated(addQuestionButton));
        ((JavascriptExecutor) driver).executeScript(
                "arguments[0].scrollIntoView({behavior: 'instant', block: 'center'});", addQ);
    }
}

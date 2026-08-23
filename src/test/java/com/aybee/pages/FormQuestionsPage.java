package com.aybee.pages;

import com.aybee.context.GlobalTestState;
import com.aybee.utils.ConfigReader;
import io.qameta.allure.Step;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.FluentWait;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

// Form Questions — step 2 (final step) of the Questionnaire flow.
//
// This is the shared, full-featured Form Questions editor toolkit, ported from the msjourney / pdp
// suites (see the aybee-form-questions skill). All authoring / answer-option / Likert / asset-upload
// methods and the Bubble.io reactive/stale/loading workarounds are carried over verbatim.
//
// Questionnaire specifics that differ from the other suites:
//   • The platform pre-adds NO questions, so manual questions start at index 1
//     (FIRST_QUESTION_INDEX). Every element ID for a question uses this 1-based index.
//   • The "What to display" dropdown (dp_show_participants_form_question_{n}) has only TWO options:
//     Just Question (just_question, the default) and Uploaded Assets (uploaded_image).
//   • The filter sidebar has only ONE tab — "Filter by responses" — keyed off prior questions'
//     answers (there is no "Filter by scenario" tab). The response-filter logic mirrors msjourney.
public class FormQuestionsPage extends BasePage {

    // Questionnaire has no pre-added questions — the first manual question is index 1.
    public static final int FIRST_QUESTION_INDEX = 1;

    // "What to display" option values (only two exist for a Questionnaire).
    public static final String DISPLAY_JUST_QUESTION   = "just_question";   // default — no selection needed
    public static final String DISPLAY_UPLOADED_ASSETS = "uploaded_image";  // "Uploaded Assets"

    // Answer-option input ids now carry a trailing section suffix — full format is
    // "{q}--answerInput-{k}-Post-Shop" (the numbers/rest are unchanged, only this suffix is new).
    // Applied to EVERY answerInput id usage (exact locators AND the [id^=...] detection selectors,
    // which additionally require [id$='<suffix>']). Other element ids (icons, dropdowns) are unchanged.
    public static final String ANSWER_INPUT_SUFFIX = "-Post-Shop";

    private final By addQuestionButton    = By.id("newproject_formquestions_addquestion_button");
    private final By addManuallyButton    = By.id("add-manually-btn");
    private final By previewJourneyButton = By.id("newproject_formquestions_previewjourney_button");
    private final By sectionTitle         = By.id("experiment-questions-title");

    // Desktop/Mobile preview chooser popup — shown after clicking Preview. We always pick Desktop.
    // Validation/error toasts now fire AFTER the Desktop selection, not on the Preview click.
    private final By previewDesktopButton = By.id("preview-desktop");
    private final By closePreviewChooser  = By.id("close-preview-chooser");
    // NEW_PREVIEW=no (default): preview opens directly, no Desktop/Mobile chooser. yes: select Desktop
    // in the chooser after clicking Preview. Reverted to no for now.
    private final boolean useNewPreview = com.aybee.utils.ConfigReader.getYesNo("NEW_PREVIEW", false);

    // Asset upload popup — File-Upload-Asset-Input is the outer container;
    // dropzone is the clickable upload button inside it (id ends with a bare "-").
    private final By fileUploadInput = By.id("File-Upload-Asset-Input");
    private final By dropzone        = By.cssSelector("[id='dropzone-']");
    private final By addUploadButton = By.id("add-Upload-Assets");

    // Filter sidebar — Questionnaire exposes only the "Filter by responses" tab.
    private final By filterByResponseTab  = By.id("filter-by-response");
    private final By addFilterQuestionBtn = By.id("button-add-question-filter-response");
    private final By applyFiltersButton   = By.id("button-apply-filters");

    // ── Scroll helper ─────────────────────────────────────────────────────────

    // Scrolls the element into view before returning it. Every interaction on the form questions
    // page uses this so elements in newly-added cards (which appear at the bottom of a growing
    // list) are always in the viewport first.
    private WebElement scrollTo(By locator) {
        WebElement el = wait.until(ExpectedConditions.presenceOfElementLocated(locator));
        ((JavascriptExecutor) driver).executeScript(
            "arguments[0].scrollIntoView({behavior: 'instant', block: 'center'});", el);
        return el;
    }

    // ── Landmark / page-load ────────────────────────────────────────────────────

    // The Add Question button sits at the bottom of the page, so scroll it into view before the
    // clickability check. Confirms the Form Questions step has loaded and is interactive.
    @Step("Wait for Form Questions step to load (Add Question button clickable)")
    public boolean isAddQuestionButtonClickable() {
        try {
            WebElement btn = new WebDriverWait(driver, 45)
                    .until(ExpectedConditions.presenceOfElementLocated(addQuestionButton));
            ((JavascriptExecutor) driver).executeScript(
                    "arguments[0].scrollIntoView({behavior:'instant',block:'center'});", btn);
            new WebDriverWait(driver, 30)
                    .until(ExpectedConditions.elementToBeClickable(addQuestionButton));
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    // ── Card lifecycle ────────────────────────────────────────────────────────

    // Checks the chevron SVG href — reliable regardless of Bubble.io's CSS hiding strategy.
    @Step("Expand question {index} if collapsed")
    public void expandIfCollapsed(int index) {
        try {
            Object href = ((JavascriptExecutor) driver).executeScript(
                "var el = document.querySelector('#icon_toggle_form_question_" + index + " use');" +
                "return el ? el.getAttribute('href') : null;");
            if (href != null && href.toString().contains("chevron-down")) {
                jsClick(By.id(index + "-toggle-group"));
                new WebDriverWait(driver, 30).until(d ->
                    ((JavascriptExecutor) d).executeScript(
                        "var el = document.querySelector('#icon_toggle_form_question_" + index + " use');" +
                        "return el ? el.getAttribute('href') : '';")
                    .toString().contains("chevron-up"));
            }
        } catch (Exception ignored) {}
    }

    @Step("Open Add Question popup and wait for question card {index}")
    public FormQuestionsPage addNewQuestion(int index) {
        // Let Bubble.io settle any pending state from the previous question before proceeding.
        try { Thread.sleep(3000); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        scrollTo(addQuestionButton);
        jsClick(addQuestionButton);
        // Some flows show an "Add manually" choice after the Add Question button; click it if it
        // appears, otherwise proceed (the question card may be created directly).
        try {
            new WebDriverWait(driver, 5)
                .until(ExpectedConditions.visibilityOfElementLocated(addManuallyButton));
            jsClick(addManuallyButton);
        } catch (Exception ignored) {
            System.out.println("[FormQuestions] add-manually-btn not shown — proceeding");
        }
        new WebDriverWait(driver, 30)
            .until(ExpectedConditions.visibilityOfElementLocated(By.id(index + "-toggle-group")));
        scrollTo(By.id(index + "-questionInput"));
        expandIfCollapsed(index);
        return this;
    }

    // ── Common question fields ─────────────────────────────────────────────────

    @Step("Enter question text for question {index}")
    public FormQuestionsPage enterQuestionText(int index, String text) {
        By inputLoc = By.id(index + "-questionInput");
        // The question card can be (or become) collapsed after it is added, leaving the input
        // present but non-interactable ("element not interactable"). Expand it, and if the input is
        // still hidden, force-expand via the toggle group and wait for it to be visible before typing.
        expandIfCollapsed(index);
        ensureQuestionInputInteractable(index, inputLoc);
        WebElement input = new WebDriverWait(driver, 15).until(
            ExpectedConditions.elementToBeClickable(inputLoc));
        scrollToCenter(input);
        input.clear();
        input.sendKeys(text);
        return this;
    }

    // Ensures the question-text input for {index} is visible/interactable. A collapsed card renders
    // the input but hidden (display:none), so isDisplayed() is a reliable "expanded?" signal — an
    // off-screen-but-expanded input still reports displayed. Only toggles when the input is hidden,
    // so it never accidentally collapses an already-open card.
    private void ensureQuestionInputInteractable(int index, By inputLoc) {
        for (int attempt = 1; attempt <= 3; attempt++) {
            if (!driver.findElements(inputLoc).isEmpty()) {
                try {
                    if (driver.findElement(inputLoc).isDisplayed()) return;
                } catch (Exception ignored) {}
            }
            System.out.println("[FormQuestions] Q" + index
                + " input hidden — expanding via toggle group (attempt " + attempt + ")");
            jsClick(By.id(index + "-toggle-group"));
            try {
                new WebDriverWait(driver, 10).until(d ->
                    !d.findElements(inputLoc).isEmpty() && d.findElement(inputLoc).isDisplayed());
                return;
            } catch (Exception ignored) {}
        }
    }

    // Dropdown is a native <select>; option values include literal quote characters per the DOM,
    // so we wrap the value in quotes. Uses JS value injection + dispatchEvent because
    // Select.selectByValue() clicks native <option> elements, which fails when they render outside
    // the viewport.
    @Step("Select question type '{typeValue}' for question {index}")
    public FormQuestionsPage selectQuestionType(int index, String typeValue) {
        By locator = By.id("dp_type_of_question_form_page_" + index);
        WebElement select = new WebDriverWait(driver, 30).until(
            ExpectedConditions.visibilityOfElementLocated(locator));
        scrollToCenter(select);
        new WebDriverWait(driver, 30).until(ExpectedConditions.elementToBeClickable(locator));
        select = driver.findElement(locator);
        ((JavascriptExecutor) driver).executeScript(
            "arguments[0].value = arguments[1];" +
            "arguments[0].dispatchEvent(new Event('change', {bubbles: true}));",
            select, "\"" + typeValue + "\"");
        new WebDriverWait(driver, 10).until(d -> {
            String v = d.findElement(locator).getAttribute("value");
            return v != null && v.contains(typeValue);
        });
        return this;
    }

    // "What to display" dropdown. For a Questionnaire it exposes only two options — just_question
    // (the default, needs no selection) and uploaded_image ("Uploaded Assets"). Skip calling this
    // for just_question. Same quote-wrapped JS injection as the type dropdown.
    @Step("Select What to Display '{showValue}' for question {index}")
    public FormQuestionsPage selectShowToParticipants(int index, String showValue) {
        By locator = By.id("dp_show_participants_form_question_" + index);
        WebElement select = new WebDriverWait(driver, 30).until(
            ExpectedConditions.visibilityOfElementLocated(locator));
        scrollToCenter(select);
        new WebDriverWait(driver, 30).until(ExpectedConditions.elementToBeClickable(locator));
        select = driver.findElement(locator);
        ((JavascriptExecutor) driver).executeScript(
            "arguments[0].value = arguments[1];" +
            "arguments[0].dispatchEvent(new Event('change', {bubbles: true}));",
            select, "\"" + showValue + "\"");
        new WebDriverWait(driver, 10).until(d -> {
            String v = d.findElement(locator).getAttribute("value");
            return v != null && v.contains(showValue);
        });
        return this;
    }

    // ── Asset upload (What to display → Uploaded Assets) ─────────────────────────

    @Step("Click asset upload field for question {index} to open upload popup")
    public FormQuestionsPage clickAssetUploadField(int index) {
        WebElement field = scrollTo(By.id(index + "-asset-upload-option-field"));
        ((JavascriptExecutor) driver).executeScript(
            "arguments[0].scrollIntoView({behavior: 'instant', block: 'center'});", field);
        jsClick(By.id(index + "-asset-upload-option-field"));
        wait.until(ExpectedConditions.presenceOfElementLocated(fileUploadInput));
        return this;
    }

    @Step("Send file path to upload input")
    public FormQuestionsPage uploadAssetFile(String filePath) {
        // Primary: sendKeys directly on the File-Upload-Asset-Input container (any file size).
        try {
            System.out.println("[FileUpload] Primary — sendKeys on File-Upload-Asset-Input");
            WebElement el = wait.until(ExpectedConditions.presenceOfElementLocated(fileUploadInput));
            el.sendKeys(filePath);
            System.out.println("[FileUpload] Primary succeeded");
        } catch (Exception e) {
            // Fallback: JS DataTransfer drop. Works for small files (< ~100KB) only.
            System.out.println("[FileUpload] Primary failed (" + e.getClass().getSimpleName()
                + ") — trying DataTransfer fallback");
            uploadFileViaDataTransfer(dropzone, filePath, mimeTypeFrom(filePath));
        }
        try { Thread.sleep(2000); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        return this;
    }

    @Step("Confirm asset upload")
    public FormQuestionsPage confirmAssetUpload() {
        try { Thread.sleep(2000); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        jsClick(addUploadButton);
        wait.until(ExpectedConditions.invisibilityOfElementLocated(addUploadButton));
        try { Thread.sleep(2000); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        return this;
    }

    // Convenience: select Uploaded Assets and upload the configured asset image in one call.
    @Step("Set question {index} to Uploaded Assets and upload the asset image")
    public FormQuestionsPage useUploadedAssets(int index) {
        selectShowToParticipants(index, DISPLAY_UPLOADED_ASSETS);
        clickAssetUploadField(index);
        uploadAssetFile(ConfigReader.get("ASSET_IMAGE_PATH"));
        confirmAssetUpload();
        return this;
    }

    // ── Limited Choice ────────────────────────────────────────────────────────

    @Step("Set min choices to {value} for question {index}")
    public FormQuestionsPage setMinChoices(int index, String value) {
        WebElement el = scrollTo(By.id("min-choice-field-" + index));
        el.clear();
        el.sendKeys(value);
        blurActiveElement();
        return this;
    }

    @Step("Set max choices to {value} for question {index}")
    public FormQuestionsPage setMaxChoices(int index, String value) {
        WebElement el = scrollTo(By.id("max-choice-field-" + index));
        el.clear();
        el.sendKeys(value);
        blurActiveElement();
        return this;
    }

    // ── Likert Scale ──────────────────────────────────────────────────────────

    @Step("Select scale type '{scaleValue}' for question {index}")
    public FormQuestionsPage selectScaleType(int index, String scaleValue) {
        By locator = By.id("dp_scale_type_form_question_" + index);
        WebElement dropdown = new WebDriverWait(driver, 30).until(
            ExpectedConditions.visibilityOfElementLocated(locator));
        scrollToCenter(dropdown);
        new WebDriverWait(driver, 30).until(ExpectedConditions.elementToBeClickable(locator));
        dropdown = driver.findElement(locator);
        ((JavascriptExecutor) driver).executeScript(
            "var sel = arguments[0], val = arguments[1];" +
            "sel.value = '\"' + val + '\"';" +
            "sel.dispatchEvent(new Event('change', {bubbles: true}));",
            dropdown, scaleValue);
        new WebDriverWait(driver, 10).until(d -> {
            String v = d.findElement(locator).getAttribute("value");
            return v != null && v.contains(scaleValue);
        });
        return this;
    }

    // Verifies the Likert scale-type dropdown for question {index} still holds {expectedScale}, and
    // re-applies it if Bubble.io reset it to null/blank/another value (a known reactive glitch that
    // silently clears the scale type between setup and preview, breaking the Likert question). Safe to
    // call right before previewing — a no-op when the value is already correct.
    @Step("Ensure Likert scale type for question {index} is still '{expectedScale}' before preview")
    public FormQuestionsPage ensureLikertScaleType(int index, String expectedScale) {
        By locator = By.id("dp_scale_type_form_question_" + index);
        String current = null;
        try {
            WebElement dropdown = scrollTo(locator);
            current = dropdown.getAttribute("value");
        } catch (Exception ignored) {}
        if (current != null && current.contains(expectedScale)) {
            System.out.println("[Likert] Q" + index + " scale type OK ('" + expectedScale + "')");
            return this;
        }
        System.out.println("[Likert] Q" + index + " scale type was '" + current
            + "' (expected '" + expectedScale + "') — re-applying");
        selectScaleType(index, expectedScale);
        return this;
    }

    @Step("Wait for {expectedCount} Likert options for question {index}")
    public FormQuestionsPage waitForLikertOptions(int index, int expectedCount) {
        WebElement card = new WebDriverWait(driver, 30).until(
            ExpectedConditions.visibilityOfElementLocated(By.id(index + "-toggle-group")));
        scrollToCenter(card);
        new WebDriverWait(driver, 60).until(d ->
            d.findElements(By.cssSelector("[id^='" + index + "--answerInput-'][id$='" + ANSWER_INPUT_SUFFIX + "']")).size() >= expectedCount);
        try { Thread.sleep(1500); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        return this;
    }

    // ── Answer options ────────────────────────────────────────────────────────

    private int countAnswerOptions(int questionIndex) {
        return driver.findElements(
            By.cssSelector("[id^='" + questionIndex + "--answerInput-'][id$='" + ANSWER_INPUT_SUFFIX + "']")).size();
    }

    @Step("Wait for answer options to appear for question {questionIndex}")
    public FormQuestionsPage waitForAnswerOptions(int questionIndex) {
        new WebDriverWait(driver, 30).until(d ->
            !d.findElements(By.cssSelector("[id^='" + questionIndex + "--answerInput-'][id$='" + ANSWER_INPUT_SUFFIX + "']")).isEmpty());
        return this;
    }

    @Step("Wait for {expectedCount} answer options on question {questionIndex}")
    public FormQuestionsPage waitForAnswerOptionCount(int questionIndex, int expectedCount) {
        new WebDriverWait(driver, 30).until(d ->
            d.findElements(
                By.cssSelector("[id^='" + questionIndex + "--answerInput-'][id$='" + ANSWER_INPUT_SUFFIX + "']")).size() >= expectedCount);
        return this;
    }

    // Deletes extras from the end first, then adds if short — ensures exactly targetCount fields.
    @Step("Ensure {targetCount} answer option fields for question {questionIndex}")
    public FormQuestionsPage ensureAnswerOptionCount(int questionIndex, int targetCount) {
        waitForAnswerOptions(questionIndex);

        // Limited Choice creates its default answer options ASYNCHRONOUSLY and with a delay — the
        // count keeps changing for a few seconds after the card renders. Wait for it to STABILISE
        // before touching anything, otherwise our add/delete clicks race the platform's own option
        // creation and a specific-index wait (answerInput-N) never resolves.
        int settled = waitForAnswerOptionCountToStabilise(questionIndex);
        System.out.println("[LimitedChoice] Q" + questionIndex + " options settled at " + settled
            + " (target " + targetCount + ")");

        // Delete extras from the highest index down. Re-read the max each pass — indices shift as
        // Bubble.io re-renders, so never assume count == highest index.
        int guard = 0;
        while (countAnswerOptions(questionIndex) > targetCount && guard++ < 20) {
            int max = highestAnswerOptionIndex(questionIndex);
            By deleteIcon = By.id(questionIndex + "-deleteIcon-" + max);
            if (driver.findElements(deleteIcon).isEmpty()) break;
            scrollTo(deleteIcon);
            int before = countAnswerOptions(questionIndex);
            jsClick(deleteIcon);
            waitForAnswerOptionCountChange(questionIndex, before, 30);
        }

        // Add if short. Wait for the COUNT to grow (index-agnostic) instead of a specific
        // answerInput-{n} — Bubble.io may auto-create the next option itself, which still satisfies
        // us. Never throw: if we can't hit the target exactly, enterAnswerText grows slots too.
        guard = 0;
        while (countAnswerOptions(questionIndex) < targetCount && guard++ < 20) {
            By addBtn = By.id(questionIndex + "-addAnswer-btn");
            scrollTo(addBtn);
            new WebDriverWait(driver, 30).until(
                ExpectedConditions.visibilityOfElementLocated(addBtn));
            int before = countAnswerOptions(questionIndex);
            jsClick(addBtn);
            if (!waitForAnswerOptionCountChange(questionIndex, before, 15)) {
                // Bubble.io swallowed the click or is still auto-creating — nudge once more and let
                // the loop re-evaluate the count on the next pass.
                jsClick(addBtn);
                waitForAnswerOptionCountChange(questionIndex, before, 15);
            }
        }

        int finalCount = countAnswerOptions(questionIndex);
        if (finalCount != targetCount) {
            System.out.println("[LimitedChoice] Q" + questionIndex + " ended with " + finalCount
                + " options (wanted " + targetCount + ") — proceeding; enterAnswerText will reconcile");
        }
        return this;
    }

    // Polls the answer-option count until it stops changing (stable across 3 consecutive 500ms
    // polls) or ~10s elapses. Absorbs Limited Choice's delayed async auto-creation. Returns the
    // settled count.
    private int waitForAnswerOptionCountToStabilise(int questionIndex) {
        int last = -1, stable = 0;
        for (int i = 0; i < 20 && stable < 3; i++) {
            int c = countAnswerOptions(questionIndex);
            if (c == last) { stable++; } else { stable = 0; last = c; }
            try { Thread.sleep(500); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        }
        return countAnswerOptions(questionIndex);
    }

    // Highest k for which questionIndex--answerInput-k exists (options can be non-contiguous
    // mid-render, so scan actual ids rather than trusting count == max index).
    private int highestAnswerOptionIndex(int questionIndex) {
        String prefix = questionIndex + "--answerInput-";
        int max = 0;
        for (WebElement el : driver.findElements(
                By.cssSelector("[id^='" + prefix + "'][id$='" + ANSWER_INPUT_SUFFIX + "']"))) {
            String id = el.getAttribute("id");
            if (id == null || !id.startsWith(prefix)) continue;
            // id is "{q}--answerInput-{k}-Post-Shop" — take the {k} between the prefix and the suffix.
            String rest = id.substring(prefix.length());
            if (rest.endsWith(ANSWER_INPUT_SUFFIX)) {
                rest = rest.substring(0, rest.length() - ANSWER_INPUT_SUFFIX.length());
            }
            try {
                int k = Integer.parseInt(rest);
                if (k > max) max = k;
            } catch (NumberFormatException ignored) {}
        }
        return max;
    }

    // Waits up to timeoutSecs for the answer-option count to differ from `before`. Returns true if
    // it changed, false on timeout (no throw) so the caller can decide how to proceed.
    private boolean waitForAnswerOptionCountChange(int questionIndex, int before, int timeoutSecs) {
        try {
            new FluentWait<>(driver)
                .withTimeout(timeoutSecs, TimeUnit.SECONDS)
                .pollingEvery(250, TimeUnit.MILLISECONDS)
                .ignoring(Exception.class)
                .until(d -> countAnswerOptions(questionIndex) != before);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Step("Enter answer text at index {answerIndex} for question {questionIndex}")
    public FormQuestionsPage enterAnswerText(int questionIndex, int answerIndex, String text) {
        By addBtn   = By.id(questionIndex + "-addAnswer-btn");
        By inputLoc = By.id(questionIndex + "--answerInput-" + answerIndex + ANSWER_INPUT_SUFFIX);

        WebElement addBtnEl = new WebDriverWait(driver, 30).until(
            ExpectedConditions.visibilityOfElementLocated(addBtn));
        scrollToCenter(addBtnEl);
        new WebDriverWait(driver, 30).until(d ->
            !d.findElements(By.cssSelector("[id^='" + questionIndex + "--answerInput-'][id$='" + ANSWER_INPUT_SUFFIX + "']")).isEmpty());
        try { Thread.sleep(1500); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }

        while (countAnswerOptions(questionIndex) < answerIndex) {
            int before = countAnswerOptions(questionIndex);
            scrollTo(addBtn);
            jsClick(addBtn);
            new FluentWait<>(driver)
                .withTimeout(5, TimeUnit.SECONDS)
                .pollingEvery(200, TimeUnit.MILLISECONDS)
                .ignoring(Exception.class)
                .until(d -> countAnswerOptions(questionIndex) > before);
        }

        addBtnEl = wait.until(ExpectedConditions.visibilityOfElementLocated(addBtn));
        scrollToCenter(addBtnEl);
        WebElement input = new WebDriverWait(driver, 30).until(
            ExpectedConditions.elementToBeClickable(inputLoc));
        scrollToCenter(input);
        input = new WebDriverWait(driver, 30).until(
            ExpectedConditions.elementToBeClickable(inputLoc));
        input.clear();
        input.sendKeys(text);
        return this;
    }

    @Step("Mark answer {answerIndex} as exclusive for question {questionIndex}")
    public FormQuestionsPage clickExclusive(int questionIndex, int answerIndex) {
        scrollTo(By.id(questionIndex + "-exclusiveIcon-" + answerIndex));
        jsClick(By.id(questionIndex + "-exclusiveIcon-" + answerIndex));
        return this;
    }

    // Enabling the toggle sets ALL answer options to randomize ON.
    @Step("Enable randomize toggle for question {questionIndex}")
    public FormQuestionsPage enableRandomizeToggle(int questionIndex) {
        scrollTo(By.id(questionIndex + "-toggle-randomize"));
        jsClick(By.id(questionIndex + "-toggle-randomize"));
        return this;
    }

    // Clicking a randomize icon after the toggle is ON disables randomize for that specific answer.
    @Step("Disable randomize for answer {answerIndex} on question {questionIndex}")
    public FormQuestionsPage disableRandomizeForAnswer(int questionIndex, int answerIndex) {
        scrollTo(By.id(questionIndex + "-randomIcon-" + answerIndex));
        jsClick(By.id(questionIndex + "-randomIcon-" + answerIndex));
        return this;
    }

    @Step("Delete answer {answerIndex} from question {questionIndex}")
    public FormQuestionsPage deleteAnswer(int questionIndex, int answerIndex) {
        By deleteIcon = By.id(questionIndex + "-deleteIcon-" + answerIndex);
        WebElement icon = scrollTo(deleteIcon);
        scrollToCenter(icon);
        new WebDriverWait(driver, 30).until(
            ExpectedConditions.visibilityOfElementLocated(deleteIcon));
        jsClick(deleteIcon);
        try { Thread.sleep(1500); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        return this;
    }

    // Waits for any late-appearing blank options, then deletes them highest-index first.
    // Returns true if any empty option was found — blank options should never appear when set up
    // correctly, so the caller records a soft failure.
    @Step("Delete any blank answer options from question {questionIndex}")
    public boolean deleteEmptyAnswerOptions(int questionIndex) {
        try { Thread.sleep(3000); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        boolean foundEmpty = false;
        int total = countAnswerOptions(questionIndex);
        for (int i = total; i >= 1; i--) {
            List<WebElement> els = driver.findElements(By.id(questionIndex + "--answerInput-" + i + ANSWER_INPUT_SUFFIX));
            if (els.isEmpty()) continue;
            String val = els.get(0).getAttribute("value");
            if (val == null || val.trim().isEmpty()) {
                foundEmpty = true;
                By deleteIcon = By.id(questionIndex + "-deleteIcon-" + i);
                if (!driver.findElements(deleteIcon).isEmpty()) {
                    scrollTo(deleteIcon);
                    jsClick(deleteIcon);
                    try { Thread.sleep(1500); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
                }
            }
        }
        return foundEmpty;
    }

    @Step("Scroll to question {index}, expand if collapsed, and delete empty answer options")
    public boolean cleanupEmptyOptions(int index) {
        scrollTo(By.id(index + "-toggle-group"));
        expandIfCollapsed(index);
        return deleteEmptyAnswerOptions(index);
    }

    // ── Filter sidebar (Filter by responses only) ────────────────────────────────

    @Step("Open filter sidebar for question {index}")
    public FormQuestionsPage openFilterSidebar(int index) {
        scrollTo(By.id("icon_filter_form_question_" + index));
        jsClick(By.id("icon_filter_form_question_" + index));
        // Questionnaire has a single "Filter by responses" tab — wait for either the tab element or
        // the Add-Question-Filter button to confirm the sidebar opened on the responses view.
        new WebDriverWait(driver, 30).until(ExpectedConditions.or(
            ExpectedConditions.visibilityOfElementLocated(filterByResponseTab),
            ExpectedConditions.visibilityOfElementLocated(addFilterQuestionBtn)));
        return this;
    }

    // The Questionnaire sidebar only has "Filter by responses", so it is normally already active
    // when the sidebar opens. Click it if it is present (harmless when already selected), then wait
    // for the Add-Question-Filter button to be interactive.
    @Step("Ensure the Filter by Responses tab is active")
    public FormQuestionsPage clickFilterByResponseTab() {
        try {
            if (!driver.findElements(filterByResponseTab).isEmpty()) {
                scrollTo(filterByResponseTab);
                new WebDriverWait(driver, 10)
                    .until(ExpectedConditions.elementToBeClickable(filterByResponseTab)).click();
            }
        } catch (Exception ignored) {}
        // Wait for a LIVE add-filter button (Bubble.io leaves a ghost copy from the previous
        // filter that By.id/elementToBeClickable would otherwise resolve and time out on).
        new WebDriverWait(driver, 15).until(d -> findLiveInstance(addFilterQuestionBtn) != null);
        return this;
    }

    @Step("Click Add Filter Question and wait for filter question dropdown {expectedFilterIndex}")
    public FormQuestionsPage clickAddFilterQuestion(int expectedFilterIndex) {
        // Click the live add-filter button, not the previous filter's lingering ghost instance.
        clickLiveInstance(addFilterQuestionBtn, 30);
        By dropdownLocator = By.id("dropdown-filter-question-" + expectedFilterIndex);
        new WebDriverWait(driver, 30).until(
            ExpectedConditions.visibilityOfElementLocated(dropdownLocator));
        // Wait for question options to populate — the dropdown appears with only the placeholder
        // at first; actual options load asynchronously from Bubble.io.
        new WebDriverWait(driver, 30).until(d ->
            new Select(d.findElement(dropdownLocator)).getOptions().size() > 1);
        return this;
    }

    // dropdown-filter-question-{j} is a native <select>; matches by partial text since long
    // question texts may be truncated in dropdown options.
    @Step("Select filter question {filterIndex} by partial text")
    public FormQuestionsPage selectFilterQuestion(int filterIndex, String partialQuestionText) {
        By dropdownLocator = By.id("dropdown-filter-question-" + filterIndex);
        // Wait for the target question to appear as an option — for later filter entries Bubble.io
        // removes already-selected questions from this dropdown dynamically.
        new WebDriverWait(driver, 30).until(d -> {
            try {
                return new Select(d.findElement(dropdownLocator)).getOptions().stream()
                    .anyMatch(o -> o.getText().contains(partialQuestionText));
            } catch (Exception e) { return false; }
        });
        // Brief pause so Bubble.io finishes any pending reactive updates before we set the value —
        // without this the selection is applied and then immediately cleared.
        try { Thread.sleep(1500); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        WebElement dropdown = driver.findElement(dropdownLocator);
        Boolean found = (Boolean) ((JavascriptExecutor) driver).executeScript(
            "var sel = arguments[0], partial = arguments[1];" +
            "for (var i = 0; i < sel.options.length; i++) {" +
            "  if (sel.options[i].text.indexOf(partial) >= 0) {" +
            "    sel.value = sel.options[i].value;" +
            "    sel.dispatchEvent(new Event('change', {bubbles: true}));" +
            "    return true;" +
            "  }" +
            "}" +
            "return false;",
            dropdown, partialQuestionText);
        if (!Boolean.TRUE.equals(found)) {
            throw new RuntimeException("Filter question not found: " + partialQuestionText);
        }
        try { Thread.sleep(2000); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        return this;
    }

    // The answer-option IDs are prefixed with the referenced question's index, not the filter entry
    // index — match by suffix to avoid hard-coding the question index here.
    @Step("Select answer option '{optionText}' for filter question {filterIndex}")
    public FormQuestionsPage selectFilterAnswerOption(int filterIndex, String optionText) {
        By locator = By.cssSelector("[id$='-select-answer-option-filter-" + optionText + "']");
        WebElement el = wait.until(ExpectedConditions.presenceOfElementLocated(locator));
        ((JavascriptExecutor) driver).executeScript(
            "arguments[0].scrollIntoView({behavior: 'instant', block: 'center'});", el);
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", el);
        return this;
    }

    @Step("Collapse filter question entry {filterIndex}")
    public FormQuestionsPage collapseFilterQuestion(int filterIndex) {
        jsClick(By.id("icon-up-filter-question-" + filterIndex));
        return this;
    }

    @Step("Delete filter question entry {filterIndex}")
    public FormQuestionsPage deleteFilterQuestion(int filterIndex) {
        jsClick(By.id("icon-delete-filter-question-" + filterIndex));
        return this;
    }

    @Step("Apply filters and wait for sidebar to close")
    public FormQuestionsPage applyFilters() {
        WebElement btn = new WebDriverWait(driver, 30).until(
            ExpectedConditions.visibilityOfElementLocated(applyFiltersButton));
        scrollToCenter(btn);
        new WebDriverWait(driver, 30).until(
            ExpectedConditions.elementToBeClickable(applyFiltersButton));
        // Real click first — jsClick bypasses Bubble.io's apply handler on this button. Fall back
        // to JS if the real click throws (e.g. element momentarily covered).
        try {
            driver.findElement(applyFiltersButton).click();
        } catch (Exception e) {
            jsClick(applyFiltersButton);
        }
        boolean closed = false;
        try {
            new WebDriverWait(driver, 5).until(
                ExpectedConditions.invisibilityOfElementLocated(applyFiltersButton));
            closed = true;
        } catch (Exception ignored) {}
        if (!closed) {
            System.out.println("[Filter] Apply button still visible after first click — retrying");
            jsClick(applyFiltersButton);
            new WebDriverWait(driver, 30).until(
                ExpectedConditions.invisibilityOfElementLocated(applyFiltersButton));
        }
        return this;
    }

    // ── Validation + Preview ────────────────────────────────────────────────────

    // Registers a click OUTSIDE the fields on the Experiment Settings title (experiment-questions-title)
    // to blur whatever field the last input left focused, then waits so Bubble.io runs its reactive
    // validation. Bubble does NOT validate a field until focus leaves it, so this must happen after
    // the last input and before previewing.
    //
    // IMPORTANT: this MUST be a REAL (native) click — a JS click does not change
    // document.activeElement or fire the field's blur event, so Bubble would never validate. (Same
    // reason ExperimentSettingsPage clicks qz-title-text natively to enable the continue button.)
    // blurActiveElement() is a belt-and-suspenders fallback in case the native click is intercepted.
    @Step("Click the Experiment Settings title to blur the last field, then wait for Bubble validation")
    public FormQuestionsPage validateAllInputs() {
        clickSectionTitle();
        // Give Bubble.io time to finish its reactive DB validation pass before the preview request.
        try { Thread.sleep(3000); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        return this;
    }

    // Commits the just-entered question fields immediately: clicks the section title so the last
    // field blurs and Bubble.io fires its reactive save right away, instead of deferring the whole
    // validation pass to preview time. Called at the END of each question's setup so preview
    // handling is faster and more reliable (mirrors the d2c precaution). Chainable.
    @Step("Commit the current question's fields by clicking the section title")
    public FormQuestionsPage commitFieldsViaTitle() {
        clickSectionTitle();
        return this;
    }

    // Native click on experiment-questions-title (with a direct-blur fallback) — the reusable
    // "click outside the field" action used both before the first preview and on every retry.
    private void clickSectionTitle() {
        try {
            scrollTo(sectionTitle);
            click(sectionTitle);   // native click → moves focus off the field → fires blur → validates
        } catch (Exception e) {
            System.out.println("[Validate] Native click on section title failed — blurring active field directly");
        }
        blurActiveElement();
    }

    private void sleep2s() {
        try { Thread.sleep(2000); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }

    // Re-triggers Bubble.io's choice-question validation by appending a single letter ("s") to the
    // first answer option of the given question, exactly like the msjourney suite. Bubble sometimes
    // reports a choice question as incomplete even when its options are filled; nudging an option's
    // text and then clicking outside the field forces Bubble to re-run and clear the stale
    // "incomplete form fields" state so the preview can open. Returns the option's updated text.
    @Step("Re-trigger validation for question {questionIndex} by appending a letter to its first option")
    private String retriggerChoiceValidation(int questionIndex) {
        sleep2s();
        scrollTo(By.id(questionIndex + "-toggle-group"));
        sleep2s();
        expandIfCollapsed(questionIndex);

        By firstAnswer = By.id(questionIndex + "--answerInput-1" + ANSWER_INPUT_SUFFIX);
        sleep2s();
        WebElement input = scrollTo(firstAnswer);
        sleep2s();
        input = new WebDriverWait(driver, 30).until(
            ExpectedConditions.elementToBeClickable(firstAnswer));
        scrollToCenter(input);
        sleep2s();
        input.sendKeys("s");   // append a letter — the actual nudge that re-triggers validation
        String updatedText = input.getAttribute("value");
        System.out.println("[Retrigger] Q" + questionIndex + " first option updated to: " + updatedText);
        sleep2s();
        return updatedText;
    }

    // Stores the retrigger's updated first-option text back into GlobalTestState so the guest
    // participant journey selects the exact text the field now holds (the retrigger permanently
    // appends a letter to the option). Maps question index → the stored selection for that question.
    // Q2 (limited) = FIRST+1, Q3 (single) = FIRST+2, Q4 (multiple) = FIRST+3; Likert questions carry
    // no stored option text and are ignored. Only the FIRST option is nudged, so only index 0 of the
    // two-option lists changes.
    private void syncRetriggeredOption(int questionIndex, String updated) {
        if (updated == null || updated.isEmpty()) return;
        if (questionIndex == FIRST_QUESTION_INDEX + 1
                && GlobalTestState.q2SelectOptions != null
                && GlobalTestState.q2SelectOptions.size() >= 2) {
            GlobalTestState.q2SelectOptions = Arrays.asList(updated, GlobalTestState.q2SelectOptions.get(1));
            System.out.println("[Retrigger] Stored updated Q2 option 1: " + updated);
        } else if (questionIndex == FIRST_QUESTION_INDEX + 2) {
            GlobalTestState.q3SelectOption = updated;
            System.out.println("[Retrigger] Stored updated Q3 option: " + updated);
        } else if (questionIndex == FIRST_QUESTION_INDEX + 3
                && GlobalTestState.q4SelectOptions != null
                && GlobalTestState.q4SelectOptions.size() >= 2) {
            GlobalTestState.q4SelectOptions = Arrays.asList(updated, GlobalTestState.q4SelectOptions.get(1));
            System.out.println("[Retrigger] Stored updated Q4 option 1: " + updated);
        }
    }

    // Set to true the instant the "incomplete form fields" toast is seen during a preview attempt.
    private boolean toastDetectedDuringPoll = false;

    // Polls up to timeoutSecs. Returns true when a new tab appears; false on toast (sets
    // toastDetectedDuringPoll) or timeout.
    private boolean pollForTabOrToast(int timeoutSecs) {
        toastDetectedDuringPoll = false;
        long deadline = System.currentTimeMillis() + (long) timeoutSecs * 1000;
        while (System.currentTimeMillis() < deadline) {
            if (driver.getWindowHandles().size() > 1) return true;
            try {
                List<WebElement> toasts = driver.findElements(
                    By.cssSelector("#toast-message, #toast-animate-in, [id^='toast-']"));
                if (!toasts.isEmpty() && toasts.get(0).isDisplayed()) {
                    toastDetectedDuringPoll = true;
                    return false;
                }
            } catch (Exception ignored) {}
            try { Thread.sleep(100); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        }
        return false;
    }

    // Opens the preview: clicks the Preview button, waits for the Desktop/Mobile chooser popup, and
    // selects Desktop (preview-desktop). Validation/error toasts fire AFTER the Desktop selection,
    // so the caller's poll for a new tab / toast happens once Desktop has been picked.
    // Reloads the Form Questions editor once, right before opening preview, so the backend data loads
    // fully (it can render incompletely in the preview journey on the very first open). Mirrors the PDP
    // precaution. The caller commits/validates fields BEFORE this reload so nothing is lost.
    public FormQuestionsPage reloadEditorForProductLoad() {
        System.out.println("[FormQuestions] Reloading editor before preview so all data loads properly");
        driver.navigate().refresh();
        new WebDriverWait(driver, 45).until(
            ExpectedConditions.presenceOfElementLocated(previewJourneyButton));
        try { Thread.sleep(3000); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        return this;
    }

    private void openPreviewChooserAndSelectDesktop() {
        scrollTo(previewJourneyButton);
        jsClick(previewJourneyButton);
        // NEW_PREVIEW=no (default): preview opens directly -- no Desktop/Mobile chooser to handle.
        if (!useNewPreview) return;
        WebElement desktop = new WebDriverWait(driver, 30).until(
            ExpectedConditions.elementToBeClickable(previewDesktopButton));
        scrollToCenter(desktop);
        jsClick(previewDesktopButton);
    }

    // Dismisses the error toast — which, in this flow, ALSO closes the Desktop/Mobile chooser popup.
    private void dismissToastAndChooser() {
        try {
            driver.findElement(By.id("dismiss-toast")).click();
            new WebDriverWait(driver, 5).until(
                ExpectedConditions.invisibilityOfElementLocated(By.cssSelector("[id^='toast-']")));
        } catch (Exception ignored) {}
    }

    // Closes the Desktop/Mobile chooser popup directly (used when NO toast is present — a toast would
    // close the chooser for us). No-op if the chooser is not currently shown.
    private void closePreviewChooserIfPresent() {
        try {
            if (!driver.findElements(closePreviewChooser).isEmpty()
                    && driver.findElement(closePreviewChooser).isDisplayed()) {
                jsClick(closePreviewChooser);
                new WebDriverWait(driver, 5).until(
                    ExpectedConditions.invisibilityOfElementLocated(closePreviewChooser));
            }
        } catch (Exception ignored) {}
    }

    // Clicks Preview, selects Desktop in the chooser, then waits for the preview tab to open, captures
    // its URL, closes it, and returns to the editor window.
    //
    // The Desktop/Mobile chooser does NOT auto-dismiss. Validation now runs after the Desktop pick:
    //   • Error → an "incomplete fields" toast; dismissing the toast (dismiss-toast) ALSO closes the
    //     chooser. We then nudge a choice question's option to re-trigger validation (retriggerQuestionIndices,
    //     tried in order — pass the multiple-choice question first), click the title, and reopen preview.
    //   • No toast + no tab → the chooser stays open, so we close it (close-preview-chooser) before retrying.
    //   • Success → the chooser stays open in the editor window, so we close it after capturing the URL.
    @Step("Open preview (select Desktop) and capture the preview URL (re-triggering validation if needed)")
    public String clickPreviewAndGetUrl(int... retriggerQuestionIndices) {
        try { Thread.sleep(2000); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        String mainWindow = driver.getWindowHandle();

        // Commit the last field (title click) then reload the editor so data loads fully before preview
        // — PDP precaution. validateAllInputs is idempotent, so it's safe if the step already called it.
        validateAllInputs();
        reloadEditorForProductLoad();

        openPreviewChooserAndSelectDesktop();

        // One attempt per retrigger candidate, plus a final attempt with a longer wait.
        final int MAX_ATTEMPTS = Math.max(3, retriggerQuestionIndices.length + 1);
        int retriggerIdx = 0;
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            boolean tabOpened = pollForTabOrToast(attempt < MAX_ATTEMPTS ? 10 : 30);
            if (tabOpened) break;
            if (attempt == MAX_ATTEMPTS) {
                throw new RuntimeException("Preview tab did not open after " + MAX_ATTEMPTS + " attempts");
            }

            if (toastDetectedDuringPoll) {
                System.out.println("[Preview] Incomplete-fields toast on attempt " + attempt
                    + " — dismissing (this also closes the Desktop/Mobile chooser)");
                dismissToastAndChooser();
            } else {
                System.out.println("[Preview] No tab or toast on attempt " + attempt
                    + " — closing the chooser and re-triggering");
                closePreviewChooserIfPresent();
            }

            // The chooser is now closed and the form is accessible — nudge a choice question's option
            // to re-trigger Bubble's validation, then click the title again before reopening preview.
            if (retriggerIdx < retriggerQuestionIndices.length) {
                int qIdx = retriggerQuestionIndices[retriggerIdx++];
                System.out.println("[Preview] Re-triggering validation on question " + qIdx);
                try {
                    String updated = retriggerChoiceValidation(qIdx);
                    // Store the updated option text so the guest journey selects the exact text.
                    syncRetriggeredOption(qIdx, updated);
                } catch (Exception e) {
                    System.out.println("[Preview] Retrigger of question " + qIdx + " failed: " + e.getMessage());
                }
            } else {
                System.out.println("[Preview] Retrigger candidates exhausted — retrying only");
            }
            clickSectionTitle();
            try { Thread.sleep(2000); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }

            openPreviewChooserAndSelectDesktop();
        }

        String previewUrl = null;
        for (String handle : driver.getWindowHandles()) {
            if (!handle.equals(mainWindow)) {
                driver.switchTo().window(handle);
                new WebDriverWait(driver, 30).until(d ->
                    d.getCurrentUrl() != null
                    && !d.getCurrentUrl().isEmpty()
                    && !d.getCurrentUrl().equals("about:blank"));
                previewUrl = driver.getCurrentUrl();
                driver.close();
                driver.switchTo().window(mainWindow);
                break;
            }
        }
        if (previewUrl == null) {
            throw new RuntimeException("Preview tab did not open or URL was not captured");
        }
        // On success the chooser stays open in the editor window — close it so later steps aren't blocked.
        closePreviewChooserIfPresent();
        System.out.println("[Preview] Captured preview URL: " + previewUrl);
        return previewUrl;
    }

    // Clears the logged-in session (cookies + localStorage + sessionStorage) and opens the captured
    // preview URL as an unauthenticated participant ("cleared cache and cookies guest"), so the
    // backend-injected demographic questions are shown instead of being skipped for a logged-in user.
    // The full guest answering journey is handled by the next feature — this only lands the guest on
    // the first screen of the preview.
    @Step("Clear cache/cookies and open the preview URL as a guest")
    public void navigateAsGuest(String previewUrl) {
        driver.get(ConfigReader.get("BASE_URL"));
        driver.manage().deleteAllCookies();
        ((JavascriptExecutor) driver).executeScript(
            "window.localStorage.clear(); window.sessionStorage.clear();");
        driver.get("about:blank");
        driver.get(previewUrl);
        // A guest sees the demographic questions (or the consent statement) first — wait for any of
        // the known first-screen landmarks so we confirm the preview loaded for the guest session.
        // Reload and retry (up to 2 reloads) to handle Bubble.io's occasional infinite-loading/blank.
        By firstScreen = By.cssSelector(
            "[id^='answer-Option-'], #continue-button, #agree-statement-button");
        waitForLandmarkElseReload(firstScreen, 20, 2);
    }
}

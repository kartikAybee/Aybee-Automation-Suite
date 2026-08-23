package com.aybee.pages;

import com.aybee.context.GlobalTestState;
import com.aybee.utils.ConfigReader;
import java.util.Arrays;
import io.qameta.allure.Step;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.FluentWait;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class FormQuestionsPage extends BasePage {

    // CTR experiments' 2 pre-added questions ARE the whole participant form (CTR adds no manual
    // questions). Whether the platform pre-adds them is backend-controlled, so it's driven by the
    // DEFAULT_QUESTIONS config flag (yes/no):
    //   yes (default) → the 2 default questions are pre-added (indices 1–2); the guest journey answers
    //                   them; FIRST_QUESTION_INDEX (=3) is where any future manual question would start.
    //   no            → no default questions; FIRST_QUESTION_INDEX=1; the guest journey skips answering
    //                   the (non-existent) default questions and proceeds straight to completion.
    // reloadAndCheckExtraQuestions and every index derive from these, so they adapt automatically.
    public static final boolean HAS_DEFAULT_QUESTIONS = ConfigReader.getYesNo("DEFAULT_QUESTIONS", true);
    public static final int DEFAULT_QUESTION_COUNT = HAS_DEFAULT_QUESTIONS ? 2 : 0;
    public static final int FIRST_QUESTION_INDEX = DEFAULT_QUESTION_COUNT + 1;

    private final By addQuestionButton    = By.id("newproject_formquestions_addquestion_button");
    private final By addManuallyButton    = By.id("add-manually-btn");
    private final By previewJourneyButton = By.id("newproject_formquestions_previewjourney_button");
    // Desktop/Mobile preview chooser popup — shown after clicking Preview. We always pick Desktop.
    // Validation/error toasts now fire AFTER the Desktop selection, not on the Preview click.
    private final By previewDesktopButton = By.id("preview-desktop");
    private final By closePreviewChooser  = By.id("close-preview-chooser");
    // NEW_PREVIEW=no (default): preview opens directly, no Desktop/Mobile chooser. yes: select Desktop
    // in the chooser after clicking Preview. Reverted to no for now.
    private final boolean useNewPreview = com.aybee.utils.ConfigReader.getYesNo("NEW_PREVIEW", false);

    // Asset upload popup — File-Upload-Asset-Input is the outer container;
    // dropzone is the clickable upload button inside it (id ends with bare "-").
    private final By fileUploadInput = By.id("File-Upload-Asset-Input");
    private final By dropzone        = By.cssSelector("[id='dropzone-']");
    private final By addUploadButton = By.id("add-Upload-Assets");

    // Filter sidebar
    private final By filterByScenarioTab  = By.id("filter-by-scenario");
    private final By filterByResponseTab  = By.id("filter-by-response");
    private final By addFilterQuestionBtn = By.id("button-add-question-filter-response");
    private final By applyFiltersButton   = By.id("button-apply-filters");

    // ── Scroll helper ─────────────────────────────────────────────────────────

    // Scrolls the element into view before returning it. Every interaction on the
    // form questions page uses this so that elements in newly-added cards (which
    // appear at the bottom of a growing list) are always in the viewport first.
    private WebElement scrollTo(By locator) {
        WebElement el = wait.until(ExpectedConditions.presenceOfElementLocated(locator));
        ((JavascriptExecutor) driver).executeScript(
            "arguments[0].scrollIntoView({behavior: 'instant', block: 'center'});", el);
        return el;
    }

    // ── CTR page load validation ──────────────────────────────────────────────

    // Waits for the add-question button to be clickable (page initial load), reloads
    // so Bubble.io fully renders both pre-added question cards, then waits again.
    @Step("Wait for form questions page to load, reload, then verify all questions are rendered")
    public FormQuestionsPage waitForFormQuestionsPageLoaded() {
        new WebDriverWait(driver, 45)
                .until(ExpectedConditions.elementToBeClickable(addQuestionButton));
        driver.navigate().refresh();
        new WebDriverWait(driver, 45)
                .until(ExpectedConditions.elementToBeClickable(addQuestionButton));
        // Extra settle time for Bubble.io to finish rendering all pre-added question cards
        // after the button becomes clickable — cards appear asynchronously after the button.
        try { Thread.sleep(3000); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        return this;
    }

    // Clicks the preview journey button, waits for a new tab to open, captures the URL,
    // closes the tab, and returns the URL. No choice-question retrigger needed for CTR
    // since the 2 pre-added questions are long-text type with no answer options to validate.
    // Opens the preview: clicks the Preview button, waits for the Desktop/Mobile chooser popup, and
    // selects Desktop (preview-desktop). Validation/error toasts fire AFTER the Desktop selection.
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

    @Step("Open preview (select Desktop) and capture preview URL (CTR)")
    public String clickPreviewAndGetUrlCtr() {
        try { Thread.sleep(2000); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        String mainWindow = driver.getWindowHandle();
        // Native click on the section title so focus leaves the last field before previewing —
        // Bubble only validates a field once it is blurred (a JS click won't fire blur). Guarded by a
        // presence check so it's a fast no-op if absent; blurActiveElement() is a fallback.
        try {
            By titleLocator = By.id("experiment-questions-title");
            if (!driver.findElements(titleLocator).isEmpty()) {
                scrollTo(titleLocator);
                click(titleLocator);
            }
        } catch (Exception ignored) {}
        blurActiveElement();
        // Wait for Bubble's reactive validation pass to complete after blurring the field, before
        // opening the preview — mirrors the questionnaire suite's validate-then-preview sequence.
        try { Thread.sleep(3000); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        openPreviewChooserAndSelectDesktop();
        new WebDriverWait(driver, 30).until(d -> d.getWindowHandles().size() > 1);
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
            throw new RuntimeException("[CTR] Preview tab did not open or URL was not captured");
        }
        // On success the chooser stays open in the editor window — close it so later steps aren't blocked.
        closePreviewChooserIfPresent();
        return previewUrl;
    }

    // ── Pre-condition check ───────────────────────────────────────────────────

    // Reloads the page first so Bubble.io fully renders all question IDs, then
    // checks for unexpected pre-added questions using icon_delete_form_question_{n}
    // (free default questions have no delete icon so only extras match).
    // Soft-fails the scenario so the overall run is marked failed, then deletes
    // each extra in descending index order so subsequent steps proceed on clean state.
    @Step("Wait for form questions page, reload, then delete any unexpected pre-added questions")
    public void reloadAndCheckExtraQuestions() {
        // Wait for the page to be fully rendered before reloading.
        wait.until(ExpectedConditions.visibilityOfElementLocated(addQuestionButton));

        // DEFAULT_QUESTIONS=no → skip the pre-added / extra-question check entirely and add our
        // questions directly (index 1), exactly like PDP. Whatever pre-added cards the template may
        // render are ignored; the participant journey is title-driven and does not expect them.
        if (!HAS_DEFAULT_QUESTIONS) {
            System.out.println("[FormQuestions] DEFAULT_QUESTIONS=no — skipping pre-added-question check; adding questions directly (PDP-style)");
            return;
        }

        driver.navigate().refresh();

        // Wait for the page to be ready again after reload.
        wait.until(ExpectedConditions.visibilityOfElementLocated(addQuestionButton));

        // Extra 5s for Bubble.io to finish rendering all question cards after the add-question
        // button becomes visible — the button appears before the question list is fully populated.
        try { Thread.sleep(5000); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }

        // Poll until the toggle-group count is stable across two consecutive 1s checks.
        // Bubble.io renders extra questions asynchronously after the page settles, so
        // a fixed sleep can end before all extras have appeared in the DOM.
        new FluentWait<>(driver)
            .withTimeout(20, TimeUnit.SECONDS)
            .pollingEvery(1, TimeUnit.SECONDS)
            .ignoring(Exception.class)
            .until(d -> {
                int first  = d.findElements(By.cssSelector("[id$='-toggle-group']")).size();
                try { Thread.sleep(1000); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
                int second = d.findElements(By.cssSelector("[id$='-toggle-group']")).size();
                return first == second;
            });

        // All questions now carry {n}-toggle-group IDs, including the 3 pre-added ones
        // (indices 1–3). Only delete extras whose index >= FIRST_QUESTION_INDEX (4+) —
        // those are unexpectedly leftover user-added questions from a previous run.
        List<WebElement> toggleGroups = driver.findElements(
            By.cssSelector("[id$='-toggle-group']"));

        // Extract numeric indices, skipping the pre-added questions (1–3).
        List<Integer> extraIndices = new ArrayList<>();
        for (WebElement tg : toggleGroups) {
            String id = tg.getAttribute("id");
            try {
                int idx = Integer.parseInt(id.replace("-toggle-group", ""));
                if (idx >= FIRST_QUESTION_INDEX) extraIndices.add(idx);
            } catch (NumberFormatException ignored) {}
        }

        if (extraIndices.isEmpty()) return;
        // Delete highest index first so earlier indices are unaffected.
        extraIndices.sort(Collections.reverseOrder());

        for (int idx : extraIndices) {
            By toggleGroup = By.id(idx + "-toggle-group");
            By deleteIcon  = By.id("icon_delete_form_question_" + idx);

            // Brief pause before each deletion — Bubble.io may still be writing the
            // question record server-side, making the delete icon unresponsive if clicked too soon.
            try { Thread.sleep(2000); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }

            // Ensure the toggle-group row is fully visible before hovering — the delete
            // icon is only revealed by hover, so the element must be in the viewport.
            WebElement toggle = new WebDriverWait(driver, 30)
                .until(ExpectedConditions.visibilityOfElementLocated(toggleGroup));
            scrollToCenter(toggle);
            toggle = new WebDriverWait(driver, 30)
                .until(ExpectedConditions.visibilityOfElementLocated(toggleGroup));

            // Hover over the toggle-group row — this reveals the delete icon.
            new Actions(driver).moveToElement(toggle).perform();
            new WebDriverWait(driver, 30)
                .until(ExpectedConditions.visibilityOfElementLocated(deleteIcon));
            ((JavascriptExecutor) driver).executeScript("arguments[0].click();",
                driver.findElement(deleteIcon));
            // Wait for the deleted card to disappear before moving to the next one.
            new WebDriverWait(driver, 30)
                .until(ExpectedConditions.invisibilityOfElementLocated(toggleGroup));
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
        wait.until(ExpectedConditions.visibilityOfElementLocated(addManuallyButton));
        jsClick(addManuallyButton);
        new WebDriverWait(driver, 30)
            .until(ExpectedConditions.visibilityOfElementLocated(By.id(index + "-toggle-group")));
        scrollTo(By.id(index + "-questionInput"));
        expandIfCollapsed(index);
        return this;
    }

    // ── Common question fields ─────────────────────────────────────────────────

    @Step("Enter question text for question {index}")
    public FormQuestionsPage enterQuestionText(int index, String text) {
        WebElement input = scrollTo(By.id(index + "-questionInput"));
        input.clear();
        input.sendKeys(text);
        return this;
    }

    // Dropdown is a native <select>; values include literal quote characters per DOM.
    // Uses JS value injection + dispatchEvent — same reason as selectShowToParticipants.
    @Step("Select question type '{typeValue}' for question {index}")
    public FormQuestionsPage selectQuestionType(int index, String typeValue) {
        WebElement select = scrollTo(By.id("dp_type_of_question_form_page_" + index));
        ((JavascriptExecutor) driver).executeScript(
            "arguments[0].value = arguments[1];" +
            "arguments[0].dispatchEvent(new Event('change', {bubbles: true}));",
            select, "\"" + typeValue + "\"");
        return this;
    }

    // Skip for "just_question" — it is the default selection.
    // Uses JS value injection + dispatchEvent to avoid Select.selectByValue() internally
    // clicking native option elements, which fails when they render outside the viewport.
    @Step("Select Show to Participants '{showValue}' for question {index}")
    public FormQuestionsPage selectShowToParticipants(int index, String showValue) {
        WebElement select = scrollTo(By.id("dp_show_participants_form_question_" + index));
        ((JavascriptExecutor) driver).executeScript(
            "arguments[0].value = arguments[1];" +
            "arguments[0].dispatchEvent(new Event('change', {bubbles: true}));",
            select, "\"" + showValue + "\"");
        return this;
    }

    @Step("Select specific product for question {index}")
    public FormQuestionsPage selectSpecificProduct(int index, String partialName) {
        By locator = By.id(index + "-product-select-dropdown");
        scrollTo(locator);
        // Wait for options to populate before selecting — Bubble.io loads them asynchronously.
        new WebDriverWait(driver, 30).until(d ->
            new Select(d.findElement(locator)).getOptions().size() > 1);
        try { Thread.sleep(1500); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        WebElement dropdown = driver.findElement(locator);
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
            dropdown, partialName);
        if (!Boolean.TRUE.equals(found)) {
            throw new RuntimeException("Product not found in dropdown: " + partialName);
        }
        try { Thread.sleep(1500); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        return this;
    }

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
        // Primary: sendKeys directly on the File-Upload-Asset-Input container.
        // Works for any file size — no base64 size limit. This was the original approach.
        try {
            System.out.println("[FileUpload] Primary — sendKeys on File-Upload-Asset-Input");
            WebElement el = wait.until(ExpectedConditions.presenceOfElementLocated(fileUploadInput));
            el.sendKeys(filePath);
            System.out.println("[FileUpload] Primary succeeded");
        } catch (Exception e) {
            // Fallback: JS DataTransfer drop. Works for small files (< ~100KB) only —
            // base64-encoding large files exceeds Selenium's JS argument size limit.
            System.out.println("[FileUpload] Primary failed (" + e.getClass().getSimpleName() + ") — trying DataTransfer fallback");
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
        // Allow Bubble.io to finish saving the uploaded asset before proceeding.
        try { Thread.sleep(2000); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
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
        // Scroll the dropdown into view first then wait for visibility — same pattern
        // as enterAnswerText: scroll before waiting so the element is in the viewport.
        WebElement dropdown = new WebDriverWait(driver, 30).until(
            ExpectedConditions.visibilityOfElementLocated(locator));
        scrollToCenter(dropdown);
        try { Thread.sleep(1000); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        // Re-fetch after scroll to avoid stale reference.
        dropdown = driver.findElement(locator);
        ((JavascriptExecutor) driver).executeScript(
            "var sel = arguments[0], val = arguments[1];" +
            "sel.value = '\"' + val + '\"';" +
            "sel.dispatchEvent(new Event('change', {bubbles: true}));",
            dropdown, scaleValue);
        try { Thread.sleep(1500); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        return this;
    }

    @Step("Wait for {expectedCount} Likert options for question {index}")
    public FormQuestionsPage waitForLikertOptions(int index, int expectedCount) {
        // Scroll the question card into view before polling — Likert options render at
        // the bottom of a growing card list and scrollIntoView is a no-op for off-screen
        // elements. Mirrors the scroll-first pattern in enterAnswerText.
        WebElement card = new WebDriverWait(driver, 30).until(
            ExpectedConditions.visibilityOfElementLocated(By.id(index + "-toggle-group")));
        scrollToCenter(card);
        new WebDriverWait(driver, 60).until(d ->
            d.findElements(By.cssSelector("[id^='" + index + "--answerInput-']")).size() >= expectedCount);
        // Brief pause for Bubble.io to finish rendering option controls (delete, randomize icons).
        try { Thread.sleep(1500); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        return this;
    }

    // ── Answer options ────────────────────────────────────────────────────────

    private int countAnswerOptions(int questionIndex) {
        return driver.findElements(
            By.cssSelector("[id^='" + questionIndex + "--answerInput-']")).size();
    }

    @Step("Wait for answer options to appear for question {questionIndex}")
    public FormQuestionsPage waitForAnswerOptions(int questionIndex) {
        new WebDriverWait(driver, 30).until(d ->
            !d.findElements(By.cssSelector("[id^='" + questionIndex + "--answerInput-']")).isEmpty());
        return this;
    }

    // Polls until exactly expectedCount answer option inputs exist. Use this for question
    // types (single choice, multiple choice) where the platform does not auto-create options
    // — just wait for the platform to finish rendering whatever count it creates.
    @Step("Wait for {expectedCount} answer options on question {questionIndex}")
    public FormQuestionsPage waitForAnswerOptionCount(int questionIndex, int expectedCount) {
        new WebDriverWait(driver, 30).until(d ->
            d.findElements(
                By.cssSelector("[id^='" + questionIndex + "--answerInput-']")).size() >= expectedCount);
        return this;
    }

    // Deletes extras from the end first, then adds if short — ensures exactly targetCount fields.
    // Each add/delete polls until the DOM count changes (no sleep) with a generous timeout
    // to handle Bubble.io backend latency on answer-option creation.
    @Step("Ensure {targetCount} answer option fields for question {questionIndex}")
    public FormQuestionsPage ensureAnswerOptionCount(int questionIndex, int targetCount) {
        waitForAnswerOptions(questionIndex);
        // Bubble.io auto-creates default answer options asynchronously — wait for the
        // count to stabilise before we start adding or deleting.
        try { Thread.sleep(3000); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        int current = countAnswerOptions(questionIndex);
        while (current > targetCount) {
            By deleteIcon   = By.id(questionIndex + "-deleteIcon-" + current);
            By deletedInput = By.id(questionIndex + "--answerInput-" + current);
            scrollTo(deleteIcon);
            jsClick(deleteIcon);
            new WebDriverWait(driver, 30).until(
                ExpectedConditions.invisibilityOfElementLocated(deletedInput));
            current = countAnswerOptions(questionIndex);
        }
        while (current < targetCount) {
            int next = current + 1;
            By addBtn  = By.id(questionIndex + "-addAnswer-btn");
            By newInput = By.id(questionIndex + "--answerInput-" + next);
            scrollTo(addBtn);
            // Wait for the button to be visible — Bubble.io may still be rendering the
            // previous option when we arrive here.
            new WebDriverWait(driver, 30).until(
                ExpectedConditions.visibilityOfElementLocated(addBtn));
            jsClick(addBtn);
            // If the new input doesn't appear within 15 s, retry the click once.
            // Bubble.io occasionally swallows the first click while processing the prior add.
            try {
                new WebDriverWait(driver, 30).until(d ->
                    !d.findElements(newInput).isEmpty());
            } catch (TimeoutException retry) {
                jsClick(addBtn);
                new WebDriverWait(driver, 30).until(d ->
                    !d.findElements(newInput).isEmpty());
            }
            current = countAnswerOptions(questionIndex);
        }
        return this;
    }

    @Step("Enter answer text at index {answerIndex} for question {questionIndex}")
    public FormQuestionsPage enterAnswerText(int questionIndex, int answerIndex, String text) {
        By addBtn   = By.id(questionIndex + "-addAnswer-btn");
        By inputLoc = By.id(questionIndex + "--answerInput-" + answerIndex);

        // Scroll the card into view first, then wait for the add-answer button to be
        // visible — after selectQuestionType / selectShowToParticipants Bubble.io
        // re-renders the answer section asynchronously, and scrollIntoView on an
        // off-screen element has no effect before the element is in the viewport.
        WebElement addBtnEl = new WebDriverWait(driver, 30).until(
            ExpectedConditions.visibilityOfElementLocated(addBtn));
        scrollToCenter(addBtnEl);

        // Wait for at least one answer input to exist — Bubble.io auto-creates default
        // options after type/show changes but does so asynchronously. Without this wait
        // single-choice and multiple-choice cards are not yet fully rendered when the
        // first enterAnswerText arrives (unlike limited-choice which went through
        // ensureAnswerOptionCount first and implicitly got this stabilisation).
        new WebDriverWait(driver, 30).until(d ->
            !d.findElements(By.cssSelector("[id^='" + questionIndex + "--answerInput-']")).isEmpty());
        // Brief pause for Bubble.io to finish any reactive cascade after the type change.
        try { Thread.sleep(1500); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }

        // Add slots only if the target index does not yet exist.
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

        // Re-scroll to the add button (keeps the full card in viewport) then to the
        // target input. Re-fetch the input reference after scroll so stale-element
        // exceptions from Bubble.io's reactive re-renders don't reach clear()/sendKeys().
        addBtnEl = wait.until(ExpectedConditions.visibilityOfElementLocated(addBtn));
        scrollToCenter(addBtnEl);
        WebElement input = new WebDriverWait(driver, 30).until(
            ExpectedConditions.elementToBeClickable(inputLoc));
        scrollToCenter(input);
        // Re-verify clickable after scroll — Bubble.io may reposition the element during
        // the scrollIntoView animation, making it briefly non-interactable.
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

    // Clicking a randomize icon after toggle is ON disables randomize for that specific answer.
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
        // Re-verify visibility after scroll — Bubble.io may reposition the element
        // during the scrollIntoView animation before the click lands.
        new WebDriverWait(driver, 30).until(
            ExpectedConditions.visibilityOfElementLocated(deleteIcon));
        jsClick(deleteIcon);
        try { Thread.sleep(1500); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        return this;
    }

    // ── Filter sidebar ────────────────────────────────────────────────────────

    @Step("Open filter sidebar for question {index}")
    public FormQuestionsPage openFilterSidebar(int index) {
        scrollTo(By.id("icon_filter_form_question_" + index));
        jsClick(By.id("icon_filter_form_question_" + index));
        wait.until(ExpectedConditions.visibilityOfElementLocated(filterByScenarioTab));
        return this;
    }

    @Step("Click Filter by Scenario tab")
    public FormQuestionsPage clickFilterByScenarioTab() {
        jsClick(filterByScenarioTab);
        // Wait for scenario tab content to load — scenario-A is always present in the sidebar.
        new WebDriverWait(driver, 30).until(
            ExpectedConditions.visibilityOfElementLocated(By.id("scenario-A")));
        return this;
    }

    @Step("Select scenario '{scenarioId}'")
    public FormQuestionsPage selectScenario(String scenarioId) {
        jsClick(By.id(scenarioId));
        return this;
    }

    // Selects the bought-product chips for both scenarios. The Filter by Scenario tab has two
    // sections — Scenario Names and Products — and you may select from only ONE section, but
    // MULTIPLE entries within it. Selecting both products (one per scenario) keeps the filter
    // from being reliant on a single scenario. Each element is clicked at most once (the OR is
    // evaluated per chip), so a chip whose truncated id matches both names is not toggled off.
    // IDs are a partial/truncated form of the displayed name, so we match word-by-word.
    @Step("Select specific bought products for scenario A and B")
    public FormQuestionsPage selectSpecificBoughtProduct(String nameA, String nameB) {
        List<WebElement> products = wait.until(
            ExpectedConditions.presenceOfAllElementsLocatedBy(
                By.cssSelector("[id^='specific-product-']")));
        boolean clicked = false;
        for (WebElement el : products) {
            String id = el.getAttribute("id").toLowerCase();
            if (idMatchesName(id, nameA) || idMatchesName(id, nameB)) {
                ((JavascriptExecutor) driver).executeScript("arguments[0].click();", el);
                clicked = true;
            }
        }
        if (!clicked) throw new RuntimeException(
            "No specific bought product chip found for: " + nameA + " / " + nameB);
        return this;
    }

    private boolean idMatchesName(String id, String name) {
        if (name == null || name.isBlank()) return false;
        for (String word : name.split("\\s+")) {
            if (word.length() > 2 && id.contains(word.toLowerCase())) return true;
        }
        return false;
    }

    // ── Section mutual-exclusion ───────────────────────────────────────────────
    // The Filter by Scenario tab has two sections — Scenario Names and Products — and only ONE
    // may have selections at a time. A chip is "selected" when Bubble adds an inline border to
    // its #id element (style contains "border-width"); unselected chips have no style attribute.
    // We clear the opposite section before selecting in the intended one so the two never mix.

    // Clears any selected Scenario Names chips — call before selecting Products.
    @Step("Clear any selected scenarios (keep only the Products section active)")
    public FormQuestionsPage clearScenarioSelection() {
        for (String id : new String[]{"scenario-A", "scenario-B"}) {
            deselectIfSelected(By.id(id));
        }
        return this;
    }

    // Clears any selected Product chips — call before selecting Scenario Names.
    @Step("Clear any selected products (keep only the Scenario Names section active)")
    public FormQuestionsPage clearProductSelection() {
        for (WebElement el : driver.findElements(By.cssSelector("[id^='specific-product-']"))) {
            if (isChipSelected(el)) {
                ((JavascriptExecutor) driver).executeScript("arguments[0].click();", el);
            }
        }
        return this;
    }

    private void deselectIfSelected(By locator) {
        List<WebElement> els = driver.findElements(locator);
        if (els.isEmpty()) return;
        WebElement el = els.get(0);
        if (isChipSelected(el)) {
            ((JavascriptExecutor) driver).executeScript("arguments[0].click();", el);
        }
    }

    // Selected chips carry an inline border (style="border-width: 2px; ...") on their #id element;
    // unselected chips have no style attribute. This is more reliable than computed border-width,
    // which CSS reports as 0 when border-style is unset.
    private boolean isChipSelected(WebElement el) {
        String style = el.getAttribute("style");
        return style != null && style.contains("border-width");
    }

    @Step("Click Filter by Responses tab")
    public FormQuestionsPage clickFilterByResponseTab() {
        scrollTo(filterByResponseTab);
        // Real click — jsClick bypasses Bubble.io's tab activation handler.
        new WebDriverWait(driver, 10).until(
            ExpectedConditions.elementToBeClickable(filterByResponseTab)).click();
        // Scenario-A element disappearing confirms the Scenario tab content is now hidden.
        new WebDriverWait(driver, 15).until(
            ExpectedConditions.invisibilityOfElementLocated(By.id("scenario-A")));
        // Add Question button becoming clickable confirms Response tab is fully active.
        new WebDriverWait(driver, 15).until(
            ExpectedConditions.elementToBeClickable(addFilterQuestionBtn));
        return this;
    }

    @Step("Click Add Filter Question and wait for filter question dropdown {expectedFilterIndex}")
    public FormQuestionsPage clickAddFilterQuestion(int expectedFilterIndex) {
        // Wait for the button to be clickable — tab switch may still be rendering.
        new WebDriverWait(driver, 30).until(
            ExpectedConditions.elementToBeClickable(addFilterQuestionBtn));
        jsClick(addFilterQuestionBtn);
        By dropdownLocator = By.id("dropdown-filter-question-" + expectedFilterIndex);
        new WebDriverWait(driver, 30).until(
            ExpectedConditions.visibilityOfElementLocated(dropdownLocator));
        // Wait for question options to populate — the dropdown appears with only the
        // placeholder at first; actual options load asynchronously from Bubble.io.
        new WebDriverWait(driver, 30).until(d ->
            new Select(d.findElement(dropdownLocator)).getOptions().size() > 1);
        return this;
    }

    // dropdown-filter-question-{j} is a native <select>; matches by partial text since
    // long question texts may be truncated in dropdown options.
    @Step("Select filter question {filterIndex} by partial text")
    public FormQuestionsPage selectFilterQuestion(int filterIndex, String partialQuestionText) {
        By dropdownLocator = By.id("dropdown-filter-question-" + filterIndex);
        // Wait for the target question to appear as an option — for later filter entries
        // Bubble.io removes already-selected questions from this dropdown dynamically.
        new WebDriverWait(driver, 30).until(d -> {
            try {
                return new Select(d.findElement(dropdownLocator)).getOptions().stream()
                    .anyMatch(o -> o.getText().contains(partialQuestionText));
            } catch (Exception e) { return false; }
        });
        // Brief pause so Bubble.io finishes any pending reactive updates before we set
        // the value — without this the selection is applied and then immediately cleared.
        try { Thread.sleep(1500); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        // Use JS to set value + dispatch a bubbling change event — selectByVisibleText
        // does not fire the change event that Bubble.io's reactive system listens to.
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

    // The answer option IDs are prefixed with the referenced question's index, not the
    // filter entry index — match by suffix to avoid hard-coding the question index here.
    @Step("Select answer option '{optionText}' for filter question {filterIndex}")
    public FormQuestionsPage selectFilterAnswerOption(int filterIndex, String optionText) {
        By locator = By.cssSelector("[id$='-select-answer-option-filter-" + optionText + "']");
        WebElement el = wait.until(ExpectedConditions.presenceOfElementLocated(locator));
        ((JavascriptExecutor) driver).executeScript(
            "arguments[0].scrollIntoView({behavior: 'instant', block: 'center'});", el);
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", el);
        return this;
    }

    // Waits for any late-appearing blank options, then deletes them highest-index first.
    // Returns true if any empty option was found — caller uses this to record a soft failure,
    // since blank options should never appear when form questions are set up correctly.
    @Step("Delete any blank answer options from question {questionIndex}")
    public boolean deleteEmptyAnswerOptions(int questionIndex) {
        try { Thread.sleep(3000); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        boolean foundEmpty = false;
        int total = countAnswerOptions(questionIndex);
        for (int i = total; i >= 1; i--) {
            List<WebElement> els = driver.findElements(By.id(questionIndex + "--answerInput-" + i));
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

    // Returns true if any empty options were found and deleted — propagated to the step
    // so a soft failure can be recorded without stopping the flow.
    @Step("Scroll to question {index}, expand if collapsed, and delete empty answer options")
    public boolean cleanupEmptyOptions(int index) {
        scrollTo(By.id(index + "-toggle-group"));
        expandIfCollapsed(index);
        return deleteEmptyAnswerOptions(index);
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
        jsClick(applyFiltersButton);
        wait.until(ExpectedConditions.invisibilityOfElementLocated(applyFiltersButton));
        return this;
    }

    // ── Preview Journey ───────────────────────────────────────────────────────

    // ── Preview Journey ── helper ─────────────────────────────────────────────

    private static void sleep2s() {
        try { Thread.sleep(2000); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }

    private void syncRetriggered(int qIdx, String updated) {
        if (updated == null || updated.isEmpty()) return;
        if (qIdx == FIRST_QUESTION_INDEX + 1
                && GlobalTestState.q2SelectOptions != null
                && GlobalTestState.q2SelectOptions.size() >= 2) {
            GlobalTestState.q2SelectOptions = Arrays.asList(updated, GlobalTestState.q2SelectOptions.get(1));
        } else if (qIdx == FIRST_QUESTION_INDEX + 2) {
            GlobalTestState.q3SelectOption = updated;
        } else if (qIdx == FIRST_QUESTION_INDEX + 3
                && GlobalTestState.q4SelectOptions != null
                && GlobalTestState.q4SelectOptions.size() >= 2) {
            GlobalTestState.q4SelectOptions = Arrays.asList(updated, GlobalTestState.q4SelectOptions.get(1));
        }
        // Q5 and Q6 — no GlobalTestState sync needed; participant form selects by scale position ID
    }

    private String retriggerChoiceValidation(int questionIndex) {
        sleep2s();
        scrollTo(By.id(questionIndex + "-toggle-group"));
        sleep2s();
        expandIfCollapsed(questionIndex);

        By addBtn      = By.id(questionIndex + "-addAnswer-btn");
        By firstAnswer = By.id(questionIndex + "--answerInput-1");

        sleep2s();
        WebElement addBtnEl = scrollTo(addBtn);
        sleep2s();
        scrollToCenter(addBtnEl);

        sleep2s();
        WebElement input = scrollTo(firstAnswer);
        sleep2s();
        new WebDriverWait(driver, 30).until(ExpectedConditions.elementToBeClickable(input));
        scrollToCenter(input);
        sleep2s();
        input.sendKeys("s");

        String updatedText = input.getAttribute("value");
        System.out.println("[Retrigger] Q" + questionIndex + " first option updated to: " + updatedText);

        sleep2s();
        jsClick(By.id(questionIndex + "-toggle-group"));
        return updatedText;
    }

    // True if the "incomplete form fields" toast was seen on the most recent clickPreviewAndGetUrl
    // True if the "incomplete form fields" toast was seen on the most recent clickPreviewAndGetUrl call.
    private boolean incompleteToastSeenOnLastPreview = false;

    public boolean wasIncompleteToastSeen() {
        return incompleteToastSeenOnLastPreview;
    }

    // Set to true by pollForTabOrToast the instant the error toast becomes visible —
    // used by clickPreviewAndGetUrl to gate the retrigger. A separate flag is needed
    // because pollForTabOrToast returns false for BOTH "toast seen" and "timeout", and
    // re-checking the DOM after the poll returns risks missing a toast that auto-fades.
    private boolean toastDetectedDuringPoll = false;

    // Polls for up to timeoutSecs seconds. Returns true when a new tab appears.
    // Returns false when the error toast is detected (sets toastDetectedDuringPoll = true)
    // or when the poll times out without either — caller checks toastDetectedDuringPoll
    // to distinguish the two cases and gate the retrigger logic accordingly.
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
        return false; // timeout — no tab, no toast
    }

    // Clicks the preview button (which opens a new tab), waits for the tab's URL to
    // resolve past about:blank, captures it, closes the tab, and returns to the main window.
    // If Bubble.io raises the "incomplete form fields" toast, dismisses it, re-triggers
    // Limited Choice validation, and retries — up to 3 attempts total.
    // Sets incompleteToastSeenOnLastPreview so the step can record a soft failure.
    @Step("Click Preview Journey and capture URL from new tab")
    public String clickPreviewAndGetUrl() {
        try { Thread.sleep(3000); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        String mainWindow = driver.getWindowHandle();
        incompleteToastSeenOnLastPreview = false;

        // Questions retriggered one-per-toast in order — each toast dismissal clears
        // the signal so the next attempt gets a fresh toast, confirming whether validation
        // is still failing after the previous edit.
        int[] retriggerQueue = {
            FIRST_QUESTION_INDEX + 1,  // Q2 — Limited Choice
            FIRST_QUESTION_INDEX + 2,  // Q3 — Single Choice
            FIRST_QUESTION_INDEX + 3,  // Q4 — Multiple Choice
            FIRST_QUESTION_INDEX + 4,  // Q5 — Likert Horizontal
            FIRST_QUESTION_INDEX + 5   // Q6 — Likert Vertical
        };
        int retriggerIdx = 0;

        // Click free-question-1 to trigger a Bubble.io DB validation pass before preview, giving the
        // backend time to mark all questions complete and clear any stale incomplete state. A
        // "free-question" is a default/pre-added question, so it only exists when DEFAULT_QUESTIONS=yes;
        // guard on the flag and presence so this no-ops when there are no default questions —
        // blurActiveElement() below provides the same blur-to-validate effect regardless.
        if (HAS_DEFAULT_QUESTIONS) {
            try {
                By freeQ = By.id("free-question-1");
                if (!driver.findElements(freeQ).isEmpty()) jsClick(freeQ);
            } catch (Exception ignored) {}
        }
        // Native click on the section title so focus actually LEAVES the last field — Bubble only
        // validates a field once it is blurred, and a JS click does not change document.activeElement
        // or fire the field's blur event. Guarded by a presence check so it's a fast no-op if this
        // experiment type has no such title element; blurActiveElement() is a fallback.
        try {
            By titleLocator = By.id("experiment-questions-title");
            if (!driver.findElements(titleLocator).isEmpty()) {
                scrollTo(titleLocator);
                click(titleLocator);
            }
        } catch (Exception ignored) {}
        blurActiveElement();
        try { Thread.sleep(3000); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }

        openPreviewChooserAndSelectDesktop();

        final int MAX_ATTEMPTS = retriggerQueue.length + 1;
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            boolean tabOpened = pollForTabOrToast(attempt < MAX_ATTEMPTS ? 10 : 30);
            if (tabOpened) break;

            if (attempt == MAX_ATTEMPTS) {
                throw new RuntimeException(
                    "Preview tab did not open after " + MAX_ATTEMPTS + " attempts");
            }

            if (toastDetectedDuringPoll) {
                incompleteToastSeenOnLastPreview = true;
                // Dismiss the toast immediately — its purpose (signal validation failed) is
                // served. Dismissing it ALSO closes the Desktop/Mobile chooser popup. The next
                // attempt will produce a fresh toast if still failing.
                dismissToastAndChooser();

                if (retriggerIdx < retriggerQueue.length) {
                    int qIdx = retriggerQueue[retriggerIdx++];
                    System.out.println("[Preview] Toast on attempt " + attempt
                        + " — retriggering question index " + qIdx);
                    String updated = retriggerChoiceValidation(qIdx);
                    syncRetriggered(qIdx, updated);
                } else {
                    System.out.println("[Preview] Toast on attempt " + attempt
                        + " — all questions already retriggered, retrying click");
                }
                try { Thread.sleep(2000); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            } else {
                System.out.println("[Preview] No tab or toast on attempt " + attempt
                    + " — closing chooser and retrying");
                closePreviewChooserIfPresent();
            }
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
        return previewUrl;
    }

    // Clears the logged-in session (cookies + localStorage + sessionStorage) and navigates
    // to the preview URL as an unauthenticated participant so that backend-injected
    // demographics questions are visible instead of being skipped for logged-in users.
    // Uses a short page-load timeout because Bubble.io's SPA never fires window.onload —
    // driver.get() with the default 300 s timeout would block forever. After catching the
    // expected timeout we wait for the first demographic question to confirm the page is ready.
    @Step("Clear session and open preview URL as guest")
    public void navigateAsGuest(String previewUrl) {
        driver.get(ConfigReader.get("BASE_URL"));
        driver.manage().deleteAllCookies();
        ((JavascriptExecutor) driver).executeScript(
            "window.localStorage.clear(); window.sessionStorage.clear();");
        driver.get("about:blank");
        driver.get(previewUrl);
        // Reload past Bubble's occasional ghost/blank first render before the page is ready.
        waitForLandmarkElseReload(By.id("continue-button"), 20, 2);
    }
}

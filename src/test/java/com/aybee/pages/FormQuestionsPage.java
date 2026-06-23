package com.aybee.pages;

import io.qameta.allure.Step;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.FluentWait;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;
import java.util.List;
import java.util.concurrent.TimeUnit;

// QAT (Quick Asset Testing) form-questions page.
//
// The generic mechanics here — card lifecycle, question text, question type, answer
// options, the "uploaded_image"/"just_question" Show options, randomize/exclusive/delete,
// Likert, and the scenario/product filter sidebar — are all the SAME as the msjourney
// FormQuestionsPage and are already exercised by that suite. This class deliberately only
// ports the helpers needed to drive the QAT-EXCLUSIVE behaviour:
//
//   • Show to Participants options unique to QAT: all_creatives, top_1_choice, specific_creative
//   • A filter sidebar with a single section (Filter by Responses only) and NO tabs
//
// Everything else is intentionally minimal — just enough to stand up valid, filterable
// questions so the new Show options and the QAT filter can be covered without re-testing
// behaviour that msjourney already covers.
public class FormQuestionsPage extends BasePage {

    // QAT shows NO pre-added/default questions — the list starts empty, so the first
    // user-added question is index 1 and every element ID is built from that index.
    public static final int FIRST_QUESTION_INDEX = 1;

    private final By addQuestionButton    = By.id("newproject_formquestions_addquestion_button");
    private final By addManuallyButton    = By.id("add-manually-btn");
    private final By previewJourneyButton = By.id("newproject_formquestions_previewjourney_button");

    // Asset upload popup (shared with msjourney — used only by the uploaded_image Show option).
    private final By fileUploadInput = By.id("File-Upload-Asset-Input");
    private final By addUploadButton = By.id("add-Upload-Assets");

    // ── QAT filter sidebar ──────────────────────────────────────────────────────
    // QAT exposes only the response-based filter, so there are NO "Filter by Scenario /
    // Filter by Responses" tabs — button-add-question-filter-response is shown directly
    // alongside the Filter-by-Responses content.
    private final By addFilterQuestionBtn = By.id("button-add-question-filter-response");
    private final By applyFiltersButton   = By.id("button-apply-filters");

    // ── Scroll helper ─────────────────────────────────────────────────────────

    private WebElement scrollTo(By locator) {
        WebElement el = wait.until(ExpectedConditions.presenceOfElementLocated(locator));
        ((JavascriptExecutor) driver).executeScript(
            "arguments[0].scrollIntoView({behavior: 'instant', block: 'center'});", el);
        return el;
    }

    // ── Card lifecycle ────────────────────────────────────────────────────────

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
        // No reload / settle-sleep — QAT has no default questions. Just wait for the add
        // button to be clickable (also the gate between consecutive adds), then open the popup.
        new WebDriverWait(driver, 30).until(
            ExpectedConditions.elementToBeClickable(addQuestionButton));
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

    @Step("Select question type '{typeValue}' for question {index}")
    public FormQuestionsPage selectQuestionType(int index, String typeValue) {
        WebElement select = scrollTo(By.id("dp_type_of_question_form_page_" + index));
        ((JavascriptExecutor) driver).executeScript(
            "arguments[0].value = arguments[1];" +
            "arguments[0].dispatchEvent(new Event('change', {bubbles: true}));",
            select, "\"" + typeValue + "\"");
        return this;
    }

    // ── Show to Participants ─────────────────────────────────────────────────────
    // QAT dropdown (dp_show_participants_form_question_{n}) values:
    //   just_question     — Just Question        (default; same as msjourney — not covered here)
    //   uploaded_image    — Uploaded Image        (asset-upload popup; same as msjourney — not here)
    //   all_creatives     — All Creatives         (BOTH uploaded images shown)            ← QAT-only
    //   top_1_choice      — Top 1 Choice          (the participant's top-ranked creative) ← QAT-only
    //   specific_creative — Specific Creative     (reveals a version-select dropdown)      ← QAT-only

    // Sets the Show dropdown value. JS value-injection + bubbling change event — Bubble's
    // reactive system listens for change, and Select.selectByValue would click native
    // options that may render outside the viewport. Skip for just_question (the default).
    @Step("Select Show to Participants '{showValue}' for question {index}")
    public FormQuestionsPage selectShowToParticipants(int index, String showValue) {
        WebElement select = scrollTo(By.id("dp_show_participants_form_question_" + index));
        ((JavascriptExecutor) driver).executeScript(
            "arguments[0].value = arguments[1];" +
            "arguments[0].dispatchEvent(new Event('change', {bubbles: true}));",
            select, "\"" + showValue + "\"");
        try { Thread.sleep(1500); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        return this;
    }

    // specific_creative reveals a second dropdown to pick which uploaded version is shown.
    // The dropdown id encodes the question index: dp-which_version-dropdown_{index}. Its
    // options are single lowercase letters in alphabetical order, one per uploaded version
    // ("a", "b", … ); with two versions only "a" and "b" exist. The chosen letter maps to
    // that version's creative, which is the image that should be displayed at preview time.
    // Match the option by exact value/text so "a" never matches a longer option.
    @Step("Select specific creative version '{version}' for question {index}")
    public FormQuestionsPage selectSpecificCreative(int index, String version) {
        By locator = By.id("dp-which_version-dropdown_" + index);
        scrollTo(locator);
        new WebDriverWait(driver, 30).until(d ->
            new Select(d.findElement(locator)).getOptions().size() > 1);
        try { Thread.sleep(1500); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        WebElement dropdown = driver.findElement(locator);
        Boolean found = (Boolean) ((JavascriptExecutor) driver).executeScript(
            "var sel = arguments[0], want = arguments[1];" +
            "for (var i = 0; i < sel.options.length; i++) {" +
            "  var o = sel.options[i];" +
            "  if (o.value === want || o.text.trim() === want) {" +
            "    sel.value = o.value;" +
            "    sel.dispatchEvent(new Event('change', {bubbles: true}));" +
            "    return true;" +
            "  }" +
            "}" +
            "return false;",
            dropdown, "\"" + version + "\"");
        if (!Boolean.TRUE.equals(found)) {
            throw new RuntimeException("[QAT] Creative version '" + version +
                "' not found in dp-which_version-dropdown_" + index);
        }
        try { Thread.sleep(1500); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        return this;
    }

    // ── Uploaded-image Show option (same as msjourney; included so QAT can reuse it) ──

    @Step("Click asset upload field for question {index} to open upload popup")
    public FormQuestionsPage clickAssetUploadField(int index) {
        scrollTo(By.id(index + "-asset-upload-option-field"));
        jsClick(By.id(index + "-asset-upload-option-field"));
        wait.until(ExpectedConditions.presenceOfElementLocated(fileUploadInput));
        return this;
    }

    @Step("Send file path to upload input")
    public FormQuestionsPage uploadAssetFile(String filePath) {
        WebElement el = wait.until(ExpectedConditions.presenceOfElementLocated(fileUploadInput));
        el.sendKeys(filePath);
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

    // ── Answer options (minimal port — enough to create filterable choice questions) ──

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

    @Step("Enter answer text at index {answerIndex} for question {questionIndex}")
    public FormQuestionsPage enterAnswerText(int questionIndex, int answerIndex, String text) {
        By addBtn   = By.id(questionIndex + "-addAnswer-btn");
        By inputLoc = By.id(questionIndex + "--answerInput-" + answerIndex);

        WebElement addBtnEl = new WebDriverWait(driver, 30).until(
            ExpectedConditions.visibilityOfElementLocated(addBtn));
        scrollToCenter(addBtnEl);

        // Bubble.io auto-creates default options asynchronously after the type change.
        new WebDriverWait(driver, 30).until(d ->
            !d.findElements(By.cssSelector("[id^='" + questionIndex + "--answerInput-']")).isEmpty());
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

    // ── QAT filter sidebar (single section, no tabs) ────────────────────────────
    // The filter icon only appears on a question when a PRIOR question is choice-based
    // (single / multiple / limited choice). Likert questions cannot be filtered on, so
    // any question used as a filter source must be choice-based.

    @Step("Open filter sidebar for question {index}")
    public FormQuestionsPage openFilterSidebar(int index) {
        scrollTo(By.id("icon_filter_form_question_" + index));
        jsClick(By.id("icon_filter_form_question_" + index));
        // No tabs in QAT — the response-filter add button is shown directly.
        wait.until(ExpectedConditions.visibilityOfElementLocated(addFilterQuestionBtn));
        return this;
    }

    @Step("Click Add Filter Question and wait for filter question dropdown {expectedFilterIndex}")
    public FormQuestionsPage clickAddFilterQuestion(int expectedFilterIndex) {
        new WebDriverWait(driver, 30).until(
            ExpectedConditions.elementToBeClickable(addFilterQuestionBtn));
        jsClick(addFilterQuestionBtn);
        By dropdownLocator = By.id("dropdown-filter-question-" + expectedFilterIndex);
        new WebDriverWait(driver, 30).until(
            ExpectedConditions.visibilityOfElementLocated(dropdownLocator));
        // Options load asynchronously — the dropdown first appears with only a placeholder.
        new WebDriverWait(driver, 30).until(d ->
            new Select(d.findElement(dropdownLocator)).getOptions().size() > 1);
        return this;
    }

    @Step("Select filter question {filterIndex} by partial text")
    public FormQuestionsPage selectFilterQuestion(int filterIndex, String partialQuestionText) {
        By dropdownLocator = By.id("dropdown-filter-question-" + filterIndex);
        new WebDriverWait(driver, 30).until(d -> {
            try {
                return new Select(d.findElement(dropdownLocator)).getOptions().stream()
                    .anyMatch(o -> o.getText().contains(partialQuestionText));
            } catch (Exception e) { return false; }
        });
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

    @Step("Select answer option '{optionText}' for filter question {filterIndex}")
    public FormQuestionsPage selectFilterAnswerOption(int filterIndex, String optionText) {
        By locator = By.cssSelector("[id$='-select-answer-option-filter-" + optionText + "']");
        WebElement el = wait.until(ExpectedConditions.presenceOfElementLocated(locator));
        ((JavascriptExecutor) driver).executeScript(
            "arguments[0].scrollIntoView({behavior: 'instant', block: 'center'});", el);
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", el);
        return this;
    }

    @Step("Apply filters and wait for sidebar to close")
    public FormQuestionsPage applyFilters() {
        jsClick(applyFiltersButton);
        wait.until(ExpectedConditions.invisibilityOfElementLocated(applyFiltersButton));
        return this;
    }

    // ── Preview Journey ──────────────────────────────────────────────────────────
    // Ported from msjourney (minus the GlobalTestState answer-text sync, which QAT does not
    // need yet). Choice questions can leave Bubble in a transient "incomplete fields" state;
    // the retrigger nudges each choice question's first answer to clear it, then retries.

    private void sleep2s() {
        try { Thread.sleep(2000); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }

    private boolean incompleteToastSeenOnLastPreview = false;
    public boolean wasIncompleteToastSeen() { return incompleteToastSeenOnLastPreview; }

    private boolean toastDetectedDuringPoll = false;

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

    // Returns the updated answer text after appending "s" so callers can sync the new value.
    private String retriggerChoiceValidation(int questionIndex) {
        sleep2s();
        scrollTo(By.id(questionIndex + "-toggle-group"));
        sleep2s();
        expandIfCollapsed(questionIndex);

        By firstAnswer = By.id(questionIndex + "--answerInput-1");
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

    // Holds the updated Q1 first-answer text if a retrigger occurred during preview, so the
    // survey step can use the actual current value instead of the original hardcoded text.
    private String retriggeredQ1Answer = null;

    public String getRetriggeredQ1Answer() { return retriggeredQ1Answer; }

    @Step("Click Preview Journey and capture URL from new tab")
    public String clickPreviewAndGetUrl(int[] retriggerQueue) {
        try { Thread.sleep(3000); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        String mainWindow = driver.getWindowHandle();
        incompleteToastSeenOnLastPreview = false;
        int retriggerIdx = 0;

        // Click Q1's first answer option to trigger Bubble's reactive validation across all
        // questions — ensures all question state is flushed to the backend before preview.
        try {
            jsClick(By.id(FIRST_QUESTION_INDEX + "--answerInput-1"));
        } catch (Exception ignored) {}
        try { Thread.sleep(3000); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }

        scrollTo(previewJourneyButton);
        jsClick(previewJourneyButton);

        final int MAX_ATTEMPTS = retriggerQueue.length + 1;
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            boolean tabOpened = pollForTabOrToast(attempt < MAX_ATTEMPTS ? 10 : 30);
            if (tabOpened) break;
            if (attempt == MAX_ATTEMPTS) {
                throw new RuntimeException("Preview tab did not open after " + MAX_ATTEMPTS + " attempts");
            }
            if (toastDetectedDuringPoll) {
                incompleteToastSeenOnLastPreview = true;
                try {
                    driver.findElement(By.id("dismiss-toast")).click();
                    new WebDriverWait(driver, 5).until(
                        ExpectedConditions.invisibilityOfElementLocated(By.cssSelector("[id^='toast-']")));
                } catch (Exception ignored) {}
                if (retriggerIdx < retriggerQueue.length) {
                    int qIdx = retriggerQueue[retriggerIdx++];
                    String updated = retriggerChoiceValidation(qIdx);
                    if (qIdx == FIRST_QUESTION_INDEX) retriggeredQ1Answer = updated;
                }
                try { Thread.sleep(2000); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            }
            scrollTo(previewJourneyButton);
            jsClick(previewJourneyButton);
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
        return previewUrl;
    }
}

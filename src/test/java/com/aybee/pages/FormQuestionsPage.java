package com.aybee.pages;

import com.aybee.utils.ConfigReader;
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
import org.testng.Assert;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class FormQuestionsPage extends BasePage {

    // The D2C test type pre-adds 3 questions automatically:
    //   Q1 (index 1) — "Why did you choose this product?"
    //                   Show: All products (with the bought product displayed beside it at preview time)
    //                   Type: Long text
    //   Q2 (index 2) — "Why did you decide against this product?"
    //                   Show: Only displayed when participant did NOT select our product
    //                         (Scenario A or B product depending on assignment); our product displayed beside
    //                   Type: Long text
    //   Q3 (index 3) — "What are your top 3 criteria when choosing a product in this category?"
    //                   Show: All products (all products displayed beside at preview time)
    //                   Type: Long text
    //
    // Whether the platform pre-adds these default questions is backend-controlled and changes often,
    // so it is driven by the DEFAULT_QUESTIONS config flag (yes/no) rather than hard-coded:
    //   yes (default) → the 3 default questions above are pre-added (indices 1–3); manual questions
    //                   start at index 4, and the participant journey expects the defaults.
    //   no            → no default questions exist; manual questions start at index 1, and the
    //                   participant journey expects none of them.
    // All element IDs and expected-count checks derive from FIRST_QUESTION_INDEX / DEFAULT_QUESTION_COUNT,
    // so they adapt automatically to whichever mode is configured.
    public static final boolean HAS_DEFAULT_QUESTIONS = ConfigReader.getYesNo("DEFAULT_QUESTIONS", true);
    public static final int DEFAULT_QUESTION_COUNT = HAS_DEFAULT_QUESTIONS ? 3 : 0;
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

    private WebElement scrollTo(By locator) {
        WebElement el = wait.until(ExpectedConditions.presenceOfElementLocated(locator));
        ((JavascriptExecutor) driver).executeScript(
            "arguments[0].scrollIntoView({behavior: 'instant', block: 'center'});", el);
        return el;
    }

    // ── Initial questions verification ────────────────────────────────────────

    // Waits for the Add Question button to be clickable (page is ready), reloads the page,
    // waits for the button to be clickable again (reload settled), then asserts that exactly
    // 3 pre-existing question toggle-groups are present (indices 1, 2, 3).
    // The reload is required to confirm Bubble.io has persisted the platform-generated questions
    // server-side — they appear in the DOM immediately but are only durable after the reload.
    @Step("Wait for Add Question button, reload, wait again, then confirm 3 initial questions are present")
    public void verifyInitialQuestionsLoaded() {
        // Page must be ready before we touch anything.
        new WebDriverWait(driver, 30).until(
            ExpectedConditions.elementToBeClickable(addQuestionButton));
        System.out.println("[FormQuestions] Add Question button is clickable — page ready");

        // DEFAULT_QUESTIONS=no → no platform default questions to verify. Skip the check entirely and
        // let the caller add our questions directly (index 1), exactly like PDP. Whatever pre-added
        // cards the template may render are ignored here; the participant journey is title-driven and
        // does not expect them.
        if (!HAS_DEFAULT_QUESTIONS) {
            System.out.println("[FormQuestions] DEFAULT_QUESTIONS=no — skipping default-question verification; adding questions directly (PDP-style)");
            return;
        }

        // yes path: reload so the platform's default cards render, then verify the expected count.
        driver.navigate().refresh();
        new WebDriverWait(driver, 30).until(
            ExpectedConditions.elementToBeClickable(addQuestionButton));
        System.out.println("[FormQuestions] Add Question button clickable after reload");

        // Wait up to 30 s for all DEFAULT_QUESTION_COUNT platform-generated cards to render (the first
        // renders fast, the rest follow asynchronously). Count only real, visible, distinct cards.
        try {
            new WebDriverWait(driver, 30).until(d -> countVisibleQuestionCards() >= DEFAULT_QUESTION_COUNT);
        } catch (Exception e) {
            System.out.println("[FormQuestions] Timed out waiting for " + DEFAULT_QUESTION_COUNT
                + " default questions — currently visible: " + countVisibleQuestionCards());
        }

        int raw = driver.findElements(By.cssSelector("[id$='-toggle-group']")).size();
        int count = countVisibleQuestionCards();
        System.out.println("[FormQuestions] Question cards after reload: " + count
            + " (raw toggle-group matches: " + raw + ", expected " + DEFAULT_QUESTION_COUNT + ")");
        Assert.assertEquals(count, DEFAULT_QUESTION_COUNT,
            "[FormQuestions] Expected " + DEFAULT_QUESTION_COUNT
                + " initial D2C default questions, found: " + count);
    }

    // Counts REAL question cards: distinct {n}-toggle-group ids that are actually on screen (displayed,
    // non-zero size, not visibility:hidden). Bubble.io leaves hidden template copies (often with the
    // same id) in the DOM, so a raw findElements(...).size() over-counts — this filters those out and
    // dedupes by id so the number reflects the cards a user would actually see.
    private int countVisibleQuestionCards() {
        java.util.Set<String> ids = new java.util.HashSet<>();
        for (WebElement el : driver.findElements(By.cssSelector("[id$='-toggle-group']"))) {
            try {
                if (!el.isDisplayed()) continue;
                if (el.getSize().getHeight() <= 0 || el.getSize().getWidth() <= 0) continue;
                if ("hidden".equals(el.getCssValue("visibility"))) continue;
                String id = el.getAttribute("id");
                if (id != null && id.endsWith("-toggle-group")) ids.add(id);
            } catch (Exception ignored) {}
        }
        return ids.size();
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
        String actual = input.getAttribute("value");
        if (!text.equals(actual)) {
            System.out.println("[FormQuestions] Q" + index + " text mismatch — expected: ["
                + text + "] got: [" + actual + "]");
        } else {
            System.out.println("[FormQuestions] Q" + index + " text confirmed: " + actual);
        }
        return this;
    }

    // Dropdown is a native <select>; values include literal quote characters per DOM.
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
    @Step("Select Show to Participants '{showValue}' for question {index}")
    public FormQuestionsPage selectShowToParticipants(int index, String showValue) {
        WebElement select = scrollTo(By.id("dp_show_participants_form_question_" + index));
        ((JavascriptExecutor) driver).executeScript(
            "arguments[0].value = arguments[1];" +
            "arguments[0].dispatchEvent(new Event('change', {bubbles: true}));",
            select, "\"" + showValue + "\"");
        return this;
    }

    // D2C-only: selecting a_b_test_product causes Bubble.io to render a disabled reference
    // dropdown at {index}-product-select-dropdown, pre-populated with Scenario A's product
    // name. The dropdown is read-only (just a reference indicator — at preview time the
    // platform shows the participant's actual assigned scenario product). Options load
    // asynchronously, so we wait for size > 1 before proceeding.
    @Step("Select 'A/B test product' show type for question {index} and wait for reference dropdown to render")
    public FormQuestionsPage selectShowAbTestProductAndWait(int index) {
        WebElement select = scrollTo(By.id("dp_show_participants_form_question_" + index));
        ((JavascriptExecutor) driver).executeScript(
            "arguments[0].value = arguments[1];" +
            "arguments[0].dispatchEvent(new Event('change', {bubbles: true}));",
            select, "\"a_b_test_product\"");

        By refDropdown = By.id(index + "-product-select-dropdown");
        new WebDriverWait(driver, 30).until(
            ExpectedConditions.presenceOfElementLocated(refDropdown));
        new WebDriverWait(driver, 30).until(d -> {
            try {
                return new Select(d.findElement(refDropdown)).getOptions().size() > 1;
            } catch (Exception e) { return false; }
        });
        String shown = new Select(driver.findElement(refDropdown))
            .getFirstSelectedOption().getText();
        System.out.println("[FormQuestions] A/B test product reference dropdown rendered for Q"
            + index + " — shows: " + shown);
        return this;
    }

    @Step("Select specific product for question {index}")
    public FormQuestionsPage selectSpecificProduct(int index, String partialName) {
        By locator = By.id(index + "-product-select-dropdown");
        scrollTo(locator);
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
        try {
            System.out.println("[FileUpload] Primary — sendKeys on File-Upload-Asset-Input");
            WebElement el = wait.until(ExpectedConditions.presenceOfElementLocated(fileUploadInput));
            el.sendKeys(filePath);
            System.out.println("[FileUpload] Primary succeeded");
        } catch (Exception e) {
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
        WebElement dropdown = new WebDriverWait(driver, 30).until(
            ExpectedConditions.visibilityOfElementLocated(locator));
        scrollToCenter(dropdown);
        try { Thread.sleep(1000); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
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
        WebElement card = new WebDriverWait(driver, 30).until(
            ExpectedConditions.visibilityOfElementLocated(By.id(index + "-toggle-group")));
        scrollToCenter(card);
        new WebDriverWait(driver, 60).until(d ->
            d.findElements(By.cssSelector("[id^='" + index + "--answerInput-']")).size() >= expectedCount);
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

    @Step("Wait for {expectedCount} answer options on question {questionIndex}")
    public FormQuestionsPage waitForAnswerOptionCount(int questionIndex, int expectedCount) {
        new WebDriverWait(driver, 30).until(d ->
            d.findElements(
                By.cssSelector("[id^='" + questionIndex + "--answerInput-']")).size() >= expectedCount);
        return this;
    }

    // Deletes extras from the end first, then adds if short — ensures exactly targetCount fields.
    @Step("Ensure {targetCount} answer option fields for question {questionIndex}")
    public FormQuestionsPage ensureAnswerOptionCount(int questionIndex, int targetCount) {
        waitForAnswerOptions(questionIndex);
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
            By addBtn   = By.id(questionIndex + "-addAnswer-btn");
            By newInput = By.id(questionIndex + "--answerInput-" + next);
            scrollTo(addBtn);
            new WebDriverWait(driver, 30).until(
                ExpectedConditions.visibilityOfElementLocated(addBtn));
            jsClick(addBtn);
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

        WebElement addBtnEl = new WebDriverWait(driver, 30).until(
            ExpectedConditions.visibilityOfElementLocated(addBtn));
        scrollToCenter(addBtnEl);

        new WebDriverWait(driver, 30).until(d ->
            !d.findElements(By.cssSelector("[id^='" + questionIndex + "--answerInput-']")).isEmpty());
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

    @Step("Enable randomize toggle for question {questionIndex}")
    public FormQuestionsPage enableRandomizeToggle(int questionIndex) {
        scrollTo(By.id(questionIndex + "-toggle-randomize"));
        jsClick(By.id(questionIndex + "-toggle-randomize"));
        return this;
    }

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
        new WebDriverWait(driver, 30).until(
            ExpectedConditions.visibilityOfElementLocated(By.id("scenario-A")));
        return this;
    }

    @Step("Select scenario '{scenarioId}'")
    public FormQuestionsPage selectScenario(String scenarioId) {
        jsClick(By.id(scenarioId));
        return this;
    }

    // Selects the bought-product chips for both scenarios.
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

    @Step("Clear any selected scenarios (keep only the Products section active)")
    public FormQuestionsPage clearScenarioSelection() {
        for (String id : new String[]{"scenario-A", "scenario-B"}) {
            deselectIfSelected(By.id(id));
        }
        return this;
    }

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

    private boolean isChipSelected(WebElement el) {
        String style = el.getAttribute("style");
        return style != null && style.contains("border-width");
    }

    @Step("Click Filter by Responses tab")
    public FormQuestionsPage clickFilterByResponseTab() {
        scrollTo(filterByResponseTab);
        new WebDriverWait(driver, 10).until(
            ExpectedConditions.elementToBeClickable(filterByResponseTab)).click();
        new WebDriverWait(driver, 15).until(
            ExpectedConditions.invisibilityOfElementLocated(By.id("scenario-A")));
        new WebDriverWait(driver, 15).until(
            ExpectedConditions.elementToBeClickable(addFilterQuestionBtn));
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

    private static void sleep2s() {
        try { Thread.sleep(2000); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }

    // Waits for the "Incomplete Fields" validation badge to disappear.
    // The badge is visible (background: destructive) when any question card has missing data;
    // it becomes visibility:hidden + display:none once all fields are valid.
    private void waitForValidationComplete() {
        By incompleteTag = By.xpath("//*[normalize-space(text())='Incomplete Fields']");
        try {
            new WebDriverWait(driver, 30).until(
                ExpectedConditions.invisibilityOfElementLocated(incompleteTag));
            System.out.println("[FormQuestions] Validation complete — Incomplete Fields badge gone");
        } catch (Exception e) {
            System.out.println("[FormQuestions] Incomplete Fields badge still visible after 30s — proceeding anyway");
        }
    }

    private boolean incompleteToastSeenOnLastPreview = false;

    public boolean wasIncompleteToastSeen() {
        return incompleteToastSeenOnLastPreview;
    }

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

    // Opens the preview: clicks the Preview button, waits for the Desktop/Mobile chooser popup, and
    // selects Desktop (preview-desktop). Validation/error toasts fire AFTER the Desktop selection.
    // Reloads the Form Questions editor once, right before opening preview, so the backend products
    // load fully (they can render incompletely in the preview journey on the very first open). Mirrors
    // the PDP precaution. The caller commits/validates fields BEFORE this reload so nothing is lost.
    public FormQuestionsPage reloadEditorForProductLoad() {
        System.out.println("[FormQuestions] Reloading editor before preview so all products load properly");
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

    // Clicks the preview button, selects Desktop in the chooser (which opens a new tab), waits for
    // the tab's URL to resolve past about:blank, captures it, and returns to the main window.
    // The Desktop/Mobile chooser does NOT auto-dismiss: on an "incomplete form fields" toast,
    // dismissing the toast also closes the chooser; with no toast the chooser stays open and we close
    // it before retrying; on success we close it after capturing the URL.
    @Step("Open preview (select Desktop) and capture URL from new tab")
    public String clickPreviewAndGetUrl() {
        try { Thread.sleep(3000); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        String mainWindow = driver.getWindowHandle();
        incompleteToastSeenOnLastPreview = false;

        // D2C: all questions are long text — no answer options, so retriggerChoiceValidation
        // must never run (it looks for addAnswer-btn / answerInput-1 which don't exist).
        int[] retriggerQueue = {};
        int retriggerIdx = 0;

        // Trigger Bubble.io's DB validation pass for every question card (1 through the last
        // manually added index), then blur the active field via the section title so all
        // reactive saves fire before the preview request is sent.
        for (int i = 1; i <= FIRST_QUESTION_INDEX; i++) {
            try {
                jsClick(By.id("free-question-" + i));
                try { Thread.sleep(500); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
            } catch (Exception ignored) {}
        }
        // Native click on the section title so focus actually LEAVES the last field — Bubble only
        // validates a field once it is blurred, and a JS click does not change document.activeElement
        // or fire the field's blur event. blurActiveElement() is a fallback if the click is intercepted.
        try {
            By titleLocator = By.id("experiment-questions-title");
            if (!driver.findElements(titleLocator).isEmpty()) {
                scrollTo(titleLocator);
                click(titleLocator);
            }
        } catch (Exception ignored) {}
        blurActiveElement();
        waitForValidationComplete();

        // Reload the editor (fields committed above) so products load fully before preview — PDP precaution.
        reloadEditorForProductLoad();

        openPreviewChooserAndSelectDesktop();

        // Attempts 1–2: normal retry on toast or no-tab.
        // Attempt 3: extra 5 s sleep before final click if both prior attempts failed.
        final int MAX_ATTEMPTS = 3;
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            boolean tabOpened = pollForTabOrToast(attempt < MAX_ATTEMPTS ? 10 : 30);
            if (tabOpened) break;

            if (attempt == MAX_ATTEMPTS) {
                throw new RuntimeException(
                    "Preview tab did not open after " + MAX_ATTEMPTS + " attempts");
            }

            if (toastDetectedDuringPoll) {
                incompleteToastSeenOnLastPreview = true;
                // Dismissing the toast also closes the Desktop/Mobile chooser popup.
                dismissToastAndChooser();

                if (retriggerIdx < retriggerQueue.length) {
                    int qIdx = retriggerQueue[retriggerIdx++];
                    System.out.println("[Preview] Toast on attempt " + attempt
                        + " — retriggering question index " + qIdx);
                    retriggerChoiceValidation(qIdx);
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

            if (attempt == 2) {
                System.out.println("[Preview] Attempt 2 failed — sleeping 5 s before final retry");
                try { Thread.sleep(5000); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
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
                // Keep this tab open — PreviewJourneyPage.navigateAsLoggedInUser() switches
                // to it directly. Closing and re-navigating from a blank tab fails because
                // Bubble.io binds the preview session to the tab it opened.
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

}

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

    // Questions 1–3 are pre-added by the platform and always present.
    // Manually added questions start at index 4 and use that index for ALL element IDs.
    public static final int FIRST_QUESTION_INDEX = 4;

    private final By addQuestionButton    = By.id("newproject_formquestions_addquestion_button");
    private final By addManuallyButton    = By.id("add-manually-btn");
    private final By previewJourneyButton = By.id("newproject_formquestions_previewjourney_button");

    // Asset upload popup
    private final By fileUploadInput = By.id("File-Upload-Asset-Input");
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
        // Native file inputs accept sendKeys even when hidden — no need to click the
        // custom Bubble.io upload widget or make the input visible first.
        driver.findElement(fileUploadInput).sendKeys(filePath);
        try { Thread.sleep(2000); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        return this;
    }

    @Step("Confirm asset upload")
    public FormQuestionsPage confirmAssetUpload() {
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

    // Matches by partial ID since the trimmed product name length varies.
    @Step("Select specific bought product containing '{partialName}'")
    public FormQuestionsPage selectSpecificBoughtProduct(String partialName) {
        List<WebElement> products = wait.until(
            ExpectedConditions.presenceOfAllElementsLocatedBy(
                By.cssSelector("[id^='specific-product-']")));
        for (WebElement el : products) {
            if (el.getAttribute("id").contains(partialName)) {
                ((JavascriptExecutor) driver).executeScript("arguments[0].click();", el);
                return this;
            }
        }
        throw new RuntimeException("Specific bought product not found: " + partialName);
    }

    @Step("Click Filter by Responses tab")
    public FormQuestionsPage clickFilterByResponseTab() {
        jsClick(filterByResponseTab);
        // Wait for the Add Question button to be visible — confirms Response tab content has loaded.
        new WebDriverWait(driver, 30).until(
            ExpectedConditions.visibilityOfElementLocated(addFilterQuestionBtn));
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

    // Touches the first answer input of the given question by typing 's'.
    // Forces Bubble.io's reactive system to re-evaluate form completion state,
    // clearing false-negative "incomplete" flags on limited, single, and multiple choice questions.
    // Returns the updated text of the first answer option so callers can sync GlobalTestState.
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

        // Read the updated value now — it reflects the appended 's' — before collapsing.
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
                List<WebElement> toasts = driver.findElements(By.id("toast-message"));
                if (!toasts.isEmpty() && toasts.get(0).isDisplayed()) {
                    toastDetectedDuringPoll = true;
                    return false;
                }
            } catch (Exception ignored) {}
            try { Thread.sleep(300); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
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

        scrollTo(previewJourneyButton);
        jsClick(previewJourneyButton);

        final int MAX_ATTEMPTS = 3;
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            // Give the last attempt the full 30 s; earlier attempts use 10 s each.
            boolean tabOpened = pollForTabOrToast(attempt < MAX_ATTEMPTS ? 10 : 30);
            if (tabOpened) break;

            if (attempt == MAX_ATTEMPTS) {
                throw new RuntimeException(
                    "Preview tab did not open after " + MAX_ATTEMPTS + " attempts");
            }

            if (toastDetectedDuringPoll) {
                // Toast was seen — dismiss it and retrigger choice question validation.
                // This is the only path that runs retrigger; if the poll timed out without
                // seeing a toast we skip straight to the retry click to save time.
                incompleteToastSeenOnLastPreview = true;
                System.out.println("[Preview] Incomplete-fields toast on attempt " + attempt + " — dismissing and retriggering");
                try {
                    driver.findElement(By.id("dismiss-toast")).click();
                    new WebDriverWait(driver, 30).until(
                        ExpectedConditions.invisibilityOfElementLocated(By.id("toast-message")));
                } catch (Exception ignored) {}
                String newQ2Opt1 = retriggerChoiceValidation(FIRST_QUESTION_INDEX + 1);
                if (newQ2Opt1 != null && !newQ2Opt1.isEmpty()
                        && GlobalTestState.q2SelectOptions != null
                        && GlobalTestState.q2SelectOptions.size() >= 2) {
                    GlobalTestState.q2SelectOptions = Arrays.asList(
                        newQ2Opt1, GlobalTestState.q2SelectOptions.get(1));
                }
                String newQ3Opt1 = retriggerChoiceValidation(FIRST_QUESTION_INDEX + 2);
                if (newQ3Opt1 != null && !newQ3Opt1.isEmpty()) {
                    GlobalTestState.q3SelectOption = newQ3Opt1;
                }
                String newQ4Opt1 = retriggerChoiceValidation(FIRST_QUESTION_INDEX + 3);
                if (newQ4Opt1 != null && !newQ4Opt1.isEmpty()
                        && GlobalTestState.q4SelectOptions != null
                        && GlobalTestState.q4SelectOptions.size() >= 2) {
                    GlobalTestState.q4SelectOptions = Arrays.asList(
                        newQ4Opt1, GlobalTestState.q4SelectOptions.get(1));
                }
                try { Thread.sleep(3000); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            } else {
                // Poll timed out — no toast seen. Skip retrigger entirely and retry the click.
                System.out.println("[Preview] No tab or toast on attempt " + attempt + " — retrying click without retrigger");
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
        driver.get(previewUrl);
        new WebDriverWait(driver, 30).until(
            ExpectedConditions.visibilityOfElementLocated(By.id("continue-button")));
    }
}

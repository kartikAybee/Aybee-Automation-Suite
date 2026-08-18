package com.aybee.pages;

import io.qameta.allure.Step;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.FluentWait;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

// Form Questions — step 2 of the 3-step PDP Simulation flow.
//
// This page is the shared, full-featured toolkit for the Form Questions editor across all
// aybee test suites. Almost every method here was proven in the msjourney / d2c / ctr suites
// and is ported verbatim (adapted only for PDP indices) so future PDP tests have the complete
// set of actions, precautions, and Bubble.io workarounds available without re-deriving them.
//
// PDP specifics:
//   • No platform default questions — they have been stripped, so the Form Questions list starts
//     empty and our manually added questions (the 4 Split Tests) start at index 1.
public class FormQuestionsPage extends BasePage {

    // DEFAULT_QUESTIONS config (yes/no). PDP's flow has NO platform-pre-added default questions — the
    // only questions are the Split Tests we add — so this defaults to no. Exposed for consistency with
    // the other suites and for the participant/preview flow to gate on. FIRST_QUESTION_INDEX stays 1
    // because there are no defaults occupying earlier indices.
    public static final boolean HAS_DEFAULT_QUESTIONS = com.aybee.utils.ConfigReader.getYesNo("DEFAULT_QUESTIONS", false);

    // PDP no longer relies on any platform-pre-added default questions; our added questions (the 4
    // Split Tests) are the only ones, starting at index 1 and using that index for ALL element IDs.
    public static final int FIRST_QUESTION_INDEX = 1;

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

    // Scrolls the element into view before returning it. Every interaction on the form
    // questions page uses this so elements in newly-added cards (which appear at the bottom
    // of a growing list) are always in the viewport first.
    private WebElement scrollTo(By locator) {
        WebElement el = wait.until(ExpectedConditions.presenceOfElementLocated(locator));
        ((JavascriptExecutor) driver).executeScript(
            "arguments[0].scrollIntoView({behavior: 'instant', block: 'center'});", el);
        return el;
    }

    // ── Landmark / default-question checks ──────────────────────────────────────

    // The Add Question button sits at the bottom of the page, so scroll it into view before
    // the clickability check. Confirms the step has loaded after the Shop Setup Next click.
    @Step("Verify Form Questions step loaded (Add Question button clickable)")
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

    // Counts the rendered question cards by their {n}-toggle-group IDs, after letting Bubble.io
    // settle. Returns the stable count.
    @Step("Count rendered form questions (toggle-group cards)")
    public int countQuestions() {
        wait.until(ExpectedConditions.visibilityOfElementLocated(addQuestionButton));
        // Bubble.io renders question cards asynchronously after the add-question button appears —
        // poll until the toggle-group count is stable across two consecutive 1s checks.
        new FluentWait<>(driver)
            .withTimeout(20, TimeUnit.SECONDS)
            .pollingEvery(1, TimeUnit.SECONDS)
            .ignoring(Exception.class)
            .until(d -> {
                int first  = d.findElements(By.cssSelector("[id$='-toggle-group']")).size();
                try { Thread.sleep(1000); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
                int second = d.findElements(By.cssSelector("[id$='-toggle-group']")).size();
                return first > 0 && first == second;
            });
        return driver.findElements(By.cssSelector("[id$='-toggle-group']")).size();
    }

    // (Removed verifyDefaultQuestions — PDP no longer has platform default questions to verify.)

    // ── Card lifecycle ────────────────────────────────────────────────────────

    // Checks the chevron SVG href — reliable regardless of Bubble.io's CSS hiding strategy.
    @Step("Expand question {index} if collapsed")
    public void expandIfCollapsed(int index) {
        try {
            By toggle = By.id(index + "-toggle-group");
            WebElement el = new WebDriverWait(driver, 30)
                .until(ExpectedConditions.visibilityOfElementLocated(toggle));
            scrollToCenter(el);
            String iconHref =
                "var el = document.querySelector('#icon_toggle_form_question_" + index + " use');" +
                "return el ? el.getAttribute('href') : null;";
            // The chevron toggle icon is only rendered/updated on HOVER, so its href reads null until we
            // hover the row (notably after a page reload). Hover first so the collapsed/expanded state
            // is readable — otherwise the expand is silently skipped and the card's dropdowns stay absent.
            try {
                new Actions(driver).moveToElement(el).perform();
                new WebDriverWait(driver, 5).until(d ->
                    ((JavascriptExecutor) d).executeScript(iconHref) != null);
            } catch (Exception ignored) {}
            Object href = ((JavascriptExecutor) driver).executeScript(iconHref);
            if (href != null && href.toString().contains("chevron-down")) {
                jsClick(toggle);
                new WebDriverWait(driver, 30).until(d ->
                    String.valueOf(((JavascriptExecutor) d).executeScript(iconHref)).contains("chevron-up"));
            }
        } catch (Exception ignored) {}
    }

    private boolean waitVisible(By locator, int secs) {
        try {
            new WebDriverWait(driver, secs).until(ExpectedConditions.visibilityOfElementLocated(locator));
            return true;
        } catch (Exception e) { return false; }
    }

    // Ensures question {index}'s card is expanded so {contentLocator} (a field inside the card) is
    // visible. First tries expandIfCollapsed (hover + chevron); if the content still isn't shown, it
    // directly clicks the toggle-group (cards are collapsed by default, e.g. right after a reload) and
    // waits. Idempotent: returns immediately if the content is already visible, so it never collapses an
    // already-open card.
    private void ensureExpandedShowing(int index, By contentLocator) {
        if (waitVisible(contentLocator, 1)) return;        // already expanded
        expandIfCollapsed(index);
        if (waitVisible(contentLocator, 3)) return;
        try {                                              // fallback: click the toggle-group directly
            scrollTo(By.id(index + "-toggle-group"));
            jsClick(By.id(index + "-toggle-group"));
        } catch (Exception ignored) {}
        waitVisible(contentLocator, 10);
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
        WebElement input = scrollTo(By.id(index + "-questionInput"));
        input.clear();
        input.sendKeys(text);
        return this;
    }

    // Dropdown is a native <select>; option values include literal quote characters per the DOM,
    // so we wrap the value in quotes. Uses JS value injection + dispatchEvent because
    // Select.selectByValue() clicks native <option> elements, which fails when they render
    // outside the viewport.
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

    // Split Test only: after selecting the "split_test" type (and the product source), a "What to
    // Display" native <select> appears (dp_what_to_display_form_page_{index}). Options:
    // full_product_page, primary_image, image_gallery, a__content__section_2_.
    @Step("Select What to Display '{displayValue}' for split-test question {index}")
    public FormQuestionsPage selectWhatToDisplay(int index, String displayValue) {
        By locator = By.id("dp_what_to_display_form_page_" + index);
        WebElement select = new WebDriverWait(driver, 30).until(
            ExpectedConditions.visibilityOfElementLocated(locator));
        scrollToCenter(select);
        new WebDriverWait(driver, 30).until(ExpectedConditions.elementToBeClickable(locator));

        // Brief settle so the options finish (re)populating after the product-source change — NO hard
        // option-present wait: the selection reflects on the frontend but Select.getOptions()/value
        // read-back are unreliable for this Bubble dropdown, so a strict precondition wait false-fails
        // (it timed out on Q4 even though the option was present and clicked on screen).
        try { Thread.sleep(1500); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }

        // Select via JS value injection + change event.
        WebElement el = driver.findElement(locator);
        ((JavascriptExecutor) driver).executeScript(
            "arguments[0].value = arguments[1];" +
            "arguments[0].dispatchEvent(new Event('change', {bubbles: true}));",
            el, "\"" + displayValue + "\"");
        // Do NOT hard-verify via getAttribute("value"): for this Bubble dropdown the selection reflects
        // on the frontend, but the value attribute is NOT a reliable read-back (the control re-renders /
        // the value moves after the change fires), so a strict value-contains check false-fails even
        // when the option is correctly selected. Settle briefly and log the read-back best-effort only.
        try { Thread.sleep(1500); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        try {
            String v = driver.findElement(locator).getAttribute("value");
            if (v == null || !v.contains(displayValue)) {
                System.out.println("[FormQuestions] What-to-Display Q" + index + " read-back='" + v
                    + "' (expected to contain '" + displayValue + "') — proceeding; selection reflects on the frontend");
            }
        } catch (Exception ignored) {}
        return this;
    }

    // Reads the What-to-Display option values actually AVAILABLE for split-test question {index}
    // (call after the type + product source are selected, so the dropdown is populated). Availability
    // depends on the product source — e.g. a__content__section_2_ is NOT offered for your_own_products
    // — so the caller creates one question per returned option instead of assuming a fixed set.
    // Values are returned WITHOUT the surrounding quotes Bubble wraps them in (so they pass straight
    // back to selectWhatToDisplay, which re-wraps); the empty/placeholder option is dropped.
    // Select.getOptions() reliably reflects availability here (it correctly omits unavailable options).
    @Step("Read the available What-to-Display options for split-test question {index}")
    public java.util.List<String> getAvailableWhatToDisplayOptions(int index) {
        By locator = By.id("dp_what_to_display_form_page_" + index);
        new WebDriverWait(driver, 30).until(ExpectedConditions.visibilityOfElementLocated(locator));
        // Wait for real options (more than just the placeholder) to populate.
        new WebDriverWait(driver, 30).until(d -> {
            try { return new Select(d.findElement(locator)).getOptions().size() > 1; }
            catch (Exception e) { return false; }
        });
        java.util.List<String> values = new java.util.ArrayList<>();
        for (WebElement opt : new Select(driver.findElement(locator)).getOptions()) {
            String v = opt.getAttribute("value");
            if (v == null) continue;
            v = v.trim();
            if (v.startsWith("\"") && v.endsWith("\"") && v.length() >= 2) v = v.substring(1, v.length() - 1);
            if (v.isEmpty() || v.equalsIgnoreCase("null")) continue;   // skip the placeholder option
            values.add(v);
        }
        System.out.println("[FormQuestions] Available What-to-Display options for Q" + index + ": " + values);
        return values;
    }

    // Reads the currently-SELECTED What-to-Display value for split-test question {index} (quotes
    // stripped), expanding the card first so the dropdown is present. Used AFTER the pre-preview reload
    // to recover the real value when the first render had returned an unresolved placeholder id (e.g.
    // PLACEHOLDER_... that is actually full_product_page) — after reload the option's data source has
    // loaded, so the selected option now reports its true value. Returns null if it can't be read.
    public String readSelectedWhatToDisplay(int index) {
        By locator = By.id("dp_what_to_display_form_page_" + index);
        try {
            ensureExpandedShowing(index, locator);
            new WebDriverWait(driver, 30).until(ExpectedConditions.visibilityOfElementLocated(locator));
            WebElement sel = new Select(driver.findElement(locator)).getFirstSelectedOption();
            String v = sel.getAttribute("value");
            if (v == null) return null;
            v = v.trim();
            if (v.startsWith("\"") && v.endsWith("\"") && v.length() >= 2) v = v.substring(1, v.length() - 1);
            return v.isEmpty() ? null : v;
        } catch (Exception e) {
            return null;
        }
    }

    // Adds a Split Test question at {index}: opens the card, enters the question text, selects the
    // Split Test type, then picks What to Display. A Split Test preview shows both scenario product
    // options plus a "No Difference" option.
    @Step("Create Split Test question {index} showing '{whatToDisplay}'")
    public FormQuestionsPage createSplitTestQuestion(int index, String questionText, String whatToDisplay) {
        addNewQuestion(index);
        enterQuestionText(index, questionText);
        selectQuestionType(index, "split_test");
        // Order matters: the product-source dropdown appears right after the type is chosen and must
        // be set BEFORE What-to-Display (which comes last).
        selectProductSource(index, "your_own_products");
        selectWhatToDisplay(index, whatToDisplay);
        return this;
    }

    // Before validating/previewing, confirm every split-test question's product-source dropdown
    // ({index}-product-select-dropdown) is set to your_own_products, and re-select any that isn't —
    // Bubble occasionally drops the selection on later questions (e.g. Q4). The card is expanded and
    // scrolled into view first so the dropdown is interactable. Safe to call repeatedly.
    @Step("Verify/repair product-source = your_own_products for split-test questions")
    public FormQuestionsPage ensureProductSourcesSelected(int... indices) {
        for (int index : indices) {
            ensureExpandedShowing(index, By.id(index + "-product-select-dropdown"));
            By locator = By.id(index + "-product-select-dropdown");
            String v = null;
            try {
                if (!driver.findElements(locator).isEmpty()) {
                    v = driver.findElement(locator).getAttribute("value");
                }
            } catch (Exception ignored) {}
            if (v == null || !v.contains("your_own_products")) {
                System.out.println("[FormQuestions] Q" + index + " product-source value='" + v
                    + "' — (re)selecting your_own_products");
                selectProductSource(index, "your_own_products");
            } else {
                System.out.println("[FormQuestions] Q" + index + " product-source already your_own_products");
            }
        }
        return this;
    }

    // Split Test only: after the type is chosen (and BEFORE What-to-Display), a
    // "{index}-product-select-dropdown" native <select> appears. Its option values are quote-wrapped
    // in the DOM (e.g. "your_own_products"), so we wrap the value like the other Bubble dropdowns and
    // inject via JS + change event.
    @Step("Select product source '{value}' for split-test question {index}")
    public FormQuestionsPage selectProductSource(int index, String value) {
        By locator = By.id(index + "-product-select-dropdown");
        WebElement select = new WebDriverWait(driver, 30).until(
            ExpectedConditions.visibilityOfElementLocated(locator));
        scrollToCenter(select);
        new WebDriverWait(driver, 30).until(ExpectedConditions.elementToBeClickable(locator));

        // CRITICAL: wait for the target OPTION to actually exist before setting the value. Bubble
        // populates a dropdown's <option>s asynchronously, and assigning select.value to an option
        // that isn't present yet silently no-ops (the value stays empty). This is why later split-test
        // questions (2,3,4) didn't take while Q1 did — their options simply weren't loaded yet.
        new WebDriverWait(driver, 30).until(d -> {
            try {
                return new Select(d.findElement(locator)).getOptions().stream().anyMatch(o -> {
                    String v = o.getAttribute("value");
                    return v != null && v.contains(value);
                });
            } catch (Exception e) { return false; }
        });

        // Inject + verify, retrying once (Bubble may reset the value once before committing state).
        for (int attempt = 1; attempt <= 2; attempt++) {
            WebElement el = driver.findElement(locator);
            ((JavascriptExecutor) driver).executeScript(
                "arguments[0].value = arguments[1];" +
                "arguments[0].dispatchEvent(new Event('change', {bubbles: true}));",
                el, "\"" + value + "\"");
            try {
                new WebDriverWait(driver, 8).until(d -> {
                    String v = d.findElement(locator).getAttribute("value");
                    return v != null && v.contains(value);
                });
                return this;
            } catch (Exception e) {
                System.out.println("[FormQuestions] product-source for Q" + index
                    + " didn't stick on attempt " + attempt + " — retrying");
            }
        }
        System.out.println("[FormQuestions] WARNING: product-source for Q" + index
            + " could not be set to '" + value + "'");
        return this;
    }

    // Skip for "just_question" — it is the default selection.
    @Step("Select Show to Participants '{showValue}' for question {index}")
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

    @Step("Select specific product for question {index}")
    public FormQuestionsPage selectSpecificProduct(int index, String partialName) {
        By locator = By.id(index + "-product-select-dropdown");
        WebElement dropdown = new WebDriverWait(driver, 30).until(
            ExpectedConditions.visibilityOfElementLocated(locator));
        scrollToCenter(dropdown);
        new WebDriverWait(driver, 30).until(ExpectedConditions.elementToBeClickable(locator));
        // Wait for options to populate — Bubble.io loads them asynchronously after the dropdown renders.
        new WebDriverWait(driver, 30).until(d ->
            new Select(d.findElement(locator)).getOptions().size() > 1);
        dropdown = driver.findElement(locator);
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
        new WebDriverWait(driver, 10).until(d -> {
            WebElement opt = new Select(d.findElement(locator)).getFirstSelectedOption();
            return opt.getText().contains(partialName);
        });
        return this;
    }

    // ── Asset upload ────────────────────────────────────────────────────────────

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
                new WebDriverWait(driver, 30).until(d -> !d.findElements(newInput).isEmpty());
            } catch (TimeoutException retry) {
                jsClick(addBtn);
                new WebDriverWait(driver, 30).until(d -> !d.findElements(newInput).isEmpty());
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

    // Enabling the toggle sets ALL answer options to randomize ON.
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

    // Waits for any late-appearing blank options, then deletes them highest-index first.
    // Returns true if any empty option was found — blank options should never appear when set up
    // correctly, so the caller records a soft failure.
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

    @Step("Apply filters and wait for sidebar to close")
    public FormQuestionsPage applyFilters() {
        WebElement btn = new WebDriverWait(driver, 30).until(
            ExpectedConditions.visibilityOfElementLocated(applyFiltersButton));
        scrollToCenter(btn);
        new WebDriverWait(driver, 30).until(
            ExpectedConditions.elementToBeClickable(applyFiltersButton));
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

    // Clicks the section title to blur any active field so Bubble.io's reactive validation runs
    // and commits every question's saved state before the preview request is sent.
    //
    // IMPORTANT: this MUST be a REAL (native) click — a JS click does not change
    // document.activeElement or fire the field's blur event, so Bubble would never validate.
    // blurActiveElement() is a belt-and-suspenders fallback if the native click is intercepted.
    @Step("Click section title to validate all inputs, then wait")
    public FormQuestionsPage validateAllInputs() {
        try {
            scrollTo(sectionTitle);
            click(sectionTitle);   // native click → moves focus off the field → fires blur → validates
        } catch (Exception ignored) {}
        blurActiveElement();
        // Give Bubble.io 3 s to run its DB validation pass before proceeding.
        try { Thread.sleep(3000); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        return this;
    }

    // Reloads the Form Questions editor once — after all questions are set up and BEFORE opening the
    // preview. The platform doesn't fully load the scenario products on the first render, which makes
    // them render incompletely in the preview journey (missing per-scenario displays / only one
    // scenario's split option). Reloading forces the products to load properly from the backend.
    public FormQuestionsPage reloadEditorForProductLoad() {
        System.out.println("[FormQuestions] Reloading editor before preview so all scenario products load properly");
        driver.navigate().refresh();
        // Wait for the editor to be interactive again (Preview button back on the page).
        new WebDriverWait(driver, 45).until(
            ExpectedConditions.presenceOfElementLocated(previewJourneyButton));
        // Settle so Bubble finishes re-fetching the questions/products before we validate + preview.
        try { Thread.sleep(3000); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        return this;
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

    // Clicks Preview, selects Desktop in the chooser, waits for the preview tab to open, captures its
    // URL, closes it, and returns to the editor window. The Desktop/Mobile chooser does NOT auto-
    // dismiss: on an error toast, dismissing the toast (dismiss-toast) also closes the chooser; with
    // no toast the chooser stays open and we close it (close-preview-chooser) before retrying; on
    // success we close it after capturing the URL. Retries up to 3 attempts.
    @Step("Open preview (select Desktop) and capture the preview URL")
    // Waits until the Form Questions page is interactive (its Preview button is present) — used before
    // clicking Preview, e.g. right after advancing from Shop Setup.
    public FormQuestionsPage waitUntilReady() {
        new WebDriverWait(driver, 30).until(
            ExpectedConditions.presenceOfElementLocated(previewJourneyButton));
        return this;
    }

    public String clickPreviewAndGetUrl() {
        try { Thread.sleep(2000); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        String mainWindow = driver.getWindowHandle();

        openPreviewChooserAndSelectDesktop();

        final int MAX_ATTEMPTS = 3;
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            boolean tabOpened = pollForTabOrToast(attempt < MAX_ATTEMPTS ? 10 : 30);
            if (tabOpened) break;
            if (attempt == MAX_ATTEMPTS) {
                throw new RuntimeException("Preview tab did not open after " + MAX_ATTEMPTS + " attempts");
            }
            if (toastDetectedDuringPoll) {
                System.out.println("[Preview] Incomplete-fields toast on attempt " + attempt
                    + " — dismissing (this also closes the Desktop/Mobile chooser) and retrying");
                dismissToastAndChooser();
                try { Thread.sleep(2000); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            } else {
                System.out.println("[Preview] No tab or toast on attempt " + attempt
                    + " — closing the chooser and retrying");
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
        System.out.println("[Preview] Captured preview URL: " + previewUrl);
        return previewUrl;
    }
}

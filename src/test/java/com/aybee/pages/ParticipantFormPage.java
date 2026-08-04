package com.aybee.pages;

import com.aybee.context.GlobalTestState;
import io.qameta.allure.Step;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.FluentWait;
import org.testng.asserts.SoftAssert;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class ParticipantFormPage extends BasePage {

    private static final By QUESTION_TITLE = By.id("final-questions-title");
    private static final By TEXT_INPUT     = By.id("textInputDeskdesktop");
    private static final By CONTINUE_BTN   = By.xpath(
        "//button[@class='clickable-element bubble-element Button cpaOkf']");
    private static final By LOGIN_MARKER   = By.id("toggle-sign-in");

    // 54 words — comfortably above the 35-word minimum.
    private static final String LONG_TEXT_ANSWER =
        "This product delivers exceptional performance and outstanding build quality. " +
        "The design is both functional and aesthetically pleasing, making it a strong contender " +
        "in its category. The pricing is competitive and the overall value for money is excellent " +
        "given the range of features offered. I would highly recommend this product to anyone " +
        "looking for quality and long-term reliability.";

    // Q2/Q3/Q4 options are read from GlobalTestState at call time so they always reflect
    // whatever texts were last entered in FormQuestionsSteps — no manual sync needed.
    private static List<String> q2Options() { return GlobalTestState.q2SelectOptions; }
    private static String       q3Option()  { return GlobalTestState.q3SelectOption;  }
    private static List<String> q4Options() { return GlobalTestState.q4SelectOptions; }

    // Default (pre-added) question titles — shown before user-configured questions.
    // D1 and D2 are mutually exclusive based on whether the participant bought our product.
    // D3 is shown in both cases.
    private static final String D1_TITLE = "You have decided to buy this product. What did you particularly like about it?";
    private static final String D2_TITLE = "Why did you decide against this product?";
    private static final String D3_TITLE = "What are your top 3 criteria when selecting a product in this category?";

    // User-configured question titles for flow verification.
    private static final String Q1_TITLE = "Describe your overall impression of the product in your own words.";
    private static final String Q2_TITLE = "Which product attributes matter most to your purchase decision?";
    private static final String Q3_TITLE = "How would you rate the value for money of this product?";
    private static final String Q4_TITLE = "Which factors would most influence your decision to buy this product again?";
    private static final String Q5_TITLE = "How likely are you to recommend this product to someone you know?";
    private static final String Q6_TITLE = "Rate your satisfaction with the following aspects of this product.";

    // ── Individual public step-level methods ─────────────────────────────────

    @Step("Answer long text question and continue")
    public void answerLongTextQuestion() {
        SoftAssert sa = new SoftAssert();
        waitForQuestionTitle(sa);
        answerLongText(sa);
        clickContinue(sa);
        sa.assertAll();
    }

    @Step("Answer limited choice question and continue")
    public void answerLimitedChoiceQuestion() {
        SoftAssert sa = new SoftAssert();
        waitForQuestionTitle(sa);
        answerLimitedChoice(sa);
        clickContinue(sa);
        sa.assertAll();
    }

    @Step("Answer single choice question and continue")
    public void answerSingleChoiceQuestion() {
        SoftAssert sa = new SoftAssert();
        waitForQuestionTitle(sa);
        answerSingleChoice(sa);
        clickContinue(sa);
        sa.assertAll();
    }

    @Step("Answer multiple choice question and continue")
    public void answerMultipleChoiceQuestion() {
        SoftAssert sa = new SoftAssert();
        waitForQuestionTitle(sa);
        answerMultipleChoice(sa);
        clickContinue(sa);
        sa.assertAll();
    }

    @Step("Answer horizontal Likert question and continue")
    public void answerHorizontalLikertQuestion() {
        SoftAssert sa = new SoftAssert();
        waitForQuestionTitle(sa);
        answerLikertHorizontal(sa);
        clickContinue(sa);
        sa.assertAll();
    }

    @Step("Answer vertical Likert question and continue")
    public void answerVerticalLikertQuestion() {
        SoftAssert sa = new SoftAssert();
        waitForQuestionTitle(sa);
        answerLikertVertical(sa);
        clickContinue(sa);
        sa.assertAll();
    }

    @Step("Verify participant form survey redirected to login page")
    public void verifyCompletionRedirect() {
        SoftAssert sa = new SoftAssert();
        try {
            new FluentWait<>(driver)
                .withTimeout(30, TimeUnit.SECONDS)
                .pollingEvery(500, TimeUnit.MILLISECONDS)
                .ignoring(Exception.class)
                .until(ExpectedConditions.presenceOfElementLocated(LOGIN_MARKER));
        } catch (Exception e) {
            sa.fail("[ParticipantForm] Survey did not redirect to login page after completion");
        }
        sa.assertAll();
    }

    // ── All-in-one fallback ───────────────────────────────────────────────────

    @Step("Answer all participant form questions and verify redirect to login page on completion")
    public void answerAllAndVerifyCompletion(String currentScenario) {
        SoftAssert sa = new SoftAssert();

        // Q1, Q2, Q6 are always shown for participants who bought our product.
        // Q3, Q4, Q5 each select both options in their filter section (both products / both
        // scenarios), so they appear in both scenarios. Q4's Show=Specific Product (Scenario A)
        // only controls which product is displayed — it still appears for Scenario B too.
        // D3 (a default question) is always shown WHEN defaults exist — expect it only if
        // DEFAULT_QUESTIONS=yes (D1/D2 are the buy/not-buy pair, handled inline, not asserted here).
        List<String> alwaysExpected = new java.util.ArrayList<>(Arrays.asList(
            Q1_TITLE, Q2_TITLE, Q3_TITLE, Q4_TITLE, Q5_TITLE, Q6_TITLE));
        if (FormQuestionsPage.HAS_DEFAULT_QUESTIONS) {
            alwaysExpected.add(0, D3_TITLE);
        }
        java.util.Set<String> seenTitles = new java.util.HashSet<>();

        // up to 3 default + 6 user-configured = 9 total (or 6 when no defaults); 12 gives margin.
        String previousTitle = null;
        for (int i = 0; i < 12; i++) {
            if (isLoginPageVisible()) break;

            String title = waitForQuestionTitle(sa, previousTitle);
            if (title == null) break;

            previousTitle = title;
            seenTitles.add(title);
            System.out.println("[ParticipantForm] Answering: " + title);

            answerQuestion(title, sa);
            clickContinue(sa);
        }

        // Verify mandatory questions were shown (partial match — title may be truncated in DOM).
        // Before failing, read final-questions-title right now — the loop may have exited
        // while the question was still on screen (e.g. login marker appeared during transition).
        // If the title is currently visible and matches, treat it as shown and skip the failure.
        for (String expected : alwaysExpected) {
            boolean found = seenTitles.stream().anyMatch(t -> t.contains(expected) || expected.contains(t));
            if (!found) {
                List<WebElement> titleEls = driver.findElements(QUESTION_TITLE);
                if (!titleEls.isEmpty()) {
                    String current = titleEls.get(0).getText().trim();
                    if (!current.isEmpty() && (current.contains(expected) || expected.contains(current))) {
                        System.out.println("[ParticipantForm] Question visible at assertion time " +
                            "but not captured during loop — treating as shown: " + expected);
                        found = true;
                    }
                }
            }
            if (!found) {
                sa.fail("[ParticipantForm] Expected question not shown: " + expected);
            }
        }
        // Q3, Q4 and Q5 each select both options within their filter section, so they appear in
        // both scenarios and are covered by the alwaysExpected check above — no per-scenario
        // assertion is needed.

        sa.assertAll();
    }

    private String waitForQuestionTitle(SoftAssert sa) {
        return waitForQuestionTitle(sa, null);
    }

    // Polls until final-questions-title has non-empty text different from previousTitle,
    // then re-reads after 500 ms to confirm it is stable — Bubble.io occasionally renders
    // a ghost title for a split second before auto-advancing, which would produce false
    // positives or wrong answer-method dispatch. Returns null when the login page appears
    // (survey complete) or the 60 s timeout expires.
    private String waitForQuestionTitle(SoftAssert sa, String previousTitle) {
        try {
            String result = new org.openqa.selenium.support.ui.WebDriverWait(driver, 60).until(d -> {
                if (isLoginPageVisible()) return "";
                List<WebElement> els = d.findElements(QUESTION_TITLE);
                if (els.isEmpty()) return null;
                String text = els.get(0).getText().trim();
                if (text.isEmpty() || text.equals(previousTitle)) return null;
                // Stability check — re-read after 500 ms. A ghost title disappears or
                // changes within this window; a real question stays.
                try { Thread.sleep(500); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
                List<WebElement> again = d.findElements(QUESTION_TITLE);
                if (again.isEmpty()) return null;
                String stable = again.get(0).getText().trim();
                return stable.equals(text) ? text : null;
            });
            // Empty string is the login-visible sentinel — exit cleanly without failing.
            return (result == null || result.isEmpty()) ? null : result;
        } catch (Exception e) {
            if (!isLoginPageVisible()) {
                sa.fail("[ParticipantForm] Question title did not appear within 60s");
            }
            return null;
        }
    }

    private void answerQuestion(String title, SoftAssert sa) {
        // Default (pre-added) questions — all text input, answered as soon as input is clickable.
        if (title.contains("You have decided to buy this product")) {
            answerLongText(sa);
        } else if (title.contains("Why did you decide against this product")) {
            answerLongText(sa);
        } else if (title.contains("What are your top 3 criteria")) {
            answerLongText(sa);
        // User-configured questions.
        } else if (title.contains("Describe your overall impression")) {
            answerLongText(sa);
        } else if (title.contains("Which product attributes matter most")) {
            answerLimitedChoice(sa);
        } else if (title.contains("How would you rate the value for money")) {
            answerSingleChoice(sa);
        } else if (title.contains("Which factors would most influence")) {
            answerMultipleChoice(sa);
        } else if (title.contains("How likely are you to recommend")) {
            answerLikertHorizontal(sa);
        } else if (title.contains("Rate your satisfaction")) {
            answerLikertVertical(sa);
        } else {
            sa.fail("[ParticipantForm] Unrecognised question title: " + title);
        }
    }

    @Step("Answer long text question (min 35 words)")
    private void answerLongText(SoftAssert sa) {
        try {
            WebElement input = new FluentWait<>(driver)
                .withTimeout(30, TimeUnit.SECONDS)
                .pollingEvery(500, TimeUnit.MILLISECONDS)
                .ignoring(Exception.class)
                .until(ExpectedConditions.elementToBeClickable(TEXT_INPUT));
            ((JavascriptExecutor) driver).executeScript(
                "arguments[0].scrollIntoView({behavior:'instant',block:'center'});", input);
            input.clear();
            input.sendKeys(LONG_TEXT_ANSWER);
            blurActiveElement();
        } catch (Exception e) {
            sa.fail("[ParticipantForm] Long text input not found: " + e.getMessage());
        }
    }

    @Step("Answer limited choice question (select non-exclusive options)")
    private void answerLimitedChoice(SoftAssert sa) {
        // Wait once for the option buttons to render — resolves in < 1s in positive flows;
        // 15s covers slow Bubble.io renders. Avoids a per-option 30s wait that wasted time
        // even when the DOM was already fully populated.
        try {
            new FluentWait<>(driver)
                .withTimeout(15, TimeUnit.SECONDS)
                .pollingEvery(300, TimeUnit.MILLISECONDS)
                .ignoring(Exception.class)
                .until(d -> !d.findElements(By.cssSelector("[id^='multiple-choice-']")).isEmpty());
        } catch (Exception e) {
            sa.fail("[ParticipantForm] Limited choice options did not render within 15s");
            return;
        }

        int selected = 0;
        for (String option : q2Options()) {
            String lc = option.toLowerCase();
            // Primary lookup: stored option text (which may have a trailing 's' appended by
            // the retrigger logic). Fallback: strip the trailing 's' in case Bubble.io reverted
            // the field to its saved value before we could read back the updated text.
            WebElement el = findMultipleChoiceElement(lc);
            if (el == null && lc.endsWith("s")) {
                el = findMultipleChoiceElement(lc.substring(0, lc.length() - 1));
            }
            if (el == null) {
                sa.fail("[ParticipantForm] Limited choice option not found in DOM: " + option);
                continue;
            }
            try {
                scrollToCenter(el);
                el.click();
                System.out.println("[ParticipantForm] Clicked limited choice option: " + option);
                try { Thread.sleep(500); } catch (InterruptedException i) { Thread.currentThread().interrupt(); }
                selected++;
            } catch (Exception e) {
                sa.fail("[ParticipantForm] Failed to click limited choice option: " + option);
            }
        }
        if (selected < q2Options().size()) {
            sa.fail("[ParticipantForm] Only " + selected + " of " + q2Options().size() + " limited choice options were selected");
        }
    }

    // Immediate (no-wait) lookup for a rendered multiple-choice element by ID suffix.
    // Only call this after confirming options are present in the DOM.
    private WebElement findMultipleChoiceElement(String lc) {
        List<WebElement> els = driver.findElements(By.id("multiple-choice-" + lc));
        return els.isEmpty() ? null : els.get(0);
    }

    @Step("Answer single choice question")
    private void answerSingleChoice(SoftAssert sa) {
        String option = q3Option();
        String lc = option.toLowerCase();
        // Step 1: wait for any choice options to render — mirrors the same pattern used in
        // answerMultipleChoice and answerLimitedChoice. This ensures the prefix check below
        // runs against a fully-rendered DOM rather than an empty one.
        try {
            new FluentWait<>(driver)
                .withTimeout(15, TimeUnit.SECONDS)
                .pollingEvery(500, TimeUnit.MILLISECONDS)
                .ignoring(Exception.class)
                .until(d -> !d.findElements(
                    By.cssSelector("[id^='single-choice-'],[id^='multiple-choice-']")).isEmpty());
        } catch (Exception e) {
            sa.fail("[ParticipantForm] Single choice options did not render: " + e.getMessage());
            return;
        }
        // Step 2: determine which ID prefix the platform rendered (no timeout needed — options are present).
        By locator = driver.findElements(By.id("single-choice-" + lc)).isEmpty()
            ? By.id("multiple-choice-" + lc)
            : By.id("single-choice-" + lc);
        // Step 3: click the resolved element.
        try {
            WebElement el = new FluentWait<>(driver)
                .withTimeout(10, TimeUnit.SECONDS)
                .pollingEvery(500, TimeUnit.MILLISECONDS)
                .ignoring(Exception.class)
                .until(ExpectedConditions.elementToBeClickable(locator));
            scrollToCenter(el);
            el.click();
        } catch (Exception e) {
            sa.fail("[ParticipantForm] Single choice option not found: " + option);
        }
    }

    @Step("Answer multiple choice question (select 2 non-exclusive options)")
    private void answerMultipleChoice(SoftAssert sa) {
        try {
            new FluentWait<>(driver)
                .withTimeout(15, TimeUnit.SECONDS)
                .pollingEvery(500, TimeUnit.MILLISECONDS)
                .ignoring(Exception.class)
                .until(d -> {
                    List<WebElement> els = d.findElements(By.cssSelector("[id^='multiple-choice-']"));
                    return els.isEmpty() ? null : els;
                });
        } catch (Exception e) {
            sa.fail("[ParticipantForm] Multiple choice options not rendered on page");
            return;
        }
        for (String option : q4Options()) {
            try {
                WebElement el = new FluentWait<>(driver)
                    .withTimeout(10, TimeUnit.SECONDS)
                    .pollingEvery(500, TimeUnit.MILLISECONDS)
                    .ignoring(Exception.class)
                    .until(ExpectedConditions.elementToBeClickable(
                        By.id("multiple-choice-" + option.toLowerCase())));
                scrollToCenter(el);
                el.click();
                try { Thread.sleep(500); } catch (InterruptedException i) { Thread.currentThread().interrupt(); }
            } catch (Exception e) {
                sa.fail("[ParticipantForm] Multiple choice option not found or not clickable: " + option);
            }
        }
    }

    @Step("Answer horizontal Likert question (click first scale option)")
    private void answerLikertHorizontal(SoftAssert sa) {
        try {
            WebElement first = new FluentWait<>(driver)
                .withTimeout(15, TimeUnit.SECONDS)
                .pollingEvery(500, TimeUnit.MILLISECONDS)
                .ignoring(Exception.class)
                .until(d -> {
                    List<WebElement> opts = d.findElements(By.cssSelector("[id^='likert-scale-']"));
                    return opts.isEmpty() ? null : opts.get(0);
                });
            scrollToCenter(first);
            first.click();
        } catch (Exception e) {
            sa.fail("[ParticipantForm] Horizontal Likert scale options not rendered: " + e.getMessage());
        }
    }

    @Step("Answer vertical Likert matrix (select first scale option per row)")
    private void answerLikertVertical(SoftAssert sa) {
        try {
            new FluentWait<>(driver)
                .withTimeout(15, TimeUnit.SECONDS)
                .pollingEvery(500, TimeUnit.MILLISECONDS)
                .ignoring(Exception.class)
                .until(d -> !d.findElements(By.cssSelector("[id^='likert-scale-']")).isEmpty());
        } catch (Exception e) {
            sa.fail("[ParticipantForm] Vertical Likert matrix options not rendered");
            return;
        }
        Object clicked = ((JavascriptExecutor) driver).executeScript(
            "var cells = document.querySelectorAll('[id^=\"likert-scale-\"]');" +
            "var seen = [];" +
            "var count = 0;" +
            "cells.forEach(function(c) {" +
            "  var row = c.parentElement;" +
            "  if (seen.indexOf(row) === -1) {" +
            "    c.click();" +
            "    seen.push(row);" +
            "    count++;" +
            "  }" +
            "});" +
            "return count;");
        if (!(clicked instanceof Long) || (Long) clicked == 0) {
            sa.fail("[ParticipantForm] Could not select any options in vertical Likert matrix");
        } else {
            System.out.println("[ParticipantForm] Vertical Likert: clicked options in " + clicked + " rows");
        }
    }

    private void clickContinue(SoftAssert sa) {
        try {
            new FluentWait<>(driver)
                .withTimeout(10, TimeUnit.SECONDS)
                .pollingEvery(500, TimeUnit.MILLISECONDS)
                .ignoring(Exception.class)
                .until(ExpectedConditions.elementToBeClickable(CONTINUE_BTN));
            jsClick(CONTINUE_BTN);
            try { Thread.sleep(1000); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        } catch (Exception e) {
            sa.fail("[ParticipantForm] Continue button not clickable: " + e.getMessage());
        }
    }

    private boolean isLoginPageVisible() {
        return !driver.findElements(LOGIN_MARKER).isEmpty();
    }
}

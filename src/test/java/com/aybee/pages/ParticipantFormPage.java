package com.aybee.pages;

import com.aybee.context.GlobalTestState;
import com.aybee.utils.ScreenshotSoftAssert;
import io.qameta.allure.Step;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.FluentWait;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.asserts.SoftAssert;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

// Participant (guest) answering of the Questionnaire form questions in the preview journey.
//
// Ported from the msjourney ParticipantFormPage — SAME elements/xpaths as every other aybee suite's
// form-question answering: question title `final-questions-title`, long-text input `textInputDeskdesktop`,
// continue button xpath //button[@class="clickable-element bubble-element Button cpaOkf"], choice options
// `multiple-choice-<textLc>` / `single-choice-<textLc>`, Likert options `[id^='likert-scale-']`, and the
// completion redirect landmark `toggle-sign-in`.
//
// Questionnaire specifics: there are NO marketplace / product-detail / opener steps (this experiment
// type has no products) and NO pre-added default questions — only the 6 matrix questions built in
// FormQuestionsSteps appear, gated by the response filters. The guest's answers below are chosen so
// every downstream filter is satisfied and all 6 questions surface.
public class ParticipantFormPage extends BasePage {

    private static final By QUESTION_TITLE = By.id("final-questions-title");
    private static final By TEXT_INPUT     = By.id("textInputDeskdesktop");
    private static final By CONTINUE_BTN   = By.xpath(
        "//button[@class='clickable-element bubble-element Button cpaOkf']");
    private static final By LOGIN_MARKER   = By.id("toggle-sign-in");

    // 54 words — comfortably above the 35-word minimum enforced on long-text questions.
    private static final String LONG_TEXT_ANSWER =
        "This product delivers exceptional performance and outstanding build quality. " +
        "The design is both functional and aesthetically pleasing, making it a strong contender " +
        "in its category. The pricing is competitive and the overall value for money is excellent " +
        "given the range of features offered. I would highly recommend this product to anyone " +
        "looking for quality and long-term reliability.";

    // Question title substrings — kept in sync with FormQuestionsSteps' Q*_TEXT.
    private static final String Q1_TITLE = "describe your overall impression";
    private static final String Q2_TITLE = "Which product attributes matter most";
    private static final String Q3_TITLE = "How would you rate the overall value";
    private static final String Q4_TITLE = "Which factors would most influence";
    private static final String Q5_TITLE = "How likely are you to recommend";
    private static final String Q6_TITLE = "Rate your satisfaction";

    // Default options the guest selects — chosen so each downstream response filter is satisfied:
    //   Q2 → Q3 filter (Q2 opt1/opt2), Q2+Q3 → Q4 filter (Q2 opt1, Q3 opt1), Q3 → Q5 filter (Q3 opt1).
    // These are FALLBACKS used only if setup did not populate GlobalTestState (e.g. running the guest
    // case standalone). The live values are read from GlobalTestState at call time so they reflect
    // any text the preview retrigger updated (a letter appended to a question's first option).
    private static final List<String> Q2_DEFAULT = Arrays.asList("Price competitiveness", "Build quality");
    private static final String       Q3_DEFAULT = "Excellent value";
    private static final List<String> Q4_DEFAULT = Arrays.asList("Price lower than alternatives", "Proven product quality");

    private static List<String> q2Select() {
        return GlobalTestState.q2SelectOptions != null ? GlobalTestState.q2SelectOptions : Q2_DEFAULT;
    }
    private static String q3Select() {
        return GlobalTestState.q3SelectOption != null ? GlobalTestState.q3SelectOption : Q3_DEFAULT;
    }
    private static List<String> q4Select() {
        return GlobalTestState.q4SelectOptions != null ? GlobalTestState.q4SelectOptions : Q4_DEFAULT;
    }

    // ── All-in-one: answer every question until completion redirect ──────────────

    // Loops the question stack, dispatching each title to its answer method and clicking Continue,
    // until the sign-in page appears (survey complete) or the iteration cap is hit. 6 questions +
    // margin. Verifies each expected question was seen.
    @Step("Answer all questionnaire form questions and verify completion redirect to sign in")
    public void answerAllAndVerifyCompletion() {
        SoftAssert sa = new ScreenshotSoftAssert();
        List<String> expected = Arrays.asList(Q1_TITLE, Q2_TITLE, Q3_TITLE, Q4_TITLE, Q5_TITLE, Q6_TITLE);
        java.util.Set<String> seen = new java.util.HashSet<>();

        String previousTitle = null;
        for (int i = 0; i < 12; i++) {
            if (isLoginPageVisible()) break;
            String title = waitForQuestionTitle(sa, previousTitle);
            if (title == null) break;
            previousTitle = title;
            seen.add(title);
            System.out.println("[ParticipantForm] Answering: " + title);
            answerQuestion(title, sa);
            clickContinue(sa);
        }

        for (String exp : expected) {
            boolean found = seen.stream().anyMatch(t -> t.contains(exp) || exp.contains(t));
            if (!found) {
                // The loop may have exited while the last question was still on screen — re-check now.
                List<WebElement> titleEls = driver.findElements(QUESTION_TITLE);
                if (!titleEls.isEmpty()) {
                    String current = titleEls.get(0).getText().trim();
                    if (!current.isEmpty() && (current.contains(exp) || exp.contains(current))) found = true;
                }
            }
            if (!found) sa.fail("[ParticipantForm] Expected question not shown to guest: " + exp);
        }
        sa.assertAll();
    }

    @Step("Verify the participant journey redirected to the sign-in page on completion")
    public void verifyCompletionRedirect() {
        SoftAssert sa = new ScreenshotSoftAssert();
        try {
            new FluentWait<>(driver)
                .withTimeout(30, TimeUnit.SECONDS)
                .pollingEvery(500, TimeUnit.MILLISECONDS)
                .ignoring(Exception.class)
                .until(ExpectedConditions.presenceOfElementLocated(LOGIN_MARKER));
        } catch (Exception e) {
            sa.fail("[ParticipantForm] Survey did not redirect to the sign-in page after completion");
        }
        sa.assertAll();
    }

    // ── Title handling ───────────────────────────────────────────────────────────

    // Polls until final-questions-title has non-empty text different from previousTitle, then
    // re-reads after 500 ms to confirm stability — Bubble.io occasionally renders a ghost title for
    // a split second before auto-advancing. Returns null when the login page appears or on timeout.
    private String waitForQuestionTitle(SoftAssert sa, String previousTitle) {
        try {
            String result = new WebDriverWait(driver, 60)
                // Re-find the title each poll AND ignore staleness — Bubble re-renders the title node
                // reactively, so a reference read mid-poll can go stale between the two getText calls.
                .ignoring(StaleElementReferenceException.class)
                .until(d -> {
                if (isLoginPageVisible()) return "";
                List<WebElement> els = d.findElements(QUESTION_TITLE);
                if (els.isEmpty()) return null;
                String text = els.get(0).getText().trim();
                if (text.isEmpty() || text.equals(previousTitle)) return null;
                try { Thread.sleep(500); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
                List<WebElement> again = d.findElements(QUESTION_TITLE);
                if (again.isEmpty()) return null;
                String stable = again.get(0).getText().trim();
                return stable.equals(text) ? text : null;
            });
            return (result == null || result.isEmpty()) ? null : result;
        } catch (Exception e) {
            if (!isLoginPageVisible()) {
                sa.fail("[ParticipantForm] Question title did not appear within 60s");
            }
            return null;
        }
    }

    // Filter dependency chain of the matrix built in FormQuestionsSteps (drives which questions appear):
    //   Q2 answers → filter that shows Q3 (Price competitiveness / Build quality) AND, with Q3, shows Q4.
    //   Q3 answer  → filter that shows Q4 (with Q2) AND shows Q5 (Excellent value).
    //   Q4, Q5, Q6 → terminal: their own answers gate NOTHING downstream.
    // So Q2 and Q3 are FILTER-DRIVING — the guest MUST click the exact configured options or the
    // downstream filtered questions won't render. Q4 (and any terminal choice question) is tolerant:
    // if a configured option can't be clicked, any available option is fine to proceed.
    private void answerQuestion(String title, SoftAssert sa) {
        if (title.contains(Q1_TITLE))      answerLongText(sa);
        else if (title.contains(Q2_TITLE)) answerChoice(sa, q2Select(), true);              // limited — drives Q3/Q4
        else if (title.contains(Q3_TITLE)) answerChoice(sa, Arrays.asList(q3Select()), true); // single — drives Q4/Q5
        else if (title.contains(Q4_TITLE)) answerChoice(sa, q4Select(), false);             // multiple — terminal
        else if (title.contains(Q5_TITLE)) answerLikertHorizontal(sa);
        else if (title.contains(Q6_TITLE)) answerLikertVertical(sa);
        else sa.fail("[ParticipantForm] Unrecognised question title: " + title);
    }

    // ── Answer methods ─────────────────────────────────────────────────────────

    @Step("Answer long text question (min 35 words)")
    private void answerLongText(SoftAssert sa) {
        // Re-find the input on each attempt (Bubble re-renders reactively → stale references);
        // retry the whole locate+type on StaleElementReferenceException.
        for (int attempt = 0; attempt < 3; attempt++) {
            try {
                WebElement input = new FluentWait<>(driver)
                    .withTimeout(30, TimeUnit.SECONDS)
                    .pollingEvery(500, TimeUnit.MILLISECONDS)
                    .ignoring(Exception.class)
                    .until(ExpectedConditions.elementToBeClickable(TEXT_INPUT));
                scrollToCenter(input);
                input.clear();
                input.sendKeys(LONG_TEXT_ANSWER);
                blurActiveElement();
                return;
            } catch (StaleElementReferenceException stale) {
                // element went stale between locate and type — loop and re-find
            } catch (Exception e) {
                sa.fail("[ParticipantForm] Long text input (textInputDeskdesktop) not found: " + e.getMessage());
                return;
            }
        }
        sa.fail("[ParticipantForm] Long text input (textInputDeskdesktop) kept going stale after 3 attempts");
    }

    // Selects the requested options robustly. Choice options render as single-choice-<lc> or
    // multiple-choice-<lc> depending on type — we look up both prefixes, tolerant of a trailing "s"
    // (the preview retrigger may append one to a question's first option).
    //
    // filterDriving = the answers here gate which downstream questions appear, so the EXACT configured
    // options must be selected; failing to click one is a hard soft-fail (downstream questions would
    // silently not render). filterDriving = false (terminal question): the answers gate nothing, so if
    // a configured option can't be clicked we fall back to any available option just to satisfy the
    // minimum-selection requirement and let Continue enable.
    @Step("Select choice options (filterDriving={filterDriving})")
    private void answerChoice(SoftAssert sa, List<String> options, boolean filterDriving) {
        try {
            new FluentWait<>(driver)
                .withTimeout(15, TimeUnit.SECONDS)
                .pollingEvery(300, TimeUnit.MILLISECONDS)
                .ignoring(Exception.class)
                .until(d -> !d.findElements(
                    By.cssSelector("[id^='single-choice-'],[id^='multiple-choice-']")).isEmpty());
        } catch (Exception e) {
            sa.fail("[ParticipantForm] Choice options did not render within 15s");
            return;
        }
        // Brief settle so all options for THIS question are rendered (a filtered question renders its
        // options a beat after its title) before we start clicking.
        try { Thread.sleep(700); } catch (InterruptedException i) { Thread.currentThread().interrupt(); }

        int selected = 0;
        java.util.Set<String> clickedIds = new java.util.HashSet<>();
        for (String option : options) {
            String id = clickChoiceOption(option);
            if (id != null) {
                selected++;
                clickedIds.add(id);
            } else if (filterDriving) {
                sa.fail("[ParticipantForm] Could not select filter-driving option '" + option
                    + "' — downstream filtered questions depend on it and may not appear");
            } else {
                System.out.println("[ParticipantForm] Requested option '" + option
                    + "' not selectable — will fall back to any available option");
            }
        }
        // Terminal question fallback: ensure at least options.size() selections so Continue enables.
        if (!filterDriving && selected < options.size()) {
            selected += clickAnyAvailableOptions(options.size() - selected, clickedIds);
        }
        if (selected == 0) {
            sa.fail("[ParticipantForm] No choice option could be selected for this question");
        }
    }

    // Robustly clicks a choice option by text: re-finds it each attempt (Bubble re-renders reactively),
    // scrolls it into view, waits for it to be clickable, tries a native click, then a JS click as a
    // fallback for intercepted/overlaid elements. Retries up to 3× on staleness/interception.
    // Returns the clicked element's id on success, or null if it could not be clicked.
    private String clickChoiceOption(String option) {
        for (int attempt = 0; attempt < 3; attempt++) {
            WebElement el = findChoiceElement(option);
            if (el == null) {
                try { Thread.sleep(500); } catch (InterruptedException i) { Thread.currentThread().interrupt(); }
                continue;
            }
            String id = null;
            try { id = el.getAttribute("id"); } catch (Exception ignored) {}
            try {
                scrollToCenter(el);
                new WebDriverWait(driver, 5).until(ExpectedConditions.elementToBeClickable(el));
                el.click();
                System.out.println("[ParticipantForm] Clicked choice option: " + option);
                try { Thread.sleep(400); } catch (InterruptedException i) { Thread.currentThread().interrupt(); }
                return id != null ? id : option;
            } catch (Exception nativeFail) {
                // Native click failed (stale / intercepted by a Bubble overlay) — re-find and JS-click.
                try {
                    WebElement again = findChoiceElement(option);
                    if (again != null) {
                        if (id == null) { try { id = again.getAttribute("id"); } catch (Exception ignored) {} }
                        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", again);
                        System.out.println("[ParticipantForm] JS-clicked choice option: " + option);
                        try { Thread.sleep(400); } catch (InterruptedException i) { Thread.currentThread().interrupt(); }
                        return id != null ? id : option;
                    }
                } catch (Exception jsFail) {
                    // fall through to next attempt
                }
            }
        }
        return null;
    }

    // Clicks up to `needed` currently-displayed choice options whose ids are not in skipIds — used as
    // the terminal-question fallback so Continue enables even if a specific configured option could
    // not be clicked. DOM order is preserved, so the first (non-exclusive) options are chosen first.
    private int clickAnyAvailableOptions(int needed, java.util.Set<String> skipIds) {
        int clicked = 0;
        for (WebElement el : driver.findElements(
                By.cssSelector("[id^='single-choice-'],[id^='multiple-choice-']"))) {
            if (clicked >= needed) break;
            try {
                if (!el.isDisplayed()) continue;
                String id = el.getAttribute("id");
                if (id != null && skipIds.contains(id)) continue;
                scrollToCenter(el);
                try { el.click(); }
                catch (Exception e) { ((JavascriptExecutor) driver).executeScript("arguments[0].click();", el); }
                if (id != null) skipIds.add(id);
                clicked++;
                System.out.println("[ParticipantForm] Fallback-clicked available option: " + id);
                try { Thread.sleep(400); } catch (InterruptedException i) { Thread.currentThread().interrupt(); }
            } catch (Exception ignored) {}
        }
        return clicked;
    }

    // Looks up a choice element by option text across both single/multiple prefixes, tolerating a
    // trailing "s" difference introduced by the preview retrigger.
    private WebElement findChoiceElement(String option) {
        String lc = option.toLowerCase();
        String[] candidates = lc.endsWith("s")
            ? new String[]{ lc, lc.substring(0, lc.length() - 1) }
            : new String[]{ lc, lc + "s" };
        for (String prefix : new String[]{"single-choice-", "multiple-choice-"}) {
            for (String text : candidates) {
                List<WebElement> els = driver.findElements(By.id(prefix + text));
                if (!els.isEmpty()) return els.get(0);
            }
        }
        return null;
    }

    @Step("Answer horizontal Likert question (click first scale option)")
    private void answerLikertHorizontal(SoftAssert sa) {
        By firstOption = By.cssSelector("[id^='likert-scale-']");
        // Wait for options to render first.
        try {
            new FluentWait<>(driver)
                .withTimeout(15, TimeUnit.SECONDS)
                .pollingEvery(500, TimeUnit.MILLISECONDS)
                .ignoring(Exception.class)
                .until(d -> !d.findElements(firstOption).isEmpty());
        } catch (Exception e) {
            sa.fail("[ParticipantForm] Horizontal Likert scale options not rendered: " + e.getMessage());
            return;
        }
        // Re-find on each attempt (reactive re-render → stale); native click then JS-click fallback.
        for (int attempt = 0; attempt < 3; attempt++) {
            try {
                List<WebElement> opts = driver.findElements(firstOption);
                if (opts.isEmpty()) { try { Thread.sleep(400); } catch (InterruptedException i) { Thread.currentThread().interrupt(); } continue; }
                WebElement first = opts.get(0);
                scrollToCenter(first);
                try { first.click(); }
                catch (Exception nativeFail) { ((JavascriptExecutor) driver).executeScript("arguments[0].click();", first); }
                return;
            } catch (StaleElementReferenceException stale) {
                // loop and re-find
            } catch (Exception e) {
                sa.fail("[ParticipantForm] Failed to click horizontal Likert option: " + e.getMessage());
                return;
            }
        }
        sa.fail("[ParticipantForm] Horizontal Likert option kept going stale after 3 attempts");
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
            sa.fail("[ParticipantForm] Could not select any options in the vertical Likert matrix");
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

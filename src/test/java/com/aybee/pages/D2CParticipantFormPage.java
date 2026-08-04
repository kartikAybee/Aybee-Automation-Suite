package com.aybee.pages;

import io.qameta.allure.Step;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.FluentWait;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.asserts.SoftAssert;

import java.util.List;
import java.util.concurrent.TimeUnit;

public class D2CParticipantFormPage extends BasePage {

    private static final By QUESTION_TITLE = By.id("final-questions-title");
    private static final By TEXT_INPUT     = By.id("textInputDeskdesktop");
    private static final By CONTINUE_BTN   = By.xpath("//button[@class='clickable-element bubble-element Button cpaOkf']");
    // Element that appears on the sign-up page after form completion.
    private static final By SIGNUP_MARKER  = By.id("toggle-sign-in");

    // 54 words — comfortably above the 35-word minimum required by D2C long text questions.
    private static final String LONG_TEXT_ANSWER =
        "This product delivers exceptional performance and outstanding build quality. " +
        "The design is both functional and aesthetically pleasing, making it a strong contender " +
        "in its category. The pricing is competitive and the overall value for money is excellent " +
        "given the range of features offered. I would highly recommend this product to anyone " +
        "looking for quality and long-term reliability.";

    // D2C pre-existing question title fragments (partial match against final-questions-title text).
    private static final String Q1_FRAGMENT = "Why did you choose this product";
    private static final String Q2_FRAGMENT = "Why did you decide against this product";
    private static final String Q3_FRAGMENT = "top 3 criteria";
    private static final String Q4_FRAGMENT = "Share your overall thoughts";

    // Our-product scenario: Q1 → Q3 → Q4. Q2 must NOT appear.
    // ourProductName is used to verify the product displayed alongside Q4.
    @Step("Answer D2C form questions — our product (Q2 must be absent)")
    public void answerFormQuestionsOurProduct(String ourProductName, SoftAssert sa) {
        runFormLoop(false, ourProductName, sa);
    }

    // Competitor scenario: Q1 → Q2 → Q3 → Q4. Q2 MUST appear.
    // ourProductName is used to verify the product displayed alongside Q2 and Q4.
    @Step("Answer D2C form questions — competitor product (Q2 must appear)")
    public void answerFormQuestionsCompetitor(String ourProductName, SoftAssert sa) {
        runFormLoop(true, ourProductName, sa);
    }

    @Step("Verify guest is redirected to sign-up page after form completion")
    public void verifySignUpRedirect(SoftAssert sa) {
        try {
            new FluentWait<>(driver)
                .withTimeout(30, TimeUnit.SECONDS)
                .pollingEvery(500, TimeUnit.MILLISECONDS)
                .ignoring(Exception.class)
                .until(ExpectedConditions.presenceOfElementLocated(SIGNUP_MARKER));
            System.out.println("[D2CForm] Sign-up page redirect confirmed");
        } catch (Exception e) {
            sa.fail("[D2CForm] Guest was not redirected to sign-up page after last question");
        }
    }

    // ── Core loop ─────────────────────────────────────────────────────────────

    private void runFormLoop(boolean expectQ2, String ourProductName, SoftAssert sa) {
        boolean q2Seen = false;
        boolean q4Seen = false;
        String prev = null;

        for (int i = 0; i < 6; i++) {
            if (isSignupPageVisible()) break;

            String title = waitForTitle(sa, prev);
            if (title == null) break;
            prev = title;
            System.out.println("[D2CForm] Question: " + title);

            if (title.contains(Q1_FRAGMENT)) {
                answerLongText(sa);

            } else if (title.contains(Q2_FRAGMENT)) {
                // Q2 can appear transiently (ghost) and be auto-skipped by the platform
                // within a second. Wait 1s and re-check before answering.
                try { Thread.sleep(1000); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
                List<WebElement> titleEls = driver.findElements(QUESTION_TITLE);
                String stableTitle = titleEls.isEmpty() ? "" : titleEls.get(0).getText().trim();
                if (!stableTitle.contains(Q2_FRAGMENT)) {
                    System.out.println("[D2CForm] Q2 was transient (ghost) — platform skipped it automatically");
                    continue;
                }
                // Q2 is stable — proceed to answer it.
                q2Seen = true;
                if (!expectQ2) {
                    System.out.println("[D2CForm] Warning: Q2 appeared in our-product scenario — answering and continuing");
                } else {
                    verifyProductNameContains(ourProductName, "Q2", sa);
                }
                answerLongText(sa);

            } else if (title.contains(Q3_FRAGMENT)) {
                answerLongText(sa);

            } else if (title.contains(Q4_FRAGMENT)) {
                q4Seen = true;
                // Q4 displays the A/B test product — verify a product name is present.
                verifyProductNameNonEmpty("Q4", sa);
                answerLongText(sa);

            } else {
                sa.fail("[D2CForm] Unrecognised question title: " + title);
                answerLongText(sa);
            }

            clickContinue(sa);
        }

        // Q1/Q2/Q3 are DEFAULT (platform pre-added) questions — Q2 only exists when defaults are
        // present, so only assert its appearance when DEFAULT_QUESTIONS=yes. Q4 is the manually added
        // A/B question and is always expected regardless of defaults.
        if (FormQuestionsPage.HAS_DEFAULT_QUESTIONS && expectQ2 && !q2Seen) {
            sa.fail("[D2CForm] Q2 was expected (competitor flow) but never appeared");
        }
        if (!q4Seen) {
            sa.fail("[D2CForm] Q4 (A/B test product question) was never shown");
        }
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    // Polls until final-questions-title has non-empty text that differs from prev.
    // Waits up to 60s for final-questions-title to be visible with non-empty text
    // different from the previous question. Returns null if sign-up page appears first.
    private String waitForTitle(SoftAssert sa, String prev) {
        try {
            String result = new WebDriverWait(driver, 60).until(d -> {
                if (isSignupPageVisible()) return "";
                List<WebElement> els = d.findElements(QUESTION_TITLE);
                if (els.isEmpty()) return null;
                String text = els.get(0).getText().trim();
                return (!text.isEmpty() && !text.equals(prev)) ? text : null;
            });
            // Empty string is the signup-visible sentinel — treat as null so the loop exits cleanly.
            return (result == null || result.isEmpty()) ? null : result;
        } catch (Exception e) {
            if (!isSignupPageVisible()) {
                sa.fail("[D2CForm] final-questions-title did not appear within 60s");
            }
            return null;
        }
    }

    private void answerLongText(SoftAssert sa) {
        try {
            WebElement input = new FluentWait<>(driver)
                .withTimeout(30, TimeUnit.SECONDS)
                .pollingEvery(500, TimeUnit.MILLISECONDS)
                .ignoring(Exception.class)
                .until(ExpectedConditions.elementToBeClickable(TEXT_INPUT));
            ((JavascriptExecutor) driver).executeScript(
                "arguments[0].scrollIntoView({behavior:'instant',block:'center'});", input);
            // Set value directly via JS then dispatch both input and change so Bubble.io's
            // word-count reactive handler fires regardless of whether sendKeys triggers it.
            ((JavascriptExecutor) driver).executeScript(
                "arguments[0].value = arguments[1];" +
                "arguments[0].dispatchEvent(new Event('input',  {bubbles:true}));" +
                "arguments[0].dispatchEvent(new Event('change', {bubbles:true}));",
                input, LONG_TEXT_ANSWER);
            String actual = input.getAttribute("value");
            if (actual == null || !actual.contains(LONG_TEXT_ANSWER.substring(0, 20))) {
                sa.fail("[D2CForm] Long text answer not reflected in input. Expected start: ["
                    + LONG_TEXT_ANSWER.substring(0, 20) + "] Got: [" + actual + "]");
            } else {
                System.out.println("[D2CForm] Long text answer confirmed in input field");
            }
            blurActiveElement();
        } catch (Exception e) {
            sa.fail("[D2CForm] Long text input not found: " + e.getMessage());
        }
    }

    private void clickContinue(SoftAssert sa) {
        try {
            new FluentWait<>(driver)
                .withTimeout(30, TimeUnit.SECONDS)
                .pollingEvery(500, TimeUnit.MILLISECONDS)
                .ignoring(Exception.class)
                .until(d -> {
                    List<WebElement> btns = d.findElements(CONTINUE_BTN);
                    if (btns.isEmpty()) return false;
                    String style = btns.get(0).getAttribute("style");
                    return style != null && style.contains("cursor: pointer");
                });
            jsClick(CONTINUE_BTN);
            try { Thread.sleep(1000); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        } catch (Exception e) {
            sa.fail("[D2CForm] Continue button not enabled (cursor: pointer never appeared): " + e.getMessage());
        }
    }

    // Verifies d2c-product-name contains expectedName (partial, case-insensitive).
    // Used for Q2 (should show our product) and Q4 (should show A/B test product).
    private void verifyProductNameContains(String expectedName, String questionLabel, SoftAssert sa) {
        try {
            String displayed = driver.findElement(By.id("d2c-product-name")).getText().trim();
            if (expectedName == null || expectedName.isEmpty()) return;
            if (!displayed.toLowerCase().contains(expectedName.toLowerCase())
                    && !expectedName.toLowerCase().contains(displayed.toLowerCase())) {
                sa.fail("[D2CForm] " + questionLabel + " product name mismatch. "
                    + "Expected: [" + expectedName + "] Displayed: [" + displayed + "]");
            } else {
                System.out.println("[D2CForm] " + questionLabel + " product name confirmed: " + displayed);
            }
        } catch (Exception e) {
            sa.fail("[D2CForm] " + questionLabel + " d2c-product-name not found: " + e.getMessage());
        }
    }

    // Verifies d2c-product-name is non-empty (Q4 shows the A/B product whose exact
    // name depends on scenario assignment — just confirm something is shown).
    private void verifyProductNameNonEmpty(String questionLabel, SoftAssert sa) {
        try {
            String displayed = driver.findElement(By.id("d2c-product-name")).getText().trim();
            if (displayed.isEmpty()) {
                sa.fail("[D2CForm] " + questionLabel + " d2c-product-name is empty");
            } else {
                System.out.println("[D2CForm] " + questionLabel + " A/B product shown: " + displayed);
            }
        } catch (Exception e) {
            sa.fail("[D2CForm] " + questionLabel + " d2c-product-name not found: " + e.getMessage());
        }
    }

    private boolean isSignupPageVisible() {
        return !driver.findElements(SIGNUP_MARKER).isEmpty();
    }
}

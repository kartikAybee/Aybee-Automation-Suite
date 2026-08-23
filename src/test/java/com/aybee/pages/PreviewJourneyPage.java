package com.aybee.pages;

import com.aybee.utils.ConfigReader;
import io.qameta.allure.Step;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.FluentWait;
import org.openqa.selenium.support.ui.WebDriverWait;
import com.aybee.utils.ScreenshotSoftAssert;
import org.testng.asserts.SoftAssert;

import java.util.Set;
import java.util.concurrent.TimeUnit;

public class PreviewJourneyPage extends BasePage {

    private final By continueButton          = By.id("continue-button");
    private final By continueStatementButton = By.id("continue-statement");
    private final By helpConfirmButton       = By.id("help-confirm");
    private final By notInterestedButton     = By.id("button-not-interest");

    // Stored on the first call so subsequent passes can close the spent preview tab
    // and re-trigger a fresh session from the editor.
    private String editorWindowHandle;

    private final By shopSetupVerifyLocator  = By.id("marketplacesimulation_shopsetup_next_button");

    // ── Demographic questions ─────────────────────────────────────────────────

    // Tracks the option value answered on the previous question so we can confirm the page
    // advanced to a genuinely different question before interacting with the next one.
    private String lastAnsweredDemographicOption = null;

    // Three-phase: wait for options to fully render → click option → click continue → advance.
    // Bubble.io renders options before attaching handlers — waiting for ANY answer-Option
    // to be clickable first, then waiting for the SPECIFIC option, ensures both the question
    // container and the individual option are ready before we interact.
    private void answerDemographicQuestion(String optionValue, SoftAssert sa) {
        By anyOption     = By.cssSelector("[id^='answer-Option-']");
        By optionLocator = By.id("answer-Option-" + optionValue);

        // Guard — confirm the previous question's option is gone before proceeding,
        // so we never interact with the same question twice.
        if (lastAnsweredDemographicOption != null) {
            By prevLocator = By.id("answer-Option-" + lastAnsweredDemographicOption);
            try {
                new WebDriverWait(driver, 10).until(
                    ExpectedConditions.invisibilityOfElementLocated(prevLocator));
            } catch (Exception e) {
                System.out.println("[DemographicQ] Previous option ["
                    + lastAnsweredDemographicOption + "] still visible — page may not have advanced");
            }
        }

        // Phase 1 — wait for any option to be clickable (question fully rendered),
        // then wait for the specific option to be clickable.
        try {
            new WebDriverWait(driver, 15).until(
                ExpectedConditions.elementToBeClickable(anyOption));
            WebElement option = new WebDriverWait(driver, 15).until(
                ExpectedConditions.elementToBeClickable(optionLocator));
            ((JavascriptExecutor) driver).executeScript(
                "arguments[0].scrollIntoView({behavior:'instant',block:'center'});", option);
            ((JavascriptExecutor) driver).executeScript("arguments[0].click();", option);
            lastAnsweredDemographicOption = optionValue;
        } catch (Exception e) {
            sa.fail("[DemographicQ] Option not found or not clickable: " + optionValue);
            return;
        }

        // Phase 2 — click continue.
        try {
            new WebDriverWait(driver, 15).until(
                ExpectedConditions.elementToBeClickable(continueButton));
            jsClick(continueButton);
        } catch (Exception e) {
            sa.fail("[DemographicQ] Continue button not clickable after selecting: " + optionValue);
            return;
        }

        // Phase 3 — wait for the page to advance (current option disappears).
        try {
            new WebDriverWait(driver, 15).until(
                ExpectedConditions.invisibilityOfElementLocated(optionLocator));
        } catch (Exception e) {
            sa.fail("[DemographicQ] Page did not advance after answering: " + optionValue);
        }
    }

    // Logged-in preview — platform only asks gender and age when a user account
    // is already associated with the session.
    @Step("Answer demographic questions — gender (Male) and age (25 to 34) only")
    public PreviewJourneyPage answerDemographicsGenderAndAge() {
        lastAnsweredDemographicOption = null;
        SoftAssert sa = new ScreenshotSoftAssert();
        answerDemographicQuestion("Male", sa);
        answerDemographicQuestion("25 to 34", sa);
        sa.assertAll();
        return this;
    }

    // Guest preview — no prior account data, platform shows all 8 demographic questions.
    @Step("Answer demographic questions Q1–Q8")
    public PreviewJourneyPage answerAllDemographicQuestions() {
        lastAnsweredDemographicOption = null;
        SoftAssert sa = new ScreenshotSoftAssert();
        answerDemographicQuestion("Male", sa);
        answerDemographicQuestion("25 to 34", sa);
        answerDemographicQuestion("Full-Time Employee", sa);
        answerDemographicQuestion("Single", sa);
        answerDemographicQuestion("Homeowner", sa);
        answerDemographicQuestion("1", sa);
        answerDemographicQuestion("<50k", sa);
        answerDemographicQuestion("Less than high school", sa);
        sa.assertAll();
        return this;
    }

    // Used when gender + age were already answered (e.g. logged-in session re-used)
    // and only Q3–Q8 remain.
    @Step("Answer demographic questions Q3–Q8 (starting from Full-Time Employee)")
    public PreviewJourneyPage answerDemographicQuestionsFromQ3() {
        lastAnsweredDemographicOption = null;
        SoftAssert sa = new ScreenshotSoftAssert();
        answerDemographicQuestion("Full-Time Employee", sa);
        answerDemographicQuestion("Single", sa);
        answerDemographicQuestion("Homeowner", sa);
        answerDemographicQuestion("1", sa);
        answerDemographicQuestion("<50k", sa);
        answerDemographicQuestion("Less than high school", sa);
        sa.assertAll();
        return this;
    }

    // ── Consent statement ─────────────────────────────────────────────────────

    @Step("Agree to consent statement and proceed to D2C product list")
    public PreviewJourneyPage agreeToConsentStatement() {
        By agreeBtn = By.id("agree-statement-button");
        new WebDriverWait(driver, 30).until(
            ExpectedConditions.presenceOfElementLocated(agreeBtn));
        // Scroll agree button into view, then click. Retry if continue doesn't become
        // clickable — Bubble.io may not have attached the handler on the first attempt.
        for (int attempt = 1; attempt <= 3; attempt++) {
            WebElement btn = driver.findElement(agreeBtn);
            ((JavascriptExecutor) driver).executeScript(
                "arguments[0].scrollIntoView({behavior:'instant',block:'center'});", btn);
            jsClick(agreeBtn);
            try {
                new WebDriverWait(driver, 5).until(
                    ExpectedConditions.elementToBeClickable(continueStatementButton));
                break;
            } catch (Exception e) {
                System.out.println("[Consent] Continue not ready after agree click attempt " + attempt + " — retrying");
            }
        }
        jsClick(continueStatementButton);
        new WebDriverWait(driver, 15).until(
            ExpectedConditions.invisibilityOfElementLocated(continueStatementButton));
        return this;
    }

    // ── Help popup ────────────────────────────────────────────────────────────

    // The D2C help popup always appears after consent — wait for it, then dismiss it before
    // proceeding with the rest of the journey.
    //
    // IMPORTANT: the popup OVERLAYS the product list, so the not-interested button can be visible
    // behind it — its visibility is NOT proof the popup is gone. The popup's presence is detected
    // SOLELY by the help-confirm button, and we only proceed once help-confirm is confirmed
    // invisible. If the popup is up but cannot be dismissed, we fail hard rather than proceed with
    // a covered UI (the previous behaviour silently continued, leaving the popup on screen).
    @Step("Wait for help popup to appear and dismiss it")
    public PreviewJourneyPage waitForHelpPopupAndDismiss() {
        // Give the popup time to appear. If it doesn't, reload once and wait again — Bubble.io
        // occasionally renders the list without firing the popup on the first load.
        if (!waitForHelpConfirmVisible(15)) {
            System.out.println("[D2C] Help popup not visible within 15s — reloading and waiting again");
            driver.navigate().refresh();
            if (!waitForHelpConfirmVisible(30)) {
                System.out.println("[D2C] Help popup never appeared after reload — assuming no popup, proceeding");
                return this;
            }
        }

        // Popup is up — click help-confirm and confirm it disappears. Retry because Bubble.io may
        // not have attached the click handler yet on the first attempt.
        for (int attempt = 1; attempt <= 4; attempt++) {
            try {
                jsClick(helpConfirmButton);
            } catch (Exception e) {
                System.out.println("[D2C] help-confirm click attempt " + attempt + " failed: " + e.getMessage());
            }
            try {
                new WebDriverWait(driver, 5).until(
                    ExpectedConditions.invisibilityOfElementLocated(helpConfirmButton));
                System.out.println("[D2C] Help popup dismissed on attempt " + attempt);
                return this;
            } catch (Exception e) {
                System.out.println("[D2C] Help popup still visible after dismiss attempt " + attempt + " — retrying");
            }
        }

        throw new AssertionError(
            "[D2C] Help popup (help-confirm) still visible after 4 dismiss attempts — refusing to proceed "
            + "with the popup still covering the UI");
    }

    // Returns true once help-confirm is visible within the timeout, false otherwise (no throw).
    private boolean waitForHelpConfirmVisible(int timeoutSeconds) {
        try {
            new WebDriverWait(driver, timeoutSeconds).until(
                ExpectedConditions.visibilityOfElementLocated(helpConfirmButton));
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    // ── Not Interested — logged-in user ───────────────────────────────────────

    // When a logged-in user (researcher) clicks Not Interested the platform redirects them
    // back to the shop setup page rather than to the login screen (which is the guest path).
    // Verified by presence of marketplacesimulation_shopsetup_next_button.
    @Step("Click Not Interested and verify redirect to shop setup page")
    public PreviewJourneyPage clickNotInterestedAndVerifyShopSetupRedirect() {
        // A delayed help popup can overlay the Not Interested button — clear it before clicking.
        dismissHelpPopupIfPresent();
        jsClick(notInterestedButton);
        try {
            new WebDriverWait(driver, 30).until(
                ExpectedConditions.visibilityOfElementLocated(shopSetupVerifyLocator));
        } catch (Exception e) {
            throw new AssertionError(
                "[D2C] Shop setup page did not appear after clicking Not Interested — "
                + "marketplacesimulation_shopsetup_next_button not visible after 30s");
        }
        System.out.println("[D2C] Redirected to shop setup page after Not Interested (logged-in)");
        return this;
    }

    // ── Session management ────────────────────────────────────────────────────

    @Step("Clear cookies and web storage for current session")
    public void clearSession() {
        String currentUrl = driver.getCurrentUrl();
        if (currentUrl == null || !currentUrl.contains("platform.aybee.ai")) {
            driver.get(ConfigReader.get("BASE_URL"));
        }
        driver.manage().deleteAllCookies();
        ((JavascriptExecutor) driver).executeScript(
            "window.localStorage.clear(); window.sessionStorage.clear();");
        // Navigate away so Bubble.io initialises a clean guest session rather than
        // reusing stale SPA state when the preview URL is loaded next.
        driver.get("about:blank");
        System.out.println("[Session] Cookies and storage cleared");
    }

    // ── Navigation ────────────────────────────────────────────────────────────

    // Pass 1: stores the editor tab handle, opens a new tab, and navigates the stored
    //         preview URL there (fresh session — no prior "not interested" state).
    // Pass 2+: closes the spent preview tab (which ended at shop setup after Not Interested),
    //          switches back to the editor tab, re-clicks the preview button to generate a
    //          fresh session, and switches to the new tab that Bubble.io opens.
    //          Re-triggering from the editor is required because Bubble.io marks a preview
    //          session as done server-side after "not interested" — reusing the stored URL
    //          in any window always redirects straight to shop setup.
    // Pass 1: clickPreviewAndGetUrl() leaves the preview tab open. Switch to it directly —
    //         it already has a live Bubble.io session with demographics ready.
    // Pass 2: close the spent tab (sitting on shop setup after Not Interested), switch back
    //         to the editor, re-click the preview button to get a fresh session in a new tab.
    // Both passes use the CTR refresh pattern if demographics are slow to appear.
    @Step("Navigate to preview URL as logged-in user")
    public void navigateAsLoggedInUser(String previewUrl) {
        By demographics = By.cssSelector("[id^='answer-Option-']");
        if (editorWindowHandle == null) {
            editorWindowHandle = driver.getWindowHandle();
            String previewHandle = null;
            for (String h : driver.getWindowHandles()) {
                if (!h.equals(editorWindowHandle)) { previewHandle = h; break; }
            }
            if (previewHandle == null) throw new RuntimeException(
                "[D2C Logged-in] Preview tab not found — clickPreviewAndGetUrl() must leave it open");
            driver.switchTo().window(previewHandle);
        } else {
            driver.close();
            driver.switchTo().window(editorWindowHandle);
            retriggerPreviewFromEditor();
        }
        waitForLandmarkElseReload(demographics, 20, 2);
    }

    private void retriggerPreviewFromEditor() {
        By previewBtn = By.id("newproject_formquestions_previewjourney_button");
        By desktopBtn = By.id("preview-desktop");
        Set<String> before = driver.getWindowHandles();
        WebElement btn = new WebDriverWait(driver, 30).until(
            ExpectedConditions.elementToBeClickable(previewBtn));
        scrollToCenter(btn);
        jsClick(previewBtn);
        // The Desktop/Mobile chooser popup appears after clicking Preview and does NOT auto-open a
        // tab — Desktop must be selected first (mirrors FormQuestionsPage.openPreviewChooserAndSelectDesktop()).
        // Best-effort so it works whether or not the chooser is shown: poll briefly, click if present.
        try {
            WebElement desktop = new WebDriverWait(driver, 10).until(
                ExpectedConditions.elementToBeClickable(desktopBtn));
            scrollToCenter(desktop);
            jsClick(desktopBtn);
            System.out.println("[D2C Logged-in] Selected Desktop in preview chooser on re-trigger");
        } catch (Exception e) {
            System.out.println("[D2C Logged-in] Desktop chooser not shown after preview click — proceeding");
        }
        String newHandle = new WebDriverWait(driver, 30).until(d -> {
            for (String h : d.getWindowHandles()) {
                if (!before.contains(h)) return h;
            }
            return null;
        });
        driver.switchTo().window(newHandle);
    }

    // Clears the session then navigates to the preview URL as a guest.
    // Handles the Bubble.io infinite loading bug — if demographic options don't appear
    // within 15 s, refreshes once and waits up to 45 s more before failing.
    @Step("Clear session and navigate to preview URL as guest (with infinite loading retry)")
    public void navigateAsGuest(String previewUrl) {
        clearSession();
        driver.get("about:blank");
        driver.get(previewUrl);
        By anyDemographicOption = By.cssSelector("[id^='answer-Option-']");
        waitForLandmarkElseReload(anyDemographicOption, 20, 2);
    }
}

package com.aybee.pages;

import com.aybee.context.ProductSnapshot;
import io.qameta.allure.Step;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import com.aybee.utils.ScreenshotSoftAssert;
import org.testng.asserts.SoftAssert;

import java.util.List;

public class MarketplaceListPage extends BasePage {

    // Shared ID across all product name elements — requires findElements not findElement.
    private static final By PRODUCT_NAME_ELEMENTS =
        By.cssSelector("[id='results-page-product-name']");

    private final By helpConfirmButton   = By.id("help-confirm");
    private final By notInterestedButton = By.id("button-not-interest");

    // Dismisses the help intro popup if it appears within 5 s — optional popup.
    @Step("Dismiss help popup if present")
    public MarketplaceListPage dismissHelpPopupIfPresent() {
        try {
            new WebDriverWait(driver, 5).until(
                ExpectedConditions.visibilityOfElementLocated(helpConfirmButton));
            jsClick(helpConfirmButton);
        } catch (Exception ignored) {}
        return this;
    }

    @Step("Assert not-interested button is present on marketplace list")
    public MarketplaceListPage assertNotInterestedButtonPresent() {
        wait.until(ExpectedConditions.visibilityOfElementLocated(notInterestedButton));
        return this;
    }

    // Soft-asserts that no select-item-overview-organic- button has an empty name suffix.
    // An empty suffix indicates a ghost product from an incomplete shop setup save.
    @Step("Assert no marketplace products have an empty product name")
    public MarketplaceListPage assertNoEmptyProductIds() {
        // Wait for the real product cards to load first — Bubble shows an empty-name ghost card
        // before injecting the real ones, and checking too early flags that transient ghost as an
        // incomplete save. Once the real cards are in, a still-empty card is a genuine ghost.
        waitForRealProductCards();
        SoftAssert sa = new ScreenshotSoftAssert();
        List<WebElement> buttons = driver.findElements(
            By.cssSelector("[id^='select-item-overview-organic-']"));
        for (WebElement btn : buttons) {
            String name = btn.getAttribute("id").replace("select-item-overview-organic-", "");
            if (name.isEmpty()) {
                sa.fail("[Marketplace] Ghost product with empty name — shop setup saved an incomplete product");
            }
        }
        sa.assertAll();
        return this;
    }

    // Bubble injects an empty-name "ghost" card before the real product cards finish loading, and
    // that ghost satisfies a naive "any select-item-overview-organic-* present" wait. These helpers
    // let the marketplace steps wait for REAL (non-empty-suffix) cards and ignore the ghost.

    // Waits until at least one real card is present, lets late cards settle, and returns the real
    // (non-ghost) card names.
    private List<String> waitForRealProductCards() {
        new WebDriverWait(driver, 30).until(d -> hasRealProductCard(d));
        try { Thread.sleep(1500); } catch (InterruptedException ignored) {}
        List<String> names = new java.util.ArrayList<>();
        for (WebElement e : driver.findElements(
                By.cssSelector("[id^='select-item-overview-organic-']"))) {
            String s = e.getAttribute("id").replace("select-item-overview-organic-", "");
            if (s != null && !s.trim().isEmpty()) names.add(s);
        }
        return names;
    }

    private boolean hasRealProductCard(org.openqa.selenium.WebDriver d) {
        for (WebElement e : d.findElements(
                By.cssSelector("[id^='select-item-overview-organic-']"))) {
            String s = e.getAttribute("id").replace("select-item-overview-organic-", "");
            if (s != null && !s.trim().isEmpty()) return true;
        }
        return false;
    }

    // Polls until our product card (matching one of the given names by exact id or partial id
    // match) is present, ignoring the transient empty-name ghost. Throws on timeout.
    private WebElement waitForProductCard(String... names) {
        return new WebDriverWait(driver, 30).until(d -> {
            for (String n : names) {
                if (n == null || n.trim().isEmpty()) continue;
                WebElement b = findById("select-item-overview-organic-" + n);
                if (b == null) b = findSelectButtonByPartialId(n);
                if (b != null) {
                    String suffix = b.getAttribute("id")
                        .replace("select-item-overview-organic-", "");
                    if (!suffix.trim().isEmpty()) return b;
                }
            }
            return null;
        });
    }

    // Clicks Not Interested — participant should be filtered out and redirected to login.
    // Throws AssertionError (not TimeoutException) so the step def can catch it and
    // record a soft failure without stopping the scenario flow.
    @Step("Click Not Interested from marketplace list and verify redirect to login page")
    public MarketplaceListPage clickNotInterestedAndVerifyFilteredOut() {
        jsClick(notInterestedButton);
        try {
            new WebDriverWait(driver, 30).until(
                ExpectedConditions.presenceOfElementLocated(By.id("toggle-sign-in")));
        } catch (Exception e) {
            throw new AssertionError(
                "[Marketplace] Participant was not filtered out — toggle-sign-in did not appear after 30s");
        }
        return this;
    }

    // Clicks Not Interested from the marketplace list as the logged-in owner.
    // The owner is redirected back to the shop setup page; verifies by waiting for
    // the shop setup next button (marketplacesimulation_shopsetup_next_button).
    // 60 s timeout — redirect via Bubble.io SPA routing can take significantly longer
    // than a normal page transition, especially after the not-interested API call completes.
    @Step("Click Not Interested from marketplace list and verify redirect to shop setup page")
    public MarketplaceListPage clickNotInterestedAndVerifyShopSetupRedirect() {
        jsClick(notInterestedButton);
        try {
            new WebDriverWait(driver, 60).until(
                ExpectedConditions.presenceOfElementLocated(
                    By.id("marketplacesimulation_shopsetup_next_button")));
        } catch (Exception e) {
            throw new AssertionError(
                "[Marketplace] Shop setup page did not appear after clicking Not Interested — " +
                "marketplacesimulation_shopsetup_next_button not found within 60s");
        }
        return this;
    }

    // Returns the full product name text from results-page-product-name elements
    // whose text contains partialName. Strips trailing "..." or "…" that CSS overflow
    // truncation appends to getText() output so the result matches the actual ID attribute.
    @Step("Find full product name containing '{partialName}'")
    public String findFullProductName(String partialName) {
        List<WebElement> els = new WebDriverWait(driver, 30).until(
            ExpectedConditions.presenceOfAllElementsLocatedBy(PRODUCT_NAME_ELEMENTS));
        for (WebElement el : els) {
            String name = el.getText().trim().replaceAll("[.…]+$", "").trim();
            if (name.toLowerCase().contains(partialName.toLowerCase())) {
                return name;
            }
        }
        throw new RuntimeException("Product not found on marketplace list: " + partialName);
    }

    // Verifies that image, price, ratings, brand, and prime status on the marketplace
    // list match the values captured from shop setup. Fields captured as null on shop
    // setup are skipped with a log message rather than failing.
    // Uses the full product name from the select button ID — getText() is CSS-truncated
    // and would produce wrong IDs for all the data fields.
    // Soft-asserts all fields so every check runs even if one fails.
    // Caller should catch AssertionError to let the flow continue past this step.
    @Step("Assert marketplace product data matches shop setup snapshot")
    public MarketplaceListPage assertProductData(ProductSnapshot snap,
                                                 String scenarioAName, String scenarioBName) {
        // Wait for our real card (scenario A or B, whichever rendered), ignoring the empty-name
        // ghost — matching too early against the ghost-only DOM threw "select button not found".
        WebElement btn;
        try {
            btn = waitForProductCard(scenarioAName, scenarioBName);
        } catch (Exception e) {
            throw new RuntimeException("Product select button not found. Scenario A: ["
                + scenarioAName + "] Scenario B: [" + scenarioBName + "]");
        }
        String fullName = btn.getAttribute("id").replace("select-item-overview-organic-", "");

        SoftAssert sa = new ScreenshotSoftAssert();

        softAssertField(sa, "Image src", snap.imageSrc,
            imgSrcFromContainer(fullName + "-product-image"));

        softAssertField(sa, "Price", snap.price,
            normalizePrice(priceTextById(fullName + "-product-price")));

        softAssertField(sa, "Ratings", snap.ratings,
            extractNumber(textById(fullName + "-total-ratings")));

        softAssertField(sa, "Brand", snap.brand,
            textById(fullName + "-product-brand"));

        // Only assert prime when shop setup positively confirmed it — the shop setup list
        // card does not reliably render the prime badge, so snap.hasPrime == false may mean
        // "not prime" OR "badge didn't render". Asserting absence would produce false failures.
        if (snap.hasPrime) {
            boolean marketplaceHasPrime = isVisibleById(fullName + "-prime-status");
            sa.assertTrue(marketplaceHasPrime,
                "[Marketplace] Prime status mismatch for: " + fullName);
        }

        sa.assertAll();
        return this;
    }

    // Selects our product card by trying both scenario names (A or B, whichever actually
    // rendered) — no dependency on scenario detection, since only one variant is present at a
    // time and we simply pick whichever exists. Polls because the marketplace re-renders
    // asynchronously and our card may load after other products; falls back to partial matching
    // the same way assertProductData does.
    @Step("Select our product from marketplace list (scenario A or B, whichever is present)")
    public String selectOurProduct(String scenarioAName, String scenarioBName) {
        WebElement btn;
        try {
            btn = new WebDriverWait(driver, 30).until(d -> {
                WebElement b = findById("select-item-overview-organic-" + scenarioAName);
                if (b == null) b = findById("select-item-overview-organic-" + scenarioBName);
                if (b == null) b = findSelectButtonByPartialId(scenarioAName);
                if (b == null) b = findSelectButtonByPartialId(scenarioBName);
                return b;
            });
        } catch (Exception e) {
            throw new RuntimeException("Our product select button not found. Scenario A: ["
                + scenarioAName + "] Scenario B: [" + scenarioBName + "]");
        }
        String resolvedName = btn.getAttribute("id").replace("select-item-overview-organic-", "");

        // Bubble stacks a transparent overlay on top of the product card, so clicks are finicky.
        // Primary: scroll the card to centre, then force a JS click on it. If that doesn't
        // register the selection (the opener-question popup never appears), fall back to a real
        // mouse hover + click via Actions — that lands on whatever overlay sits on top exactly as
        // a user's click would, firing Bubble's select workflow.
        JavascriptExecutor js = (JavascriptExecutor) driver;
        js.executeScript("arguments[0].scrollIntoView({block:'center'});", btn);
        js.executeScript("arguments[0].click();", btn);

        if (!openerPopupAppeared(5)) {
            System.out.println("[Marketplace] JS click did not open the opener popup — "
                + "falling back to mouse hover + click for: " + resolvedName);
            new Actions(driver).moveToElement(btn).click().perform();
        }
        System.out.println("[Marketplace] Selected our product: " + resolvedName);
        return resolvedName;
    }

    // The opener-question popup (next-open-product button) appears only once a product is
    // actually selected — the reliable signal that a click landed on Bubble's select workflow.
    private boolean openerPopupAppeared(int timeoutSecs) {
        try {
            new WebDriverWait(driver, timeoutSecs).until(
                ExpectedConditions.presenceOfElementLocated(By.id("next-open-product")));
            return true;
        } catch (Exception notYet) {
            return false;
        }
    }

    // Selects our product using the exact full names captured from ASIN lookup during
    // shop setup — constructs the precise button ID without any partial matching.
    // Scenario A is tried first; whichever is present in the DOM is the active assignment.
    @Step("Select our product by scenario name (A or B whichever is visible)")
    public void selectOurProductByScenarioNames(String scenarioAName, String scenarioBName) {
        // Wait for at least one select button to be present — the marketplace re-renders
        // asynchronously after returning from product detail or cart, and findById without
        // a wait returns null on an unloaded DOM.
        new WebDriverWait(driver, 15).until(
            ExpectedConditions.presenceOfElementLocated(
                By.cssSelector("[id^='select-item-overview-organic-']")));
        WebElement btn = findById("select-item-overview-organic-" + scenarioAName);
        if (btn == null) btn = findById("select-item-overview-organic-" + scenarioBName);
        if (btn == null) {
            throw new RuntimeException(
                "Our product select button not found. Scenario A: ["
                + scenarioAName + "] Scenario B: [" + scenarioBName + "]");
        }
        String name = btn.getAttribute("id").replace("select-item-overview-organic-", "");
        scrollToCenter(btn);
        btn.click();
        System.out.println("[Marketplace] Selected our product: " + name);
    }

    // Collects all select buttons, identifies which one is ours (Scenario A or B),
    // then JS-clicks the first non-our, non-ghost button.
    // Polls until at least one such button exists — the marketplace re-renders
    // asynchronously after returning from cart, and ghost elements (empty suffix)
    // appear before real product cards are injected by Bubble.io.
    @Step("Select a competitor product from marketplace list")
    public String selectCompetitorProduct(String scenarioAName, String scenarioBName) {
        String ourIdA = "select-item-overview-organic-" + scenarioAName;
        String ourIdB = "select-item-overview-organic-" + scenarioBName;

        // Poll until a real competitor button (non-empty suffix, not our product) is present.
        String competitorId = new WebDriverWait(driver, 30).until(d -> {
            List<WebElement> all = d.findElements(
                By.cssSelector("[id^='select-item-overview-organic-']"));
            for (WebElement b : all) {
                String id = b.getAttribute("id");
                String suffix = id.replace("select-item-overview-organic-", "");
                if (suffix.isEmpty()) continue;
                if (id.equals(ourIdA) || id.equals(ourIdB)) continue;
                return id;
            }
            return null;
        });

        // Use findById (document.getElementById) — same as selectOurProduct — to get the
        // exact clickable element rather than the outer wrapper returned by CSS selector.
        WebElement btn = new WebDriverWait(driver, 10).until(d -> findById(competitorId));
        String name = competitorId.replace("select-item-overview-organic-", "");
        scrollToCenter(btn);
        btn.click();
        System.out.println("[Marketplace] Selected competitor: " + name);
        return name;
    }

    // Detects which scenario the current participant is in by reading the full product name
    // from select-item-overview-organic-* button IDs — getText() is CSS-truncated and
    // would never match the full stored name. Only one version of our product appears
    // at a time. Returns "A", "B", or "unknown".
    @Step("Detect which scenario (A or B) the current participant is assigned to")
    public String detectCurrentScenario(String scenarioAName, String scenarioBName) {
        // Wait for the real cards — reading names off the ghost-only DOM always yields "unknown".
        List<String> names = waitForRealProductCards();
        for (String fullName : names) {
            if (fullName.equalsIgnoreCase(scenarioAName)) return "A";
            if (fullName.equalsIgnoreCase(scenarioBName)) return "B";
        }
        System.out.println("[Marketplace] Scenario not detected. Scenario A: [" + scenarioAName
            + "] Scenario B: [" + scenarioBName + "]");
        System.out.println("[Marketplace] Real product card names found:");
        for (String n : names) System.out.println("  " + n);
        return "unknown";
    }

    // Scans all select-item-overview-organic-* buttons and returns the best match for
    // partialName against the product-name portion of each ID (case-insensitive).
    // Priority: (1) exact match, (2) product name starts with term, (3) contains term.
    // This ensures products that share a common prefix but differ at the end are
    // correctly distinguished — a longer, more specific search term always wins.
    private WebElement findSelectButtonByPartialId(String partialName) {
        Object result = ((JavascriptExecutor) driver).executeScript(
            "var els = document.querySelectorAll('[id^=\"select-item-overview-organic-\"]');" +
            "var prefix = 'select-item-overview-organic-';" +
            "var term = arguments[0].toLowerCase();" +
            "var exact = null, startsWith = null, contains = null;" +
            "for (var i = 0; i < els.length; i++) {" +
            "  var name = els[i].id.substring(prefix.length).toLowerCase();" +
            "  if (name === term)                          { exact      = els[i]; break; }" +
            "  if (name.indexOf(term) === 0 && !startsWith){ startsWith = els[i]; }" +
            "  else if (name.indexOf(term) >= 0 && !contains){ contains = els[i]; }" +
            "}" +
            "return exact || startsWith || contains || null;",
            partialName);
        return (result instanceof WebElement) ? (WebElement) result : null;
    }

    private void softAssertField(SoftAssert sa, String label, String expected, String actual) {
        if (expected == null || expected.isEmpty()) {
            System.out.println("[Marketplace] Skipping " + label + " — shop setup value was null");
            return;
        }
        if (actual == null || actual.isEmpty()) {
            System.out.println("[Marketplace] Skipping " + label + " — marketplace value was null");
            return;
        }
        sa.assertEquals(actual, expected, "[Marketplace] " + label + " mismatch");
    }
}

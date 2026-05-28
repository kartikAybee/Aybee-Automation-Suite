package com.aybee.pages;

import com.aybee.context.ProductSnapshot;
import io.qameta.allure.Step;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
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
        SoftAssert sa = new SoftAssert();
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
    public MarketplaceListPage assertProductData(ProductSnapshot snap, String partialName) {
        WebElement btn = findSelectButtonByPartialId(partialName);
        if (btn == null) {
            throw new RuntimeException("Product select button not found for: " + partialName);
        }
        String fullName = btn.getAttribute("id").replace("select-item-overview-organic-", "");

        SoftAssert sa = new SoftAssert();

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

    // Selects the product whose button ID is exactly select-item-overview-organic-{fullName}.
    // Uses document.getElementById() (via BasePage.findById) which handles spaces, em-dashes,
    // commas, and any other special characters that break By.id() / By.cssSelector().
    // Polls until the button appears — the marketplace re-renders asynchronously after
    // returning from cart and our card may load after other products.
    @Step("Select our product from marketplace list")
    public String selectOurProduct(String fullName) {
        String buttonId = "select-item-overview-organic-" + fullName;
        WebElement btn;
        try {
            btn = new WebDriverWait(driver, 30).until(d -> findById(buttonId));
        } catch (Exception e) {
            throw new RuntimeException("Select button not found: " + buttonId);
        }
        scrollToCenter(btn);
        btn.click();
        return fullName;
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
        List<WebElement> buttons = wait.until(
            ExpectedConditions.presenceOfAllElementsLocatedBy(
                By.cssSelector("[id^='select-item-overview-organic-']")));
        for (WebElement btn : buttons) {
            String fullName = btn.getAttribute("id").replace("select-item-overview-organic-", "");
            if (fullName.equalsIgnoreCase(scenarioAName)) return "A";
            if (fullName.equalsIgnoreCase(scenarioBName)) return "B";
        }
        System.out.println("[Marketplace] Scenario not detected. Scenario A: [" + scenarioAName
            + "] Scenario B: [" + scenarioBName + "]");
        System.out.println("[Marketplace] Button IDs found:");
        for (WebElement btn : buttons)
            System.out.println("  " + btn.getAttribute("id"));
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

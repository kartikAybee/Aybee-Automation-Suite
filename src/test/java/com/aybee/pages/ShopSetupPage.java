package com.aybee.pages;

import com.aybee.context.ProductSnapshot;
import io.qameta.allure.Step;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.FluentWait;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.util.List;
import java.util.concurrent.TimeUnit;

public class ShopSetupPage extends BasePage {

    private final By addNewProductButton      = By.id("marketplacesimulation_shopsetup_addnewproduct_button");
    private final By addViaSearchMenuItem     = By.id("btn-add-via-search-amazon-menu");
    // No stable ID for the Auto-fill with Competitors tab — use the third group in its container.
    private final By autoFillCompetitorsTab   = By.xpath("(//div[@class=\"clickable-element bubble-element Group coaQaCaR bubble-r-container flex column\"])[3]");
    private final By countryUsaButton         = By.id("btn-country-asin-United States");
    private final By asinInputField           = By.id("marketplacesimulation_shopsetup_asin_element");
    // Clicking asin-title-text fires blur on the ASIN input, triggering Bubble.io's reactive
    // validation and transitioning the search button from ghost → enabled.
    private final By asinTitleText            = By.id("asin-title-text");
    private final By asinSearchButton         = By.id("marketplacesimulation_shopsetup_asinsearch_addproductstoshop_button");
    private final By addVariationButton  = By.id("marketplacesimulation_shopsetup_addvariation_button");
    private final By productNameField    = By.id("edit_product_name_scenario_Scenario B");
    private final By productPriceField   = By.id("edit_product_price_scenario_Scenario B");
    private final By saveChangesButton   = By.id("marketplacesimulation_shopsetup_addscenario_savechanges_button");
    private final By deleteMainPicButton = By.id("delete-main-picture-scenario-Scenario B");
    private final By nextStageButton     = By.id("marketplacesimulation_shopsetup_next_button");

    @Step("Click Add New Product")
    public ShopSetupPage clickAddNewProduct() {
        wait.until(ExpectedConditions.elementToBeClickable(addNewProductButton));
        jsClick(addNewProductButton);
        wait.until(ExpectedConditions.visibilityOfElementLocated(addViaSearchMenuItem));
        return this;
    }

    @Step("Select Add via Search, choose Auto-fill with Competitors tab, select USA")
    public ShopSetupPage clickAddViaAsin() {
        jsClick(addViaSearchMenuItem);
        // Auto-fill with Competitors tab — no stable ID, located by XPath.
        wait.until(ExpectedConditions.visibilityOfElementLocated(autoFillCompetitorsTab));
        jsClick(autoFillCompetitorsTab);
        // Select United States as the search country.
        wait.until(ExpectedConditions.visibilityOfElementLocated(countryUsaButton));
        jsClick(countryUsaButton);
        // ASIN input becoming visible confirms the form is ready for input.
        wait.until(ExpectedConditions.visibilityOfElementLocated(asinInputField));
        return this;
    }

    @Step("Enter ASIN: {asin}")
    public ShopSetupPage enterAsin(String asin) {
        // By.id targets the <input> element directly — no child lookup needed.
        WebElement input = wait.until(ExpectedConditions.presenceOfElementLocated(asinInputField));
        input.clear();
        input.sendKeys(asin);
        return this;
    }

    @Step("Click ASIN title text to enable search button")
    public ShopSetupPage clickAsinTitleToEnableGo() {
        jsClick(asinTitleText);
        return this;
    }

    @Step("Click Search and wait for products to load")
    public ShopSetupPage clickGoAndWaitForProducts() {
        clickWhenEnabled(asinSearchButton);
        // 10s fixed sleep lets Bubble.io finish the Amazon product data fetch and fully
        // render all product cards before we start polling for element states.
        try { Thread.sleep(10_000); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        // addVariationButton becoming clickable confirms the ASIN lookup completed.
        new WebDriverWait(driver, 60)
                .until(ExpectedConditions.elementToBeClickable(addVariationButton));
        // Explicitly confirm Scenario A's product card image has loaded using the
        // {productName}-image element ID — proves the card data is fully rendered
        // before any further interactions with the shop setup list.
        waitForScenarioAImageLoaded();
        return this;
    }

    // Waits until every [id$='-image'] container on the page has an img with a non-empty src.
    // All containers are required to load — none are treated as ghosts or skipped.
    private void waitForScenarioAImageLoaded() {
        new WebDriverWait(driver, 60).until(d -> {
            List<WebElement> containers = d.findElements(By.cssSelector("[id$='-image']"));
            if (containers.isEmpty()) return false;
            for (WebElement container : containers) {
                List<WebElement> imgs = container.findElements(By.tagName("img"));
                if (imgs.isEmpty()) return false;
                String src = imgs.get(0).getAttribute("src");
                if (src == null || src.trim().isEmpty()) return false;
            }
            System.out.println("[ShopSetup] All product card images loaded");
            return true;
        });
    }

    @Step("Click Add Variation to create second scenario")
    public ShopSetupPage clickAddVariation() {
        WebElement btn = new WebDriverWait(driver, 30)
                .until(ExpectedConditions.elementToBeClickable(addVariationButton));
        ((JavascriptExecutor) driver).executeScript(
                "arguments[0].scrollIntoView({behavior:'instant',block:'center'});", btn);
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", btn);
        // Brief pause for Bubble.io to create the Scenario B record server-side before
        // the popup fields are rendered.
        try { Thread.sleep(3_000); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        new WebDriverWait(driver, 30)
                .until(ExpectedConditions.visibilityOfElementLocated(productNameField));
        return this;
    }

    @Step("Delete main picture of second scenario")
    public ShopSetupPage deleteMainPicture() {
        jsClick(deleteMainPicButton);
        return this;
    }

    // CTR-specific: deleting the first picture triggers a Bubble.io bug that clears all popup
    // fields. Re-enter the exact Scenario A name and price so both scenarios are identical
    // except for the image — the visual element being optimised in CTR tests.
    @Step("Restore Scenario A name after picture delete (CTR — only image differs)")
    public ShopSetupPage restoreScenarioAFields(ProductSnapshot scenarioA) {
        WebElement nameInput = new WebDriverWait(driver, 30)
                .until(ExpectedConditions.visibilityOfElementLocated(productNameField));
        ((JavascriptExecutor) driver).executeScript(
                "arguments[0].scrollIntoView({behavior:'instant',block:'center'});", nameInput);
        nameInput.clear();
        nameInput.sendKeys(scenarioA.truncatedName);
        final String expectedName = scenarioA.truncatedName;
        try {
            new WebDriverWait(driver, 5).until(d -> {
                String v = d.findElement(productNameField).getAttribute("value");
                return expectedName.equals(v);
            });
        } catch (Exception e) {
            System.out.println("[ShopSetup CTR] Name did not persist — re-typing: " + expectedName);
            WebElement retry = driver.findElement(productNameField);
            retry.clear();
            retry.sendKeys(expectedName);
        }
        capturedScenarioAName = scenarioA.truncatedName;
        capturedScenarioBName = scenarioA.truncatedName;
        return this;
    }

    // The original name before trimming (Scenario A) and the trimmed name (Scenario B)
    // are stored here during removeFirstWordFromProductName() so ShopSetupSteps can
    // retrieve them and put them in ScenarioContext for marketplace competitor detection.
    private String capturedScenarioAName;
    private String capturedScenarioBName;

    public String getCapturedScenarioAName() { return capturedScenarioAName; }
    public String getCapturedScenarioBName() { return capturedScenarioBName; }

    // Removes the first word and the space that follows it from the fetched product name.
    // Also captures both the original name (Scenario A) and the trimmed name (Scenario B)
    // so callers can store them for later use in competitor detection.
    @Step("Remove first word from product name and capture both scenario names")
    public ShopSetupPage removeFirstWordFromProductName() {
        WebElement input = new WebDriverWait(driver, 30)
                .until(ExpectedConditions.visibilityOfElementLocated(productNameField));
        ((JavascriptExecutor) driver).executeScript(
                "arguments[0].scrollIntoView({behavior: 'instant', block: 'center'});", input);
        // Scenario A name must be populated before we read it — waitForScenarioAPopupReady()
        // already confirmed this, but re-check with a short wait in case a reactive event
        // cleared the field between popup-ready and this call (e.g., after deleteMainPicture).
        new WebDriverWait(driver, 30).until(d -> {
            String v = d.findElement(productNameField).getAttribute("value");
            return v != null && !v.trim().isEmpty();
        });
        input = driver.findElement(productNameField);
        input.click();
        capturedScenarioAName = input.getAttribute("value");
        int firstSpace = capturedScenarioAName.indexOf(' ');
        capturedScenarioBName = firstSpace >= 0
                ? capturedScenarioAName.substring(firstSpace + 1)
                : capturedScenarioAName;
        input.clear();
        input.sendKeys(capturedScenarioBName);

        // Verify the typed name persisted — Bubble.io reactive events can reset the field
        // immediately after sendKeys. Re-send if the value doesn't match within 5 s.
        final String expectedName = capturedScenarioBName;
        try {
            new WebDriverWait(driver, 5).until(d -> {
                String v = d.findElement(productNameField).getAttribute("value");
                return expectedName.equals(v);
            });
        } catch (Exception e) {
            System.out.println("[ShopSetup] Scenario B name did not persist after sendKeys — re-typing: " + expectedName);
            WebElement retryInput = driver.findElement(productNameField);
            retryInput.clear();
            retryInput.sendKeys(expectedName);
        }
        return this;
    }

    @Step("Set product variant price to {price}")
    public ShopSetupPage setProductPrice(String price) {
        WebElement input = wait.until(ExpectedConditions.presenceOfElementLocated(productPriceField));
        // Triple-click selects all existing content before typing — clear() alone can leave
        // residual content in Bubble.io number fields, causing the price to be entered
        // incorrectly (e.g. 89.99 becoming 890.99).
        input.click();
        input.click();
        input.click();
        input.sendKeys(price);
        return this;
    }

    // Returns the src of the main picture currently shown in the Scenario B popup.
    // Call BEFORE deleteMainPicture() to capture Scenario A's image, or AFTER to capture
    // Scenario B's image (falls back to ASIN default once the custom picture is deleted).
    @Step("Capture main picture src from Scenario B popup element")
    public String captureImageFromPopupElement() {
        String src = imgSrcFromContainer("main-picture-scenario-Scenario B");
        System.out.println("[ShopSetup] Captured popup image src: " + src);
        return src;
    }

    // Captures Scenario B's snapshot from the popup fields after all edits are applied.
    // Ratings, brand, and hasPrime are not editable in the popup — inherited from Scenario A.
    @Step("Capture Scenario B snapshot from popup fields before saving")
    public ProductSnapshot captureScenarioBFromPopup(ProductSnapshot scenarioASnap) {
        WebElement nameInput  = wait.until(ExpectedConditions.visibilityOfElementLocated(productNameField));
        WebElement priceInput = wait.until(ExpectedConditions.presenceOfElementLocated(productPriceField));
        ProductSnapshot snap = new ProductSnapshot();
        snap.truncatedName = nameInput.getAttribute("value");
        snap.price         = normalizePrice(priceInput.getAttribute("value"));
        snap.imageSrc      = captureImageFromPopupElement();
        snap.ratings       = scenarioASnap.ratings;
        snap.brand         = scenarioASnap.brand;
        snap.hasPrime      = scenarioASnap.hasPrime;
        System.out.println("[ShopSetup] Captured Scenario B from popup: name=" + snap.truncatedName
            + " price=" + snap.price + " ratings=" + snap.ratings + " brand=" + snap.brand);
        return snap;
    }

    @Step("Save scenario changes")
    public ShopSetupPage saveChanges() {
        // Guard: Bubble.io reactive events (picture deletion, price entry) can reset the
        // name field. Blur first to flush any in-flight reactive events, then verify and
        // re-type up to 3 times before proceeding to the save click.
        if (capturedScenarioBName != null && !capturedScenarioBName.isEmpty()) {
            blurActiveElement();
            try { Thread.sleep(500); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
            try {
                for (int attempt = 1; attempt <= 3; attempt++) {
                    String currentName = driver.findElement(productNameField).getAttribute("value");
                    if (currentName != null && currentName.trim().equals(capturedScenarioBName)) {
                        System.out.println("[ShopSetup] Scenario B name confirmed before save: " + currentName);
                        break;
                    }
                    System.out.println("[ShopSetup] Scenario B name missing before save (attempt "
                        + attempt + ") — re-typing: " + capturedScenarioBName);
                    WebElement nameInput = wait.until(
                        ExpectedConditions.visibilityOfElementLocated(productNameField));
                    nameInput.clear();
                    nameInput.sendKeys(capturedScenarioBName);
                    blurActiveElement();
                    try { Thread.sleep(800); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
                }
            } catch (Exception e) {
                System.out.println("[ShopSetup] Could not verify/restore Scenario B name before save: "
                    + e.getMessage());
            }
        }

        // This button does not follow the ghost→filled CSS pattern used by other submit buttons,
        // so clickWhenEnabled may time out. Fall back to jsClick in that case.
        try {
            clickWhenEnabled(saveChangesButton);
        } catch (Exception e) {
            jsClick(saveChangesButton);
        }
        // Wait for the Scenario B edit popup to close before returning — the popup field
        // disappearing confirms Bubble.io finished processing the save before the caller
        // tries to locate the newly-created Scenario B card in the shop setup list.
        new WebDriverWait(driver, 15).until(
            ExpectedConditions.invisibilityOfElementLocated(productNameField));
        // Give Bubble.io time to write the new card's dynamic ID after the popup closes.
        try { Thread.sleep(3_000); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        return this;
    }

    // Waits for the popup to be fully populated from the ASIN fetch — both name and price
    // must be non-empty before the caller proceeds with edits. Called right after
    // clickAddVariation() opens the popup. Returns immediately once both fields have data;
    // does NOT capture a snapshot since list-card capture (after saving) is more complete.
    @Step("Wait for Scenario A ASIN data to populate popup fields")
    public void waitForScenarioAPopupReady() {
        // Wait for the name field — first field Bubble.io populates after the ASIN fetch.
        new WebDriverWait(driver, 30).until(d -> {
            String v = d.findElement(productNameField).getAttribute("value");
            return v != null && !v.trim().isEmpty();
        });
        System.out.println("[ShopSetup] Scenario A name populated: "
            + driver.findElement(productNameField).getAttribute("value"));
        // Wait for the price field — confirms the full ASIN product record loaded, not just
        // the title. Without this, removeFirstWordFromProductName may read an incomplete state.
        new WebDriverWait(driver, 30).until(d -> {
            String v = d.findElement(productPriceField).getAttribute("value");
            return v != null && !v.trim().isEmpty();
        });
        System.out.println("[ShopSetup] Scenario A price populated: "
            + driver.findElement(productPriceField).getAttribute("value"));
    }

    // Kept for backward compatibility — delegates to the new focused wait.
    @Step("Capture Scenario A snapshot from popup fields before edits")
    public ProductSnapshot captureScenarioAFromPopup() {
        waitForScenarioAPopupReady();
        WebElement nameInput  = driver.findElement(productNameField);
        WebElement priceInput = driver.findElement(productPriceField);
        ProductSnapshot snap = new ProductSnapshot();
        snap.truncatedName = nameInput.getAttribute("value");
        snap.price         = normalizePrice(priceInput.getAttribute("value"));
        snap.imageSrc      = captureImageFromPopupElement();
        System.out.println("[ShopSetup] Captured Scenario A from popup: name=" + snap.truncatedName
            + " price=" + snap.price);
        return snap;
    }

    // Looks up the Scenario B product card by waiting for a card whose ID starts with the
    // first 30 chars of exactName. Using a prefix rather than the full name because Bubble.io
    // may normalize or truncate the name when writing the card ID, making full-string
    // getElementById unreliable. 30 chars is unique across all competitor products.
    // saveChanges() closes the popup but Bubble.io renders the new card asynchronously,
    // so we wait up to 60 s for it to appear.
    @Step("Capture product snapshot for scenario with exact name '{exactName}'")
    public ProductSnapshot captureProductSnapshotByExactName(String exactName) {
        final String prefix = exactName.substring(0, Math.min(30, exactName.length())).toLowerCase();
        try {
            new WebDriverWait(driver, 60).until(d -> {
                List<WebElement> els = d.findElements(By.cssSelector("[id^='shop-setup-product-']"));
                return els.stream().anyMatch(e -> {
                    String suffix = e.getAttribute("id").replace("shop-setup-product-", "");
                    return !suffix.isEmpty() && suffix.toLowerCase().startsWith(prefix);
                });
            });
        } catch (Exception e) {
            logAvailableProductIds();
            throw new RuntimeException("Scenario B card not found in shop setup for: " + exactName);
        }
        List<WebElement> els = driver.findElements(By.cssSelector("[id^='shop-setup-product-']"));
        for (WebElement el : els) {
            String suffix = el.getAttribute("id").replace("shop-setup-product-", "");
            if (!suffix.isEmpty() && suffix.toLowerCase().startsWith(prefix)) {
                return buildSnap(suffix);
            }
        }
        logAvailableProductIds();
        throw new RuntimeException("No product found in shop setup for: " + exactName);
    }

    private void waitForProductCards() {
        new WebDriverWait(driver, 30).until((org.openqa.selenium.support.ui.ExpectedCondition<Boolean>) d -> {
            List<WebElement> els = d.findElements(By.cssSelector("[id^='shop-setup-product-']"));
            return els.stream().anyMatch(e -> {
                String id = e.getAttribute("id");
                return id != null && id.length() > "shop-setup-product-".length();
            });
        });
    }

    // Waits until every present shop-setup-product-* card has a loaded image src.
    // Image load is the last async event Bubble.io fires after the ASIN/variation data
    // fetch completes, so a non-empty src on every card = all data is ready to read.
    @Step("Wait for all shop-setup product card images to load")
    public void waitForAllProductCardsLoaded() {
        new FluentWait<>(driver)
            .withTimeout(60, TimeUnit.SECONDS)
            .pollingEvery(1, TimeUnit.SECONDS)
            .ignoring(Exception.class)
            .until(d -> {
                List<WebElement> cards = d.findElements(
                    By.cssSelector("[id^='shop-setup-product-']"));
                if (cards.isEmpty()) return false;
                for (WebElement card : cards) {
                    String suffix = card.getAttribute("id").replace("shop-setup-product-", "");
                    if (suffix.isEmpty()) continue;
                    List<WebElement> containers = d.findElements(By.id(suffix + "-image"));
                    if (containers.isEmpty()) return false;
                    List<WebElement> imgs = containers.get(0).findElements(By.tagName("img"));
                    if (imgs.isEmpty()) return false;
                    String src = imgs.get(0).getAttribute("src");
                    if (src == null || src.trim().isEmpty()) return false;
                }
                return true;
            });
        System.out.println("[ShopSetup] All product card images loaded");
        // Extra settle time after confirming image load — Bubble.io may still be writing
        // dynamic IDs to newly-created product cards a moment after the images appear.
        try { Thread.sleep(3_000); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }

    private ProductSnapshot buildSnap(String truncated) {
        ProductSnapshot snap = new ProductSnapshot();
        snap.truncatedName = truncated;
        snap.imageSrc = imgSrcFromContainer(truncated + "-image");
        if (snap.imageSrc == null) System.out.println("[ShopSetup] Image src null for: " + truncated);
        snap.price = normalizePrice(textById(truncated + "-price"));
        if (snap.price == null) System.out.println("[ShopSetup] Price null for: " + truncated);
        snap.ratings = extractNumber(textById(truncated + "-total-ratings"));
        if (snap.ratings == null) System.out.println("[ShopSetup] Ratings null for: " + truncated);
        snap.brand = textById(truncated + "-brand");
        if (snap.brand == null) System.out.println("[ShopSetup] Brand null for: " + truncated);
        snap.hasPrime = isVisibleById(truncated + "-prime-status");
        System.out.println("[ShopSetup] Captured snapshot: name=" + truncated
            + " price=" + snap.price + " ratings=" + snap.ratings
            + " brand=" + snap.brand + " prime=" + snap.hasPrime);
        return snap;
    }

    private void logAvailableProductIds() {
        List<WebElement> all = driver.findElements(By.cssSelector("[id^='shop-setup-product-']"));
        System.out.println("[ShopSetup] Available product IDs (" + all.size() + "):");
        for (WebElement e : all) System.out.println("  " + e.getAttribute("id"));
    }

    // Scrolls btn-next-project into the viewport before clicking — the button may be below
    // the fold after products and variations are added.
    @Step("Scroll to Next button and proceed to form questions stage")
    public void proceedToFormQuestions() {
        WebElement nextBtn = wait.until(ExpectedConditions.presenceOfElementLocated(nextStageButton));
        ((JavascriptExecutor) driver).executeScript(
                "arguments[0].scrollIntoView({behavior: 'instant', block: 'center'});", nextBtn);
        clickWhenEnabled(nextStageButton);
    }
}

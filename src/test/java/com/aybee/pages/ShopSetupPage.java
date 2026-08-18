package com.aybee.pages;

import com.aybee.context.ProductSnapshot;
import io.qameta.allure.Step;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.util.List;

// Shop Setup — step 1 of the 3-step PDP Simulation flow.
//
// Scenarios are addressed by 1-based index: A=1, B=2, C=3, ... The platform builds element IDs
// from either the index (e.g. "2-edit-product-shop-setup") or the scenario letter
// (e.g. "trigger-delete-Scenario C"). Both forms are handled here via scenarioLetter().
public class ShopSetupPage extends BasePage {

    private final By addNewProductButton = By.id("marketplacesimulation_shopsetup_addnewproduct_button");
    private final By asinInputField      = By.id("marketplacesimulation_shopsetup_asin_element");
    // Clicking asin-title-text fires blur on the ASIN input, triggering Bubble.io's reactive
    // validation and transitioning the Go button from ghost → enabled.
    private final By asinTitleText       = By.id("asin-title-text");
    private final By asinGoButton        = By.id("marketplacesimulation_shopsetup_asin_go_button");
    private final By saveChangesButton   = By.id("marketplacesimulation_shopsetup_addscenario_savechanges_button");
    private final By nextStageButton     = By.id("marketplacesimulation_shopsetup_next_button");
    // Close button of the manual add/edit product popup (which opens when a scenario is added/opened).
    private final By closeManualEditBtn  = By.id("btn_close_add_edit_manually");

    // Maps a 1-based scenario index to its letter: 1→A, 2→B, 3→C, ...
    private static String scenarioLetter(int index) {
        return String.valueOf((char) ('A' + index - 1));
    }

    // Attribute-selector locator: safe for IDs that contain spaces ("trigger-delete-Scenario C")
    // or start with a digit ("2-edit-product-shop-setup"), both of which break the CSS "#id" form.
    private static By byId(String id) {
        return By.cssSelector("[id='" + id + "']");
    }


    // ── Scroll helpers ──────────────────────────────────────────────────────────
    // Shop-setup elements — especially fields inside the edit-product popup — can render below the
    // fold. Every interaction scrolls its target into the centre of the viewport first.
    private WebElement scrollTo(By locator) {
        WebElement el = new WebDriverWait(driver, 30)
                .until(ExpectedConditions.presenceOfElementLocated(locator));
        ((JavascriptExecutor) driver).executeScript(
                "arguments[0].scrollIntoView({behavior:'instant',block:'center'});", el);
        return el;
    }

    // Scroll-into-view then JS-click. Does NOT delegate to BasePage.jsClick (so a project-wide
    // find/replace of jsClick -> jsClickScrolled can't recurse).
    private void jsClickScrolled(By locator) {
        WebElement el = scrollTo(locator);
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", el);
    }


    // The Add New Product button becoming clickable is the landmark that confirms the Shop Setup
    // step has fully loaded after the experiment is created. Waits up to 45 s to cover the
    // server-side setup that runs after country selection.
    @Step("Verify Shop Setup step loaded (Add New Product button clickable)")
    public boolean isAddNewProductButtonClickable() {
        try {
            new WebDriverWait(driver, 45)
                    .until(ExpectedConditions.elementToBeClickable(addNewProductButton));
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    // ── ASIN product entry ────────────────────────────────────────────────────

    @Step("Add product to Scenario {scenarioIndex} via ASIN: {asin}")
    public ShopSetupPage addProductViaAsin(int scenarioIndex, String asin) {
        By addAsinBtn = By.id("pdp-add-asin-btn-" + scenarioIndex);
        // Open the Add-via-ASIN input for this scenario.
        new WebDriverWait(driver, 30).until(ExpectedConditions.elementToBeClickable(addAsinBtn));
        jsClickScrolled(addAsinBtn);
        // Enter the ASIN.
        WebElement input = wait.until(ExpectedConditions.visibilityOfElementLocated(asinInputField));
        input.clear();
        input.sendKeys(asin);
        // Click the ASIN title text to blur the input so Bubble.io enables the Go button.
        jsClickScrolled(asinTitleText);
        // Wait for the Go button to become enabled/clickable, then click it.
        scrollTo(asinGoButton);
        try {
            clickWhenEnabled(asinGoButton, 30);
        } catch (Exception e) {
            // Fallback if this button doesn't follow the ghost→filled colour pattern.
            new WebDriverWait(driver, 30).until(ExpectedConditions.elementToBeClickable(asinGoButton));
            jsClickScrolled(asinGoButton);
        }
        // Confirm the product card image actually loaded. The container ID is "{full product name}-image"
        // (product can be anything) and "loaded" means its inner <img> has a real (non-empty) src.
        waitForAllProductImagesLoaded();
        return this;
    }

    // Reliable "product fully loaded" gate. Two conditions must hold:
    //   1. At least one product card is rendered — id "shop-setup-product-{dynamic product name}".
    //   2. EVERY [id$='-image'] container's <img> has resolved to a real https URL. The src starts
    //      empty/placeholder and the actual source appears late, so we require "https" specifically
    //      rather than merely non-empty.
    private void waitForAllProductImagesLoaded() {
        new WebDriverWait(driver, 60).until(d -> {
            // 1. Product card (with dynamic name) present.
            if (d.findElements(By.cssSelector("[id^='shop-setup-product-']")).isEmpty()) return false;
            // 2. Every product image resolved to an https src.
            List<WebElement> containers = d.findElements(By.cssSelector("[id$='-image']"));
            if (containers.isEmpty()) return false;
            for (WebElement c : containers) {
                List<WebElement> imgs = c.findElements(By.tagName("img"));
                if (imgs.isEmpty()) return false;
                String src = imgs.get(0).getAttribute("src");
                if (src == null || !src.contains("https")) return false;
            }
            System.out.println("[ShopSetup] All product cards + images loaded (cards="
                + d.findElements(By.cssSelector("[id^='shop-setup-product-']")).size()
                + ", images=" + containers.size() + ")");
            return true;
        });
    }

    // ── Scenario management ─────────────────────────────────────────────────────

    // Clicks Add New Product and waits for the new scenario's edit button ("{index}-edit-product-shop-setup")
    // to appear. Returns true if it appeared within the timeout.
    @Step("Add a scenario and verify scenario {newIndex} appears")
    public boolean addScenarioAndWaitForEdit(int newIndex) {
        // Before adding another scenario, wait for the existing product image(s) to finish loading
        // (the reliable "product loaded" signal), then for the Add New Product button to be clickable.
        waitForAllProductImagesLoaded();
        new WebDriverWait(driver, 30)
                .until(ExpectedConditions.elementToBeClickable(addNewProductButton));
        jsClickScrolled(addNewProductButton);
        try {
            new WebDriverWait(driver, 30).until(
                    ExpectedConditions.visibilityOfElementLocated(byId(newIndex + "-edit-product-shop-setup")));
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    // Deletes an entire scenario: click its delete trigger, then confirm on the small confirm button,
    // then wait for the confirm dialog to close. Adding a scenario does NOT open a popup, so no
    // popup handling is needed here.
    @Step("Delete scenario {index}")
    public ShopSetupPage deleteScenario(int index) {
        String letter = scenarioLetter(index);
        // Wait for the scenario's product image to finish loading on the shop-setup page before
        // deleting — the image src resolves late (starts empty → https), and deleting while the card
        // is still settling can leave the delete/confirm controls unresponsive. This gate requires
        // every product card's <img> to have an https src, which includes this scenario's image.
        waitForAllProductImagesLoaded();
        jsClickScrolled(byId("delete-scenario-pdp-Scenario " + letter));
        By confirm = byId("delete-scenario-small-Scenario " + letter);
        new WebDriverWait(driver, 15).until(ExpectedConditions.elementToBeClickable(confirm));
        jsClickScrolled(confirm);
        // Wait for the confirm dialog to close (deletion committed). Bubble keeps ids in the DOM, so
        // check DISPLAY state, not presence.
        new WebDriverWait(driver, 15).until(d -> {
            List<WebElement> els = d.findElements(confirm);
            if (els.isEmpty()) return true;
            try { return !els.get(0).isDisplayed(); } catch (Exception e) { return true; }
        });
        System.out.println("[ShopSetup] Scenario " + letter + " deleted");
        return this;
    }

    // ── Scenario editing ────────────────────────────────────────────────────────

    @Step("Open scenario {index} for editing")
    public ShopSetupPage openScenarioForEdit(int index) {
        jsClickScrolled(byId(index + "-edit-product-shop-setup"));
        // The name field becoming visible confirms the edit popup opened.
        new WebDriverWait(driver, 30).until(ExpectedConditions.visibilityOfElementLocated(
                byId("edit_product_name_scenario_Scenario " + scenarioLetter(index))));
        return this;
    }

    @Step("Delete main picture of scenario {index}")
    public ShopSetupPage deleteMainPicture(int index) {
        jsClickScrolled(byId("delete-main-picture-scenario-Scenario " + scenarioLetter(index)));
        return this;
    }

    // Removes the first word (and the following space) from the scenario's product name — same
    // approach used in the msjourney suite. Re-types if a Bubble.io reactive event clears the field.
    @Step("Remove first word from scenario {index} product name")
    public ShopSetupPage removeFirstWordFromProductName(int index) {
        String letter = scenarioLetter(index);
        By nameField = byId("edit_product_name_scenario_Scenario " + letter);
        WebElement input = new WebDriverWait(driver, 30)
                .until(ExpectedConditions.visibilityOfElementLocated(nameField));
        ((JavascriptExecutor) driver).executeScript(
                "arguments[0].scrollIntoView({behavior:'instant',block:'center'});", input);
        // The name must be populated before we read it.
        new WebDriverWait(driver, 30).until(d -> {
            String v = d.findElement(nameField).getAttribute("value");
            return v != null && !v.trim().isEmpty();
        });
        input = driver.findElement(nameField);
        input.click();
        String original = input.getAttribute("value");
        int firstSpace = original.indexOf(' ');
        String trimmed = firstSpace >= 0 ? original.substring(firstSpace + 1) : original;
        input.clear();
        input.sendKeys(trimmed);

        // Bubble.io reactive events can reset the field immediately after sendKeys — re-type if so.
        final String expected = trimmed;
        try {
            new WebDriverWait(driver, 5).until(d ->
                    expected.equals(d.findElement(nameField).getAttribute("value")));
        } catch (Exception e) {
            System.out.println("[ShopSetup] Scenario " + letter + " name did not persist — re-typing: " + expected);
            WebElement retry = driver.findElement(nameField);
            retry.clear();
            retry.sendKeys(expected);
        }
        System.out.println("[ShopSetup] Scenario " + letter + " name trimmed: '" + original + "' -> '" + trimmed + "'");
        return this;
    }

    // Saves the open scenario's changes, then waits for the Next button to become clickable —
    // which only happens once the edit-scenario container has closed.
    @Step("Save scenario changes")
    public ShopSetupPage saveChanges() {
        // This button may not follow the ghost→filled colour pattern — fall back to jsClick.
        scrollTo(saveChangesButton);
        try {
            clickWhenEnabled(saveChangesButton);
        } catch (Exception e) {
            jsClickScrolled(saveChangesButton);
        }
        new WebDriverWait(driver, 30).until(ExpectedConditions.elementToBeClickable(nextStageButton));
        return this;
    }

    // ── Scenario snapshot capture (for later product-detail comparison) ──────────
    //
    // Called just before proceeding to Form Questions. Opens each scenario's edit popup
    // ({index}-edit-product-shop-setup), reads name/price/brand/main-image/prime from the popup
    // fields, then closes it (btn_close_add_edit_manually). Ratings are NOT in the popup, so they
    // are not captured here.
    @Step("Capture Scenario {index} product details from its edit popup")
    public ProductSnapshot captureScenarioSnapshot(int index) {
        String letter = scenarioLetter(index);
        jsClickScrolled(byId(index + "-edit-product-shop-setup"));

        By nameField = byId("edit_product_name_scenario_Scenario " + letter);
        new WebDriverWait(driver, 30)
                .until(ExpectedConditions.visibilityOfElementLocated(nameField));

        ProductSnapshot snap = new ProductSnapshot();
        // Full product name straight from the field value (NOT truncated) — the detail page shows
        // this same full name, so matching is exact.
        snap.truncatedName = valueOf(nameField);
        snap.price    = normalizePrice(valueOf(byId("edit_product_price_scenario_Scenario " + letter)));
        snap.brand    = valueOf(byId("edit_product_brand_scenario_Scenario " + letter));
        snap.ratings  = valueOf(byId("edit_product_number_star_rating_scenario_Scenario " + letter));
        // main-picture-scenario-Scenario {L} contains an <img>; getElementById handles the space in the id.
        snap.imageSrc = imgSrcFromContainer("main-picture-scenario-Scenario " + letter);
        snap.hasPrime = readPrimeSelected(byId("edit_product_activate_prime_scenario_Scenario " + letter));

        System.out.println("[ShopSetup] Captured Scenario " + letter + ": name=" + snap.truncatedName
            + " price=" + snap.price + " brand=" + snap.brand + " ratings=" + snap.ratings
            + " prime=" + snap.hasPrime + " img=" + snap.imageSrc);

        // Close the popup — wait for the close button to be clickable first (it can be slow to
        // become interactive), then confirm the popup's name field is gone.
        new WebDriverWait(driver, 30)
                .until(ExpectedConditions.elementToBeClickable(closeManualEditBtn));
        jsClickScrolled(closeManualEditBtn);
        new WebDriverWait(driver, 15)
                .until(ExpectedConditions.invisibilityOfElementLocated(nameField));
        return snap;
    }

    // Reads an <input> value by locator (scrolls it into view first — popup fields can be below the fold).
    private String valueOf(By locator) {
        try {
            new WebDriverWait(driver, 15).until(ExpectedConditions.presenceOfElementLocated(locator));
            return scrollTo(locator).getAttribute("value");
        } catch (Exception e) {
            return null;
        }
    }

    // Reads the prime dropdown's default-selected option. The <select> has plain-valued options
    // value="true" (text "Yes") and value="false" (text "No"), plus a placeholder and a BLANK
    // option. We read the currently-selected option and treat value "true" (or text "Yes") as prime.
    private boolean readPrimeSelected(By locator) {
        try {
            new WebDriverWait(driver, 15).until(ExpectedConditions.presenceOfElementLocated(locator));
            WebElement sel = scrollTo(locator);
            WebElement opt = new Select(sel).getFirstSelectedOption();
            String val = opt.getAttribute("value");
            String txt = opt.getText() == null ? "" : opt.getText().trim();
            boolean prime = "true".equalsIgnoreCase(val) || "Yes".equalsIgnoreCase(txt);
            System.out.println("[ShopSetup] Scenario prime dropdown selected value='" + val
                + "' text='" + txt + "' -> hasPrime=" + prime);
            return prime;
        } catch (Exception e) {
            System.out.println("[ShopSetup] Could not read prime dropdown: " + e.getMessage());
            return false;
        }
    }

    // True when the Shop Setup page is currently shown (its Next button is present and visible). Used to
    // decide whether a preview open landed back on Shop Setup (consumed link) and needs recovery.
    public boolean isOnShopSetup() {
        try {
            return !driver.findElements(nextStageButton).isEmpty()
                && driver.findElement(nextStageButton).isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }

    @Step("Proceed to the form questions step")
    public ShopSetupPage proceedToFormQuestions() {
        WebElement nextBtn = wait.until(ExpectedConditions.presenceOfElementLocated(nextStageButton));
        ((JavascriptExecutor) driver).executeScript(
                "arguments[0].scrollIntoView({behavior:'instant',block:'center'});", nextBtn);
        clickWhenEnabled(nextStageButton);
        return this;
    }
}

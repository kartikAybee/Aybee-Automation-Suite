package com.aybee.pages;

import io.qameta.allure.Step;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.FluentWait;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.asserts.SoftAssert;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class D2CProductListPage extends BasePage {

    private static final String NAME_PREFIX  = "d2c-item-name-";
    private static final String PRICE_PREFIX = "d2c-item-price-";
    private final By nextOpenerBtn = By.id("next-open-product");

    @Step("Wait for D2C product list to load")
    public D2CProductListPage waitUntilLoaded() {
        new WebDriverWait(driver, 30).until(
            ExpectedConditions.visibilityOfElementLocated(
                By.cssSelector("[id^='d2c-item-name-']")));
        // A delayed help popup can render over the list — clear it so it doesn't block selection.
        dismissHelpPopupIfPresent();
        return this;
    }

    // Returns the product name suffixes from all visible d2c-item-name-* elements.
    public List<String> getAllProductNames() {
        List<WebElement> els = driver.findElements(
            By.cssSelector("[id^='d2c-item-name-']"));
        List<String> names = new ArrayList<>();
        for (WebElement el : els) {
            String id = el.getAttribute("id");
            if (id != null && id.startsWith("d2c-item-name-")) {
                names.add(id.substring("d2c-item-name-".length()));
            }
        }
        return names;
    }

    @Step("Assert product '{productName}' is visible in D2C product list")
    public D2CProductListPage assertProductInList(String productName, SoftAssert sa) {
        List<String> names = getAllProductNames();
        boolean found = names.stream().anyMatch(n ->
            n.equalsIgnoreCase(productName)
            || n.toLowerCase().contains(productName.toLowerCase())
            || productName.toLowerCase().contains(n.toLowerCase()));
        if (!found) {
            sa.fail("[D2CList] Product not found in list. Expected: [" + productName + "]. Available: " + names);
        } else {
            System.out.println("[D2CList] Product confirmed in list: " + productName);
        }
        return this;
    }

    // Reads the price text from d2c-item-price-{productName}.
    // findById handles product names with spaces and special characters.
    @Step("Read list price for product '{productName}'")
    public String getProductPriceFromList(String productName) {
        String id = PRICE_PREFIX + productName.toLowerCase();
        WebElement el = new WebDriverWait(driver, 10).until(d -> findById(id));
        if (el == null) {
            throw new RuntimeException("[D2CList] Price element not found: " + id);
        }
        return el.getText().trim();
    }

    @Step("Assert D2C list data matches shop setup for '{productName}'")
    public D2CProductListPage assertProductDataMatchesSetup(
            String productName, String expectedPrice, SoftAssert sa) {
        assertProductInList(productName, sa);
        try {
            String listPrice = getProductPriceFromList(productName);
            String expectedFormatted = expectedPrice.startsWith("$") ? expectedPrice : "$" + expectedPrice;
            if (!listPrice.equals(expectedFormatted)) {
                sa.fail("[D2C Product List page] Price mismatch for product [" + productName + "] — "
                    + "expected (from shop-setup price): [" + expectedFormatted + "] "
                    + "but shown on the product list card: [" + listPrice + "]");
            } else {
                System.out.println("[D2CList] List price confirmed: " + listPrice);
            }
        } catch (Exception e) {
            sa.fail("[D2CList] Could not verify list price for " + productName + ": " + e.getMessage());
        }
        return this;
    }

    // Finds the competitor product name (not Scenario A or B) without clicking anything.
    @Step("Find competitor product name from D2C product list")
    public String findCompetitorProductName(String scenarioAName, String scenarioBName) {
        List<String> all = getAllProductNames();
        return all.stream()
            .filter(n -> !matchesOurProduct(n, scenarioAName, scenarioBName))
            .findFirst()
            .orElseThrow(() -> new RuntimeException(
                "[D2CList] No competitor product found. Products: " + all
                + " | Ours A: [" + scenarioAName + "] B: [" + scenarioBName + "]"));
    }

    // Clicks the product's d2c-item-name-{productName} button.
    // Returns the exact name suffix from the element's ID (preserves original casing).
    @Step("Select product '{productName}' from D2C product list")
    public String selectProduct(String productName) {
        // Guard against a delayed help popup overlaying the list right before we click.
        dismissHelpPopupIfPresent();
        WebElement btn = findById(NAME_PREFIX + productName);
        if (btn == null) btn = scanForProductButton(productName);
        if (btn == null) {
            throw new RuntimeException("[D2CList] Product button not found: " + productName);
        }
        String actualName = btn.getAttribute("id").substring(NAME_PREFIX.length());
        scrollToCenter(btn);
        btn.click();
        System.out.println("[D2CList] Selected product: " + actualName);
        return actualName;
    }

    // Selects the first option from the opener popup and waits for the D2C product page.
    @Step("Answer opener question popup and wait for D2C product page")
    public void answerOpenerQuestion() {
        // A delayed help popup can surface over the opener — clear it before interacting.
        dismissHelpPopupIfPresent();
        new WebDriverWait(driver, 30).until(
            ExpectedConditions.presenceOfElementLocated(nextOpenerBtn));
        List<WebElement> options = new FluentWait<>(driver)
            .withTimeout(15, TimeUnit.SECONDS)
            .pollingEvery(500, TimeUnit.MILLISECONDS)
            .ignoring(Exception.class)
            .until(d -> {
                List<WebElement> els = d.findElements(By.cssSelector("[id^='open-product-']"));
                return els.isEmpty() ? null : els;
            });
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", options.get(0));
        jsClick(nextOpenerBtn);
        new WebDriverWait(driver, 30).until(
            ExpectedConditions.invisibilityOfElementLocated(nextOpenerBtn));
        new WebDriverWait(driver, 30).until(
            ExpectedConditions.presenceOfElementLocated(By.id("d2c-product-name")));
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private boolean matchesOurProduct(String name, String nameA, String nameB) {
        String lc = name.toLowerCase();
        return (nameA != null && (lc.equalsIgnoreCase(nameA)
                || lc.contains(nameA.toLowerCase()) || nameA.toLowerCase().contains(lc)))
            || (nameB != null && (lc.equalsIgnoreCase(nameB)
                || lc.contains(nameB.toLowerCase()) || nameB.toLowerCase().contains(lc)));
    }

    // Case-insensitive scan fallback when exact findById fails.
    private WebElement scanForProductButton(String partialName) {
        Object result = ((JavascriptExecutor) driver).executeScript(
            "var els = document.querySelectorAll('[id^=\"d2c-item-name-\"]');" +
            "var term = arguments[0].toLowerCase();" +
            "for (var i = 0; i < els.length; i++) {" +
            "  var n = els[i].id.substring(13).toLowerCase();" +  // len("d2c-item-name-") = 13
            "  if (n === term || n.indexOf(term) === 0 || n.indexOf(term) >= 0 || term.indexOf(n) >= 0) return els[i];" +
            "}" +
            "return null;",
            partialName);
        return (result instanceof WebElement) ? (WebElement) result : null;
    }

}

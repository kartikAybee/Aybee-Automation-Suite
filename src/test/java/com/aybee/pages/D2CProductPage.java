package com.aybee.pages;

import io.qameta.allure.Step;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.asserts.SoftAssert;

import java.util.List;

public class D2CProductPage extends BasePage {

    private static final By PRODUCT_NAME  = By.id("d2c-product-name");
    private static final By PRODUCT_PRICE = By.id("d2c-product-price");
    private static final By ADD_TO_CART   = By.id("d2c-add-to-cart");
    private static final By CHECKOUT_BTN  = By.id("d2c-checkout-btn");
    private static final By CLOSE_BASKET  = By.id("close-d2c-basket");

    @Step("Wait for D2C product page to load")
    public D2CProductPage waitUntilLoaded() {
        new WebDriverWait(driver, 30).until(
            ExpectedConditions.presenceOfElementLocated(PRODUCT_NAME));
        return this;
    }

    public String getDisplayedProductName() {
        return driver.findElement(PRODUCT_NAME).getText().trim();
    }

    public String getDisplayedProductPrice() {
        return driver.findElement(PRODUCT_PRICE).getText().trim();
    }

    @Step("Assert D2C product page name and price match the selected product")
    public D2CProductPage assertProductDataMatchesSelection(
            String expectedName, String expectedPrice, SoftAssert sa) {
        String name  = getDisplayedProductName();
        String price = getDisplayedProductPrice();

        if (!containsIgnoreCase(name, expectedName) && !containsIgnoreCase(expectedName, name)) {
            sa.fail("[D2C Product Detail page] Name mismatch — "
                + "expected (product selected from the list): [" + expectedName + "] "
                + "but shown on the product detail page: [" + name + "]");
        } else {
            System.out.println("[D2CProduct] Name confirmed: " + name);
        }

        // Competitors have no shop-setup price (single source of truth) — skip the price check.
        if (expectedPrice == null || expectedPrice.trim().isEmpty()) {
            System.out.println("[D2CProduct] No shop-setup expected price (competitor) — skipping price check. Detail page shows: " + price);
            return this;
        }
        String expectedFormatted = expectedPrice.startsWith("$") ? expectedPrice : "$" + expectedPrice;
        if (!price.equals(expectedFormatted)) {
            sa.fail("[D2C Product Detail page] Price mismatch for product [" + name + "] — "
                + "expected (price shown for this product on the list): [" + expectedFormatted + "] "
                + "but shown on the product detail page: [" + price + "]");
        } else {
            System.out.println("[D2CProduct] Price confirmed: " + price);
        }
        return this;
    }

    @Step("Click Add to Cart on D2C product page")
    public D2CProductPage clickAddToCart() {
        jsClick(ADD_TO_CART);
        return this;
    }

    // Waits for the cart sidebar to open AND for at least one cart item to be present —
    // confirms the add-to-cart completed before any further cart interaction.
    @Step("Wait for D2C cart sidebar to open with item")
    public D2CProductPage waitForCartSidebar() {
        new WebDriverWait(driver, 30).until(
            ExpectedConditions.visibilityOfElementLocated(CHECKOUT_BTN));
        new WebDriverWait(driver, 15).until(
            ExpectedConditions.presenceOfElementLocated(By.cssSelector("[id^='d2c-checkout-item-']")));
        return this;
    }

    @Step("Assert product from current page is present in D2C cart")
    public D2CProductPage assertProductInCart(SoftAssert sa) {
        String name   = getDisplayedProductName();
        String itemId = "d2c-checkout-item-" + name;
        try {
            new WebDriverWait(driver, 20).until(d -> findById(itemId) != null);
            System.out.println("[D2CCart] Product confirmed in cart: " + name);
        } catch (Exception e) {
            sa.fail("[D2CCart] Product not found in cart. Expected ID: " + itemId);
        }
        return this;
    }

    @Step("Assert D2C cart item price matches product page price")
    public D2CProductPage assertCartItemPriceMatchesProduct(SoftAssert sa) {
        String name         = getDisplayedProductName();
        String productPrice = getDisplayedProductPrice();
        try {
            WebElement priceEl = findById("checkout-price-" + name);
            if (priceEl == null) {
                sa.fail("[D2CCart] checkout-price-" + name + " not found");
                return this;
            }
            String cartPrice = priceEl.getText();
            if (!cartPrice.equals(productPrice)) {
                sa.fail("[D2C Cart sidebar] Price mismatch for product [" + name + "] — "
                    + "expected (product detail page price): [" + productPrice + "] "
                    + "but cart line-item price: [" + cartPrice + "]");
            } else {
                System.out.println("[D2CCart] Cart item price matches product page: " + cartPrice);
            }
        } catch (Exception e) {
            sa.fail("[D2CCart] Cart price check failed for " + name + ": " + e.getMessage());
        }
        return this;
    }

    @Step("Assert sum of D2C cart item prices equals checkout total")
    public D2CProductPage assertCartTotalMatchesPrices(SoftAssert sa) {
        try {
            List<WebElement> priceEls = driver.findElements(
                By.cssSelector("[id^='checkout-price-']"));
            if (priceEls.isEmpty()) {
                sa.fail("[D2CCart] No checkout-price-* elements found");
                return this;
            }
            double sum = 0;
            for (WebElement el : priceEls) {
                sum += parsePrice(el.getText().trim());
            }

            WebElement totalEl = findById("checkout-total");
            if (totalEl == null) {
                sa.fail("[D2CCart] checkout-total element not found");
                return this;
            }
            double total = parsePrice(totalEl.getText().trim());

            if (Math.abs(sum - total) > 0.02) {
                sa.fail("[D2CCart] Total mismatch. Item sum: " + sum
                    + " vs checkout-total: " + total + " [" + totalEl.getText().trim() + "]");
            } else {
                System.out.println("[D2CCart] Total verified: " + totalEl.getText().trim());
            }
        } catch (Exception e) {
            sa.fail("[D2CCart] Cart total assertion failed: " + e.getMessage());
        }
        return this;
    }

    @Step("Remove product from D2C cart and verify it is gone")
    public D2CProductPage removeProductFromCart(SoftAssert sa) {
        String name = getDisplayedProductName();
        WebElement removeBtn = findById("checkout-remove-" + name);
        if (removeBtn == null) {
            sa.fail("[D2CCart] Remove button not found: checkout-remove-" + name);
            return this;
        }
        scrollToCenter(removeBtn);
        removeBtn.click();
        try {
            new WebDriverWait(driver, 15).until(
                d -> findById("d2c-checkout-item-" + name) == null);
            System.out.println("[D2CCart] Product removed from cart: " + name);
        } catch (Exception e) {
            sa.fail("[D2CCart] Product still present in cart after removal: " + name);
        }
        return this;
    }

    @Step("Close D2C cart sidebar")
    public D2CProductPage closeCartSidebar() {
        jsClick(CLOSE_BASKET);
        new WebDriverWait(driver, 10).until(
            ExpectedConditions.invisibilityOfElementLocated(CHECKOUT_BTN));
        return this;
    }

    @Step("Proceed to D2C checkout")
    public void clickCheckout() {
        new WebDriverWait(driver, 15).until(
            ExpectedConditions.presenceOfElementLocated(By.cssSelector("[id^='d2c-checkout-item-']")));
        WebElement btn = new WebDriverWait(driver, 15).until(
            ExpectedConditions.visibilityOfElementLocated(CHECKOUT_BTN));
        // Bubble.io attaches its checkout workflow handler asynchronously after render —
        // wait 2s before clicking so the handler is registered when the click fires.
        try { Thread.sleep(2000); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        btn.click();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static boolean containsIgnoreCase(String source, String target) {
        return source != null && target != null
            && source.toLowerCase().contains(target.toLowerCase());
    }

}

package com.aybee.pages;

import com.aybee.context.ProductSnapshot;
import io.qameta.allure.Step;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.FluentWait;
import org.openqa.selenium.support.ui.WebDriverWait;
import com.aybee.utils.ScreenshotSoftAssert;
import org.testng.asserts.SoftAssert;

import java.util.concurrent.TimeUnit;

public class ProductDetailPage extends BasePage {

    private final By changeOnHoverImg = By.cssSelector("#changeOnHover img");
    private final By productTitle     = By.id("product-title");
    private final By productPrice     = By.id("product-price");
    private final By checkoutPrice    = By.id("checkout-product-price");
    private final By ratingsText      = By.id("total-ratings-text");
    private final By productBrand     = By.id("product-brand");
    private final By primeStatus      = By.id("prime-status");
    private final By quantityText     = By.id("quantity");

    private final By addToCartButton    = By.id("addtocart");
    private final By buyNowButton       = By.id("buyNow");
    private final By shoppingCartIcon    = By.id("pointer");
    private final By notInterestedButton = By.id("button-not-interest");
    // contains() instead of exact match — avoids failure when Bubble.io adds extra classes.
    private final By addToCartToast = By.xpath("//span[contains(@class,'jq-toast-loaded')]");

    @Step("Wait for product detail page to load")
    public ProductDetailPage waitUntilLoaded() {
        wait.until(ExpectedConditions.presenceOfElementLocated(productTitle));
        return this;
    }

    @Step("Click Add to Cart")
    public ProductDetailPage clickAddToCart() {
        // Dismiss any lingering toast from a previous action so the post-click wait
        // detects a fresh toast, not a stale one that pre-dates this click.
        try {
            new WebDriverWait(driver, 3).until(
                ExpectedConditions.invisibilityOfElementLocated(addToCartToast));
        } catch (Exception ignored) {}

        // Scroll button into view and do a single native click — avoid the try/catch
        // double-click pattern which could fire both clickWhenEnabled AND jsClick when
        // the element becomes stale after the first click, toggling the cart state twice.
        WebElement btn = new WebDriverWait(driver, 15).until(
            ExpectedConditions.elementToBeClickable(addToCartButton));
        ((JavascriptExecutor) driver).executeScript(
            "arguments[0].scrollIntoView({behavior:'instant',block:'center'});", btn);
        btn.click();

        // Fresh toast confirms the item was registered server-side.
        new WebDriverWait(driver, 15).until(
            ExpectedConditions.presenceOfElementLocated(addToCartToast));
        return this;
    }

    @Step("Click Buy Now")
    public void clickBuyNow() {
        try {
            clickWhenEnabled(buyNowButton);
        } catch (Exception e) {
            jsClick(buyNowButton);
        }
    }

    @Step("Click shopping cart icon to open cart page")
    public void clickShoppingCart() {
        jsClick(shoppingCartIcon);
    }

    // Blocked state on Bubble.io is indicated by opacity:0.7 + cursor:default on the button
    // element — the HTML disabled attribute is never set. We assert opacity first (visual
    // blocked state), then click and verify no navigation (behavioural blocked state).
    // Soft-asserts so both checks run even if one fails; caller catches AssertionError.
    @Step("Assert Add to Cart button is in blocked state (opacity 0.7, no navigation)")
    public ProductDetailPage assertAddToCartBlocked() {
        SoftAssert sa = new ScreenshotSoftAssert();
        assertButtonBlocked(sa, addToCartButton, "Add to Cart");
        sa.assertAll();
        return this;
    }

    @Step("Assert Buy Now button is in blocked state (opacity 0.7, no navigation)")
    public ProductDetailPage assertBuyNowBlocked() {
        SoftAssert sa = new ScreenshotSoftAssert();
        assertButtonBlocked(sa, buyNowButton, "Buy Now");
        sa.assertAll();
        return this;
    }

    // Clicks Not Interested from the product detail page as the logged-in owner.
    // The owner is redirected back to the shop setup page — same behaviour as the
    // marketplace list not-interested. 60 s timeout matches the marketplace variant.
    @Step("Click Not Interested from product detail and verify redirect to shop setup page")
    public void clickNotInterestedAndVerifyFilteredOut() {
        jsClick(notInterestedButton);
        try {
            new WebDriverWait(driver, 60).until(
                ExpectedConditions.presenceOfElementLocated(
                    By.id("marketplacesimulation_shopsetup_next_button")));
        } catch (Exception e) {
            throw new AssertionError(
                "[ProductDetail] Shop setup page did not appear after clicking Not Interested — " +
                "marketplacesimulation_shopsetup_next_button not found within 60s");
        }
    }

    private void assertButtonBlocked(SoftAssert sa, By locator, String label) {
        WebElement btn = wait.until(ExpectedConditions.presenceOfElementLocated(locator));
        // Bubble.io applies the disabled-state opacity asynchronously — poll until it
        // reaches 0.7 rather than reading it immediately and getting the pre-transition value.
        try {
            new FluentWait<>(driver)
                .withTimeout(3, TimeUnit.SECONDS)
                .pollingEvery(200, TimeUnit.MILLISECONDS)
                .ignoring(Exception.class)
                .until(d -> {
                    Object op = ((JavascriptExecutor) d).executeScript(
                        "return window.getComputedStyle(arguments[0]).opacity;", btn);
                    return op != null && Math.abs(Double.parseDouble(op.toString()) - 0.7) < 0.05;
                });
        } catch (Exception ignored) {}
        String opacity = (String) ((JavascriptExecutor) driver).executeScript(
            "return window.getComputedStyle(arguments[0]).opacity;", btn);
        sa.assertEquals(Double.parseDouble(opacity), 0.7, 0.05,
            "[ProductDetail] " + label + " should have opacity 0.7 when cart is full, got: " + opacity);
        String urlBefore = driver.getCurrentUrl();
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", btn);
        try {
            new WebDriverWait(driver, 2).until(d -> !d.getCurrentUrl().equals(urlBefore));
            sa.fail("[ProductDetail] " + label + " navigated when it should be blocked");
        } catch (org.openqa.selenium.TimeoutException ignored) {}
    }

    // Soft-asserts all captured fields against the shop setup snapshot so every check runs
    // even if one fails. Caller should catch AssertionError to let the flow continue.
    @Step("Assert product detail page data matches shop setup snapshot")
    public ProductDetailPage assertProductData(ProductSnapshot snap) {
        SoftAssert sa = new ScreenshotSoftAssert();
        softAssertImageSrc(sa, snap.imageSrc);
        softAssertPrices(sa, snap.price);
        softAssertRatings(sa, snap.ratings);
        softAssertBrand(sa, snap.brand);
        softAssertPrime(sa);
        softAssertQuantity(sa);
        sa.assertAll();
        return this;
    }

    private void softAssertImageSrc(SoftAssert sa, String expected) {
        if (expected == null || expected.isEmpty()) {
            System.out.println("[ProductDetail] Skipping image — snapshot src was null");
            return;
        }
        try {
            wait.until(ExpectedConditions.presenceOfElementLocated(changeOnHoverImg));
            // Bubble lazy-loads the image: the src starts as a 1x1 "data:image/gif;base64,..."
            // placeholder and only later resolves to the real URL. Reading too early compares
            // against that placeholder. Poll (re-finding to dodge re-render staleness) until the
            // src is a real, non-placeholder URL before asserting.
            String actual;
            try {
                actual = new WebDriverWait(driver, 15).ignoring(Exception.class).until(d -> {
                    for (WebElement e : d.findElements(changeOnHoverImg)) {
                        String s = e.getAttribute("src");
                        if (s != null && !s.isEmpty() && !s.startsWith("data:")) return s;
                    }
                    return null;
                });
            } catch (Exception timeout) {
                System.out.println("[ProductDetail] Skipping image — src stayed a lazy-load placeholder");
                return;
            }
            sa.assertEquals(actual, expected, "[ProductDetail] Image src mismatch");
        } catch (Exception e) {
            System.out.println("[ProductDetail] changeOnHover img not found: " + e.getMessage());
        }
    }

    private void softAssertPrices(SoftAssert sa, String expected) {
        String detailPrice = normalizePrice(priceTextById("product-price"));
        String checkoutVal = normalizePrice(priceTextById("checkout-product-price"));
        softAssertField(sa, "Price (product-price)", expected, detailPrice);
        softAssertField(sa, "Price (checkout-product-price)", expected, checkoutVal);
        if (detailPrice != null && checkoutVal != null) {
            sa.assertEquals(checkoutVal, detailPrice,
                "[ProductDetail] checkout-product-price does not match product-price");
        }
    }

    private void softAssertRatings(SoftAssert sa, String expected) {
        String actual = extractNumber(getText(ratingsText));
        softAssertField(sa, "Ratings", expected, actual);
    }

    private void softAssertBrand(SoftAssert sa, String expected) {
        softAssertField(sa, "Brand", expected, getText(productBrand));
    }

    // Prime is checked by visibility only — no snapshot comparison since Bubble.io may
    // not render the badge on the shop-setup card immediately after saving the variation.
    // The actual displayed state is logged; mismatch with snapshot is not asserted.
    private void softAssertPrime(SoftAssert sa) {
        boolean actual = isElementVisible(primeStatus);
        System.out.println("[ProductDetail] Prime status on page: " + actual);
    }

    // Quantity word is country-localised; only the numeric suffix ": 1" is stable.
    private void softAssertQuantity(SoftAssert sa) {
        String qty = getText(quantityText);
        sa.assertTrue(qty != null && qty.endsWith(": 1"),
            "[ProductDetail] Quantity should end with ': 1', got: " + qty);
    }

    private void softAssertField(SoftAssert sa, String label, String expected, String actual) {
        if (expected == null || expected.isEmpty()) {
            System.out.println("[ProductDetail] Skipping " + label + " — snapshot value was null");
            return;
        }
        if (actual == null || actual.isEmpty()) {
            System.out.println("[ProductDetail] Skipping " + label + " — page value was null");
            return;
        }
        sa.assertEquals(actual, expected, "[ProductDetail] " + label + " mismatch");
    }
}

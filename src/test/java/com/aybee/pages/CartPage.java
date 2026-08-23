package com.aybee.pages;

import com.aybee.context.ProductSnapshot;
import io.qameta.allure.Step;
import org.openqa.selenium.By;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import com.aybee.utils.ScreenshotSoftAssert;
import org.testng.asserts.SoftAssert;

public class CartPage extends BasePage {

    // Shared — present in both empty and filled states.
    private final By continueShoppingBtn = By.id("continue-shopping-btn");

    // Empty-cart state.
    private final By emptyCartText       = By.id("empty-cart-text");
    private final By cartTotal           = By.id("cart-total");

    // Filled-cart state.
    private final By cartQuantity        = By.id("cart-quantity");
    private final By cartProductDelete   = By.id("cart-product-delete");
    private final By selectedProductPrice = By.id("selected-product-price");
    private final By orderTotal          = By.id("order-total");
    private final By confirmCheckoutBtn  = By.id("confirm-checkout");

    // ── Empty-cart assertions ─────────────────────────────────────────────────

    @Step("Assert cart is empty")
    public CartPage assertEmptyCart() {
        SoftAssert sa = new ScreenshotSoftAssert();
        try {
            new WebDriverWait(driver, 30).until(
                ExpectedConditions.visibilityOfElementLocated(emptyCartText));
        } catch (Exception e) {
            sa.fail("[Cart] empty-cart-text not visible — cart may not be empty");
        }
        sa.assertAll();
        return this;
    }

    @Step("Click Continue Shopping (returns to product list)")
    public void clickContinueShopping() {
        jsClick(continueShoppingBtn);
    }

    // ── Filled-cart assertions ────────────────────────────────────────────────

    // Verifies image src, quantity text, product price against snapshot, and that
    // all price display areas are visible. Soft-asserts all fields so every check runs
    // even if one fails. Caller should catch AssertionError to let the flow continue.
    @Step("Assert cart contains our product and matches snapshot")
    public CartPage assertCartWithItem(ProductSnapshot snap) {
        // Hard-fail immediately if the cart is empty — product was never added.
        try {
            new WebDriverWait(driver, 5).until(
                ExpectedConditions.visibilityOfElementLocated(emptyCartText));
            throw new AssertionError(
                "[Cart] Product was not added to cart — empty-cart-text is visible");
        } catch (AssertionError e) {
            throw e;
        } catch (Exception ignored) {}

        SoftAssert sa = new ScreenshotSoftAssert();
        assertCartImage(sa, snap.imageSrc);
        assertCartQuantity(sa);
        assertCartPrices(sa, snap.price);
        sa.assertAll();
        return this;
    }

    private void assertCartImage(SoftAssert sa, String expected) {
        String actual = imgSrcFromContainer("selected-product-image");
        if (expected == null || expected.isEmpty()) {
            System.out.println("[Cart] Skipping image — snapshot src was null");
            return;
        }
        if (actual == null || actual.isEmpty()) {
            System.out.println("[Cart] Skipping image — cart page src was null");
            return;
        }
        sa.assertEquals(actual, expected, "[Cart] Product image src mismatch");
    }

    private void assertCartQuantity(SoftAssert sa) {
        String qty = getText(cartQuantity);
        sa.assertTrue(qty != null && qty.contains("1"),
            "[Cart] cart-quantity should contain '1', got: " + qty);
    }

    private void assertCartPrices(SoftAssert sa, String snapshotPrice) {
        String itemPrice = normalizePrice(priceTextById("selected-product-price"));
        if (snapshotPrice != null && !snapshotPrice.isEmpty()
                && itemPrice != null && !itemPrice.isEmpty()) {
            sa.assertEquals(itemPrice, snapshotPrice, "[Cart] selected-product-price mismatch");
        } else {
            System.out.println("[Cart] Skipping selected-product-price — one value was null");
        }
        sa.assertTrue(isElementVisible(cartTotal),
            "[Cart] cart-total should be visible when cart has an item");
        sa.assertTrue(isElementVisible(orderTotal),
            "[Cart] order-total should be visible when cart has an item");
    }

    @Step("Delete item from cart and verify cart becomes empty")
    public CartPage deleteItemAndAssertEmpty() {
        jsClick(cartProductDelete);
        assertEmptyCart();
        return this;
    }

    @Step("Click Confirm Checkout and wait for participant form questions")
    public void clickConfirmCheckout() {
        wait.until(ExpectedConditions.elementToBeClickable(confirmCheckoutBtn));
        jsClick(confirmCheckoutBtn);
        // After checkout the flow either shows the participant form (final-questions-title, whose text
        // Bubble populates asynchronously — poll for non-empty) or, when there are no questions to
        // answer (e.g. DEFAULT_QUESTIONS=no with no manual questions), redirects straight to completion
        // (toggle-sign-in). Accept whichever appears so no fake 60s timeout fires expecting a form.
        // 60s covers slow checkout processing and form page render after redirect.
        new WebDriverWait(driver, 60).until(d -> {
            try {
                if (!d.findElements(By.id("toggle-sign-in")).isEmpty()) return true;
                String text = d.findElement(By.id("final-questions-title")).getText().trim();
                return !text.isEmpty();
            } catch (Exception e) {
                return false;
            }
        });
    }
}

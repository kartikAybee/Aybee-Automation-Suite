package com.aybee.steps;

import com.aybee.context.ScenarioContext;
import com.aybee.pages.D2CParticipantFormPage;
import com.aybee.pages.D2CProductListPage;
import com.aybee.pages.D2CProductPage;
import com.aybee.pages.PreviewJourneyPage;
import io.cucumber.java.en.And;
import com.aybee.utils.ScreenshotSoftAssert;
import org.testng.asserts.SoftAssert;

import java.util.List;

public class PreviewJourneySteps {

    private final ScenarioContext        context;
    private final PreviewJourneyPage     previewPage  = new PreviewJourneyPage();
    private final D2CProductListPage     listPage     = new D2CProductListPage();
    private final D2CProductPage         productPage  = new D2CProductPage();
    private final D2CParticipantFormPage formPage     = new D2CParticipantFormPage();

    // The single source of truth for the expected price — the shop-setup value ($). Used to verify
    // the product detail page. Never sourced from the list card (which renders €), so currencies
    // never mix between the stored expected and the page-under-test.
    private String expectedProductPrice;

    public PreviewJourneySteps(ScenarioContext context) {
        this.context = context;
    }

    // ── Logged-in preview ─────────────────────────────────────────────────────

    @And("I navigate to the preview URL as a logged-in user")
    public void iNavigateToPreviewUrlAsLoggedInUser() {
        if (context.previewUrl == null) {
            throw new RuntimeException(
                "Preview URL not stored — 'I capture the experiment preview URL' must run first");
        }
        previewPage.navigateAsLoggedInUser(context.previewUrl);
    }

    @And("I answer the gender and age demographic questions")
    public void iAnswerGenderAndAgeDemographicQuestions() {
        try {
            previewPage.answerDemographicsGenderAndAge();
        } catch (AssertionError e) {
            context.softAssert.fail("[Preview] Gender/age demographics failed: " + e.getMessage());
        }
    }

    @And("I agree to the consent statement and proceed to the D2C product page")
    public void iAgreeToConsentStatementAndProceedToD2C() {
        previewPage.agreeToConsentStatement();
    }

    @And("I wait for the help popup and dismiss it")
    public void iWaitForHelpPopupAndDismissIt() {
        previewPage.waitForHelpPopupAndDismiss();
    }

    @And("I click not interested and verify I am redirected to shop setup")
    public void iClickNotInterestedAndVerifyShopSetup() {
        try {
            previewPage.clickNotInterestedAndVerifyShopSetupRedirect();
        } catch (AssertionError e) {
            context.softAssert.fail("[Preview] Not Interested shop-setup redirect failed: " + e.getMessage());
        }
    }

    // ── Guest preview ─────────────────────────────────────────────────────────

    @And("I clear the session")
    public void iClearTheSession() {
        previewPage.clearSession();
    }

    @And("I navigate to the preview URL as a guest")
    public void iNavigateToPreviewUrlAsGuest() {
        if (context.previewUrl == null) {
            throw new RuntimeException(
                "Preview URL not stored — 'I capture the experiment preview URL' must run first");
        }
        previewPage.navigateAsGuest(context.previewUrl);
    }

    @And("I answer all demographic questions")
    public void iAnswerAllDemographicQuestions() {
        try {
            previewPage.answerAllDemographicQuestions();
        } catch (AssertionError e) {
            context.softAssert.fail("[Preview] All demographics failed: " + e.getMessage());
        }
    }

    // ── D2C product list ──────────────────────────────────────────────────────

    // Verifies Scenario A's product is in the list and its price matches shop setup.
    @And("I verify the D2C product list contains our product with matching price")
    public void iVerifyD2CProductListContainsOurProduct() {
        listPage.waitUntilLoaded();
        String ourName  = resolveOurProductName();
        String ourPrice = resolveOurProductPrice();
        listPage.assertProductDataMatchesSetup(ourName, ourPrice, context.softAssert);
    }

    // Selects the first available product — used in the logged-in Not Interested check.
    // Always picks dynamically from the rendered list; stored names are not used here.
    @And("I select any product from the D2C product list and answer the opener question")
    public void iSelectAnyProductAndAnswerOpener() {
        listPage.waitUntilLoaded();
        List<String> names = listPage.getAllProductNames();
        if (names.isEmpty()) {
            throw new RuntimeException(
                "[D2C] No products visible in D2C list after scroll retry — cannot select product for Not Interested check");
        }
        String nameToSelect = names.get(0);
        System.out.println("[D2C] Selecting product for Not Interested check: " + nameToSelect);
        listPage.selectProduct(nameToSelect);
        listPage.answerOpenerQuestion();
    }

    // Expected price is always the shop-setup value (single source of truth). We deliberately do
    // NOT read the list card price — its € symbol would mix currencies with the detail page's $.
    @And("I select our product from the D2C product list and answer the opener question")
    public void iSelectOurProductAndAnswerOpener() {
        String ourName = resolveOurProductName();
        expectedProductPrice = resolveOurProductPrice();
        System.out.println("[D2C] Expected price (shop setup): " + expectedProductPrice);
        listPage.selectProduct(ourName);
        listPage.answerOpenerQuestion();
    }

    @And("I select a competitor product from the D2C product list and answer the opener question")
    public void iSelectCompetitorProductAndAnswerOpener() {
        listPage.waitUntilLoaded();
        List<String> all = listPage.getAllProductNames();
        String competitorName = all.stream()
            .filter(n -> !isOurProduct(n))
            .findFirst()
            .orElseThrow(() -> new RuntimeException(
                "[D2C] No competitor found in list. Products: " + all
                + " | Ours A: [" + context.scenarioAProductName
                + "] B: [" + context.scenarioBProductName + "]"));
        System.out.println("[D2C] Competitor selected: " + competitorName);
        // Competitors are not in shop setup, so there is no single-source expected price to compare;
        // clear it so the detail-page check skips the competitor price assertion rather than reusing
        // a stale value from a previous product.
        expectedProductPrice = null;
        listPage.selectProduct(competitorName);
        listPage.answerOpenerQuestion();
    }

    private boolean isOurProduct(String name) {
        String lc = name.toLowerCase();
        return (context.scenarioAProductName != null
                    && lc.contains(context.scenarioAProductName.toLowerCase()))
            || (context.scenarioBProductName != null
                    && lc.contains(context.scenarioBProductName.toLowerCase()))
            || (context.scenarioAProductName != null
                    && context.scenarioAProductName.toLowerCase().contains(lc))
            || (context.scenarioBProductName != null
                    && context.scenarioBProductName.toLowerCase().contains(lc));
    }

    // ── D2C product page ──────────────────────────────────────────────────────

    // Verifies the product detail page name and price match what was shown on the list.
    @And("I verify the D2C product page name and price match the selection")
    public void iVerifyD2CProductPageDetailsMatchSelection() {
        productPage.waitUntilLoaded();
        String currentName = productPage.getDisplayedProductName();
        productPage.assertProductDataMatchesSelection(
            currentName, expectedProductPrice, context.softAssert);
    }

    @And("I add the product to the D2C cart")
    public void iAddTheProductToTheD2CCart() {
        productPage.clickAddToCart()
                   .waitForCartSidebar();
    }

    // All cart assertions read d2c-product-name dynamically from the current product page.
    @And("I verify the D2C cart contains the product with correct price and total")
    public void iVerifyD2CCartContainsProductWithCorrectPriceAndTotal() {
        productPage.assertProductInCart(context.softAssert)
                   .assertCartItemPriceMatchesProduct(context.softAssert)
                   .assertCartTotalMatchesPrices(context.softAssert);
    }

    @And("I remove the product from the D2C cart and verify it is empty")
    public void iRemoveProductFromD2CCartAndVerifyEmpty() {
        productPage.removeProductFromCart(context.softAssert);
    }

    @And("I close the D2C cart sidebar")
    public void iCloseTheD2CCartSidebar() {
        productPage.closeCartSidebar();
    }

    @And("I add the product to the D2C cart again")
    public void iAddTheProductToTheD2CCartAgain() {
        productPage.clickAddToCart()
                   .waitForCartSidebar();
    }

    @And("I proceed to D2C checkout")
    public void iProceedToD2CCheckout() {
        productPage.clickCheckout();
    }

    // ── D2C form questions ────────────────────────────────────────────────────

    @And("I answer the D2C form questions for our product and verify Q2 is absent")
    public void iAnswerD2CFormQuestionsOurProduct() {
        SoftAssert sa = new ScreenshotSoftAssert();
        formPage.answerFormQuestionsOurProduct(resolveOurProductName(), sa);
        sa.assertAll();
    }

    @And("I answer the D2C form questions for competitor product and verify Q2 is shown")
    public void iAnswerD2CFormQuestionsCompetitor() {
        SoftAssert sa = new ScreenshotSoftAssert();
        formPage.answerFormQuestionsCompetitor(resolveOurProductName(), sa);
        sa.assertAll();
    }

    @And("I verify the guest is redirected to the sign-up page")
    public void iVerifyGuestRedirectedToSignUp() {
        SoftAssert sa = new ScreenshotSoftAssert();
        formPage.verifySignUpRedirect(sa);
        sa.assertAll();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private String resolveOurProductName() {
        if (context.scenarioBProductName != null && !context.scenarioBProductName.trim().isEmpty()) {
            return context.scenarioBProductName;
        }
        if (context.scenarioAProductName != null && !context.scenarioAProductName.trim().isEmpty()) {
            return context.scenarioAProductName;
        }
        throw new RuntimeException(
            "[D2C] No product name available — ensure shop setup ran and stored product names");
    }

    private String resolveOurProductPrice() {
        if (context.scenarioBProduct != null && context.scenarioBProduct.price != null) {
            return context.scenarioBProduct.price;
        }
        if (context.scenarioAProduct != null && context.scenarioAProduct.price != null) {
            return context.scenarioAProduct.price;
        }
        return "";
    }
}

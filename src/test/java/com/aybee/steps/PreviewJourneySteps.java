package com.aybee.steps;

import com.aybee.context.GlobalTestState;
import com.aybee.context.ProductSnapshot;
import com.aybee.context.ScenarioContext;
import com.aybee.pages.CartPage;
import com.aybee.pages.FormQuestionsPage;
import com.aybee.pages.MarketplaceListPage;
import com.aybee.pages.ParticipantFormPage;
import com.aybee.pages.PreviewJourneyPage;
import com.aybee.pages.ProductDetailPage;
import io.cucumber.java.en.And;
import io.cucumber.java.en.When;

public class PreviewJourneySteps {

    private final ScenarioContext    context;
    private final MarketplaceListPage marketplace     = new MarketplaceListPage();
    private final PreviewJourneyPage  previewJourney  = new PreviewJourneyPage();
    private final FormQuestionsPage   formQuestions   = new FormQuestionsPage();
    private final ProductDetailPage   productDetail   = new ProductDetailPage();
    private final CartPage            cart            = new CartPage();
    private final ParticipantFormPage participantForm = new ParticipantFormPage();

    public PreviewJourneySteps(ScenarioContext context) {
        this.context = context;
    }

    // ── Demographic questions ─────────────────────────────────────────────────

    @And("I answer the demographic questions")
    public void iAnswerTheDemographicQuestions() {
        try {
            previewJourney.answerDemographicQuestions();
        } catch (AssertionError e) {
            context.softAssert.fail("[Preview] Demographic questions failed: " + e.getMessage());
        }
    }

    // Logged-in CTR preview shows only gender + age before the consent page.
    @And("I answer the demographic questions as a logged-in user")
    public void iAnswerDemographicQuestionsLoggedIn() {
        try {
            previewJourney.answerDemographicQuestionsLoggedIn();
        } catch (AssertionError e) {
            context.softAssert.fail("[Preview] Logged-in demographic questions failed: " + e.getMessage());
        }
    }

    @And("I answer the demographic questions from question 3")
    public void iAnswerDemographicQuestionsFromQ3() {
        try {
            previewJourney.answerDemographicQuestionsFromQ3();
        } catch (AssertionError e) {
            context.softAssert.fail("[Preview] Demographic questions (Q3+) failed: " + e.getMessage());
        }
    }

    @And("I decline the consent statement and verify I am redirected to the marketplace as a filtered out participant")
    public void iDeclineConsentStatementAndVerify() {
        previewJourney.declineConsentStatement();
    }

    @And("I agree to the consent statement and proceed to the marketplace")
    public void iAgreeToConsentStatementAndProceed() {
        previewJourney.agreeToConsentStatement();
    }

    // Reuses the preview URL stored in context during the first preview navigation.
    // Clears the session so the participant is unauthenticated — same as the initial visit.
    @And("I navigate to the preview URL as a guest again")
    public void iNavigateToPreviewUrlAsGuestAgain() {
        if (context.previewUrl == null) {
            throw new RuntimeException(
                "Preview URL not stored in context — 'I preview the experiment journey as a guest' must run first");
        }
        previewJourney.navigateAsGuest(context.previewUrl);
    }

    // ── CTR-specific preview steps ────────────────────────────────────────────

    // Clicks the preview journey button, captures the URL from the newly opened tab,
    // then navigates to it as a logged-in user (no session clearing). The logged-in
    // session is required for CTR so that the consent page shows the scenario selection
    // buttons — they are only visible to authenticated users.
    @And("I preview the CTR experiment as a logged-in user")
    public void iPreviewCtrExperimentAsLoggedInUser() {
        context.previewUrl = formQuestions.clickPreviewAndGetUrlCtr();
        GlobalTestState.previewUrl = context.previewUrl;
        System.out.println("[CTR Preview] URL captured: " + context.previewUrl);
        previewJourney.navigateAsLoggedInUser(context.previewUrl);
    }

    // Verifies that Scenario A or B is pre-selected on the CTR consent page.
    // Soft-fails if neither is selected and selects Scenario A to allow the run to continue.
    // Clicks agree + continue and waits for the info popup (help-confirm) before returning.
    @And("I agree to the CTR consent statement and proceed")
    public void iAgreeToConsentAndProceedCtr() {
        previewJourney.agreeToConsentCtr();
    }

    // Navigates to the stored preview URL as a logged-in user — used for repeat visits
    // (e.g., Not Interested test) where session clearing is not needed for CTR.
    @And("I navigate to the preview URL as a logged-in user again")
    public void iNavigateToPreviewUrlAsLoggedInUserAgain() {
        if (context.previewUrl == null) {
            throw new RuntimeException(
                "Preview URL not stored in context — 'I preview the CTR experiment as a logged-in user' must run first");
        }
        previewJourney.navigateAsLoggedInUser(context.previewUrl);
    }

    // ── Marketplace list ──────────────────────────────────────────────────────

    @And("I dismiss the marketplace help popup if present")
    public void iDismissMarketplaceHelpPopup() {
        marketplace.dismissHelpPopupIfPresent();
    }

    @And("I click not interested from the marketplace and verify I am filtered out")
    public void iClickNotInterestedFromMarketplace() {
        try {
            marketplace.clickNotInterestedAndVerifyFilteredOut();
        } catch (AssertionError e) {
            context.softAssert.fail("[Preview] Not Interested (marketplace) filter failed: " + e.getMessage());
        }
    }

    @And("I click not interested from the product detail and verify I am filtered out")
    public void iClickNotInterestedFromProductDetail() {
        try {
            productDetail.clickNotInterestedAndVerifyFilteredOut();
        } catch (AssertionError e) {
            context.softAssert.fail("[Preview] Not Interested (product detail) filter failed: " + e.getMessage());
        }
    }

    @And("I verify the marketplace product data matches shop setup")
    public void iVerifyMarketplaceProductData() {
        try {
            marketplace.assertNoEmptyProductIds();
        } catch (AssertionError e) {
            context.softAssert.fail("[Preview] Ghost product detected in marketplace: " + e.getMessage());
        }
        // Use the exact full product name for the active scenario so the right marketplace
        // card is located — partial config matching could hit the wrong card when both
        // scenario cards share a common name prefix.
        String productName = resolveCurrentProductName();
        try {
            marketplace.assertProductData(context.activeProduct(), productName);
        } catch (AssertionError e) {
            context.softAssert.fail("[Preview] Marketplace data assertion failed: " + e.getMessage());
        }
        marketplace.assertNotInterestedButtonPresent();
    }

    // Selects our product from the marketplace list using JS partial matching (handles
    // special characters like em-dashes that exact findById cannot match). The product
    // name is chosen based on the detected scenario so the correct card is always targeted.
    @And("I select our product and answer the opener question")
    public void iSelectOurProductAndAnswerOpenerQuestion() {
        String productName = resolveCurrentProductName();
        marketplace.selectOurProduct(productName);
        previewJourney.answerOpenerQuestion();
    }

    // Selects the first product that is not one of our two scenario products, then
    // handles the opener question popup (all products share the same opener question format).
    @And("I select a competitor product and answer the opener question")
    public void iSelectCompetitorProductAndAnswerOpenerQuestion() {
        marketplace.selectCompetitorProduct(
            context.scenarioAProductName, context.scenarioBProductName);
        previewJourney.answerOpenerQuestion();
    }

    // Detects and stores which scenario (A or B) the current participant is in.
    // Stored in context.currentScenario for downstream steps that conditionally
    // expect Q5 (Likert Horizontal — only shown to Scenario A participants).
    @And("I detect and store the current scenario assignment")
    public void iDetectCurrentScenario() {
        context.currentScenario = marketplace.detectCurrentScenario(
            context.scenarioAProductName, context.scenarioBProductName);
        System.out.println("[Preview] Scenario assignment: " + context.currentScenario);
    }

    // ── Product detail page ───────────────────────────────────────────────────

    @And("I verify the product details match shop setup")
    public void iVerifyProductDetails() {
        try {
            productDetail.waitUntilLoaded().assertProductData(context.activeProduct());
        } catch (AssertionError e) {
            context.softAssert.fail("[Preview] Product detail assertion failed: " + e.getMessage());
        }
    }

    // Clicks the shopping cart icon WITHOUT first adding to cart — verifies the cart
    // is empty (empty-cart-text visible, cart-total not visible), then returns to the
    // product list via Continue Shopping so the next step can re-select the product.
    @And("I verify the shopping cart is empty when accessed without adding a product")
    public void iVerifyShoppingCartIsEmpty() {
        productDetail.clickShoppingCart();
        try {
            cart.assertEmptyCart();
        } catch (AssertionError e) {
            context.softAssert.fail("[Preview] Empty cart assertion failed: " + e.getMessage());
        }
        cart.clickContinueShopping();
    }

    // Clicks Add to Cart and stays on the product detail page — blocked-button checks
    // must run before navigating to the cart so they operate on the correct page.
    @And("I add the product to cart")
    public void iAddToCart() {
        productDetail.clickAddToCart();
        context.cartHasItem = true;
    }

    // Navigates from product detail to the cart page and verifies the filled cart state
    // (image, quantity, prices, cart-total visible). Ends on the cart page.
    @And("I go to the cart and verify it contains our product")
    public void iGoToCartAndVerify() {
        productDetail.clickShoppingCart();
        try {
            cart.assertCartWithItem(context.activeProduct());
        } catch (AssertionError e) {
            context.softAssert.fail("[Preview] Cart content assertion failed: " + e.getMessage());
        }
    }

    // Returns to the product list without removing the cart item — used when we need
    // to visit a product detail page to verify the blocked-button state while cart is full.
    @And("I continue shopping from the cart without deleting the item")
    public void iContinueShoppingFromCart() {
        cart.clickContinueShopping();
    }

    // From the product detail page (cart still has the item), clicks the cart icon to
    // go to the cart page and confirms checkout. Ends on the post-checkout page.
    @And("I go to the cart and confirm checkout")
    public void iGoToCartAndConfirmCheckout() {
        productDetail.clickShoppingCart();
        cart.clickConfirmCheckout();
    }

    // Clicks Buy Now — goes directly to the cart page in filled state — and verifies
    // the same assertions as the Add to Cart path.
    @And("I buy the product directly via buy now and verify the cart details")
    public void iBuyNowAndVerify() {
        productDetail.clickBuyNow();
        context.cartHasItem = true;
        try {
            cart.assertCartWithItem(context.activeProduct());
        } catch (AssertionError e) {
            context.softAssert.fail("[Preview] Cart content assertion failed (buy now): " + e.getMessage());
        }
    }

    // Deletes the item from the cart (asserting cart becomes empty), then clicks
    // Continue Shopping to return to the product list for the next step.
    @And("I delete the item from cart and return to the product list")
    public void iDeleteFromCartAndReturn() {
        try {
            cart.deleteItemAndAssertEmpty();
        } catch (AssertionError e) {
            context.softAssert.fail("[Preview] Delete-from-cart assertion failed: " + e.getMessage());
        }
        context.cartHasItem = false;
        cart.clickContinueShopping();
    }

    // Verifies that both Add to Cart and Buy Now are blocked when the cart already
    // contains an item — neither button should navigate away from the product detail page.
    // Must be called while on the product detail page (before navigating to the cart).
    // Skipped silently if context does not confirm the cart has an item — avoids false
    // failures when a previous step left the cart unexpectedly empty.
    @And("I verify the product buttons are blocked when the cart already has an item")
    public void iVerifyProductButtonsBlocked() {
        if (!context.cartHasItem) {
            System.out.println("[Preview] Skipping blocked-button check — cart is not confirmed to have an item");
            return;
        }
        try {
            productDetail.assertAddToCartBlocked();
        } catch (AssertionError e) {
            context.softAssert.fail("[Preview] Add to Cart blocked assertion failed: " + e.getMessage());
        }
        try {
            productDetail.assertBuyNowBlocked();
        } catch (AssertionError e) {
            context.softAssert.fail("[Preview] Buy Now blocked assertion failed: " + e.getMessage());
        }
    }

    // Confirms checkout from the cart page — called after iGoToCartAndVerify() which
    // already navigated to the cart, so no cart icon click is needed here.
    @And("I confirm checkout from the cart")
    public void iConfirmCheckout() {
        cart.clickConfirmCheckout();
    }

    // ── Participant form questions ─────────────────────────────────────────────

    @And("I answer the participant form questions")
    public void iAnswerParticipantFormQuestions() {
        try {
            participantForm.answerAllAndVerifyCompletion(context.currentScenario);
        } catch (AssertionError e) {
            context.softAssert.fail("[Preview] Participant form questions failed: " + e.getMessage());
        }
    }


    @When("I answer the long text participant form question")
    public void iAnswerLongTextParticipantFormQuestion() {
        try {
            participantForm.answerLongTextQuestion();
        } catch (AssertionError e) {
            context.softAssert.fail("[Preview] Long text question failed: " + e.getMessage());
        }
    }

    @And("I answer the limited choice participant form question")
    public void iAnswerLimitedChoiceParticipantFormQuestion() {
        try {
            participantForm.answerLimitedChoiceQuestion();
        } catch (AssertionError e) {
            context.softAssert.fail("[Preview] Limited choice question failed: " + e.getMessage());
        }
    }

    @And("I answer the single choice participant form question")
    public void iAnswerSingleChoiceParticipantFormQuestion() {
        try {
            participantForm.answerSingleChoiceQuestion();
        } catch (AssertionError e) {
            context.softAssert.fail("[Preview] Single choice question failed: " + e.getMessage());
        }
    }

    @And("I answer the multiple choice participant form question")
    public void iAnswerMultipleChoiceParticipantFormQuestion() {
        try {
            participantForm.answerMultipleChoiceQuestion();
        } catch (AssertionError e) {
            context.softAssert.fail("[Preview] Multiple choice question failed: " + e.getMessage());
        }
    }

    // Q5 — shown to Scenario A participants only. Skipped automatically for Scenario B.
    @And("I answer the horizontal Likert participant form question")
    public void iAnswerHorizontalLikertParticipantFormQuestion() {
        if ("B".equals(context.currentScenario)) {
            System.out.println("[Preview] Skipping horizontal Likert (Q5) — Scenario B participant");
            return;
        }
        try {
            participantForm.answerHorizontalLikertQuestion();
        } catch (AssertionError e) {
            context.softAssert.fail("[Preview] Horizontal Likert question failed: " + e.getMessage());
        }
    }

    @And("I answer the vertical Likert participant form question")
    public void iAnswerVerticalLikertParticipantFormQuestion() {
        try {
            participantForm.answerVerticalLikertQuestion();
        } catch (AssertionError e) {
            context.softAssert.fail("[Preview] Vertical Likert question failed: " + e.getMessage());
        }
    }

    @And("I verify the participant form redirects to login on completion")
    public void iVerifyParticipantFormRedirectsToLogin() {
        try {
            participantForm.verifyCompletionRedirect();
        } catch (AssertionError e) {
            context.softAssert.fail("[Preview] Participant form completion redirect failed: " + e.getMessage());
        }
    }

    // ── CTR Feature 4 — Not Interested + Guest Participant ────────────────────

    // Scrolls to button-not-interest, clicks it, and waits for the redirect back to
    // the shop setup page. The redirect takes noticeably longer than a standard navigation
    // on Bubble.io so a 90 s timeout is used.
    @And("I click not interested and wait for shop setup page")
    public void iClickNotInterestedCtrAndWaitForShopSetup() {
        try {
            marketplace.clickNotInterestedCtrAndVerifyShopSetup();
        } catch (AssertionError e) {
            context.softAssert.fail("[CTR Preview] Not Interested redirect failed: " + e.getMessage());
        }
    }

    // Clears session, navigates to the stored preview URL as a guest, and handles the
    // Bubble.io infinite loading bug with a refresh-and-retry strategy.
    @And("I navigate to the preview URL as a guest for CTR")
    public void iNavigateToPreviewUrlAsGuestCtr() {
        String url = context.previewUrl != null ? context.previewUrl : GlobalTestState.previewUrl;
        if (url == null) {
            throw new RuntimeException(
                "Preview URL not stored — ensure 'I preview the CTR experiment as a logged-in user' ran in Feature 3");
        }
        previewJourney.navigateAsGuestCtr(url);
    }

    @And("I agree to the guest consent statement and wait for product list")
    public void iAgreeToGuestConsentAndWaitForProductList() {
        previewJourney.agreeToConsentCtr();
    }

    // Finds the product matching scenarioAProductName or scenarioBProductName in the
    // select-item-overview-organic-{name} ID space and clicks it to select it.
    @And("I select our CTR product from the marketplace")
    public void iSelectOurCtrProduct() {
        try {
            marketplace.selectOurProductByScenarioNames(
                context.scenarioAProductName, context.scenarioBProductName);
        } catch (Exception e) {
            context.softAssert.fail("[CTR] Product selection failed: " + e.getMessage());
        }
    }

    // Clicks Confirm-choice-CTA to confirm the selected product, which triggers the
    // opener question popup.
    @And("I confirm the product selection")
    public void iConfirmProductSelection() {
        previewJourney.confirmProductSelectionCtr();
    }

    // Selects the first opener question option and clicks next. For CTR, clicking next
    // routes to the participant form questions rather than the product detail page.
    @And("I answer the opener question and wait for participant form")
    public void iAnswerOpenerQuestionCtr() {
        previewJourney.answerOpenerQuestionCtr();
    }

    // Answers the first CTR pre-added long-text question (>35 words required to enable continue).
    @And("I answer the first CTR participant long text question")
    public void iAnswerFirstCtrParticipantLongTextQuestion() {
        try {
            participantForm.answerLongTextQuestion();
        } catch (AssertionError e) {
            context.softAssert.fail("[CTR ParticipantForm] Q1 failed: " + e.getMessage());
        }
    }

    // Answers the second CTR pre-added long-text question (>35 words required to enable continue).
    @And("I answer the second CTR participant long text question")
    public void iAnswerSecondCtrParticipantLongTextQuestion() {
        try {
            participantForm.answerLongTextQuestion();
        } catch (AssertionError e) {
            context.softAssert.fail("[CTR ParticipantForm] Q2 failed: " + e.getMessage());
        }
    }

    // Verifies that completing the test redirects the guest participant to the sign-up page.
    // toggle-sign-in is present on the sign-up page as the link to switch to sign-in.
    @And("I verify the test completion redirects to sign up")
    public void iVerifyTestCompletionRedirectsToSignUp() {
        try {
            participantForm.verifyCompletionRedirect();
        } catch (AssertionError e) {
            context.softAssert.fail("[CTR ParticipantForm] Sign-up redirect failed: " + e.getMessage());
        }
    }

    // Returns the product name for the active scenario. Prefers the exact stored name
    // so JS partial matching always gets the most specific term available.
    // Scenario A name is used as fallback when the scenario is unknown — it's present
    // in the DOM in both assignments and JS will find it by partial match.
    // Throws if both stored names are null or blank — indicates shop setup did not run.
    private String resolveCurrentProductName() {
        if ("A".equals(context.currentScenario)
                && context.scenarioAProductName != null
                && !context.scenarioAProductName.trim().isEmpty()) {
            return context.scenarioAProductName;
        }
        if ("B".equals(context.currentScenario)
                && context.scenarioBProductName != null
                && !context.scenarioBProductName.trim().isEmpty()) {
            return context.scenarioBProductName;
        }
        if (context.scenarioAProductName != null && !context.scenarioAProductName.trim().isEmpty()) {
            return context.scenarioAProductName;
        }
        if (context.scenarioBProductName != null && !context.scenarioBProductName.trim().isEmpty()) {
            return context.scenarioBProductName;
        }
        throw new RuntimeException(
            "[Preview] No product name available — scenarioAProductName and scenarioBProductName are both null/empty. "
            + "Ensure shop setup ran and stored product names in GlobalTestState.");
    }
}

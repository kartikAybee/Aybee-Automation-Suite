package com.aybee.steps;

import com.aybee.context.ScenarioContext;
import com.aybee.pages.FormQuestionsPage;
import com.aybee.pages.NotInterestedQuestionsPage;
import com.aybee.pages.PreviewJourneyPage;
import com.aybee.pages.ProductDetailPage;
import com.aybee.pages.ShopSetupPage;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.testng.Assert;

// Not-Interested preview journey — segregated from the form-questions setup (case4).
// Uses the preview URL captured in case4 (persisted across features via GlobalTestState).
public class NotInterestedPreviewSteps {

    private final ScenarioContext context;
    private final PreviewJourneyPage preview = new PreviewJourneyPage();
    private final ProductDetailPage productDetail = new ProductDetailPage();
    private final NotInterestedQuestionsPage notInterested = new NotInterestedQuestionsPage();
    private final ShopSetupPage shopSetup = new ShopSetupPage();
    private final FormQuestionsPage formQuestions = new FormQuestionsPage();

    public NotInterestedPreviewSteps(ScenarioContext context) {
        this.context = context;
    }

    @When("I open the captured preview URL as a logged-in user")
    public void iOpenTheCapturedPreviewUrlAsLoggedInUser() {
        // Single, deterministic entry: always open the preview via the Form Questions page's Preview
        // button. The browser is reused across scenarios, so a prior owner scenario may have left us on
        // the Shop Setup page — advance to Form Questions first. Clicking Preview mints a FRESH preview
        // URL every time, so we never reuse a link a previous scenario already consumed.
        if (shopSetup.isOnShopSetup()) {
            System.out.println("[Preview] On Shop Setup — advancing to Form Questions before previewing");
            shopSetup.proceedToFormQuestions();
        }
        formQuestions.waitUntilReady();
        String url = formQuestions.clickPreviewAndGetUrl();
        context.previewUrl = url;
        boolean reached = preview.navigateAsLoggedInUser(url);
        Assert.assertTrue(reached,
            "Demographic questions never appeared after opening a fresh preview URL from the Form Questions page");
    }

    @And("I answer the gender and age demographics")
    public void iAnswerTheGenderAndAgeDemographics() {
        preview.answerDemographicsGenderAndAge();
    }

    @And("I agree to the consent statement")
    public void iAgreeToTheConsentStatement() {
        preview.agreeToConsentStatement();
    }

    @And("I dismiss the help popup on the product page")
    public void iDismissTheHelpPopup() {
        preview.dismissHelpPopupIfPresent();
    }

    @And("I close the rating overlay on the product detail page")
    public void iCloseTheRatingOverlay() {
        productDetail.waitUntilLoaded();
        productDetail.closeRatingOverlay();
    }

    @And("I click Not Interested")
    public void iClickNotInterested() {
        productDetail.clickNotInterested();
    }

    // "No Buying Intent" path — same question flow, triggered from the rating overlay directly
    // (no overlay-close step needed).
    @And("I click No Buying Intent")
    public void iClickNoBuyingIntent() {
        productDetail.clickNoBuyingIntent();
    }

    @Then("I should answer all Not-Interested questions and be redirected to the Shop Setup page")
    public void iShouldAnswerAllNotInterestedQuestions() {
        notInterested.answerNotInterestedQuestions();
    }
}

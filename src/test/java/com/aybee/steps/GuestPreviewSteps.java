package com.aybee.steps;

import com.aybee.context.ScenarioContext;
import com.aybee.pages.NotInterestedQuestionsPage;
import com.aybee.pages.PreviewJourneyPage;
import com.aybee.pages.ProductDetailPage;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.testng.Assert;

// Guest preview journey — clears the session and runs as an unauthenticated participant (all 8
// demographics), then verifies the product detail page against the assigned scenario's snapshot.
// Reuses "I agree to the consent statement" and "I dismiss the help popup on the product page"
// from NotInterestedPreviewSteps (shared Cucumber glue).
public class GuestPreviewSteps {

    private final ScenarioContext context;
    private final PreviewJourneyPage preview = new PreviewJourneyPage();
    private final ProductDetailPage productDetail = new ProductDetailPage();
    private final NotInterestedQuestionsPage questions = new NotInterestedQuestionsPage();

    public GuestPreviewSteps(ScenarioContext context) {
        this.context = context;
    }

    @When("I open the captured preview URL as a guest")
    public void iOpenTheCapturedPreviewUrlAsGuest() {
        Assert.assertTrue(context.previewUrl != null && !context.previewUrl.isEmpty(),
            "No preview URL available — the experiment setup case (case3) must run first");
        preview.navigateAsGuest(context.previewUrl);
    }

    @When("I answer all guest demographic questions")
    public void iAnswerAllGuestDemographicQuestions() {
        preview.answerAllDemographicQuestions();
    }

    @Then("I verify the product detail page matches the assigned scenario")
    public void iVerifyTheProductDetailMatchesTheAssignedScenario() {
        productDetail.verifyProductMatchesAssignedScenario(context);
    }

    // After the detail reads, handle the timed buy-now popup: wait for it, close the slider, reopen it.
    @Then("I verify the buy-now popup appears, then closes and reopens the slider")
    public void iVerifyTheBuyNowPopupCloseAndReopen() {
        productDetail.verifyBuyPopupCloseAndReopen();
    }

    @When("I move the decision slider, submit Buy Now, and land on the question page")
    public void iMoveSliderSubmitBuyNowAndLandOnQuestions() {
        productDetail.rateAndBuyNow(65);
    }

    // case6 completion: the guest answers our 4 manually-added Split Test questions (the only
    // questions, since defaults are stripped) and the survey completes.
    @Then("I answer all the manually added split test questions as a guest")
    public void iAnswerAllTheSplitTestQuestionsAsGuest() {
        questions.answerSplitTestQuestionsAsGuest();
    }
}

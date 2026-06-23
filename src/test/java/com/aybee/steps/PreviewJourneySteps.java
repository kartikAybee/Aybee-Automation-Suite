package com.aybee.steps;

import com.aybee.context.ScenarioContext;
import com.aybee.pages.PreviewJourneyPage;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Then;

public class PreviewJourneySteps {

    private final ScenarioContext context;
    private final PreviewJourneyPage page = new PreviewJourneyPage();

    public PreviewJourneySteps(ScenarioContext context) {
        this.context = context;
    }

    // Logged-in preview pass: accept gender + age + consent, click Not Interested, and verify
    // the redirect back to the asset-upload step (qat_assets_next_button).
    @Then("I preview as a logged-in user and decline via Not Interested")
    public void iPreviewLoggedInAndDeclineViaNotInterested() {
        page.previewLoggedInAndClickNotInterested(context.previewUrl, context.softAssert);
    }

    // Hand-off to the guest journeys: clear the session and re-open the preview URL as a guest.
    @And("I clear the session and open the preview as a guest")
    public void iClearSessionAndOpenPreviewAsGuest() {
        page.navigateAsGuest(context.previewUrl);
    }

    // Guest pass: every demographic question is shown — answer all in order, asserting each.
    @And("I answer all demographic questions in order as a guest")
    public void iAnswerAllDemographicsAsGuest() {
        page.answerAllDemographicsAsGuest(context.softAssert);
    }

    // Guest pass: agree to the consent statement, which leads to the QAT selection page.
    @And("I agree to the consent statement as a guest")
    public void iAgreeToConsentAsGuest() {
        page.agreeToConsent(context.softAssert);
    }

    // Verify all uploaded creatives (captured at upload time) are shown on the selection page.
    @Then("all uploaded creatives should be displayed on the QAT selection page")
    public void allUploadedCreativesShouldBeDisplayed() {
        page.verifyAllCreativesDisplayed(context.uploadedCreativeSrc, context.softAssert);
    }

    // Pick a version (choose-version{X}), give the >50-word reason, and advance to the questions.
    @And("I select creative version {string} and continue to the questions")
    public void iSelectVersionAndContinue(String version) {
        context.selectedCreativeVersion = version;
        page.selectVersionAndContinue(version, context.softAssert);
    }

    // Answer the revealed survey questions in order, with clicks (same handling as msjourney),
    // verifying the creative shown on each question against the srcs captured at upload time.
    @Then("I answer all QAT survey questions in order")
    public void iAnswerAllSurveyQuestions() {
        page.answerAllSurveyQuestions(
            context.uploadedCreativeSrc, context.selectedCreativeVersion,
            context.q1SurveyAnswer, context.softAssert);
    }
}

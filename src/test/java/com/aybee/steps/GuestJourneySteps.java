package com.aybee.steps;

import com.aybee.context.ScenarioContext;
import com.aybee.pages.ParticipantFormPage;
import com.aybee.pages.PreviewJourneyPage;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.testng.Assert;

// Guest participant journey through the Questionnaire preview — a non-logged-in user answers the
// demographic questions, the consent statement, and every form question set up in case3.
// Reuses PreviewJourneyPage (demographics + consent, shared across suites) and the questionnaire
// ParticipantFormPage (form-question answering with the standard IDs/xpaths).
public class GuestJourneySteps {

    private final ScenarioContext context;
    private final PreviewJourneyPage previewJourney = new PreviewJourneyPage();
    private final ParticipantFormPage participantForm = new ParticipantFormPage();

    public GuestJourneySteps(ScenarioContext context) {
        this.context = context;
    }

    @When("I open the questionnaire preview as a guest")
    public void iOpenTheQuestionnairePreviewAsAGuest() {
        Assert.assertTrue(context.previewUrl != null && !context.previewUrl.isEmpty(),
                "No preview URL available — the experiment setup case (case3) must run first");
        previewJourney.navigateAsGuest(context.previewUrl);
    }

    @And("I answer all guest demographic questions")
    public void iAnswerAllGuestDemographicQuestions() {
        try {
            previewJourney.answerAllDemographicQuestions();
        } catch (AssertionError e) {
            context.softAssert.fail("[Guest] Demographic questions failed: " + e.getMessage());
        }
    }

    @And("I agree to the consent statement")
    public void iAgreeToTheConsentStatement() {
        previewJourney.agreeToConsentStatement();
    }

    @And("I answer all the questionnaire form questions")
    public void iAnswerAllTheQuestionnaireFormQuestions() {
        try {
            participantForm.answerAllAndVerifyCompletion();
        } catch (AssertionError e) {
            context.softAssert.fail("[Guest] Form question answering failed: " + e.getMessage());
        }
    }

    @Then("the participant journey should redirect to sign in on completion")
    public void theParticipantJourneyShouldRedirectToSignIn() {
        participantForm.verifyCompletionRedirect();
    }
}

package com.aybee.steps;

import com.aybee.context.ScenarioContext;
import com.aybee.pages.ExperimentSettingsPage;
import com.aybee.pages.NewExperimentPopup;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Then;
import org.testng.Assert;

// Step definitions for the Marketing & Ads → Questionnaire experiment creation flow.
// The navigation steps ("I navigate to the experiments page", "I click add new experiment") live
// in ExperimentsSteps — Cucumber shares step definitions globally across glue classes.
public class QuestionnaireCreationSteps {

    private final ScenarioContext context;
    private final NewExperimentPopup popup = new NewExperimentPopup();

    public QuestionnaireCreationSteps(ScenarioContext context) {
        this.context = context;
    }

    @And("I select the Marketing and Ads use case")
    public void iSelectTheMarketingAndAdsUseCase() {
        popup.selectMarketingAndAdsUseCase();
    }

    @Then("the Questionnaire test type option should be available")
    public void theQuestionnaireTestTypeShouldBeAvailable() {
        Assert.assertTrue(popup.isQuestionnaireTypeAvailable(),
                "Questionnaire test type card was not available in the Marketing & Ads section");
    }

    @And("I scroll to and select the Questionnaire test type")
    public void iScrollToAndSelectTheQuestionnaireTestType() {
        popup.selectQuestionnaire();
    }

    @And("I select United States as the target market")
    public void iSelectUnitedStatesAsTheTargetMarket() {
        popup.selectUnitedStates();
    }

    @Then("the study objective step should be loaded")
    public void theStudyObjectiveStepShouldBeLoaded() {
        Assert.assertTrue(new ExperimentSettingsPage().isLoaded(),
                "Experiment settings step (Study Objective) did not load — "
                        + "the richtext-editor-0 study objective editor never appeared");
    }
}

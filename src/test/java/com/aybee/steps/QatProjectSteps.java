package com.aybee.steps;

import com.aybee.context.ScenarioContext;
import com.aybee.pages.QatProjectPage;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

public class QatProjectSteps {

    private final ScenarioContext context;
    private final QatProjectPage qatProjectPage = new QatProjectPage();

    public QatProjectSteps(ScenarioContext context) {
        this.context = context;
    }

    @When("I upload the Scenario A asset image")
    public void iUploadTheScenarioAAssetImage() {
        // Baseline before the first upload so the new creative is detected as "new".
        qatProjectPage.snapshotCreativeBaseline();
        String src = qatProjectPage.uploadAssetImageA();
        context.softAssert.assertNotNull(src,
            "[QatProject] Could not capture version A creative image src after upload");
        context.uploadedCreativeSrc.put("a", src);
        System.out.println("[QatProject] Stored version a creative src: " + src);
    }

    @When("I upload the Scenario B asset image")
    public void iUploadTheScenarioBAssetImage() {
        String src = qatProjectPage.uploadAssetImageB();
        context.softAssert.assertNotNull(src,
            "[QatProject] Could not capture version B creative image src after upload");
        context.uploadedCreativeSrc.put("b", src);
        System.out.println("[QatProject] Stored version b creative src: " + src);
    }

    @Then("both Scenario asset images should be uploaded")
    public void bothScenarioAssetImagesShouldBeUploaded() {
        context.softAssert.assertTrue(qatProjectPage.isAssetAUploaded(),
            "[QatProject] Scenario A upload not confirmed — delete-version-A did not appear");
        context.softAssert.assertTrue(qatProjectPage.isAssetBUploaded(),
            "[QatProject] Scenario B upload not confirmed — delete-version-B did not appear");
    }

    @When("I proceed to set up form questions")
    public void iProceedToSetUpFormQuestions() {
        qatProjectPage.proceedToFormQuestions();
    }

    @Then("the form questions add-question button should be clickable")
    public void theFormQuestionsAddQuestionButtonShouldBeClickable() {
        context.softAssert.assertTrue(qatProjectPage.isFormQuestionsStageReady(),
            "[QatProject] Form questions stage not ready — " +
            "newproject_formquestions_addquestion_button did not become clickable");
    }
}

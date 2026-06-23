package com.aybee.steps;

import com.aybee.context.ScenarioContext;
import com.aybee.pages.FormQuestionsPage;
import io.cucumber.java.en.And;

// ── D2C Form Questions ────────────────────────────────────────────────────────
// The D2C test type pre-adds 3 questions automatically (indices 1–3):
//
//   Q1 — "Why did you choose this product?"
//         Show: all products; the bought product is displayed beside the question at preview time.
//         Type: Long text
//         Filter: none (all participants see this).
//
//   Q2 — "Why did you decide against this product?"
//         Show: only displayed when the participant did NOT select our product
//               (Scenario A product if assigned to A; Scenario B product if assigned to B).
//               Our product is shown beside the question at preview time.
//         Type: Long text
//         Filter: Scenario section → select only the scenario's product so the question
//                 is suppressed for participants who bought our product.
//
//   Q3 — "What are your top 3 criteria when choosing a product in this category?"
//         Show: all products; all products are displayed beside the question at preview time.
//         Type: Long text
//         Filter: none (all participants see this).
//
// Manually added questions start at FIRST_QUESTION_INDEX (4):
//
//   Q4 — "Share your overall thoughts on the product shown to you in this test."
//         Show: a_b_test_product — renders a disabled reference dropdown showing Scenario A's
//               product name; at preview time the platform shows the participant's actual
//               assigned scenario product (A or B), not necessarily Scenario A.
//         Type: Long text
//         Filter: none.
//
// All question type methods (long text, limited choice, single choice, multiple choice,
// likert scale, filter sidebar) are retained below for future reference.

public class FormQuestionsSteps {

    private final ScenarioContext context;
    private final FormQuestionsPage page = new FormQuestionsPage();

    public FormQuestionsSteps(ScenarioContext context) {
        this.context = context;
    }

    // ── Initial questions verification ────────────────────────────────────────
    // Waits for the Add Question button to be clickable, reloads, waits again,
    // and asserts that exactly 3 pre-existing questions (indices 1–3) are present.

    @And("I verify the initial form questions are loaded")
    public void iVerifyTheInitialFormQuestionsAreLoaded() {
        page.verifyInitialQuestionsLoaded();
    }

    // ── Q4 — A/B test product long text question ──────────────────────────────

    @And("I add an A\\/B test product long text form question")
    public void iAddAbTestProductLongTextFormQuestion() {
        page.addNewQuestion(FormQuestionsPage.FIRST_QUESTION_INDEX)
            .enterQuestionText(FormQuestionsPage.FIRST_QUESTION_INDEX,
                "Share your overall thoughts on the product shown to you in this test.")
            .selectQuestionType(FormQuestionsPage.FIRST_QUESTION_INDEX, "long_text")
            .selectShowAbTestProductAndWait(FormQuestionsPage.FIRST_QUESTION_INDEX);
    }

    // ── Capture preview URL ───────────────────────────────────────────────────
    // Clicks the preview button, captures the URL from the new tab, and stores it
    // in context. Navigation to the preview URL (logged-in or guest) is handled by
    // PreviewJourneySteps in the subsequent PreviewJourney feature.

    @And("I capture the experiment preview URL")
    public void iCaptureTheExperimentPreviewUrl() {
        String previewUrl = page.clickPreviewAndGetUrl();
        if (page.wasIncompleteToastSeen()) {
            System.out.println("[FormQuestions] WARNING: Incomplete-fields toast appeared " +
                "but was handled — preview URL captured successfully");
        }
        context.previewUrl = previewUrl;
        System.out.println("[FormQuestions] Preview URL captured: " + previewUrl);
    }
}

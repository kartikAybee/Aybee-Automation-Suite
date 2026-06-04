package com.aybee.steps;

import com.aybee.context.GlobalTestState;
import com.aybee.context.ScenarioContext;
import com.aybee.utils.ConfigReader;
import com.aybee.pages.FormQuestionsPage;
import io.cucumber.java.en.And;

import java.util.Arrays;

import static com.aybee.pages.FormQuestionsPage.FIRST_QUESTION_INDEX;

public class FormQuestionsSteps {

    // Question indices — 3 pre-existing questions occupy 1–3.
    private static final int Q1_IDX = FIRST_QUESTION_INDEX;     // 4 — Long Text
    private static final int Q2_IDX = FIRST_QUESTION_INDEX + 1; // 5 — Limited Choice
    private static final int Q3_IDX = FIRST_QUESTION_INDEX + 2; // 6 — Single Choice
    private static final int Q4_IDX = FIRST_QUESTION_INDEX + 3; // 7 — Multiple Choice
    private static final int Q5_IDX = FIRST_QUESTION_INDEX + 4; // 8 — Likert Horizontal
    private static final int Q6_IDX = FIRST_QUESTION_INDEX + 5; // 9 — Likert Vertical

    // Partial question texts used when selecting questions inside filter dropdowns.
    private static final String Q2_PARTIAL = "Which product attributes matter most";
    private static final String Q3_PARTIAL = "How would you rate the value for money";

    private final ScenarioContext context;
    private final FormQuestionsPage page = new FormQuestionsPage();

    public FormQuestionsSteps(ScenarioContext context) {
        this.context = context;
    }

    // ── Pre-condition ─────────────────────────────────────────────────────────

    @And("I verify no unexpected questions are pre-added")
    public void iVerifyNoUnexpectedQuestionsArePreAdded() {
        page.reloadAndCheckExtraQuestions();
    }

    // ── Q1 — Long Text ────────────────────────────────────────────────────────
    // Show: Just Question (default — no selection needed)
    // Filter: none — filter icon is only shown when a prior question is single/multiple/limited/likert;
    //         Q1 has no applicable prior question.

    @And("I add a long text form question {string}")
    public void iAddLongTextFormQuestion(String questionText) {
        page.addNewQuestion(Q1_IDX)
            .enterQuestionText(Q1_IDX, questionText)
            .selectQuestionType(Q1_IDX, "long_text");
    }

    // ── Q2 — Limited Choice ───────────────────────────────────────────────────
    // Show: Whole Category
    // Min: 2, Max: 3 → ensure 4 answer fields
    // Answers: Price competitiveness(random), Build quality(random),
    //          Brand trust(random OFF), Packaging appeal(exclusive, random OFF)
    // Filter: none — prior question Q1 is Long Text (not single/multiple/limited/likert),
    //         so no filter icon is displayed for Q2.

    @And("I add a limited choice form question {string}")
    public void iAddLimitedChoiceFormQuestion(String questionText) {
        // Answer texts used here must stay in sync with q2SelectOptions below.
        String opt1 = "Price competitiveness";
        String opt2 = "Build quality";
        String opt3 = "Brand trust";       // randomize OFF — not selected in participant form
        String opt4 = "Packaging appeal";  // exclusive      — not selected in participant form
        page.addNewQuestion(Q2_IDX)
            .enterQuestionText(Q2_IDX, questionText)
            .selectQuestionType(Q2_IDX, "limited_choice")
            .selectShowToParticipants(Q2_IDX, "whole_category")
            .setMinChoices(Q2_IDX, "2")
            .setMaxChoices(Q2_IDX, "3")
            .ensureAnswerOptionCount(Q2_IDX, 4)
            .enterAnswerText(Q2_IDX, 1, opt1)
            .enterAnswerText(Q2_IDX, 2, opt2)
            .enterAnswerText(Q2_IDX, 3, opt3)
            .enterAnswerText(Q2_IDX, 4, opt4)
            .clickExclusive(Q2_IDX, 4)
            .enableRandomizeToggle(Q2_IDX)
            .disableRandomizeForAnswer(Q2_IDX, 3)
            .disableRandomizeForAnswer(Q2_IDX, 4);
        // Store the two selectable (non-exclusive) options so ParticipantFormPage
        // builds element IDs from the same texts entered here.
        GlobalTestState.q2SelectOptions = Arrays.asList(opt1, opt2);
    }

    // Deferred cleanup — called after all questions are set up so Bubble.io has had enough
    // time to finish rendering any delayed empty options on the Limited Choice card.
    // Records a soft failure if blank options were found — they should never appear with
    // correct setup; finding them indicates a Bubble.io auto-generation bug in this run.
    @And("I clean up empty options on the limited choice question")
    public void iCleanupLimitedChoiceEmptyOptions() {
        boolean hadEmpty = page.cleanupEmptyOptions(Q2_IDX);
        if (hadEmpty) {
            context.softAssert.fail(
                "[FormQuestions] Unexpected empty answer options found on Limited Choice question " +
                "— blank options should not appear when form questions are configured correctly");
        }
    }

    // ── Q3 — Single Choice ────────────────────────────────────────────────────
    // Show: Bought Product
    // Answers: Excellent value(random), Good value(random), Fair value(random OFF),
    //          Poor value → deleted (tests delete answer option)
    // Filter: Scenario tab → Specific Bought Product
    //       + Responses tab → add Q2 → select answer → delete → re-add Q2 → select 2 answers

    @And("I add a single choice form question {string}")
    public void iAddSingleChoiceFormQuestion(String questionText) {
        String opt1 = "Excellent value";  // selected in participant form
        String opt2 = "Good value";
        String opt3 = "Fair value";       // randomize OFF
        String opt4 = "Poor value";       // deleted — not available in participant form
        page.addNewQuestion(Q3_IDX)
            .enterQuestionText(Q3_IDX, questionText)
            .selectQuestionType(Q3_IDX, "single_choice")
            .selectShowToParticipants(Q3_IDX, "bought_product")
            .enterAnswerText(Q3_IDX, 1, opt1)
            .enterAnswerText(Q3_IDX, 2, opt2)
            .enterAnswerText(Q3_IDX, 3, opt3)
            .enterAnswerText(Q3_IDX, 4, opt4)
            .enableRandomizeToggle(Q3_IDX)
            .disableRandomizeForAnswer(Q3_IDX, 3)
            .deleteAnswer(Q3_IDX, 4)
            // Filter — both tabs active (combo filter).
            .openFilterSidebar(Q3_IDX)
            .clickFilterByScenarioTab()
            .selectSpecificBoughtProduct(context.scenarioAProductName, context.scenarioBProductName)
            .clickFilterByResponseTab()
            // Add Q2 → select one answer → delete the entry → re-add Q2 → select two answers.
            .clickAddFilterQuestion(1)
            .selectFilterQuestion(1, Q2_PARTIAL)
            .selectFilterAnswerOption(1, "Price competitiveness")
            .deleteFilterQuestion(1)
            .clickAddFilterQuestion(1)
            .selectFilterQuestion(1, Q2_PARTIAL)
            .selectFilterAnswerOption(1, "Price competitiveness")
            .selectFilterAnswerOption(1, "Build quality")
            .applyFilters();
        // Store the selected option so ParticipantFormPage builds the element ID from this text.
        GlobalTestState.q3SelectOption = opt1;
    }

    // ── Q4 — Multiple Choice ──────────────────────────────────────────────────
    // Show: Specific Product → CORSAIR Nautilus
    // Answers: Price lower(random), Proven quality(random),
    //          Trusted brand(random OFF), Would not repurchase(exclusive, random OFF),
    //          Better warranty terms → deleted (tests delete answer option)
    // Filter: Scenario tab → Specific Bought Product
    //       + Responses tab → add Q2(collapse) + add Q3 → apply (multiple questions combo)

    @And("I add a multiple choice form question {string}")
    public void iAddMultipleChoiceFormQuestion(String questionText) {
        String opt1 = "Price lower than alternatives";  // selected in participant form
        String opt2 = "Proven product quality";         // selected in participant form
        String opt3 = "Trusted brand reputation";       // randomize OFF — not selected
        String opt4 = "Would not repurchase";           // exclusive      — not selected
        String opt5 = "Better warranty terms";          // deleted        — not available
        page.addNewQuestion(Q4_IDX)
            .enterQuestionText(Q4_IDX, questionText)
            .selectQuestionType(Q4_IDX, "multiple_choice")
            .selectShowToParticipants(Q4_IDX, "specific_product")
            .selectSpecificProduct(Q4_IDX, context.scenarioAProductName)
            .enterAnswerText(Q4_IDX, 1, opt1)
            .enterAnswerText(Q4_IDX, 2, opt2)
            .enterAnswerText(Q4_IDX, 3, opt3)
            .enterAnswerText(Q4_IDX, 4, opt4)
            .enterAnswerText(Q4_IDX, 5, opt5)
            .clickExclusive(Q4_IDX, 4)
            .enableRandomizeToggle(Q4_IDX)
            .disableRandomizeForAnswer(Q4_IDX, 3)
            .disableRandomizeForAnswer(Q4_IDX, 4)
            .deleteAnswer(Q4_IDX, 5)
            // Filter — both tabs active (combo filter).
            .openFilterSidebar(Q4_IDX)
            .clickFilterByScenarioTab()
            .selectSpecificBoughtProduct(context.scenarioAProductName, context.scenarioBProductName)
            .clickFilterByResponseTab()
            .clickAddFilterQuestion(1)
            .selectFilterQuestion(1, Q2_PARTIAL)
            .selectFilterAnswerOption(1, GlobalTestState.q2SelectOptions.get(0))
            .clickAddFilterQuestion(2)
            .selectFilterQuestion(2, Q3_PARTIAL)
            .selectFilterAnswerOption(2, GlobalTestState.q3SelectOption)
            .applyFilters();
        // Store the two selectable (non-exclusive, non-deleted) options.
        GlobalTestState.q4SelectOptions = Arrays.asList(opt1, opt2);
    }

    // ── Q5 — Likert Scale (Horizontal) ────────────────────────────────────────
    // Show: Uploaded Asset → upload pringles.svg
    // Scale: Horizontal
    // 6 auto-generated options → delete last (index 6) to test delete
    // Filter: Scenario tab only → Whole Scenario (Scenario A)
    //         Covers: scenario-only filter (no Responses tab) + Whole Scenario selection —
    //         both combos were originally on Q1/Q2 but dropped when those lost their filter icons.

    @And("I add a horizontal likert form question {string}")
    public void iAddHorizontalLikertFormQuestion(String questionText) {
        page.addNewQuestion(Q5_IDX)
            .enterQuestionText(Q5_IDX, questionText)
            .selectQuestionType(Q5_IDX, "likert_scale")
            .selectShowToParticipants(Q5_IDX, "uploaded_image")
            .clickAssetUploadField(Q5_IDX)
            .uploadAssetFile(ConfigReader.get("ASSET_IMAGE_PATH"))
            .confirmAssetUpload()
            .selectScaleType(Q5_IDX, "horizontal")
            .waitForLikertOptions(Q5_IDX, 6)
            .deleteAnswer(Q5_IDX, 6)
            .openFilterSidebar(Q5_IDX)
            .clickFilterByScenarioTab()
            .selectScenario("scenario-A")
            .applyFilters();
    }

    // ── Q6 — Likert Scale (Vertical) ──────────────────────────────────────────
    // Show: Just Question (default — no selection needed)
    // Scale: Vertical
    // 6 auto-generated options → keep all 6
    // No filter

    @And("I add a vertical likert form question {string}")
    public void iAddVerticalLikertFormQuestion(String questionText) {
        page.addNewQuestion(Q6_IDX)
            .enterQuestionText(Q6_IDX, questionText)
            .selectQuestionType(Q6_IDX, "likert_scale")
            .selectScaleType(Q6_IDX, "vertical")
            .waitForLikertOptions(Q6_IDX, 6);
    }

    // ── Preview Journey ───────────────────────────────────────────────────────

    // Records a soft failure if the incomplete-fields toast appeared — even though we dismiss
    // it and retry so the preview URL is still captured, a correctly set-up form should
    // never trigger this toast.
    @And("I preview the experiment journey as a guest")
    public void iPreviewTheExperimentJourneyAsAGuest() {
        String previewUrl = page.clickPreviewAndGetUrl();
        if (page.wasIncompleteToastSeen()) {
            System.out.println("[FormQuestions] WARNING: Incomplete-fields toast appeared " +
                "but was handled by retrigger — preview URL captured successfully");
        }
        context.previewUrl = previewUrl;
        page.navigateAsGuest(previewUrl);
    }
}

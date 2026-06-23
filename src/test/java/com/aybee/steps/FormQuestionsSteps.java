package com.aybee.steps;

import com.aybee.context.ScenarioContext;
import com.aybee.pages.FormQuestionsPage;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Then;

import static com.aybee.pages.FormQuestionsPage.FIRST_QUESTION_INDEX;

public class FormQuestionsSteps {

    // ── QAT Show-to-Participants coverage matrix ────────────────────────────────
    // Only the Show options EXCLUSIVE to QAT are exercised here. "just_question" and
    // "uploaded_image" behave exactly as in msjourney and are covered by that suite, so
    // they are intentionally not repeated. Question types / answer mechanics are likewise
    // only used as scaffolding (we need choice-based questions to drive the filter).
    //
    //  Q  | idx | Type            | Show to Participants | Subchoice               | Filter (response, single-section)     | Deferred verification (at preview)
    // ----+-----+-----------------+----------------------+-------------------------+---------------------------------------+------------------------------------------
    //  Q1 |  1  | single_choice   | all_creatives        | none (both images shown)| none — first added, no prior question | both uploaded creatives displayed
    //  Q2 |  2  | single_choice   | top_1_choice         | none (top-ranked image) | on Q1 (choice-based ✓)                | the top-choice creative displayed
    //  Q3 |  3  | multiple_choice | specific_creative    | version dropdown (b)    | on Q1                                 | selected version (b) creative displayed
    //
    // Q1 is choice-based so it serves as the filter SOURCE for Q2/Q3. The QAT filter
    // requires the source question to be choice-based — Likert questions cannot be filtered on.
    //
    // Each Show selection is recorded into the ScenarioContext at setup time. The displayed
    // creative images are NOT verified here — that happens later, at preview time, by reading
    // these recorded selections (see memory: project_qat_creative_verification).

    private static final int Q1_IDX = FIRST_QUESTION_INDEX;     // 1 — all_creatives
    private static final int Q2_IDX = FIRST_QUESTION_INDEX + 1; // 2 — top_1_choice
    private static final int Q3_IDX = FIRST_QUESTION_INDEX + 2; // 3 — specific_creative

    // Partial of Q1's question text, used to pick Q1 inside the filter-question dropdown.
    private static final String Q1_PARTIAL = "rate the overall appeal";
    // Q1 answer option reused as the filter answer for Q2/Q3.
    private static final String Q1_FILTER_ANSWER = "Strongly appealing";

    private final ScenarioContext context;
    private final FormQuestionsPage page = new FormQuestionsPage();

    public FormQuestionsSteps(ScenarioContext context) {
        this.context = context;
    }

    // ── Q1 — Show: All Creatives ────────────────────────────────────────────────
    @And("I add a form question showing all creatives {string}")
    public void iAddAllCreativesQuestion(String questionText) {
        page.addNewQuestion(Q1_IDX)
            .enterQuestionText(Q1_IDX, questionText)
            .selectQuestionType(Q1_IDX, "single_choice")
            .selectShowToParticipants(Q1_IDX, "all_creatives")
            .enterAnswerText(Q1_IDX, 1, Q1_FILTER_ANSWER)
            .enterAnswerText(Q1_IDX, 2, "Not appealing");
        // Record for preview-time verification: this question should display BOTH uploaded
        // creatives (version a AND b).
        context.recordShowSelection(Q1_IDX, questionText, "all_creatives", null);
    }

    // ── Q2 — Show: Top 1 Choice + response filter on Q1 ─────────────────────────
    @And("I add a form question showing the top choice creative {string}")
    public void iAddTopChoiceQuestion(String questionText) {
        page.addNewQuestion(Q2_IDX)
            .enterQuestionText(Q2_IDX, questionText)
            .selectQuestionType(Q2_IDX, "single_choice")
            .selectShowToParticipants(Q2_IDX, "top_1_choice")
            .enterAnswerText(Q2_IDX, 1, "Yes, it stood out")
            .enterAnswerText(Q2_IDX, 2, "No difference")
            // QAT filter — single section (responses only), no tabs. Q1 is choice-based.
            .openFilterSidebar(Q2_IDX)
            .clickAddFilterQuestion(1)
            .selectFilterQuestion(1, Q1_PARTIAL)
            .selectFilterAnswerOption(1, Q1_FILTER_ANSWER)
            .applyFilters();
        // Record for preview-time verification: this question should display the participant's
        // TOP-CHOICE creative (which version that is depends on their ranking at preview).
        context.recordShowSelection(Q2_IDX, questionText, "top_1_choice", null);
    }

    // ── Q3 — Show: Specific Creative (version dropdown) + response filter on Q1 ──
    // Version selected as lowercase "b" → version b's creative should display at preview.
    @And("I add a form question showing a specific creative version {string}")
    public void iAddSpecificCreativeQuestion(String questionText) {
        String version = "b";
        page.addNewQuestion(Q3_IDX)
            .enterQuestionText(Q3_IDX, questionText)
            .selectQuestionType(Q3_IDX, "multiple_choice")
            .selectShowToParticipants(Q3_IDX, "specific_creative")
            .selectSpecificCreative(Q3_IDX, version)   // show version b
            .enterAnswerText(Q3_IDX, 1, "Colour")
            .enterAnswerText(Q3_IDX, 2, "Layout")
            .openFilterSidebar(Q3_IDX)
            .clickAddFilterQuestion(1)
            .selectFilterQuestion(1, Q1_PARTIAL)
            .selectFilterAnswerOption(1, Q1_FILTER_ANSWER)
            .applyFilters();
        // Record for preview-time verification: this question should display the SELECTED
        // version's creative (version "b").
        context.recordShowSelection(Q3_IDX, questionText, "specific_creative", version);
    }

    // ── Preview Journey — validates the whole setup ─────────────────────────────
    @Then("I preview the QAT experiment journey")
    public void iPreviewTheQatJourney() {
        // Retrigger queue = the choice questions added above (all three are choice-based).
        String previewUrl = page.clickPreviewAndGetUrl(new int[]{Q1_IDX, Q2_IDX, Q3_IDX});
        if (page.wasIncompleteToastSeen()) {
            System.out.println("[QAT FormQuestions] Incomplete-fields toast appeared but was " +
                "handled by retrigger — preview URL captured successfully");
        }
        context.previewUrl = previewUrl;
        String retriggeredQ1 = page.getRetriggeredQ1Answer();
        if (retriggeredQ1 != null) {
            context.q1SurveyAnswer = retriggeredQ1;
            System.out.println("[QAT FormQuestions] Q1 answer updated after retrigger: " + retriggeredQ1);
        }
        System.out.println("[QAT FormQuestions] Preview URL: " + previewUrl);
        System.out.println("[QAT FormQuestions] Show selections to verify at preview:");
        context.showSelections.values().forEach(s -> System.out.println("  - " + s));
    }
}

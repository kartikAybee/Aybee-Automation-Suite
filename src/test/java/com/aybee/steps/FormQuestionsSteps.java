package com.aybee.steps;

import com.aybee.context.GlobalTestState;
import com.aybee.context.ScenarioContext;
import com.aybee.pages.FormQuestionsPage;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Then;
import org.testng.Assert;

import java.util.Arrays;

// Form Questions — the final step of the Questionnaire experiment setup.
//
// Builds a MATRIX of questions that, with minimal repetition, exercises the full Questionnaire
// authoring surface and then verifies as a preview journey:
//
//   Idx  Type              What to display    Options / behaviour                 Filter by responses
//   ───  ───────────────   ───────────────    ─────────────────────────────────  ─────────────────────
//   Q1   Long Text         Just Question      free text                           none (prior Q1 = long)
//   Q2   Limited Choice    Uploaded Assets    min2/max3, 4 opts, exclusive+random none (prior = long text)
//   Q3   Single Choice     Just Question      4 opts, delete 1, randomize         Q2 answers (1 prior)
//   Q4   Multiple Choice   Uploaded Assets    5 opts, exclusive, delete 1, random Q2 + Q3 (2 priors)
//   Q5   Likert Horizontal Just Question      6 auto opts, delete last            Q3 answers (1 prior)
//   Q6   Likert Vertical   Just Question      6 auto opts, keep all               none
//
// This covers every question type, both "What to display" options (Just Question ×4, Uploaded
// Assets ×2), and the single "Filter by responses" tab with both one-prior and two-prior filters —
// the widest coverage with the fewest questions. The filter-by-response logic follows msjourney:
// each filter entry picks a prior question and one/more of its answer options.
public class FormQuestionsSteps {

    // ── Question text (kept in fields so the filter partials stay in sync) ──────
    private static final String Q1_TEXT = "In your own words, describe your overall impression of this product.";
    private static final String Q2_TEXT = "Which product attributes matter most in your purchase decision?";
    private static final String Q3_TEXT = "How would you rate the overall value for money of this product?";
    private static final String Q4_TEXT = "Which factors would most influence you to buy this product again?";
    private static final String Q5_TEXT = "How likely are you to recommend this product to someone you know?";
    private static final String Q6_TEXT = "Rate your satisfaction with the following aspects of this product.";

    // Partial texts used to pick a prior question inside a filter dropdown (options may be truncated).
    private static final String Q2_PARTIAL = "Which product attributes matter most";
    private static final String Q3_PARTIAL = "How would you rate the overall value";

    // Q2 (Limited Choice) answer options — the first two are selectable in filters.
    private static final String Q2_OPT1 = "Price competitiveness";
    private static final String Q2_OPT2 = "Build quality";
    private static final String Q2_OPT3 = "Brand trust";        // randomize OFF
    private static final String Q2_OPT4 = "Packaging appeal";    // exclusive

    // Q3 (Single Choice) answer options.
    private static final String Q3_OPT1 = "Excellent value";
    private static final String Q3_OPT2 = "Good value";
    private static final String Q3_OPT3 = "Fair value";          // randomize OFF
    private static final String Q3_OPT4 = "Poor value";          // deleted

    private final ScenarioContext context;
    private final FormQuestionsPage page = new FormQuestionsPage();

    // Questionnaire pre-adds NO questions, so manual questions start at index 1.
    private static final int BASE = FormQuestionsPage.FIRST_QUESTION_INDEX; // 1

    public FormQuestionsSteps(ScenarioContext context) {
        this.context = context;
    }

    private int q(int offset) { return BASE + offset; }

    // ── Page load ────────────────────────────────────────────────────────────────

    @And("the form questions step should be loaded")
    public void theFormQuestionsStepShouldBeLoaded() {
        Assert.assertTrue(page.isAddQuestionButtonClickable(),
                "Form Questions step did not load — the Add Question button never became clickable");
    }

    // ── Q1 — Long Text, Just Question ────────────────────────────────────────────
    @And("I add a long text question shown as just question")
    public void iAddLongTextQuestion() {
        page.addNewQuestion(q(0))
            .enterQuestionText(q(0), Q1_TEXT)
            .selectQuestionType(q(0), "long_text");
        // What to display defaults to Just Question — no selection needed.
    }

    // ── Q2 — Limited Choice, Uploaded Assets ─────────────────────────────────────
    @And("I add a limited choice question shown as uploaded assets")
    public void iAddLimitedChoiceUploadedAssets() {
        page.addNewQuestion(q(1))
            .enterQuestionText(q(1), Q2_TEXT)
            .selectQuestionType(q(1), "limited_choice")
            .useUploadedAssets(q(1))
            .setMinChoices(q(1), "2")
            .setMaxChoices(q(1), "3")
            .ensureAnswerOptionCount(q(1), 4)
            .enterAnswerText(q(1), 1, Q2_OPT1)
            .enterAnswerText(q(1), 2, Q2_OPT2)
            .enterAnswerText(q(1), 3, Q2_OPT3)
            .enterAnswerText(q(1), 4, Q2_OPT4)
            .clickExclusive(q(1), 4)
            .enableRandomizeToggle(q(1))
            .disableRandomizeForAnswer(q(1), 3)
            .disableRandomizeForAnswer(q(1), 4);
        // Store the two selectable (non-exclusive) options the guest will pick — the preview
        // retrigger may later update index 0 in GlobalTestState if it appends a letter to it.
        GlobalTestState.q2SelectOptions = Arrays.asList(Q2_OPT1, Q2_OPT2);
    }

    // ── Q3 — Single Choice, Just Question, filter by Q2 responses ─────────────────
    @And("I add a single choice question filtered by a prior response")
    public void iAddSingleChoiceFilteredByPriorResponse() {
        page.addNewQuestion(q(2))
            .enterQuestionText(q(2), Q3_TEXT)
            .selectQuestionType(q(2), "single_choice")
            .enterAnswerText(q(2), 1, Q3_OPT1)
            .enterAnswerText(q(2), 2, Q3_OPT2)
            .enterAnswerText(q(2), 3, Q3_OPT3)
            .enterAnswerText(q(2), 4, Q3_OPT4)
            .enableRandomizeToggle(q(2))
            .disableRandomizeForAnswer(q(2), 3)
            .deleteAnswer(q(2), 4)
            // Filter by responses — reference Q2, add one answer, delete the entry, re-add and
            // select two answers (exercises add/delete/re-add of a filter entry, like msjourney).
            .openFilterSidebar(q(2))
            .clickFilterByResponseTab()
            .clickAddFilterQuestion(1)
            .selectFilterQuestion(1, Q2_PARTIAL)
            .selectFilterAnswerOption(1, Q2_OPT1)
            .deleteFilterQuestion(1)
            .clickAddFilterQuestion(1)
            .selectFilterQuestion(1, Q2_PARTIAL)
            .selectFilterAnswerOption(1, Q2_OPT1)
            .selectFilterAnswerOption(1, Q2_OPT2)
            .applyFilters();
        // Store the single option the guest will pick for Q3.
        GlobalTestState.q3SelectOption = Q3_OPT1;
    }

    // ── Q4 — Multiple Choice, Uploaded Assets, filter by Q2 + Q3 responses ────────
    @And("I add a multiple choice question shown as uploaded assets filtered by prior responses")
    public void iAddMultipleChoiceUploadedAssetsFiltered() {
        String opt1 = "Price lower than alternatives";
        String opt2 = "Proven product quality";
        String opt3 = "Trusted brand reputation";   // randomize OFF
        String opt4 = "Would not repurchase";        // exclusive
        String opt5 = "Better warranty terms";       // deleted
        page.addNewQuestion(q(3))
            .enterQuestionText(q(3), Q4_TEXT)
            .selectQuestionType(q(3), "multiple_choice")
            .useUploadedAssets(q(3))
            .enterAnswerText(q(3), 1, opt1)
            .enterAnswerText(q(3), 2, opt2)
            .enterAnswerText(q(3), 3, opt3)
            .enterAnswerText(q(3), 4, opt4)
            .enterAnswerText(q(3), 5, opt5)
            .clickExclusive(q(3), 4)
            .enableRandomizeToggle(q(3))
            .disableRandomizeForAnswer(q(3), 3)
            .disableRandomizeForAnswer(q(3), 4)
            .deleteAnswer(q(3), 5)
            // Two prior filters: Q2 (collapse after selecting) + Q3.
            .openFilterSidebar(q(3))
            .clickFilterByResponseTab()
            .clickAddFilterQuestion(1)
            .selectFilterQuestion(1, Q2_PARTIAL)
            .selectFilterAnswerOption(1, Q2_OPT1)
            .collapseFilterQuestion(1)
            .clickAddFilterQuestion(2)
            .selectFilterQuestion(2, Q3_PARTIAL)
            .selectFilterAnswerOption(2, Q3_OPT1)
            .applyFilters();
        // Store the two non-exclusive options the guest will pick for Q4 (multiple choice) — the
        // retrigger targets this question first, so index 0 here is the most likely to be updated.
        GlobalTestState.q4SelectOptions = Arrays.asList(opt1, opt2);
    }

    // ── Q5 — Likert Horizontal, Just Question, filter by Q3 responses ─────────────
    @And("I add a horizontal likert question filtered by a prior response")
    public void iAddHorizontalLikertFiltered() {
        page.addNewQuestion(q(4))
            .enterQuestionText(q(4), Q5_TEXT)
            .selectQuestionType(q(4), "likert_scale")
            .selectScaleType(q(4), "horizontal")
            .waitForLikertOptions(q(4), 6)
            .deleteAnswer(q(4), 6)
            .openFilterSidebar(q(4))
            .clickFilterByResponseTab()
            .clickAddFilterQuestion(1)
            .selectFilterQuestion(1, Q3_PARTIAL)
            .selectFilterAnswerOption(1, Q3_OPT1)
            .applyFilters();
    }

    // ── Q6 — Likert Vertical, Just Question ───────────────────────────────────────
    @And("I add a vertical likert question")
    public void iAddVerticalLikert() {
        page.addNewQuestion(q(5))
            .enterQuestionText(q(5), Q6_TEXT)
            .selectQuestionType(q(5), "likert_scale")
            .selectScaleType(q(5), "vertical")
            .waitForLikertOptions(q(5), 6);
    }

    // ── Validation + Preview ─────────────────────────────────────────────────────

    @And("I clean up empty options on the limited choice question")
    public void iCleanUpEmptyOptionsOnLimitedChoice() {
        page.cleanupEmptyOptions(q(1));
    }

    @And("I validate all questions and capture the preview URL")
    public void iValidateAllQuestionsAndCaptureThePreviewUrl() {
        // Bubble.io can silently reset a Likert scale-type dropdown to null between setup and preview —
        // re-assert each Likert question's type just before validating so the preview doesn't break.
        page.ensureLikertScaleType(q(4), "horizontal");  // Q5
        page.ensureLikertScaleType(q(5), "vertical");    // Q6
        page.validateAllInputs();
        // If the preview does not open, re-trigger choice validation by nudging an option (append a
        // letter) then re-clicking the title — the multiple-choice question (Q4) is tried FIRST since
        // it is the one that has thrown the incomplete-fields error, then the other choice/Likert
        // questions as fallbacks.
        context.previewUrl = page.clickPreviewAndGetUrl(q(3), q(2), q(1), q(4), q(5));
    }

    @And("I open the preview URL as a cleared-cache guest")
    public void iOpenThePreviewUrlAsAClearedCacheGuest() {
        Assert.assertTrue(context.previewUrl != null && !context.previewUrl.isEmpty(),
                "No preview URL available to open as a guest");
        page.navigateAsGuest(context.previewUrl);
    }

    @Then("the preview URL should be captured")
    public void thePreviewUrlShouldBeCaptured() {
        Assert.assertTrue(context.previewUrl != null && !context.previewUrl.isEmpty(),
                "Preview URL was not captured after clicking Preview Journey");
    }
}

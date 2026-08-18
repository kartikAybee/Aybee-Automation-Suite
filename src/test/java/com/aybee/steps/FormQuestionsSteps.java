package com.aybee.steps;

import com.aybee.context.GlobalTestState;
import com.aybee.context.ScenarioContext;
import com.aybee.pages.FormQuestionsPage;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.testng.Assert;

import java.util.List;

public class FormQuestionsSteps {

    private final ScenarioContext context;
    private final FormQuestionsPage formQuestions = new FormQuestionsPage();

    public FormQuestionsSteps(ScenarioContext context) {
        this.context = context;
    }

    // Creates ONE split-test question per What-to-Display option that is actually AVAILABLE for the
    // product source (your_own_products). The available options depend on the product source — e.g.
    // a__content__section_2_ is not offered for your_own_products — so rather than hard-coding four,
    // we create the first question's shell, read the real options from its dropdown, and build a
    // question for each. The exact option list (order preserved) is stored in GlobalTestState so the
    // preview journeys answer exactly this many split-test questions. PDP has no default questions,
    // so these are the only questions and start at index 1.
    @And("I create four Split Test questions covering each What to Display option")
    public void iCreateFourSplitTestQuestions() {
        int base = FormQuestionsPage.FIRST_QUESTION_INDEX; // 1
        // First question shell (type + product source) so the What-to-Display dropdown populates.
        formQuestions.addNewQuestion(base)
                     .enterQuestionText(base, "Dummy split test question " + base + " — which version do you prefer?")
                     .selectQuestionType(base, "split_test")
                     .selectProductSource(base, "your_own_products");
        List<String> opts = formQuestions.getAvailableWhatToDisplayOptions(base);
        Assert.assertFalse(opts.isEmpty(),
            "No What-to-Display options were available for the split-test questions");
        // First question uses the first available option.
        formQuestions.selectWhatToDisplay(base, opts.get(0));
        // One additional split-test question per remaining available option.
        for (int i = 1; i < opts.size(); i++) {
            int idx = base + i;
            formQuestions.createSplitTestQuestion(idx,
                "Dummy split test question " + idx + " — What to display: " + opts.get(i),
                opts.get(i));
        }
        // Store the exact available options (order preserved) so the preview journeys answer this many.
        GlobalTestState.splitDisplayOptions = opts;
        System.out.println("[FormQuestions] Created " + opts.size()
            + " split-test question(s) for options: " + opts);
    }

    @And("I validate all inputs and open the preview as a logged-in user")
    public void iValidateAllInputsAndOpenPreview() {
        // Before clicking the title to validate, make sure every split-test question's product-source
        // dropdown is set to your_own_products (re-selects any that dropped it). The number of questions
        // is dynamic — one per available What-to-Display option — so derive the indices from the stored list.
        int base = FormQuestionsPage.FIRST_QUESTION_INDEX;
        int count = (GlobalTestState.splitDisplayOptions != null) ? GlobalTestState.splitDisplayOptions.size() : 0;
        int[] indices = new int[count];
        for (int i = 0; i < count; i++) indices[i] = base + i;
        // The platform doesn't fully load scenario products on first render (they show up incompletely
        // in the preview journey), so reload the editor once before opening the preview to force a
        // proper load. Re-verify the product-source dropdowns afterwards since the page was reloaded.
        // Commit the last question BEFORE reloading: click the section title (native click → blur) and
        // wait so Bubble stores the just-typed field value — otherwise the reload discards it because
        // the field never lost focus.
        formQuestions.validateAllInputs();
        formQuestions.reloadEditorForProductLoad();
        formQuestions.ensureProductSourcesSelected(indices);
        // After the reload the What-to-Display option values have loaded properly, so re-read the
        // actually-selected value on each question and correct the expected list. This recovers any
        // first-render placeholder id (e.g. PLACEHOLDER_... that is really full_product_page) so the
        // preview journey verifies/answers against the true expected displays.
        if (GlobalTestState.splitDisplayOptions != null && !GlobalTestState.splitDisplayOptions.isEmpty()) {
            List<String> corrected = new java.util.ArrayList<>(GlobalTestState.splitDisplayOptions);
            for (int i = 0; i < indices.length; i++) {
                String v = formQuestions.readSelectedWhatToDisplay(indices[i]);
                if (v != null && i < corrected.size() && !v.equals(corrected.get(i))) {
                    System.out.println("[FormQuestions] Corrected expected What-to-Display for Q" + indices[i]
                        + ": '" + corrected.get(i) + "' -> '" + v + "'");
                    corrected.set(i, v);
                }
            }
            GlobalTestState.splitDisplayOptions = corrected;
            System.out.println("[FormQuestions] Expected split-display options after reload: " + corrected);
        }
        formQuestions.validateAllInputs();
        context.previewUrl = formQuestions.clickPreviewAndGetUrl();
    }

    @Then("the preview URL should be captured")
    public void thePreviewUrlShouldBeCaptured() {
        Assert.assertTrue(context.previewUrl != null && !context.previewUrl.isEmpty(),
            "Preview URL was not captured after clicking Preview Journey");
    }
}

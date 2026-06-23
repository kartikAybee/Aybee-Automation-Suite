package com.aybee.context;

import org.testng.asserts.SoftAssert;

import java.util.LinkedHashMap;
import java.util.Map;

public class ScenarioContext {
    public final SoftAssert softAssert = new SoftAssert();

    // Captured from the preview-journey tab; reused to drive the logged-in preview and,
    // later, the guest journey.
    public String previewUrl;

    // Uploaded creative image src per version letter ("a" → version A, "b" → version B),
    // captured at asset-upload time. Compared against the creative displayed at preview time
    // (normalize both with QatProjectPage.creativeKey to drop responsive query params).
    public final Map<String, String> uploadedCreativeSrc = new LinkedHashMap<>();

    // The creative version the guest picked on the selection page ("A" / "B"). Drives the
    // later top_1_choice creative check (the chosen version should be the one shown).
    public String selectedCreativeVersion;

    // Q1's first answer text as it stands when the survey runs. Starts as the originally
    // entered value; updated if a retrigger appended "s" during the preview flow.
    public String q1SurveyAnswer = "Strongly appealing";

    // QAT Show-to-Participants selections captured at form-setup time, keyed by question
    // index (insertion-ordered). The displayed creative images can only be checked once the
    // journey is previewed, so setup records each selection here and the preview step verifies
    // the rendered images against it. See memory: project_qat_creative_verification.
    public final Map<Integer, QatShowSelection> showSelections = new LinkedHashMap<>();

    public void recordShowSelection(int questionIndex, String questionText, String showType, String version) {
        showSelections.put(questionIndex,
            new QatShowSelection(questionIndex, questionText, showType, version));
    }
}

package com.aybee.context;

// A Show-to-Participants selection captured during QAT form-question setup.
//
// The creative images that should be displayed for a question can only be verified once the
// journey is previewed (the participant view is what actually renders them). So setup records
// WHAT was configured here, and the preview step asserts the displayed images against it.
public class QatShowSelection {

    public final int questionIndex;
    public final String questionText;
    public final String showType;   // all_creatives | top_1_choice | specific_creative
    public final String version;    // specific_creative only: "a" / "b"; otherwise null

    public QatShowSelection(int questionIndex, String questionText, String showType, String version) {
        this.questionIndex = questionIndex;
        this.questionText = questionText;
        this.showType = showType;
        this.version = version;
    }

    @Override
    public String toString() {
        return "Q" + questionIndex + " show=" + showType
            + (version != null ? " version=" + version : "")
            + " [" + questionText + "]";
    }
}

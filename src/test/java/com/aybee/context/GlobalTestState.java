package com.aybee.context;

import java.util.Arrays;
import java.util.List;

public class GlobalTestState {

    public static String previewUrl;
    public static ProductSnapshot scenarioAProduct;
    public static ProductSnapshot scenarioBProduct;
    public static String scenarioAProductName;
    public static String scenarioBProductName;
    public static String currentScenario;
    public static boolean cartHasItem;

    // Participant form option texts — stored during form questions setup so
    // ParticipantFormPage always uses IDs built from the same text that was entered,
    // even if the answer texts are changed in FormQuestionsSteps.
    public static List<String> q2SelectOptions = Arrays.asList("Price competitiveness", "Build quality");
    public static String       q3SelectOption  = "Excellent value";
    public static List<String> q4SelectOptions = Arrays.asList("Price lower than alternatives", "Proven product quality");

    public static void saveFrom(ScenarioContext ctx) {
        if (ctx.previewUrl         != null) previewUrl         = ctx.previewUrl;
        if (ctx.scenarioAProduct   != null) scenarioAProduct   = ctx.scenarioAProduct;
        if (ctx.scenarioBProduct   != null) scenarioBProduct   = ctx.scenarioBProduct;
        if (ctx.scenarioAProductName != null) scenarioAProductName = ctx.scenarioAProductName;
        if (ctx.scenarioBProductName != null) scenarioBProductName = ctx.scenarioBProductName;
        if (ctx.currentScenario    != null) currentScenario    = ctx.currentScenario;
        cartHasItem = ctx.cartHasItem;
    }

    public static void restoreInto(ScenarioContext ctx) {
        if (previewUrl         != null) ctx.previewUrl         = previewUrl;
        if (scenarioAProduct   != null) ctx.scenarioAProduct   = scenarioAProduct;
        if (scenarioBProduct   != null) ctx.scenarioBProduct   = scenarioBProduct;
        if (scenarioAProductName != null) ctx.scenarioAProductName = scenarioAProductName;
        if (scenarioBProductName != null) ctx.scenarioBProductName = scenarioBProductName;
        if (currentScenario    != null) ctx.currentScenario    = currentScenario;
        ctx.cartHasItem = cartHasItem;
    }
}

package com.aybee.context;

public class GlobalTestState {

    public static String previewUrl;

    // Choice-question option texts the guest participant will select. Written during Form Questions
    // setup and UPDATED by the preview retrigger (which appends a letter to a question's first
    // option). Kept here as plain statics — they persist across scenarios in the same JVM, so the
    // guest journey (a later scenario) reads exactly the text each field now holds. Not routed
    // through saveFrom/restoreInto to avoid a stale ScenarioContext value clobbering a retrigger update.
    public static java.util.List<String> q2SelectOptions;   // Q2 limited choice — 2 selected options
    public static String                  q3SelectOption;    // Q3 single choice — the selected option
    public static java.util.List<String> q4SelectOptions;   // Q4 multiple choice — 2 selected options
    public static ProductSnapshot scenarioAProduct;
    public static ProductSnapshot scenarioBProduct;
    public static String scenarioAProductName;
    public static String scenarioBProductName;
    public static String currentScenario;
    public static boolean cartHasItem;

    public static void saveFrom(ScenarioContext ctx) {
        if (ctx.previewUrl           != null) previewUrl           = ctx.previewUrl;
        if (ctx.scenarioAProduct     != null) scenarioAProduct     = ctx.scenarioAProduct;
        if (ctx.scenarioBProduct     != null) scenarioBProduct     = ctx.scenarioBProduct;
        if (ctx.scenarioAProductName != null && !ctx.scenarioAProductName.isBlank()) scenarioAProductName = ctx.scenarioAProductName;
        if (ctx.scenarioBProductName != null && !ctx.scenarioBProductName.isBlank()) scenarioBProductName = ctx.scenarioBProductName;
        if (ctx.currentScenario      != null) currentScenario      = ctx.currentScenario;
        cartHasItem = ctx.cartHasItem;
    }

    public static void restoreInto(ScenarioContext ctx) {
        if (previewUrl           != null) ctx.previewUrl           = previewUrl;
        if (scenarioAProduct     != null) ctx.scenarioAProduct     = scenarioAProduct;
        if (scenarioBProduct     != null) ctx.scenarioBProduct     = scenarioBProduct;
        if (scenarioAProductName != null && !scenarioAProductName.isBlank()) ctx.scenarioAProductName = scenarioAProductName;
        if (scenarioBProductName != null && !scenarioBProductName.isBlank()) ctx.scenarioBProductName = scenarioBProductName;
        if (currentScenario      != null) ctx.currentScenario      = currentScenario;
        ctx.cartHasItem = cartHasItem;
    }
}

package com.aybee.context;

public class GlobalTestState {

    public static String previewUrl;
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

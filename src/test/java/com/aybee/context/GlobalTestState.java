package com.aybee.context;

public class GlobalTestState {

    public static String previewUrl;
    public static ProductSnapshot scenarioAProduct;
    public static ProductSnapshot scenarioBProduct;
    public static String scenarioAProductName;
    public static String scenarioBProductName;
    public static String currentScenario;
    public static boolean cartHasItem;

    // The What-to-Display option values actually available for the split-test questions (read from the
    // dropdown at setup, since availability depends on the product source). One split-test question is
    // created per option, and the preview journeys answer exactly this many, in this order. Plain
    // static (persists across scenarios in-JVM); not routed through saveFrom/restoreInto.
    public static java.util.List<String> splitDisplayOptions;

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

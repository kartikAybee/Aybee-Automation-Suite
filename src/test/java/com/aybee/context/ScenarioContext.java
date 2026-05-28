package com.aybee.context;

import org.testng.asserts.SoftAssert;

public class ScenarioContext {

    public final SoftAssert softAssert = new SoftAssert();

    // Captured during shop setup before any edits — the unmodified Scenario A product state.
    public ProductSnapshot scenarioAProduct;

    // Captured during shop setup after all Scenario B edits are saved — modified price/name/image.
    public ProductSnapshot scenarioBProduct;

    // Returns the snapshot for whichever scenario the current participant is assigned to.
    // Falls back to an empty snapshot (all nulls) so structural checks still run without NPE.
    public ProductSnapshot activeProduct() {
        if ("A".equals(currentScenario)) return scenarioAProduct != null ? scenarioAProduct : new ProductSnapshot();
        if ("B".equals(currentScenario)) return scenarioBProduct != null ? scenarioBProduct : new ProductSnapshot();
        return new ProductSnapshot();
    }

    // Captured when preview tab is opened; reused to navigate again for the decline-consent flow.
    public String previewUrl;

    // Detected during the preview — "A", "B", or "unknown". Used to conditionally
    // expect Q5 (Likert Horizontal shown only to Scenario A participants).
    public String currentScenario;

    // Product names captured from shop setup Scenario editing.
    // scenarioAProductName = original name from ASIN lookup (shown to Scenario A participants).
    // scenarioBProductName = first word removed (shown to Scenario B participants).
    // Used in the marketplace preview to distinguish our product from competitor products.
    public String scenarioAProductName;
    public String scenarioBProductName;

    // Set to true after a successful Add to Cart or Buy Now; cleared after item is deleted.
    // Used to guard the blocked-button check so it only runs when we know the cart has an item.
    public boolean cartHasItem = false;
}

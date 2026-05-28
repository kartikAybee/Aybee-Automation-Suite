@marketplaceAndProductDetail
Feature: Marketplace and Product Detail Page Verification

  # No Background — browser is on the marketplace after the demographic questions scenario.

  @case4
  Scenario: Verify marketplace data and exercise all product detail and cart interactions
    When I dismiss the marketplace help popup if present
    And I detect and store the current scenario assignment
    And I verify the marketplace product data matches shop setup

    # Empty cart check — cart icon (id: pointer) is globally accessible from the marketplace;
    # no product selection or opener question needed.
    And I verify the shopping cart is empty when accessed without adding a product

    # Visit 1 — product detail verification + Buy Now path: verify full cart then delete item
    And I select our product and answer the opener question
    And I verify the product details match shop setup
    And I buy the product directly via buy now and verify the cart details
    And I delete the item from cart and return to the product list

    # Visit 2 — Add to Cart path: verify full cart then continue shopping (item stays)
    And I select our product and answer the opener question
    And I add the product to cart
    And I go to the cart and verify it contains our product
    And I continue shopping from the cart without deleting the item

    # Visit 3 — blocked-button check on a competitor (cart still has item from Visit 2).
    # Our own product does not show the blocked state when it is already in the cart;
    # a competitor product does, because the cart is occupied by a different item.
    And I select a competitor product and answer the opener question
    And I verify the product buttons are blocked when the cart already has an item
    And I go to the cart and confirm checkout

package com.aybee.context;

public class ProductSnapshot {

    // Suffix of the shop-setup-product-{name} ID — truncated by Bubble.io.
    public String truncatedName;

    // Inner <img> src captured from the shop setup product list.
    public String imageSrc;

    // Price with non-numeric characters stripped (digits and "." only).
    public String price;

    // Leading numeric token from the ratings display (digits only).
    public String ratings;

    public String brand;

    // True if the prime-status element was visible on the shop setup product list.
    public boolean hasPrime;
}

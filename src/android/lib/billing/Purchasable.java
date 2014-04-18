package android.lib.billing;

/**
 * Represents the details of a billable product.
 */
public final class Purchasable {
    private final String productId;
    private final String title;
    private final String description;
    private final String price;

    Purchasable(final String productId, final String title, final String description, final String price) {
        this.productId   = productId;
        this.title       = title;
        this.description = description;
        this.price       = price;
    }

    /**
     * Returns the product ID for the billable product.
     * @return The product ID for the billable product.
     */
    public String getProductId() {
        return this.productId;
    }

    /**
     * Returns the title of the billable product.
     * @return The title of the billable product.
     */
    public String getTitle() {
        return this.title;
    }

    /**
     * Returns the description of the billable product.
     * @return The description of the billable product.
     */
    public String getDescription() {
        return this.description;
    }

    /**
     * Returns the formatted prices of the billable product, including its currency sign. The price does not include tax.
     * @return The formatted prices of the billable product, including its currency sign.
     */
    public String getPrice() {
        return this.price;
    }
}

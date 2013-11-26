package android.lib.iab;

public abstract class Billable {
    /**
     * Product ID.
     */
    private final String productId;

    /**
     * Product name.
     */
    private final String title;

    /**
     * Product description.
     */
    private final String description;

    /**
     * Product price.
     */
    private final String price;

    Billable(final String productId, final String title, final String description, final String price) {
        this.productId   = productId;
        this.title       = title;
        this.description = description;
        this.price       = price;
    }

    public String getProductId() {
        return this.productId;
    }

    public String getTitle() {
        return this.title;
    }

    public String getDescription() {
        return this.description;
    }

    public String getPrice() {
        return this.price;
    }
}

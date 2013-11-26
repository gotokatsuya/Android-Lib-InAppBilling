package android.lib.iab;

public abstract class Product {
    /**
     * Purchase order ID.
     */
    private final String orderId;

    /**
     * Product ID.
     */
    private final String productId;

    /**
     * Purchase date in milliseconds.
     */
    private final long billingTime;

    protected Product(final String orderId, final String productId, final long billingTime) {
        this.orderId     = orderId;
        this.productId   = productId;
        this.billingTime = billingTime;
    }

    public String getOrderId() {
        return this.orderId;
    }

    public String getProductId() {
        return this.productId;
    }

    public long getBillingTime() {
        return this.billingTime;
    }
}

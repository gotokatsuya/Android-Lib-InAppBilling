package android.lib.iab;

public final class GoogleProduct extends Product {
    public static final int STATE_PURCHASED = 0;
    public static final int STATE_CANCELED  = 1;
    public static final int STATE_REFUNDED  = 2;

    private final int    status;
    private final int    billingState;
    private final String developerPayload;
    private final String billingToken;

    GoogleProduct(final int status, final String orderId, final String productId, final long billingTime, final int billingState, final String developerPayload, final String billingToken) {
        super(orderId, productId, billingTime);

        this.status           = status;
        this.billingState     = billingState;
        this.developerPayload = developerPayload;
        this.billingToken     = billingToken;
    }

    public int getStatus() {
        return this.status;
    }

    public int getBillingState() {
        return this.billingState;
    }

    public String getDeveloperPayload() {
        return this.developerPayload;
    }

    public String getBillingToken() {
        return this.billingToken;
    }
}

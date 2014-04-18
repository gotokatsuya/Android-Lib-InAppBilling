package android.lib.billing;

import java.util.Date;

/**
 * Represents the details of a purchase order.
 */
public final class Order {
    /** Represents an unknown order state. */
    public static final int STATE_UNKNOWN   = -1;
    /** Represents a purchased order. */
    public static final int STATE_PURCHASED = 0;
    /** Represents a canceled order. */
    public static final int STATE_CANCELED  = 1;
    /** Represents a refunded order. */
    public static final int STATE_REFUNDED  = 2;

    private final String orderId;
    private final String packageName;
    private final String productId;
    private final Date   purchaseDate;
    private final int    purchaseState;
    private final String developerPayload;
    private final String purchaseToken;

    Order(final String orderId, final String packageName, final String productId, final Date purchaseDate, final int purchaseState, final String developerPayload, final String purchaseToken) {
        this.orderId          = orderId;
        this.packageName      = packageName;
        this.productId        = productId;
        this.purchaseDate     = purchaseDate;
        this.purchaseState    = purchaseState;
        this.developerPayload = developerPayload;
        this.purchaseToken    = purchaseToken;
    }

    /**
     * Returns a unique order identifier for the transaction. This corresponds to the Google Wallet Order ID.
     * @return A unique order identifier for the transaction.
     */
    public String getOrderId() {
        return this.orderId;
    }

    /**
     * Returns the application package from which the purchase originated.
     * @return The application package from which the purchase originated.
     */
    public String getPackageName() {
        return this.packageName;
    }

    /**
     * Returns the product identifier. Every product has a product ID, which you must specify in the application's product list on the Google Play Developer Console.
     * @return The product identifier.
     */
    public String getProductId() {
        return this.productId;
    }

    /**
     * Returns the time the product was purchased, in milliseconds since Jan 1, 1970.
     * @return The time the product was purchased, in milliseconds since Jan 1, 1970.
     */
    public Date getPurchaseDate() {
        return this.purchaseDate;
    }

    /**
     * Returns the purchase state of the order.
     * @return The purchase state of the order. Possible values are {@link #STATE_PURCHASED}, {@link #STATE_CANCELED}, and {@link #STATE_REFUNDED}.
     */
    public int getPurchaseState() {
        return this.purchaseState;
    }

    /**
     * Returns a developer-specified string that contains supplemental information about an order.
     * @return A developer-specified string that contains supplemental information about an order.
     */
    public String getDeveloperPayload() {
        return this.developerPayload;
    }

    /**
     * Returns a token that uniquely identifies a purchase for a given product and user pair.
     * @return A token that uniquely identifies a purchase for a given product and user pair.
     */
    public String getPurchaseToken() {
        return this.purchaseToken;
    }
}

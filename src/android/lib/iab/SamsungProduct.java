package android.lib.iab;

public final class SamsungProduct extends Product {
    public static final String ITEM_TYPE_CONSUMABLE     = "00"; //$NON-NLS-1$
    public static final String ITEM_TYPE_NON_CONSUMABLE = "01"; //$NON-NLS-1$
    public static final String ITEM_TYPE_SUBSCRIPTION   = "02"; //$NON-NLS-1$
    public static final String ITEM_TYPE_ALL            = "10"; //$NON-NLS-1$

    private final String productName;
    private final String price;
    private final String description;
    private final String paymentId;
    private final String imageUrl;
    private final String downloadUrl;
    private final String itemType;

    public SamsungProduct(final String productId, final String productName, final String description, final String price, final long billingTime, final String orderId, final String paymentId, final String imageUrl, final String downloadUrl, final String itemType) {
        super(orderId, productId, billingTime);

        this.productName = productName;
        this.price       = price;
        this.description = description;
        this.paymentId   = paymentId;
        this.imageUrl    = imageUrl;
        this.downloadUrl = downloadUrl;
        this.itemType    = itemType;
    }

    public String getProductName() {
        return this.productName;
    }

    public String getPrice() {
        return this.price;
    }

    public String getDescription() {
        return this.description;
    }

    public String getPaymentId() {
        return this.paymentId;
    }

    public String getImageUrl() {
        return this.imageUrl;
    }

    public String getDownloadUrl() {
        return this.downloadUrl;
    }

    public String getItemType() {
        return this.itemType;
    }

}

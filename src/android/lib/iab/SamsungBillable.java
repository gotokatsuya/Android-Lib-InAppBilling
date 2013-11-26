package android.lib.iab;

public final class SamsungBillable extends Billable {
    public static final String ITEM_TYPE_CONSUMABLE     = "00"; //$NON-NLS-1$
    public static final String ITEM_TYPE_NON_CONSUMABLE = "01"; //$NON-NLS-1$
    public static final String ITEM_TYPE_SUBSCRIPTION   = "02"; //$NON-NLS-1$
    public static final String ITEM_TYPE_ALL            = "10"; //$NON-NLS-1$

    public static final String DURATION_UNIT_YEAR  = "YEAR";  //$NON-NLS-1$
    public static final String DURATION_UNIT_MONTH = "MONTH"; //$NON-NLS-1$
    public static final String DURATION_UNIT_WEEK  = "WEEK";  //$NON-NLS-1$
    public static final String DURATION_UNIT_DAY   = "DAY";   //$NON-NLS-1$

    private final String imageUrl;
    private final String downloadUrl;
    private final String durationUnit;
    private final int    duration;
    private final String itemType;

    SamsungBillable(final String productId, final String title, final String description, final String price, final String imageUrl, final String downloadUrl, final String durationUnit, final int duration, final String itemType) {
        super(productId, title, description, price);

        this.imageUrl     = imageUrl;
        this.downloadUrl  = downloadUrl;
        this.durationUnit = durationUnit;
        this.duration     = duration;
        this.itemType     = itemType;
    }

    public String getImageUrl() {
        return this.imageUrl;
    }

    public String getDownloadUrl() {
        return this.downloadUrl;
    }

    public String getDurationUnit() {
        return this.durationUnit;
    }

    public int getDuration() {
        return this.duration;
    }

    public String getItemType() {
        return this.itemType;
    }
}

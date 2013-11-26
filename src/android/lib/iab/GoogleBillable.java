package android.lib.iab;

public final class GoogleBillable extends Billable {
    private final int status;

    public GoogleBillable(final int status, final String productId, final String title, final String description, final String price) {
        super(productId, title, description, price);

        this.status = status;
    }

    public int getStatus() {
        return this.status;
    }
}

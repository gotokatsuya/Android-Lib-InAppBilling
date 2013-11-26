package android.lib.iab;

import java.util.List;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Pair;

public abstract class IabService {
    protected final Activity          activity;
    protected final ServiceConnection connection;

    protected IabService(final Activity activity) {
        this.activity = activity;

        this.connection = new ServiceConnection() {
            @Override
            public void onServiceConnected(final ComponentName name, final IBinder service) {
                IabService.this.onServiceConnected(name, service);
            }

            @Override
            public void onServiceDisconnected(final ComponentName name) {
                IabService.this.onServiceDisconnected(name);
            }
        };
    }

    public abstract void onCreate();

    public abstract void onDestroy();

    public abstract Product onActivityResult(int requestCode, int resultCode, Intent data);

    /**
     * Returns product details for a list of product IDs, or for a product group ID.
     * <p>{@link #queryPurchasableProducts(String...)} must not be called on the main thread.
     * Otherwise, ANR may occur.</p>
     * @param productIds a list of purchasable product IDs, or a product group ID.
     * @return product details.
     */
    public abstract List<Billable> queryPurchasableProducts(final String... productIds) throws RemoteException;

    /**
     * Returns product details for a list of product IDs, or for a product group ID.
     * <p>{@link #querySubscribableProducts(String...)} must not be called on the main thread.
     * Otherwise, ANR may occur.</p>
     * @param productIds a list of subscribable product IDs, or a product group ID.
     * @return product details.
     */
    public abstract List<Billable> querySubscribableProducts(final String... productIds) throws RemoteException;

    /**
     * Returns the current un-consumed products owned by the user.
     * <p>{@link #queryPurchasedProducts()} must not be called on the main thread.
     * Otherwise, ANR may occur.</p>
     * @param productGroupId the product group ID to query.
     * <p>May not be used.</p>
     * @return the current un-consumed products owned by the user.
     */
    public abstract Pair<Integer, List<Product>> queryPurchasedProducts(String productGroupId) throws RemoteException;

    /**
     * Returns the current un-consumed products subscribed by the user.
     * <p>{@link #querySubscribedProducts()} must not be called on the main thread.
     * Otherwise, ANR may occur.</p>
     * @param productGroupId the product group ID to query.
     * <p>May not be used.</p>
     * @return the current un-consumed products subscribed by the user.
     */
    public abstract Pair<Integer, List<Product>> querySubscribedProducts(String productGroupId) throws RemoteException;

    public abstract void purchaseProduct(String productGroupId, String productId) throws RemoteException;

    public abstract void subscribeProduct(String productGroupId, String productId) throws RemoteException;

    protected abstract void onServiceConnected(final ComponentName name, final IBinder service);

    protected abstract void onServiceDisconnected(final ComponentName name);
}

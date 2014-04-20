package android.lib.billing;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender.SendIntentException;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import android.util.Pair;

import com.android.vending.billing.IInAppBillingService;

/**
 * Provides a straightforward and simple interface for sending in-app billing requests and managing in-app transactions in Google Play.
 */
public final class BillingManager {
    /**
     * Callback when your app is connected to Google Play.
     */
    public interface OnConnectListener {
        /**
         * Callback when your app is connected to Google Play.
         */
        void onConnect();
    }

    /**
     * Callback when your app is disconnected from Google Play.
     */
    public interface OnDisconnectListener {
        /**
         * Callback when your app is disconnected from Google Play.
         */
        void onDisconnect();
    }

    public static final int BILLING_RESPONSE_RESULT_OK                  = 0;
    /** User pressed back or canceled a dialog */
    public static final int BILLING_RESPONSE_RESULT_USER_CANCELED       = 1;
    /** Billing API version is not supported for the type requested */
    public static final int BILLING_RESPONSE_RESULT_BILLING_UNAVAILABLE = 3;
    /** Requested product is not available for purchase */
    public static final int BILLING_RESPONSE_RESULT_ITEM_UNAVAILABLE    = 4;
    /** Invalid arguments provided to the API. This error can also indicate that the application was not correctly signed or properly set up for in-app billing in Google Play, or does not have the necessary permissions in its manifest */
    public static final int BILLING_RESPONSE_RESULT_DEVELOPER_ERROR     = 5;
    /** Fatal error during the API action */
    public static final int BILLING_RESPONSE_RESULT_ERROR               = 6;
    /** Failure to purchase since item is already owned */
    public static final int BILLING_RESPONSE_RESULT_ITEM_ALREADY_OWNED  = 7;
    /** Failure to consume since item is not owned */
    public static final int BILLING_RESPONSE_RESULT_ITEM_NOT_OWNED      = 8;

    private static final String BILLING_INTENT             = "com.android.vending.billing.InAppBIND"; //$NON-NLS-1$
    private static final String RESPONSE_CODE              = "RESPONSE_CODE";                         //$NON-NLS-1$
    private static final String KEY_ITEM_ID                = "ITEM_ID_LIST";                          //$NON-NLS-1$
    private static final String PURCHASE_TYPE_INAPP        = "inapp";                                 //$NON-NLS-1$
    private static final String PURCHASE_TYPE_SUBSCRIPTION = "subs";                                  //$NON-NLS-1$
    private static final String BUY_INTENT                 = "BUY_INTENT";                            //$NON-NLS-1$
    private static final String DETAILS_LIST               = "DETAILS_LIST";                          //$NON-NLS-1$
    private static final String TITLE                      = "title";                                 //$NON-NLS-1$
    private static final String DESCRIPTION                = "description";                           //$NON-NLS-1$
    private static final String PRICE                      = "price";                                 //$NON-NLS-1$
    private static final String INAPP_PURCHASE_DATA        = "INAPP_PURCHASE_DATA";                   //$NON-NLS-1$
    private static final String INAPP_PURCHASE_DATA_LIST   = "INAPP_PURCHASE_DATA_LIST";              //$NON-NLS-1$
    private static final String INAPP_CONTINUATION_TOKEN   = "INAPP_CONTINUATION_TOKEN";              //$NON-NLS-1$
    private static final String ORDER_ID                   = "orderId";                               //$NON-NLS-1$
    private static final String PACKAGE_NAME               = "packageName";                           //$NON-NLS-1$
    private static final String PRODUCT_ID                 = "productId";                             //$NON-NLS-1$
    private static final String PURCHASE_TIME              = "purchaseTime";                          //$NON-NLS-1$
    private static final String PURCHASE_STATE             = "purchaseState";                         //$NON-NLS-1$
    private static final String DEVELOPER_PAYLOAD          = "developerPayload";                      //$NON-NLS-1$
    private static final String PURCHASE_TOKEN             = "purchaseToken";                         //$NON-NLS-1$

    private static final int INAPP_API_VERSION = 3;

    private final Activity          activity;
    private final ServiceConnection connection;

    private IInAppBillingService service;
    private boolean              connected;

    private OnConnectListener    onConnectListener;
    private OnDisconnectListener onDisconnectListener;

    private int requestCode;

    /**
     * Creates a new {@link BillingManager} for your Activity.
     * @param activity The activity that initiates purchase requests.
     */
    public BillingManager(final Activity activity) {
        this.activity = activity;

        this.connection = new ServiceConnection() {
            @Override
            public void onServiceConnected(final ComponentName name, final IBinder service) {
                BillingManager.this.service   = IInAppBillingService.Stub.asInterface(service);
                BillingManager.this.connected = true;

                if (BillingManager.this.onConnectListener != null) {
                    BillingManager.this.onConnectListener.onConnect();
                }
            }

            @Override
            public void onServiceDisconnected(final ComponentName name) {
                BillingManager.this.service   = null;
                BillingManager.this.connected = false;

                if (BillingManager.this.onDisconnectListener != null) {
                    BillingManager.this.onDisconnectListener.onDisconnect();
                }
            }
        };
    }

    /**
     * Initializes the in-app billing service and connects to Google Play.
     */
    public void onCreate() {
        this.activity.bindService(new Intent(BillingManager.BILLING_INTENT), this.connection, Context.BIND_AUTO_CREATE);
    }

    /**
     * Cleans up any resources used by the in-app billing service and disconnects from Google Play.
     */
    public void onDestroy() {
        if (this.service != null) {
            this.activity.unbindService(this.connection);
        }
    }

    /**
     * Passes the purchase result from a call to {@link #purchase(int, String, String)} or {@link #subscribe(int, String, String)} and returns a pair of request result and a list of {@link Order}s.
     * @param requestCode An integer to identify the request in the current session.
     * @param resultCode The value of <code>resultCode</code> in your {@link Activity#onActivityResult}.
     * @param data The value of <code>data</code> in your {@link Activity#onActivityResult}.
     * @return A pair of request result and a list of {@link Order}s. The value of {@link #BILLING_RESPONSE_RESULT_OK} represents the request was successful; error otherwise.
     */
    public Pair<Integer, Order> onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        if (this.service == null) {
            return null;
        }

        if (requestCode == 0) {
            return null;
        }

        if (requestCode == this.requestCode) {
            if (resultCode == Activity.RESULT_OK) {
                if (data.getIntExtra(BillingManager.RESPONSE_CODE, 0) == BillingManager.BILLING_RESPONSE_RESULT_OK) {
                    try {
                        final JSONObject json = new JSONObject(data.getStringExtra(BillingManager.INAPP_PURCHASE_DATA));

                        return Pair.create(Integer.valueOf(json.getInt(BillingManager.RESPONSE_CODE)), new Order(json.getString(BillingManager.ORDER_ID), json.getString(BillingManager.PACKAGE_NAME), json.getString(BillingManager.PRODUCT_ID), new Date(json.getLong(BillingManager.PURCHASE_TIME)), json.getInt(BillingManager.PURCHASE_STATE), json.getString(BillingManager.DEVELOPER_PAYLOAD), json.getString(BillingManager.PURCHASE_TOKEN)));
                    } catch (final JSONException e) {
                        Log.e(this.getClass().getName(), e.getMessage(), e);
                    }
                }

                return Pair.create(Integer.valueOf(BillingManager.BILLING_RESPONSE_RESULT_ERROR), null);
            } else if (resultCode == Activity.RESULT_CANCELED) {
                return Pair.create(Integer.valueOf(BillingManager.BILLING_RESPONSE_RESULT_USER_CANCELED), null);
            }
        }

        return null;
    }

    /**
     * Returns <code>true</code> if your app is connected to Google Play; <code>false</code> otherwise.
     * <p>Once connected, you can initiate purchase request for in-app products or subscriptions.</p>
     * @return <code>true</code> if your app is connected to Google Play; <code>false</code> otherwise.
     */
    public boolean isServiceConnected() {
        return this.connected;
    }

    public void setOnConnectListener(final OnConnectListener onConnectListener) {
        this.onConnectListener = onConnectListener;
    }

    public void setOnDisconnectListener(final OnDisconnectListener onDisconnectListener) {
        this.onDisconnectListener = onDisconnectListener;
    }

    /**
     * Queries product details from Google Play that are available for purchase.
     * <p>Note: Do not call {@link #queryPurchasableProducts} method on the main thread. Calling this method triggers a network request which could block your main thread.</p>
     * @param productIds The product IDs to query.
     * @return A pair of request result and a list of {@link Purchasable}s. The value of {@link #BILLING_RESPONSE_RESULT_OK} represents the request was successful; error otherwise.
     * @throws IllegalStateException if your app is not connected to Google Play.
     * @throws RemoteException if the request cannot be completed by Google Play.
     */
    public Pair<Integer, List<Purchasable>> queryPurchasableProducts(final String... productIds) throws IllegalStateException, RemoteException {
        return this.queryPurchasables(BillingManager.PURCHASE_TYPE_INAPP, productIds);
    }

    /**
     * Queries subscription details from Google Play that are available for subscription.
     * <p>Note: Do not call {@link #queryPurchasableSubscriptions} method on the main thread. Calling this method triggers a network request which could block your main thread.</p>
     * @param productIds The product IDs to query.
     * @return A pair of request result and a list of {@link Purchasable}s. The value of {@link #BILLING_RESPONSE_RESULT_OK} represents the request was successful; error otherwise.
     * @throws IllegalStateException if your app is not connected to Google Play.
     * @throws RemoteException if the request cannot be completed by Google Play.
     */
    public Pair<Integer, List<Purchasable>> queryPurchasableSubscriptions(final String... productIds) throws IllegalStateException, RemoteException {
        return this.queryPurchasables(BillingManager.PURCHASE_TYPE_SUBSCRIPTION, productIds);
    }

    private Pair<Integer, List<Purchasable>> queryPurchasables(final String type, final String... productIds) throws IllegalStateException, RemoteException {
        if (this.service == null) {
            throw new IllegalStateException();
        }

        final Bundle bundle = new Bundle();
        bundle.putStringArrayList(BillingManager.KEY_ITEM_ID, new ArrayList<String>(Arrays.asList(productIds)));

        final Bundle            responses    = this.service.getSkuDetails(BillingManager.INAPP_API_VERSION, this.activity.getPackageName(), type, bundle);
        final int               responseCode = responses.getInt(BillingManager.RESPONSE_CODE, BillingManager.BILLING_RESPONSE_RESULT_ERROR);
        final List<Purchasable> products     = new ArrayList<Purchasable>();

        if (responseCode == BillingManager.BILLING_RESPONSE_RESULT_OK) {
            for (final String response : responses.getStringArrayList(BillingManager.DETAILS_LIST)) {
                try {
                    final JSONObject json = new JSONObject(response);

                    products.add(new Purchasable(json.getString(BillingManager.PRODUCT_ID), json.getString(BillingManager.TITLE), json.getString(BillingManager.DESCRIPTION), json.getString(BillingManager.PRICE)));
                } catch (final JSONException e) {
                    Log.e(this.getClass().getName(), e.getMessage(), e);
                }
            }
        }

        return Pair.create(Integer.valueOf(responseCode), products);
    }

    /**
     * Queries information about purchased products made by a user from your app.
     * <p>Note: Do not call {@link #queryPurchasedProducts} method on the main thread. Calling this method triggers a network request which could block your main thread.</p>
     * @return A pair of request result and a list of {@link Order}s. The value of {@link #BILLING_RESPONSE_RESULT_OK} represents the request was successful; error otherwise.
     * @throws IllegalStateException if your app is not connected to Google Play.
     * @throws RemoteException if the request cannot be completed by Google Play.
     */
    public Pair<Integer, List<Order>> queryPurchasedProducts() throws IllegalStateException, RemoteException {
        return this.queryPurchased(BillingManager.PURCHASE_TYPE_SUBSCRIPTION, null);
    }

    /**
     * Queries information about purchased subscriptions made by a user from your app.
     * <p>Note: Do not call {@link #queryPurchasedSubscriptions} method on the main thread. Calling this method triggers a network request which could block your main thread.</p>
     * @return A pair of request result and a list of {@link Order}s. The value of {@link #BILLING_RESPONSE_RESULT_OK} represents the request was successful; error otherwise.
     * @throws IllegalStateException if your app is not connected to Google Play.
     * @throws RemoteException if the request cannot be completed by Google Play.
     */
    public Pair<Integer, List<Order>> queryPurchasedSubscriptions() throws IllegalStateException, RemoteException {
        return this.queryPurchased(BillingManager.PURCHASE_TYPE_SUBSCRIPTION, null);
    }

    private Pair<Integer, List<Order>> queryPurchased(final String type, final String continuationToken) throws IllegalStateException, RemoteException {
        if (this.service == null) {
            throw new IllegalStateException();
        }

        final Bundle      responses    = this.service.getPurchases(BillingManager.INAPP_API_VERSION, this.activity.getPackageName(), type, continuationToken);
        final int         responseCode = responses.getInt(BillingManager.RESPONSE_CODE, BillingManager.BILLING_RESPONSE_RESULT_ERROR);
        final List<Order> products     = new ArrayList<Order>();

        if (responseCode == BillingManager.BILLING_RESPONSE_RESULT_OK) {
            for (final String response : responses.getStringArrayList(BillingManager.INAPP_PURCHASE_DATA_LIST)) {
                try {
                    final JSONObject json = new JSONObject(response);

                    products.add(new Order(json.getString(BillingManager.ORDER_ID), json.getString(BillingManager.PACKAGE_NAME), json.getString(BillingManager.PRODUCT_ID), new Date(json.getLong(BillingManager.PURCHASE_TIME)), json.getInt(BillingManager.PURCHASE_STATE), json.getString(BillingManager.DEVELOPER_PAYLOAD), json.getString(BillingManager.PURCHASE_TOKEN)));
                } catch (final JSONException e) {
                    Log.e(this.getClass().getName(), e.getMessage(), e);
                }
            }

            final String token = responses.getString(BillingManager.INAPP_CONTINUATION_TOKEN);

            if (token != null) {
                final Pair<Integer, List<Order>> pair = this.queryPurchased(type, token);

                if (pair.first.intValue() == BillingManager.BILLING_RESPONSE_RESULT_OK) {
                    products.addAll(pair.second);
                } else {
                    products.addAll(pair.second);

                    return Pair.create(pair.first, products);
                }
            }
        }

        return Pair.create(Integer.valueOf(responseCode), products);
    }

    /**
     * Launches a purchase flow for a product. The purchase result will be delivered to {@link Activity#onActivityResult(int, int, Intent)} method, in which you should call {@link #onActivityResult(int, int, Intent)} to get the purchase result.
     * <p>Note: Do not call {@link #purchase} method on the main thread. Calling this method triggers a network request which could block your main thread.</p>
     * @param requestCode An integer to identify the request in the current session.
     * @param productId The product ID to purchase.
     * @param developerPayload Specify any additional arguments that you want Google Play to send back along with the purchase information.
     * @return {@link #BILLING_RESPONSE_RESULT_OK} if the request was successful; error otherwise.
     * @throws IllegalStateException if your app is not connected to Google Play.
     * @throws SendIntentException if there was an error in sending an intent for purchase.
     * @throws RemoteException if the request cannot be completed by Google Play.
     */
    public int purchase(final int requestCode, final String productId, final String developerPayload) throws IllegalStateException, SendIntentException, RemoteException {
        return this.purchase(requestCode, BillingManager.PURCHASE_TYPE_INAPP, productId, developerPayload);
    }

    /**
     * Launches a purchase flow for a subscription. The purchase result will be delivered to {@link Activity#onActivityResult(int, int, Intent)} method, in which you should call {@link #onActivityResult(int, int, Intent)} to get the purchase result.
     * <p>Note: Do not call {@link #subscribe} method on the main thread. Calling this method triggers a network request which could block your main thread.</p>
     * @param requestCode An integer to identify the request in the current session.
     * @param productId The product ID to subscribe.
     * @param developerPayload Specify any additional arguments that you want Google Play to send back along with the purchase information.
     * @return {@link #BILLING_RESPONSE_RESULT_OK} if the request was successful; error otherwise.
     * @throws IllegalStateException if your app is not connected to Google Play.
     * @throws SendIntentException if there was an error in sending an intent for purchase.
     * @throws RemoteException if the request cannot be completed by Google Play.
     */
    public int subscribe(final int requestCode, final String productId, final String developerPayload) throws IllegalStateException, SendIntentException, RemoteException {
        return this.purchase(requestCode, BillingManager.PURCHASE_TYPE_SUBSCRIPTION, productId, developerPayload);
    }

    private int purchase(final int requestCode, final String type, final String productId, final String developerPayload) throws IllegalStateException, SendIntentException, RemoteException {
        if (this.service == null) {
            throw new IllegalStateException();
        }

        final Bundle response     = this.service.getBuyIntent(BillingManager.INAPP_API_VERSION, this.activity.getPackageName(), productId, type, developerPayload);
        final int    responseCode = response.getInt(BillingManager.RESPONSE_CODE, BillingManager.BILLING_RESPONSE_RESULT_ERROR);

        if (responseCode == BillingManager.BILLING_RESPONSE_RESULT_OK) {
            this.activity.startIntentSenderForResult(((PendingIntent)response.getParcelable(BillingManager.BUY_INTENT)).getIntentSender(), requestCode, new Intent(), 0, 0, 0);
        }

        return responseCode;
    }

    /**
     * Sends a consumption request to Google Play and passes the <code>purchaseToken</code> that identifies the purchase to be removed.
     * <p>Once a product is purchased, it is considered to be "owned" and cannot be purchased from Google Play. You must send a consumption request for the product before Google Play makes it available for purchase again.</p>
     * <p>Note: Products are consumable, but subscriptions are not.</p>
     * <p>Note: Do not call {@link #consume} method on the main thread. Calling this method triggers a network request which could block your main thread.</p>
     * @param purchaseToken Part of the data in an {@link Order} that uniquely identifies a product and user pair.
     * @return {@link #BILLING_RESPONSE_RESULT_OK} if the request was successful; error otherwise.
     * @throws IllegalStateException if your app is not connected to Google Play.
     * @throws RemoteException if the request cannot be completed by Google Play.
     */
    public int consume(final String purchaseToken) throws IllegalStateException, RemoteException {
        if (this.service == null) {
            throw new IllegalStateException();
        }

        return this.service.consumePurchase(BillingManager.INAPP_API_VERSION, this.activity.getPackageName(), purchaseToken);
    }
}

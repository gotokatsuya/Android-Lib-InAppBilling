package android.lib.iab;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender.SendIntentException;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import android.util.Pair;

import com.android.vending.billing.IInAppBillingService;

public final class GoogleIabService extends IabService {
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
    private static final String RESPONSE_CODE              = "RESPONSE_CODE";                                        //$NON-NLS-1$
    private static final String KEY_ITEM_ID                = "ITEM_ID_LIST";                                         //$NON-NLS-1$
    private static final String PURCHASE_TYPE_INAPP        = "inapp";                                                //$NON-NLS-1$
    private static final String PURCHASE_TYPE_SUBSCRIPTION = "subs";                                                 //$NON-NLS-1$
    private static final String BUY_INTENT                 = "BUY_INTENT";                                           //$NON-NLS-1$
    private static final String DETAILS_LIST               = "DETAILS_LIST";                                         //$NON-NLS-1$
    private static final String TITLE                      = "title";                                                //$NON-NLS-1$
    private static final String DESCRIPTION                = "description";                                          //$NON-NLS-1$
    private static final String PRICE                      = "price";                                                //$NON-NLS-1$
    private static final String INAPP_PURCHASE_DATA        = "INAPP_PURCHASE_DATA";                                  //$NON-NLS-1$
    private static final String INAPP_PURCHASE_DATA_LIST   = "INAPP_PURCHASE_DATA_LIST";                             //$NON-NLS-1$
    private static final String ORDER_ID                   = "orderId";                                              //$NON-NLS-1$
    private static final String PRODUCT_ID                 = "productId";                                            //$NON-NLS-1$
    private static final String PURCHASE_TIME              = "purchaseTime";                                         //$NON-NLS-1$
    private static final String PURCHASE_STATE             = "purchaseState";                                        //$NON-NLS-1$
    private static final String DEVELOPER_PAYLOAD          = "developerPayload";                                     //$NON-NLS-1$
    private static final String PURCHASE_TOKEN             = "purchaseToken";                                        //$NON-NLS-1$

    private IInAppBillingService service;
    private boolean              connected;

    public GoogleIabService(final Activity activity) {
        super(activity);
    }

    @Override
    public void onCreate() {
        this.activity.bindService(new Intent(GoogleIabService.BILLING_INTENT), this.connection, Context.BIND_AUTO_CREATE);
    }

    @Override
    public void onDestroy() {
        if (this.service != null) {
            this.activity.unbindService(this.connection);
        }
    }

    @Override
    public Product onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        if (data.getIntExtra(GoogleIabService.RESPONSE_CODE, 0) == GoogleIabService.BILLING_RESPONSE_RESULT_OK) {
            try {
                final JSONObject json = new JSONObject(data.getStringExtra(GoogleIabService.INAPP_PURCHASE_DATA));

                return new GoogleProduct(data.getIntExtra(GoogleIabService.RESPONSE_CODE, GoogleIabService.BILLING_RESPONSE_RESULT_ERROR), json.getString(GoogleIabService.ORDER_ID), json.getString(GoogleIabService.PRODUCT_ID), json.getLong(GoogleIabService.PURCHASE_TIME), json.getInt(GoogleIabService.PURCHASE_STATE), json.getString(GoogleIabService.DEVELOPER_PAYLOAD), json.getString(GoogleIabService.PURCHASE_TOKEN));
            } catch (final JSONException e) {
                Log.e(this.getClass().getName(), e.getMessage(), e);
            }
        }

        return new GoogleProduct(GoogleIabService.BILLING_RESPONSE_RESULT_ERROR, null, null, 0, 0, null, null);
    }

    public boolean isServiceConnected() {
        return this.connected;
    }

    @Override
    public List<Billable> queryPurchasableProducts(final String... productIds) throws RemoteException {
        if (this.service == null) {
            return null;
        }

        final List<Billable> billables = new ArrayList<Billable>();

        for (final String productId : Arrays.asList(productIds)) {
            Bundle bundle = new Bundle();
            bundle.putStringArrayList(GoogleIabService.KEY_ITEM_ID, new ArrayList<String>(Arrays.asList(productId)));

            bundle = this.service.getSkuDetails(3, this.activity.getPackageName(), GoogleIabService.PURCHASE_TYPE_INAPP, bundle);

            if (bundle.getInt(GoogleIabService.RESPONSE_CODE) == GoogleIabService.BILLING_RESPONSE_RESULT_OK) {
                try {
                    final int responseCode = bundle.getInt(GoogleIabService.RESPONSE_CODE);

                    for (final String details : bundle.getStringArrayList(GoogleIabService.DETAILS_LIST)) {
                        final JSONObject json = new JSONObject(details);

                        billables.add(new GoogleBillable(responseCode, productId, json.getString(GoogleIabService.TITLE), json.getString(GoogleIabService.DESCRIPTION), json.getString(GoogleIabService.PRICE)));
                    }
                } catch (final JSONException e) {
                    Log.e(this.getClass().getName(), e.getMessage(), e);
                }
            }
        }

        return billables;
    }

    @Override
    public List<Billable> querySubscribableProducts(final String... productIds) throws RemoteException {
        if (this.service == null) {
            return null;
        }

        final List<Billable> billables = new ArrayList<Billable>();

        for (final String productId : Arrays.asList(productIds)) {
            Bundle bundle = new Bundle();
            bundle.putStringArrayList(GoogleIabService.KEY_ITEM_ID, new ArrayList<String>(Arrays.asList(productId)));

            bundle = this.service.getSkuDetails(3, this.activity.getPackageName(), GoogleIabService.PURCHASE_TYPE_SUBSCRIPTION, bundle);

            if (bundle.getInt(GoogleIabService.RESPONSE_CODE) == GoogleIabService.BILLING_RESPONSE_RESULT_OK) {
                try {

                    final int responseCode = bundle.getInt(GoogleIabService.RESPONSE_CODE);

                    for (final String details : bundle.getStringArrayList(GoogleIabService.DETAILS_LIST)) {
                        final JSONObject json = new JSONObject(details);

                        billables.add(new GoogleBillable(responseCode, productId, json.getString(GoogleIabService.TITLE), json.getString(GoogleIabService.DESCRIPTION), json.getString(GoogleIabService.PRICE)));
                    }
                } catch (final JSONException e) {
                    Log.e(this.getClass().getName(), e.getMessage(), e);
                }
            }
        }

        return billables;
    }

    @Override
    public Pair<Integer, List<Product>> queryPurchasedProducts(final String productGroupId) throws RemoteException {
        if (this.service == null) {
            return null;
        }

        final List<Product> products     = new ArrayList<Product>();
        final Bundle        bundle       = this.service.getPurchases(3, this.activity.getPackageName(), GoogleIabService.PURCHASE_TYPE_INAPP, null);
        final int           responseCode = bundle.getInt(GoogleIabService.RESPONSE_CODE, GoogleIabService.BILLING_RESPONSE_RESULT_ERROR);

        if (bundle.getInt(GoogleIabService.RESPONSE_CODE, GoogleIabService.BILLING_RESPONSE_RESULT_ERROR) == GoogleIabService.BILLING_RESPONSE_RESULT_OK) {
            for (final String data : bundle.getStringArrayList(GoogleIabService.INAPP_PURCHASE_DATA_LIST)) {
                try {
                    final JSONObject json = new JSONObject(data);

                    products.add(new GoogleProduct(responseCode, json.getString(GoogleIabService.ORDER_ID), json.getString(GoogleIabService.PRODUCT_ID), json.getLong(GoogleIabService.PURCHASE_TIME), json.getInt(GoogleIabService.PURCHASE_STATE), json.getString(GoogleIabService.DEVELOPER_PAYLOAD), json.getString(GoogleIabService.PURCHASE_TOKEN)));
                } catch (final JSONException e) {
                    Log.e(this.getClass().getName(), e.getMessage(), e);
                }
            }
        }

        return Pair.create(Integer.valueOf(responseCode), products);
    }

    @Override
    public Pair<Integer, List<Product>> querySubscribedProducts(final String productGroupId) throws RemoteException {
        if (this.service == null) {
            return null;
        }

        final List<Product> products     = new ArrayList<Product>();
        final Bundle        bundle       = this.service.getPurchases(3, this.activity.getPackageName(), GoogleIabService.PURCHASE_TYPE_SUBSCRIPTION, null);
        final int           responseCode = bundle.getInt(GoogleIabService.RESPONSE_CODE, GoogleIabService.BILLING_RESPONSE_RESULT_ERROR);

        if (bundle.getInt(GoogleIabService.RESPONSE_CODE, GoogleIabService.BILLING_RESPONSE_RESULT_ERROR) == GoogleIabService.BILLING_RESPONSE_RESULT_OK) {
            for (final String data : bundle.getStringArrayList(GoogleIabService.INAPP_PURCHASE_DATA_LIST)) {
                try {
                    final JSONObject json = new JSONObject(data);

                    products.add(new GoogleProduct(responseCode, json.getString(GoogleIabService.ORDER_ID), json.getString(GoogleIabService.PRODUCT_ID), json.getLong(GoogleIabService.PURCHASE_TIME), json.getInt(GoogleIabService.PURCHASE_STATE), json.getString(GoogleIabService.DEVELOPER_PAYLOAD), json.getString(GoogleIabService.PURCHASE_TOKEN)));
                } catch (final JSONException e) {
                    Log.e(this.getClass().getName(), e.getMessage(), e);
                }
            }
        }

        return Pair.create(Integer.valueOf(responseCode), products);
    }

    @Override
    public void purchaseProduct(final String productGroupId, final String productId) throws RemoteException {
        try {
            this.purchaseProduct(productGroupId, productId, null);
        } catch (final SendIntentException e) {
            Log.e(this.getClass().getName(), e.getMessage(), e);
        }
    }

    /**
     * @param productGroupId ignored.
     */
    public void purchaseProduct(final String productGroupId, final String productId, final String developerPayload) throws RemoteException, SendIntentException {
        if (this.service != null) {
            final Bundle bundle = this.service.getBuyIntent(3, this.activity.getPackageName(), productId, GoogleIabService.PURCHASE_TYPE_INAPP, developerPayload);

            if (bundle.getInt(GoogleIabService.RESPONSE_CODE) == GoogleIabService.BILLING_RESPONSE_RESULT_OK) {
                this.activity.startIntentSenderForResult(((PendingIntent)bundle.getParcelable(GoogleIabService.BUY_INTENT)).getIntentSender(), new Random().nextInt(1000), new Intent(), 0, 0, 0);
            }
        }
    }

    @Override
    public void subscribeProduct(final String productGroupId, final String productId) throws RemoteException {
        try {
            this.subscribeProduct(productGroupId, productId, null);
        } catch (final SendIntentException e) {
            Log.e(this.getClass().getName(), e.getMessage(), e);
        }
    }

    /**
     * @param productGroupId ignored.
     */
    public void subscribeProduct(final String productGroupId, final String productId, final String developerPayload) throws RemoteException, SendIntentException {
        if (this.service != null) {
            final Bundle bundle = this.service.getBuyIntent(3, this.activity.getPackageName(), productId, GoogleIabService.PURCHASE_TYPE_SUBSCRIPTION, developerPayload);

            if (bundle.getInt(GoogleIabService.RESPONSE_CODE) == GoogleIabService.BILLING_RESPONSE_RESULT_OK) {
                this.activity.startIntentSenderForResult(((PendingIntent)bundle.getParcelable(GoogleIabService.BUY_INTENT)).getIntentSender(), new Random().nextInt(1000), new Intent(), 0, 0, 0);
            }
        }
    }

    public int consumeProduct(final String billingToken) throws RemoteException {
        if (this.service == null) {
            return GoogleIabService.BILLING_RESPONSE_RESULT_ERROR;
        }

        return this.service.consumePurchase(3, this.activity.getPackageName(), billingToken);
    }

    @Override
    protected void onServiceConnected(final ComponentName name, final IBinder service) {
        this.service   = IInAppBillingService.Stub.asInterface(service);
        this.connected = true;
    }

    @Override
    protected void onServiceDisconnected(final ComponentName name) {
        this.service   = null;
        this.connected = false;
    }
}

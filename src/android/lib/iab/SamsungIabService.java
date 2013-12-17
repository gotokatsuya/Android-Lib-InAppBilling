package android.lib.iab;

import java.io.UnsupportedEncodingException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.apache.http.HttpException;
import org.apache.http.protocol.HTTP;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.lib.net.HttpClient;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.text.TextUtils;
import android.util.Log;
import android.util.Pair;
import android.widget.Toast;

import com.sec.android.iap.IAPConnector;

public final class SamsungIabService extends IabService {
    public static final int MODE_PRODUCTION   = 0;
    public static final int MODE_TEST_SUCCESS = 1;
    public static final int MODE_TEST_FAILURE = -1;

    public static final int REQUEST_PAYMENT = 1000;

    /**
     * No error.
     */
    public static final int ERROR_NONE = 0;

    /**
     * Failed to initialize Samsung In-app purchase application.
     */
    public static final int ERROR_INIT = -1000;

    /**
     * Samsung In-app purchase application upgrade required.
     */
    public static final int ERROR_UPGRADE_REQUIRED = -1001;

    /**
     * Common error occurred while running Samsung In-app purchase application.
     */
    public static final int ERROR_COMMON = -1002;

    /**
     * Product is already purchased.
     */
    public static final int ERROR_ALREADY_PURCHASED = -1003;

    /**
     * Invalid configuration supplied to Samsung in-app purchase application.
     */
    public static final int ERROR_CONFIGURATION = -1004;

    /**
     * Product does not exist.
     */
    public static final int ERROR_INVALID_PRODUCT = -1005;

    /**
     * Product may have been purchased, confirmation required.
     */
    public static final int ERROR_MAY_HAVE_BEEN_PURCHASED = -1006;

    /**
     * Item group does not exist.
     */
    public static final int ERROR_INVALID_ITEM_GROUP = -1007;

    /**
     * Network connection not available.
     */
    public static final int ERROR_NETWORK_UNAVAILABLE = -1008;

    /**
     * IO error.
     */
    public static final int ERROR_IO = -1009;

    /**
     * Socket timed out.
     */
    public static final int ERROR_TIMEOUT_SOCKET = -1010;

    /**
     * Connection timed out.
     */
    public static final int ERROR_TIMEOUT_CONNECT = -1011;

    private static final String IAP_PACKAGE_NAME     = "com.sec.android.iap";                                    //$NON-NLS-1$
    private static final String IAP_PAYMENT_ACTIVITY = "com.sec.android.iap.activity.PaymentMethodListActivity"; //$NON-NLS-1$
    private static final String IAP_SERVICE          = "com.sec.android.iap.service.iapService";                 //$NON-NLS-1$

    private static final String EXTRA_STATUS_CODE      = "STATUS_CODE";      //$NON-NLS-1$
    private static final String EXTRA_ERROR_STRING     = "ERROR_STRING";     //$NON-NLS-1$
    private static final String EXTRA_UPGRADE_URL      = "IAP_UPGRADE_URL";  //$NON-NLS-1$
    private static final String EXTRA_RESULT_LIST      = "RESULT_LIST";      //$NON-NLS-1$
    private static final String EXTRA_THIRD_PARTY_NAME = "THIRD_PARTY_NAME"; //$NON-NLS-1$
    private static final String EXTRA_ITEM_GROUP_ID    = "ITEM_GROUP_ID";    //$NON-NLS-1$
    private static final String EXTRA_ITEM_ID          = "ITEM_ID";          //$NON-NLS-1$
    private static final String EXTRA_RESULT_OBJECT    = "RESULT_OBJECT";    //$NON-NLS-1$

    private static final String EXTRA_M_ITEM_ID            = "mItemId";                         //$NON-NLS-1$
    private static final String EXTRA_M_ITEM_NAME          = "mItemName";                       //$NON-NLS-1$
    private static final String EXTRA_M_ITEM_PRICE_STRING  = "mItemPriceString";                //$NON-NLS-1$
    private static final String EXTRA_M_ITEM_DESC          = "mItemDesc";                       //$NON-NLS-1$
    private static final String EXTRA_M_ITEM_IMAGE_URL     = "mItemImageUrl";                   //$NON-NLS-1$
    private static final String EXTRA_M_ITEM_DOWNLOAD_URL  = "mItemDownloadUrl";                //$NON-NLS-1$
    private static final String EXTRA_M_DURATION_UNIT      = "mSubscriptionDurationUnit";       //$NON-NLS-1$
    private static final String EXTRA_M_DURATION           = "mSubscriptionDurationMultiplier"; //$NON-NLS-1$
    private static final String EXTRA_M_ITEM_TYPE          = "mItemType";                       //$NON-NLS-1$
    private static final String EXTRA_M_ITEM_PURCHASE_DATE = "mPurchaseDate";                   //$NON-NLS-1$
    private static final String EXTRA_M_ITEM_PURCHASE_ID   = "mPurchaseId";                     //$NON-NLS-1$
    private static final String EXTRA_M_ITEM_PAYMENT_ID    = "mPaymentId";                      //$NON-NLS-1$
    private static final String EXTRA_M_VERIFY_URL         = "mVerifyUrl";                      //$NON-NLS-1$

    private static final String EXTRA_STATUS     = "status";    //$NON-NLS-1$
    private static final String EXTRA_PAYMENT_ID = "paymentId"; //$NON-NLS-1$

    private static final int START_NUM = 1;
    private static final int END_NUM   = 1000;

    private static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyyMMdd"); //$NON-NLS-1$

    private final OnInitListener listener;

    private IAPConnector connector;
    private int          mode;

    public SamsungIabService(final Activity activity, final OnInitListener listener) {
        super(activity);

        this.listener = listener;
    }

    @Override
    public void onCreate() {
        if (this.connector == null) {
            this.activity.bindService(new Intent(SamsungIabService.IAP_SERVICE), this.connection, Context.BIND_AUTO_CREATE);
        }
    }

    @Override
    public void onDestroy() {
        if (this.connector != null) {
            this.activity.unbindService(this.connection);
        }
    }

    @Override
    public Product onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        if (requestCode == SamsungIabService.REQUEST_PAYMENT) {
            if (resultCode == Activity.RESULT_OK) {
                if (data != null) {
                    final Bundle extras = data.getExtras();

                    if (extras != null) {
                        try {
                            final JSONObject json            = new JSONObject(extras.getString(SamsungIabService.EXTRA_RESULT_OBJECT));
                            final String     verificationUrl = json.getString(SamsungIabService.EXTRA_M_VERIFY_URL);

                            this.verifyPurchase(verificationUrl, json.getString(SamsungIabService.EXTRA_M_ITEM_PURCHASE_ID), json.getString(SamsungIabService.EXTRA_M_ITEM_PAYMENT_ID));

                            return new SamsungProduct(
                                            json.getString(SamsungIabService.EXTRA_M_ITEM_ID),
                                            json.getString(SamsungIabService.EXTRA_M_ITEM_NAME),
                                            json.getString(SamsungIabService.EXTRA_M_ITEM_DESC),
                                            json.getString(SamsungIabService.EXTRA_M_ITEM_PRICE_STRING),
                                            json.getLong(SamsungIabService.EXTRA_M_ITEM_PURCHASE_DATE),
                                            json.getString(SamsungIabService.EXTRA_M_ITEM_PURCHASE_ID),
                                            json.getString(SamsungIabService.EXTRA_M_ITEM_PAYMENT_ID),
                                            json.getString(SamsungIabService.EXTRA_M_ITEM_IMAGE_URL),
                                            json.getString(SamsungIabService.EXTRA_M_ITEM_DOWNLOAD_URL),
                                            json.getString(SamsungIabService.EXTRA_M_ITEM_TYPE));
                        } catch (final JSONException e) {
                            Log.e(this.getClass().getName(), e.getMessage(), e);
                        }
                    }
                }
            }
        }

        return null;
    }

    public int getMode() {
        return this.mode;
    }

    public void setMode(final int mode) {
        this.mode = mode;
    }

    public void init(final int mode) {
        final Handler intermediaHandler = new Handler() {
            @Override
            public void handleMessage(final Message message) {
                if (message.what == SamsungIabService.ERROR_UPGRADE_REQUIRED) {
                    final String[] tokens = (String[])message.obj;

                    new AlertDialog.Builder(SamsungIabService.this.activity)
                    .setTitle(R.string.upgrade_iap)
                    .setMessage(tokens[0])
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(final DialogInterface dialog, final int which) {
                            try {
                                SamsungIabService.this.activity.startActivity(new Intent().setData(Uri.parse(tokens[1])).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                            } catch (final ActivityNotFoundException e) {
                                Log.w(this.getClass().getName(), e.getMessage(), e);
                            }
                        }
                    }).show();
                } else {
                    if (SamsungIabService.this.listener != null) {
                        SamsungIabService.this.listener.onInitCompleted(message.what);
                    }
                }
            }
        };

        new AsyncTask<Integer, Void, Bundle>() {
            @Override
            protected Bundle doInBackground(final Integer... params) {
                try {
                    return SamsungIabService.this.connector.init(params[0].intValue());
                } catch (final RemoteException e) {
                    Log.e(this.getClass().getName(), e.getMessage(), e);
                }

                return null;
            }

            @Override
            protected void onPostExecute(final Bundle bundle) {
                if (bundle == null) {
                    intermediaHandler.sendEmptyMessage(0);
                } else {
                    final int status = bundle.getInt(SamsungIabService.EXTRA_STATUS_CODE);

                    if (status == 0) {
                        intermediaHandler.sendEmptyMessage(0);
                    } else {
                        if (status == SamsungIabService.ERROR_UPGRADE_REQUIRED) {
                            intermediaHandler.sendMessage(intermediaHandler.obtainMessage(-1, new String[] { bundle.getString(SamsungIabService.EXTRA_ERROR_STRING), bundle.getString(SamsungIabService.EXTRA_UPGRADE_URL) }));
                        } else {
                            intermediaHandler.sendEmptyMessage(status);
                        }
                    }
                }
            }
        }.execute(Integer.valueOf(mode));
    }

    @Override
    public List<Billable> queryPurchasableProducts(final String... productIds) throws RemoteException {
        final List<Billable> consumeables    = this.queryPurchasableProducts(productIds[0], true);
        final List<Billable> nonConsumeables = this.queryPurchasableProducts(productIds[0], false);
        final List<Billable> products        = new ArrayList<Billable>();

        if (consumeables != null && consumeables.size() > 0) {
            products.addAll(consumeables);
        }

        if (nonConsumeables != null && nonConsumeables.size() > 0) {
            products.addAll(nonConsumeables);
        }

        return products;
    }

    private List<Billable> queryPurchasableProducts(final String productId, final boolean consumeable) throws RemoteException {
        final Bundle bundle = this.connector.getItemList(this.mode, this.activity.getPackageName(), productId, SamsungIabService.START_NUM, SamsungIabService.END_NUM, consumeable ? SamsungBillable.ITEM_TYPE_CONSUMABLE : SamsungBillable.ITEM_TYPE_NON_CONSUMABLE);
        final int    status = bundle.getInt(SamsungIabService.EXTRA_STATUS_CODE);

        if (status == SamsungIabService.ERROR_NONE) {
            final List<Billable> products = new ArrayList<Billable>();

            for (final String item : bundle.getStringArrayList(SamsungIabService.EXTRA_RESULT_LIST)) {
                try {
                    final JSONObject json = new JSONObject(item);

                    products.add(new SamsungBillable(
                                    json.getString(SamsungIabService.EXTRA_M_ITEM_ID),
                                    json.getString(SamsungIabService.EXTRA_M_ITEM_NAME),
                                    json.getString(SamsungIabService.EXTRA_M_ITEM_DESC),
                                    json.getString(SamsungIabService.EXTRA_M_ITEM_PRICE_STRING),
                                    json.getString(SamsungIabService.EXTRA_M_ITEM_IMAGE_URL),
                                    json.getString(SamsungIabService.EXTRA_M_ITEM_DOWNLOAD_URL), null, 0, consumeable ? SamsungBillable.ITEM_TYPE_CONSUMABLE : SamsungBillable.ITEM_TYPE_NON_CONSUMABLE));
                } catch (final JSONException e) {
                    Log.e(this.getClass().getName(), e.getMessage(), e);
                }
            }
        }

        return Collections.EMPTY_LIST;
    }

    @Override
    public List<Billable> querySubscribableProducts(final String... productIds) throws RemoteException {
        final Bundle bundle = this.connector.getItemList(this.mode, this.activity.getPackageName(), productIds[0], SamsungIabService.START_NUM, SamsungIabService.END_NUM, SamsungBillable.ITEM_TYPE_SUBSCRIPTION);
        final int    status = bundle.getInt(SamsungIabService.EXTRA_STATUS_CODE);

        if (status == SamsungIabService.ERROR_NONE) {
            final List<Billable> products = new ArrayList<Billable>();

            for (final String item : bundle.getStringArrayList(SamsungIabService.EXTRA_RESULT_LIST)) {
                try {
                    final JSONObject json = new JSONObject(item);

                    products.add(new SamsungBillable(
                                    json.getString(SamsungIabService.EXTRA_M_ITEM_ID),
                                    json.getString(SamsungIabService.EXTRA_M_ITEM_NAME),
                                    json.getString(SamsungIabService.EXTRA_M_ITEM_DESC),
                                    json.getString(SamsungIabService.EXTRA_M_ITEM_PRICE_STRING),
                                    json.getString(SamsungIabService.EXTRA_M_ITEM_IMAGE_URL),
                                    json.getString(SamsungIabService.EXTRA_M_ITEM_DOWNLOAD_URL),
                                    json.getString(SamsungIabService.EXTRA_M_DURATION_UNIT),
                                    json.getInt(SamsungIabService.EXTRA_M_DURATION),
                                    SamsungBillable.ITEM_TYPE_SUBSCRIPTION));
                } catch (final JSONException e) {
                    Log.e(this.getClass().getName(), e.getMessage(), e);
                }
            }
        }

        return Collections.EMPTY_LIST;
    }

    @Override
    public Pair<Integer, List<Product>> queryPurchasedProducts(final String productGroupId) throws RemoteException {
        final Pair<Integer, List<Product>> consumeables    = this.queryPurchasedProducts(productGroupId, true);
        final Pair<Integer, List<Product>> nonConsumeables = this.queryPurchasedProducts(productGroupId, false);
        final List<Product>                products        = new ArrayList<Product>();

        if (consumeables != null) {
            if (consumeables.first.intValue() == SamsungIabService.ERROR_NONE) {
                products.addAll(consumeables.second);
            } else {
                return consumeables;
            }
        }

        if (nonConsumeables != null) {
            if (nonConsumeables.first.intValue() == SamsungIabService.ERROR_NONE) {
                products.addAll(nonConsumeables.second);
            } else {
                return nonConsumeables;
            }
        }

        return Pair.create(Integer.valueOf(SamsungIabService.ERROR_NONE), products);
    }

    private Pair<Integer, List<Product>> queryPurchasedProducts(final String productGroupId, final boolean consumeable) throws RemoteException {
        final Bundle bundle = this.connector.getItemsInbox(this.activity.getPackageName(), productGroupId, SamsungIabService.START_NUM, SamsungIabService.END_NUM, SamsungIabService.DATE_FORMAT.format(new Date(0)), SamsungIabService.DATE_FORMAT.format(new Date()));
        final int    status = bundle.getInt(SamsungIabService.EXTRA_STATUS_CODE);

        if (status == SamsungIabService.ERROR_NONE) {
            final List<Product> products = new ArrayList<Product>();

            for (final String item : bundle.getStringArrayList(SamsungIabService.EXTRA_RESULT_LIST)) {
                try {
                    final JSONObject json = new JSONObject(item);

                    products.add(new SamsungProduct(
                                    json.getString(SamsungIabService.EXTRA_M_ITEM_ID),
                                    json.getString(SamsungIabService.EXTRA_M_ITEM_NAME),
                                    json.getString(SamsungIabService.EXTRA_M_ITEM_PRICE_STRING),
                                    json.getString(SamsungIabService.EXTRA_M_ITEM_DESC),
                                    json.getLong(SamsungIabService.EXTRA_M_ITEM_PURCHASE_DATE),
                                    json.getString(SamsungIabService.EXTRA_M_ITEM_PURCHASE_ID),
                                    json.getString(SamsungIabService.EXTRA_M_ITEM_PAYMENT_ID),
                                    json.getString(SamsungIabService.EXTRA_M_ITEM_IMAGE_URL),
                                    json.getString(SamsungIabService.EXTRA_M_ITEM_DOWNLOAD_URL),
                                    consumeable ? SamsungProduct.ITEM_TYPE_CONSUMABLE : SamsungProduct.ITEM_TYPE_NON_CONSUMABLE));
                } catch (final JSONException e) {
                    Log.e(this.getClass().getName(), e.getMessage(), e);
                }
            }
        }

        return Pair.create(Integer.valueOf(status), (List<Product>)Collections.EMPTY_LIST);
    }

    @Override
    public Pair<Integer, List<Product>> querySubscribedProducts(final String productGroupId) throws RemoteException {
        final Bundle bundle = this.connector.getItemsInbox(this.activity.getPackageName(), productGroupId, SamsungIabService.START_NUM, SamsungIabService.END_NUM, SamsungIabService.DATE_FORMAT.format(new Date(0)), SamsungIabService.DATE_FORMAT.format(new Date()));
        final int    status = bundle.getInt(SamsungIabService.EXTRA_STATUS_CODE);

        if (status == SamsungIabService.ERROR_NONE) {
            final List<Product> products = new ArrayList<Product>();

            for (final String item : bundle.getStringArrayList(SamsungIabService.EXTRA_RESULT_LIST)) {
                try {
                    final JSONObject json = new JSONObject(item);

                    products.add(new SamsungProduct(
                                    json.getString(SamsungIabService.EXTRA_M_ITEM_ID),
                                    json.getString(SamsungIabService.EXTRA_M_ITEM_NAME),
                                    json.getString(SamsungIabService.EXTRA_M_ITEM_PRICE_STRING),
                                    json.getString(SamsungIabService.EXTRA_M_ITEM_DESC),
                                    json.getLong(SamsungIabService.EXTRA_M_ITEM_PURCHASE_DATE),
                                    json.getString(SamsungIabService.EXTRA_M_ITEM_PURCHASE_ID),
                                    json.getString(SamsungIabService.EXTRA_M_ITEM_PAYMENT_ID),
                                    json.getString(SamsungIabService.EXTRA_M_ITEM_IMAGE_URL),
                                    json.getString(SamsungIabService.EXTRA_M_ITEM_DOWNLOAD_URL),
                                    SamsungProduct.ITEM_TYPE_SUBSCRIPTION));
                } catch (final JSONException e) {
                    Log.e(this.getClass().getName(), e.getMessage(), e);
                }
            }
        }

        return Pair.create(Integer.valueOf(status), (List<Product>)Collections.EMPTY_LIST);
    }

    @Override
    public void purchaseProduct(final String productGroupId, final String productId) throws RemoteException {
        final Bundle bundle = new Bundle();

        bundle.putString(SamsungIabService.EXTRA_THIRD_PARTY_NAME, this.activity.getPackageName());
        bundle.putString(SamsungIabService.EXTRA_ITEM_GROUP_ID, productGroupId);
        bundle.putString(SamsungIabService.EXTRA_ITEM_ID, productId);

        this.activity.startActivityForResult(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER).setComponent(new ComponentName(SamsungIabService.IAP_SERVICE, SamsungIabService.IAP_PAYMENT_ACTIVITY)).putExtras(bundle), SamsungIabService.REQUEST_PAYMENT);
    }

    @Override
    public void subscribeProduct(final String productGroupId, final String productId) throws RemoteException {
    }

    @Override
    protected void onServiceConnected(final ComponentName name, final IBinder service) {
        this.connector = IAPConnector.Stub.asInterface(service);

        if (this.listener != null) {
            this.listener.onInitReady();
        }
    }

    @Override
    protected void onServiceDisconnected(final ComponentName name) {
        this.connector = null;
    }

    private void verifyPurchase(final String url, final String purchaseId, final String paymentId) {
        final ProgressDialog progressDialog = ProgressDialog.show(this.activity, null, null, true, false);

        final Handler handler = new Handler() {
            @Override
            public void handleMessage(final Message message) {
                if (progressDialog.isShowing()) {
                    progressDialog.dismiss();
                }

                if (message.what == 0) {
                    new AlertDialog.Builder(SamsungIabService.this.activity)
                    .setTitle(R.string.payment_error)
                    .setMessage(R.string.invalid_purchase)
                    .setPositiveButton(android.R.string.ok, null)
                    .show();
                } else if (message.what == 1) {
                    Toast.makeText(SamsungIabService.this.activity, R.string.payment_success, Toast.LENGTH_SHORT).show();
                }
            }
        };

        new AsyncTask<Void, Void, Boolean>() {
            @Override
            protected void onPreExecute() {
                if (TextUtils.isEmpty(url) || TextUtils.isEmpty(purchaseId) || TextUtils.isEmpty(paymentId)) {
                    this.cancel(true);
                }
            }

            @Override
            protected Boolean doInBackground(final Void... params) {
                try {
                    final String response = new String(HttpClient.get(url), HTTP.UTF_8);

                    if (TextUtils.isEmpty(response)) {
                        return Boolean.FALSE;
                    }

                    try {
                        final JSONObject json = new JSONObject(response);

                        if (Boolean.FALSE.toString().toLowerCase().equals(json.getString(SamsungIabService.EXTRA_STATUS))) {
                            if (paymentId.equals(json.getString(SamsungIabService.EXTRA_PAYMENT_ID))) {
                                return Boolean.TRUE;
                            }
                        }

                        return Boolean.FALSE;
                    } catch (final JSONException e) {
                        Log.e(this.getClass().getName(), e.getMessage(), e);
                    }

                    return Boolean.FALSE;
                } catch (final UnsupportedEncodingException e) {
                    Log.e(this.getClass().getName(), e.getMessage(), e);
                } catch (final HttpException e) {
                    Log.e(this.getClass().getName(), e.getMessage(), e);
                }

                return Boolean.FALSE;
            }

            @Override
            protected void onPostExecute(final Boolean result) {
                if (result.booleanValue()) {
                    handler.sendEmptyMessage(1);
                } else {
                    handler.sendEmptyMessage(0);
                }
            }

            @Override
            protected void onCancelled() {
                handler.sendEmptyMessage(-1);

                super.onCancelled();
            }
        }.execute();
    }

    public static final class IapUtils {
        public static final int REQUEST_ACCOUNT_AUTHORIZATION = 1001;

        private static final String IAP_INSTALLATION_LINK     = "samsungapps://ProductDetail/com.sec.android.iap"; //$NON-NLS-1$
        private static final String IAP_ACCOUNT_AUTHORIZATION = "com.sec.android.iap.activity.AccountActivity";    //$NON-NLS-1$

        private static final int FLAG_INCLUDE_STOPPED_PACKAGES = 32;
        private static final int IAP_HASHCODE                  = 0x7a7eaf4b;

        private IapUtils() {
        }

        public static boolean isIapInstalled(final Context context) {
            try {
                context.getPackageManager().getApplicationInfo(SamsungIabService.IAP_PACKAGE_NAME, PackageManager.GET_META_DATA);

                return true;
            } catch (final NameNotFoundException e) {
                Log.w(IapUtils.class.getName(), e.getMessage(), e);
            }

            final Intent intent = new Intent().setData(Uri.parse(IapUtils.IAP_INSTALLATION_LINK));

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR1) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | IapUtils.FLAG_INCLUDE_STOPPED_PACKAGES);
            } else {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            }

            context.startActivity(intent);

            return false;
        }

        public static boolean isIapValid(final Activity activity) {
            try {
                if (activity.getPackageManager().getPackageInfo(SamsungIabService.IAP_PACKAGE_NAME, PackageManager.GET_SIGNATURES).signatures[0].hashCode() == IapUtils.IAP_HASHCODE) {
                    activity.startActivityForResult(new Intent().setComponent(new ComponentName(SamsungIabService.IAP_PACKAGE_NAME, IapUtils.IAP_ACCOUNT_AUTHORIZATION)), IapUtils.REQUEST_ACCOUNT_AUTHORIZATION);

                    return true;
                }

                IapUtils.showAlertAndFinish(activity);
            } catch (final Exception e) {
                Log.w(IapUtils.class.getName(), e.getMessage(), e);
            }

            return false;
        }

        public static boolean onActivityResult(final int requestCode, final int resultCode) {
            return requestCode == IapUtils.REQUEST_ACCOUNT_AUTHORIZATION && resultCode == Activity.RESULT_OK;
        }

        private static void showAlertAndFinish(final Activity activity) {
            new AlertDialog.Builder(activity)
            .setTitle(R.string.invalid_iap_package)
            .setMessage(R.string.invalid_iap_package)
            .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(final DialogInterface dialog, final int which) {
                    dialog.dismiss();

                    activity.finish();
                }
            }).show();
        }
    }
}

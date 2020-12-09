package mm;

import androidx.annotation.NonNull;

import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.ConsumeParams;
import com.android.billingclient.api.ConsumeResponseListener;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.android.billingclient.api.SkuDetails;
import com.android.billingclient.api.SkuDetailsParams;
import com.android.billingclient.api.SkuDetailsResponseListener;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.WritableMap;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class MassiveMediaPaymentsModule extends ReactContextBaseJavaModule {

    private final PromiseCache cache;
    private final PurchasesUpdatedListener purchasesUpdateListener = new PurchasesUpdatedListener() {
        @Override
        public void onPurchasesUpdated(BillingResult billingResult, List<Purchase> purchases) {
            if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK
                    && purchases != null) {
                for (Purchase purchase : purchases) {
                    onProductPurchased(purchase);
                }
            } else {
                if (cache.hasPromise(PromiseConstants.PURCHASE_OR_SUBSCRIBE)) {
                    cache.rejectPromise(PromiseConstants.PURCHASE_OR_SUBSCRIBE, "Purchase or subscribe failed with error: " + billingResult.getResponseCode());
                }
            }
        }

        void onProductPurchased(Purchase purchase) {
            if (cache.hasPromise(PromiseConstants.PURCHASE_OR_SUBSCRIBE)) {
                WritableMap map = Factory.getTransactionDetails(purchase);
                cache.resolvePromise(PromiseConstants.PURCHASE_OR_SUBSCRIBE, map);
            }
        }
    };
    private final BillingClient billingClient;


    MassiveMediaPaymentsModule(ReactApplicationContext reactContext) {
        super(reactContext);
        billingClient = BillingClient.newBuilder(reactContext)
                .setListener(purchasesUpdateListener)
                .enablePendingPurchases()
                .build();
        cache = new PromiseCache();
    }

    @ReactMethod
    public static void close(Promise promise) {
        promise.resolve(true);
    }

    @Override
    public String getName() {
        return "MassiveMediaPayments";
    }

    @Override
    public void onCatalystInstanceDestroy() {
        if (billingClient != null) {
            billingClient.endConnection();
        }
    }

    @ReactMethod
    public void open(Promise promise) {
        cache.clearPromises();
        if (cache.putPromise(PromiseConstants.OPEN, promise)) {
            billingClient.startConnection(new BillingClientStateListener() {
                @Override
                public void onBillingSetupFinished(@NonNull BillingResult billingResult) {
                    // The BillingClient is ready. You can query purchases here.
                    if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                        cache.resolvePromise(PromiseConstants.OPEN, true);
                    }
                }

                @Override
                public void onBillingServiceDisconnected() {
                    // Try to restart the connection on the next request to
                    // Google Play by calling the startConnection() method.
                    cache.resolvePromise(PromiseConstants.OPEN, false);
                }
            });
        } else {
            promise.reject("UNSPECIFIED", "Previous open operation is not resolved.");
        }
    }

    @ReactMethod
    public void getPendingTransactions(Promise promise) {
        if (billingClient.isReady()) {
            Purchase.PurchasesResult purchasesResult = billingClient.queryPurchases(BillingClient.SkuType.INAPP);
            WritableArray arr = Arguments.createArray();

            for (Purchase purchase : Collections.unmodifiableList(purchasesResult.getPurchasesList())) {
                arr.pushMap(Factory.getTransaction(purchase));
            }

            promise.resolve(arr);
        } else {
            promise.reject("UNSPECIFIED", "Channel is not opened. Call open().");
        }
    }

    @ReactMethod
    public void getProducts(ReadableArray productIds, Promise promise) {
        if (billingClient.isReady()) {
            try {
                ArrayList<String> productIdList = new ArrayList<>();
                for (Object id : productIds.toArrayList()) {
                    productIdList.add(String.valueOf(id));
                }
                if (cache.putPromise(PromiseConstants.PRODUCTS, promise)) {
                    SkuDetailsParams.Builder params = SkuDetailsParams.newBuilder();
                    params.setSkusList(productIdList).setType(BillingClient.SkuType.INAPP);
                    billingClient.querySkuDetailsAsync(params.build(),
                            new SkuDetailsResponseListener() {
                                @Override
                                public void onSkuDetailsResponse(@NonNull BillingResult billingResult,
                                                                 List<SkuDetails> skuDetailsList) {
                                    cache.resolvePromise(PromiseConstants.PRODUCTS, Factory.getProductList(skuDetailsList));
                                }
                            });
                } else {
                    promise.reject("UNSPECIFIED", "Previous open operation is not resolved.");
                }
            } catch (Exception ex) {
                promise.reject("UNSPECIFIED", "Failure on getting product details: " + ex.getMessage());
            }
        } else {
            promise.reject("UNSPECIFIED", "Channel is not opened. Call open().");
        }
    }

    @ReactMethod
    public void isPurchased(String productId, Promise promise) {
        if (billingClient.isReady()) {
            Purchase.PurchasesResult purchasesResult = billingClient.queryPurchases(BillingClient.SkuType.INAPP);

            for (Purchase purchase : Collections.unmodifiableList(purchasesResult.getPurchasesList())) {
                if (purchase.getSku().equalsIgnoreCase(productId)) {
                    promise.resolve(purchase.getPurchaseState() == Purchase.PurchaseState.PURCHASED);
                    break;
                }
            }
        } else {
            promise.reject("UNSPECIFIED", "Channel is not opened. Call open().");
        }
    }

    @ReactMethod
    public void purchase(String productId, String developerPayload, Promise promise) {
        if (getCurrentActivity() != null) {
            if (billingClient.isReady()) {
                if (cache.putPromise(PromiseConstants.PURCHASE_OR_SUBSCRIBE, promise)) {
                    SkuDetailsParams.Builder params = SkuDetailsParams.newBuilder();
                    params.setSkusList(Arrays.asList(productId)).setType(BillingClient.SkuType.INAPP);
                    billingClient.querySkuDetailsAsync(params.build(),
                            new SkuDetailsResponseListener() {
                                @Override
                                public void onSkuDetailsResponse(@NonNull BillingResult billingResult,
                                                                 List<SkuDetails> skuDetailsList) {
                                    BillingFlowParams billingFlowParams = BillingFlowParams.newBuilder()
                                            .setSkuDetails(skuDetailsList.get(0))
                                            .build();
                                    int responseCode = billingClient.launchBillingFlow(getCurrentActivity(), billingFlowParams).getResponseCode();
                                    if (responseCode != BillingClient.BillingResponseCode.OK) {
                                        cache.rejectPromise(PromiseConstants.PURCHASE_OR_SUBSCRIBE, "Could not start purchase process.");
                                    }
                                }
                            });
                } else {
                    promise.reject("UNSPECIFIED", "Previous purchase or subscribe operation is not resolved.");
                }
            } else {
                promise.reject("UNSPECIFIED", "Channel is not opened. Call open().");
            }
        } else {
            promise.reject("UNSPECIFIED", "No current activity.");
        }
    }

    @ReactMethod
    public void finishTransaction(String productId, Promise promise) {
        if (billingClient.isReady()) {
            if (cache.putPromise(PromiseConstants.PURCHASE_OR_SUBSCRIBE, promise)) {
                Purchase.PurchasesResult purchasesResult = billingClient.queryPurchases(BillingClient.SkuType.INAPP);
                Purchase purchase = null;
                for (Purchase lookup : Collections.unmodifiableList(purchasesResult.getPurchasesList())) {
                    if (lookup.getSku().equalsIgnoreCase(productId)) {
                        purchase = lookup;
                        break;
                    }
                }
                if (purchase != null) {
                    ConsumeParams consumeParams =
                            ConsumeParams.newBuilder()
                                    .setPurchaseToken(purchase.getPurchaseToken())
                                    .build();

                    ConsumeResponseListener listener = new ConsumeResponseListener() {
                        @Override
                        public void onConsumeResponse(BillingResult billingResult, @NonNull String purchaseToken) {
                            if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                                cache.resolvePromise(PromiseConstants.CONSUME, true);
                            } else {
                                cache.rejectPromise(PromiseConstants.CONSUME, "Consume failed with error: " + billingResult.getResponseCode());
                            }
                        }
                    };

                    billingClient.consumeAsync(consumeParams, listener);
                } else {
                    promise.reject("UNSPECIFIED", "Could not find product with id " + productId + " to consume.");
                }
            }
        } else {
            promise.reject("UNSPECIFIED", "Channel is not opened. Call open().");
        }
    }
}

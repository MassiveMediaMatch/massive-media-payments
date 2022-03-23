package mm;

import android.util.Log;

import androidx.annotation.NonNull;

import com.android.billingclient.api.AcknowledgePurchaseParams;
import com.android.billingclient.api.AcknowledgePurchaseResponseListener;
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
import com.facebook.react.bridge.ReadableMap;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class MassiveMediaPaymentsModule extends ReactContextBaseJavaModule {
    static final String LOG_TAG = MassiveMediaPaymentsModule.class.getSimpleName();

    private final PromiseCache cache;
    private final PurchasesUpdatedListener purchasesUpdateListener = new PurchasesUpdatedListener() {
        @Override
        public void onPurchasesUpdated(BillingResult billingResult, List<Purchase> purchases) {
            Log.v(LOG_TAG, "onPurchasesUpdated (" + billingResult.getResponseCode() + ")");
            if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                if (purchases != null) {
                    for (Purchase purchase : purchases) {
                        onProductPurchased(purchase);
                    }
                } else {
                    // downgrades don't return a purchases, but still do return success, just pass null
                    cache.resolvePromise(PromiseConstants.PURCHASE_OR_SUBSCRIBE, null);
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
        Log.v(LOG_TAG, "Open Connection");
        cache.clearPromises();
        cache.putPromise(PromiseConstants.OPEN, promise);
        billingClient.startConnection(new BillingClientStateListener() {
            @Override
            public void onBillingSetupFinished(@NonNull BillingResult billingResult) {
                // The BillingClient is ready. You can query purchases here.
                if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                    cache.resolvePromise(PromiseConstants.OPEN, true);
                } else {
                    cache.rejectPromise(PromiseConstants.OPEN, String.valueOf(billingResult.getResponseCode()));
                }
            }

            @Override
            public void onBillingServiceDisconnected() {
                // Try to restart the connection on the next request to
                // Google Play by calling the startConnection() method.
                cache.resolvePromise(PromiseConstants.OPEN, false);
            }
        });
    }

    @ReactMethod
    public void getPendingTransactions(Promise promise) {
        Log.v(LOG_TAG, "Get Pending Transactions");
        if (billingClient.isReady()) {
            Purchase.PurchasesResult inAppResult = billingClient.queryPurchases(BillingClient.SkuType.INAPP);
            Purchase.PurchasesResult subsResult = billingClient.queryPurchases(BillingClient.SkuType.SUBS);
            WritableArray arr = Arguments.createArray();
            if (inAppResult.getPurchasesList() != null) {
                for (Purchase purchase : Collections.unmodifiableList(inAppResult.getPurchasesList())) {
                    arr.pushMap(Factory.getTransaction(purchase));
                }
            }
            if (subsResult.getPurchasesList() != null) {
                for (Purchase purchase : Collections.unmodifiableList(subsResult.getPurchasesList())) {
                    if (!purchase.isAcknowledged()) {
                        arr.pushMap(Factory.getTransaction(purchase));
                    }
                }
            }
            promise.resolve(arr);
        } else {
            promise.reject("UNSPECIFIED", "Channel is not opened. Call open().");
        }
    }

    @ReactMethod
    public void getProducts(ReadableArray productIds, Promise promise) {
        Log.v(LOG_TAG, "Get Products " + productIds.toString());
        getListOfProducts(productIds, false, promise);

    }

    @ReactMethod
    public void getSubscriptions(ReadableArray subIds, Promise promise) {
        Log.v(LOG_TAG, "Get getSubscriptions " + subIds.toString());
        getListOfProducts(subIds, true, promise);

    }

    private void getListOfProducts(ReadableArray productIds, final boolean isSubs, Promise promise) {
        if (billingClient.isReady()) {
            final String promiseCacheKey = isSubs ? PromiseConstants.SUBS : PromiseConstants.PRODUCTS;
            try {
                ArrayList<String> productIdList = new ArrayList<>();
                for (Object id : productIds.toArrayList()) {
                    productIdList.add(String.valueOf(id));
                }
                cache.putPromise(promiseCacheKey, promise);
                SkuDetailsParams.Builder params = SkuDetailsParams.newBuilder();
                params.setSkusList(productIdList).setType(isSubs ? BillingClient.SkuType.SUBS : BillingClient.SkuType.INAPP);
                billingClient.querySkuDetailsAsync(params.build(),
                        new SkuDetailsResponseListener() {
                            @Override
                            public void onSkuDetailsResponse(@NonNull BillingResult billingResult,
                                                             List<SkuDetails> skuDetailsList) {
                                if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                                    cache.resolvePromise(promiseCacheKey, Factory.getProductList(skuDetailsList));
                                } else {
                                    cache.rejectPromise(promiseCacheKey, "Getting " + (isSubs ? "subs" : "products") + " failed with error: " + billingResult.getResponseCode());
                                }
                            }
                        });

            } catch (Exception ex) {
                promise.reject("UNSPECIFIED", "Failure on getting " + (isSubs ? "subs" : "products") + " details: " + ex.getMessage());
            }
        } else {
            promise.reject("UNSPECIFIED", "Channel is not opened. Call open().");
        }
    }

    @ReactMethod
    public void isPurchased(String productId, Promise promise) {
        Log.v(LOG_TAG, "Is Purchased " + productId);
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
    public void purchase(String productId, final String accountId, ReadableMap config, Promise promise) {
        Log.v(LOG_TAG, "Purchase " + productId + " with " + accountId);
        SkuDetailsParams.Builder params = SkuDetailsParams.newBuilder();
        params.setSkusList(Arrays.asList(productId)).setType(BillingClient.SkuType.INAPP);

        BillingFlowParams.Builder billingFlowParams = BillingFlowParams.newBuilder()
                .setObfuscatedAccountId(accountId);

        launchPurchaseFlow(params, billingFlowParams, promise);
    }

    @ReactMethod
    public void purchaseSubscription(String productId, final String accountId, ReadableMap config, Promise promise) {
        Log.v(LOG_TAG, "purchase Subscription " + productId + " with " + accountId);
        BillingResult result = billingClient.isFeatureSupported(BillingClient.FeatureType.SUBSCRIPTIONS);
        if (result.getResponseCode() == BillingClient.BillingResponseCode.OK) {
            SkuDetailsParams.Builder params = SkuDetailsParams.newBuilder();
            params.setSkusList(Arrays.asList(productId)).setType(BillingClient.SkuType.SUBS);

            BillingFlowParams.Builder billingFlowParams = BillingFlowParams.newBuilder()
                    .setObfuscatedAccountId(accountId);

            launchPurchaseFlow(params, billingFlowParams, promise);
        } else {
            promise.reject("UNSPECIFIED", "Subscriptions are not available on this android devices.");
        }
    }

    @ReactMethod
    public void purchaseProration(String productId, String originalProductId, final String originalPurchaseToken, final int prorationMode, final String accountId, ReadableMap config, final Promise promise) {
        Log.v(LOG_TAG, "purchase Proration " + productId + " with " + accountId + ", mode=" + prorationMode);
        BillingResult result = billingClient.isFeatureSupported(BillingClient.FeatureType.SUBSCRIPTIONS_UPDATE);
        if (result.getResponseCode() == BillingClient.BillingResponseCode.OK) {
            final SkuDetailsParams.Builder params = SkuDetailsParams.newBuilder();
            params.setSkusList(Arrays.asList(productId)).setType(BillingClient.SkuType.SUBS);

            BillingFlowParams.Builder billingFlowParams = BillingFlowParams.newBuilder()
                    .setObfuscatedAccountId(accountId)
                    .setOldSku(originalProductId, originalPurchaseToken)
                    .setReplaceSkusProrationMode(prorationMode);

            launchPurchaseFlow(params, billingFlowParams, promise);
        } else {
            promise.reject("UNSPECIFIED", "Subscription updating is not available on this android devices.");
        }
    }

    private void launchPurchaseFlow(final SkuDetailsParams.Builder skuParams, final BillingFlowParams.Builder billingParams, final Promise promise) {
        if (getCurrentActivity() != null) {
            if (billingClient.isReady()) {
                cache.putPromise(PromiseConstants.PURCHASE_OR_SUBSCRIBE, promise);
                billingClient.querySkuDetailsAsync(skuParams.build(),
                        new SkuDetailsResponseListener() {
                            @Override
                            public void onSkuDetailsResponse(@NonNull BillingResult billingResult,
                                                             List<SkuDetails> skuDetailsList) {
                                if (skuDetailsList.isEmpty()) {
                                    promise.reject("UNSPECIFIED", "No Sku Details found for " + skuParams.build().getSkusList().toString() + ".");
                                } else {
                                    billingParams.setSkuDetails(skuDetailsList.get(0));
                                    int responseCode = billingClient.launchBillingFlow(getCurrentActivity(), billingParams.build()).getResponseCode();
                                    if (responseCode != BillingClient.BillingResponseCode.OK) {
                                        cache.rejectPromise(PromiseConstants.PURCHASE_OR_SUBSCRIBE, "Could not start purchase process.");
                                    }
                                }
                            }
                        });
            } else {
                promise.reject("UNSPECIFIED", "Channel is not opened. Call open().");
            }
        } else {
            promise.reject("UNSPECIFIED", "No current activity.");
        }
    }

    @ReactMethod
    public void finishTransaction(String productId, Promise promise) {
        Log.v(LOG_TAG, "Finish Transaction for " + productId);
        if (billingClient.isReady()) {
            cache.putPromise(PromiseConstants.CONSUME, promise);
            // try to find if product is consumable
            Purchase.PurchasesResult purchasesResult = billingClient.queryPurchases(BillingClient.SkuType.INAPP);
            Purchase purchase = null;
            for (Purchase lookup : Collections.unmodifiableList(purchasesResult.getPurchasesList())) {
                if (lookup.getSku().equalsIgnoreCase(productId)) {
                    purchase = lookup;
                    break;
                }
            }

            if (purchase != null) {
                Log.v(LOG_TAG, "found purchase " + productId + " to consume.");
                consume(purchase);
            } else {
                // try to find a sub now, and acknowledge if possible
                purchasesResult = billingClient.queryPurchases(BillingClient.SkuType.SUBS);
                for (Purchase lookup : Collections.unmodifiableList(purchasesResult.getPurchasesList())) {
                    if (lookup.getSku().equalsIgnoreCase(productId)) {
                        purchase = lookup;
                        break;
                    }
                }

                if (purchase != null) {
                    Log.v(LOG_TAG, "found purchase " + productId + " to acknowledge.");
                    acknowledge(purchase);
                } else {
                    Log.v(LOG_TAG, "could not find purchase " + productId + " to consume or acknowledge.");
                    cache.rejectPromise(PromiseConstants.CONSUME, "Could not find product with id " + productId + " to consume or acknowledge.");
                }
            }
        } else {
            promise.reject("UNSPECIFIED", "Channel is not opened. Call open().");
        }
    }

    private void acknowledge(final Purchase purchase) {
        Log.v(LOG_TAG, "Acknowledge Transaction for " + purchase.getOrderId());
        if (purchase.getPurchaseState() == Purchase.PurchaseState.PURCHASED) {
            if (!purchase.isAcknowledged()) {
                AcknowledgePurchaseParams acknowledgePurchaseParams =
                        AcknowledgePurchaseParams.newBuilder()
                                .setPurchaseToken(purchase.getPurchaseToken())
                                .build();
                billingClient.acknowledgePurchase(acknowledgePurchaseParams, new AcknowledgePurchaseResponseListener() {
                    @Override
                    public void onAcknowledgePurchaseResponse(@NonNull BillingResult billingResult) {
                        Log.v(LOG_TAG, "Acknowledge Transaction for " + purchase.getOrderId() + " resulted with response: " + billingResult.getResponseCode());
                        if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                            cache.resolvePromise(PromiseConstants.CONSUME, true);
                        } else {
                            cache.rejectPromise(PromiseConstants.CONSUME, "Acknowledge failed with error: " + billingResult.getResponseCode());
                        }
                    }
                });
            } else {
                cache.rejectPromise(PromiseConstants.CONSUME, "Product with id " + purchase.getOrderId() + " is already acknowledged.");
            }
        } else {
            cache.rejectPromise(PromiseConstants.CONSUME, "Product with id " + purchase.getOrderId() + " is not purchased. State=" + purchase.getPurchaseState());
        }
    }

    private void consume(final Purchase purchase) {
        Log.v(LOG_TAG, "Consume Transaction for " + purchase.getOrderId());
        ConsumeParams consumeParams =
                ConsumeParams.newBuilder()
                        .setPurchaseToken(purchase.getPurchaseToken())
                        .build();

        ConsumeResponseListener listener = new ConsumeResponseListener() {
            @Override
            public void onConsumeResponse(BillingResult billingResult, @NonNull String purchaseToken) {
                Log.v(LOG_TAG, "Consume Transaction for " + purchase.getOrderId() + " resulted with response: " + billingResult.getResponseCode());
                if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                    cache.resolvePromise(PromiseConstants.CONSUME, true);
                } else {
                    cache.rejectPromise(PromiseConstants.CONSUME, "Consume failed with error: " + billingResult.getResponseCode());
                }
            }
        };

        billingClient.consumeAsync(consumeParams, listener);
    }
}

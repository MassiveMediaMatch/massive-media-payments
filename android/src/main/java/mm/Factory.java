package mm;

import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.SkuDetails;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.WritableMap;

import java.util.List;

class Factory {
    static WritableMap getTransaction(Purchase purchase) {
        WritableMap map = Arguments.createMap();

        map.putString("productId", purchase.getSku());
        map.putString("orderId", purchase.getOrderId());
        map.putString("developerPayload", purchase.getDeveloperPayload());
        map.putString("receiptData", purchase.getOriginalJson());
        map.putString("receiptSignature", purchase.getSignature());
        map.putString("purchaseToken", purchase.getPurchaseToken());

        return map;
    }

    static WritableArray getProductList(List<SkuDetails> skuDetailsList) {
        WritableArray arr = Arguments.createArray();
        for (SkuDetails detail : skuDetailsList) {
            WritableMap map = Arguments.createMap();

            map.putString("productId", detail.getSku());
            map.putString("title", detail.getTitle());
            map.putString("description", detail.getDescription());
            map.putBoolean("isSubscription", false);
            map.putString("currency", detail.getPriceCurrencyCode());
            map.putDouble("price", detail.getPriceAmountMicros() / 1000000.0);
            map.putString("localizedPrice", detail.getPrice());

            // intro price
            map.putDouble("introductoryPrice", detail.getIntroductoryPriceAmountMicros() / 1000000.0);
            map.putString("introductoryLocalizedPrice", detail.getIntroductoryPrice());

            arr.pushMap(map);
        }
        return arr;
    }

    static WritableMap getTransactionDetails(Purchase purchase) {
        WritableMap map = Arguments.createMap();

        map.putString("receiptData", purchase.getOriginalJson());
        map.putString("receiptSignature", purchase.getSignature());
        map.putString("productId", purchase.getSku());
        map.putString("orderId", purchase.getOrderId());
        map.putString("purchaseToken", purchase.getPurchaseToken());
        map.putDouble("purchaseTime", purchase.getPurchaseTime());
        map.putInt("purchaseState", purchase.getPurchaseState());
        map.putBoolean("autoRenewing", purchase.isAutoRenewing());
        map.putString("developerPayload", purchase.getDeveloperPayload());

        return map;
    }
}

package mm;

import android.util.Log;

import com.facebook.react.bridge.Promise;

import java.util.HashMap;

public class PromiseCache {
    static final String LOG_TAG = "MassiveMediaPayments" + PromiseCache.class.getSimpleName();

    HashMap<String, Promise> promiseCache = new HashMap<>();

    synchronized void resolvePromise(String key, Object value) {
        if (promiseCache.containsKey(key)) {
            Promise promise = promiseCache.get(key);
            promise.resolve(value);
            promiseCache.remove(key);
        } else {
            Log.w(LOG_TAG, String.format("Tried to resolve promise: %s - but does not exist in cache", key));
        }
    }

    synchronized void rejectPromise(String key, String reason) {
        if (promiseCache.containsKey(key)) {
            Promise promise = promiseCache.get(key);
            promise.reject("EUNSPECIFIED", reason);
            promiseCache.remove(key);
        } else {
            Log.w(LOG_TAG, String.format("Tried to reject promise: %s - but does not exist in cache", key));
        }
    }

    synchronized void putPromise(String key, Promise promise) {
        promiseCache.put(key, promise);
    }

    synchronized Boolean hasPromise(String key) {
        return promiseCache.containsKey(key);
    }

    synchronized void clearPromises() {
        promiseCache.clear();
    }
}

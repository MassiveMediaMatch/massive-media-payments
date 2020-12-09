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

    synchronized Boolean putPromise(String key, Promise promise) {
        if (!promiseCache.containsKey(key)) {
            promiseCache.put(key, promise);
            return true;
        } else {
            Log.w(LOG_TAG, String.format("Tried to put promise: %s - already exists in cache", key));
        }
        return false;
    }

    synchronized Boolean hasPromise(String key) {
        return promiseCache.containsKey(key);
    }

    synchronized void clearPromises() {
        promiseCache.clear();
    }
}

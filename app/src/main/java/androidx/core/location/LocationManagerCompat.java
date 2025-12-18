/*
 * Copyright 2019 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.core.location;

import static android.Manifest.permission.ACCESS_COARSE_LOCATION;
import static android.Manifest.permission.ACCESS_FINE_LOCATION;

import static java.util.concurrent.TimeUnit.NANOSECONDS;

import android.annotation.SuppressLint;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.annotation.RequiresPermission;
import android.util.Log;

import androidx.core.os.CancellationSignal;

import java.util.concurrent.Executor;
import java.util.function.Consumer;

public class LocationManagerCompat {


    private static final long GET_CURRENT_LOCATION_TIMEOUT_MS = 30 * 1000;
    private static final long MAX_CURRENT_LOCATION_AGE_MS = 10 * 1000;

    /**
     * Asynchronously returns a single current location fix from the given provider. This may
     * activate sensors in order to compute a new location. The given callback will be invoked once
     * and only once, either with a valid location or with a null location if the provider was
     * unable to generate a valid location.
     *
     * <p>A client may supply an optional {@link CancellationSignal}. If this is used to cancel the
     * operation, no callback should be expected after the cancellation.
     *
     * <p>This method may return locations from the very recent past (on the order of several
     * seconds), but will never return older locations (for example, several minutes old or older).
     * Clients may rely upon the guarantee that if this method returns a location, it will represent
     * the best estimation of the location of the device in the present moment.
     *
     * <p>Clients calling this method from the background may notice that the method fails to
     * determine a valid location fix more often than while in the foreground. Background
     * applications may be throttled in their location accesses to some degree.
     */
    @RequiresPermission(anyOf = {ACCESS_COARSE_LOCATION, ACCESS_FINE_LOCATION})
    public static void getCurrentLocation(@NonNull LocationManager locationManager,
                                          @NonNull String provider,
                                          @Nullable CancellationSignal cancellationSignal,
                                          @NonNull Executor executor,
                                          final @NonNull Consumer<Location> consumer) {
        if (Build.VERSION.SDK_INT >= 30) {
            Api30Impl.getCurrentLocation(locationManager, provider, cancellationSignal, executor,
                    consumer);
            return;
        }

        if (cancellationSignal != null) {
            cancellationSignal.throwIfCanceled();
        }

        final Location location = locationManager.getLastKnownLocation(provider);
        if (location != null) {
            long locationAgeMs =
                    SystemClock.elapsedRealtime() - getElapsedRealtimeMillis(location);
            if (locationAgeMs < MAX_CURRENT_LOCATION_AGE_MS) {
                executor.execute(() -> consumer.accept(location));
                return;
            }
        }

        final CancellableLocationListener listener =
                new CancellableLocationListener(locationManager, executor, consumer);
        locationManager.requestLocationUpdates(provider, 0, 0, listener,
                Looper.getMainLooper());

        if (cancellationSignal != null) {
            cancellationSignal.setOnCancelListener(listener::cancel);
        }

        listener.startTimeout(GET_CURRENT_LOCATION_TIMEOUT_MS);
    }

    /**
     * Return the time of this fix, in milliseconds of elapsed real-time since system boot.
     *
     * @see #getElapsedRealtimeNanos(Location)
     */
    public static long getElapsedRealtimeMillis(@NonNull Location location) {
        return NANOSECONDS.toMillis(location.getElapsedRealtimeNanos());
    }

    @RequiresApi(30)
    private static class Api30Impl {
        private Api30Impl() {
            // This class is not instantiable.
        }

        @RequiresPermission(anyOf = {ACCESS_COARSE_LOCATION, ACCESS_FINE_LOCATION})
        static void getCurrentLocation(LocationManager locationManager, @NonNull String provider,
                                       @Nullable CancellationSignal cancellationSignal,
                                       @NonNull Executor executor, final @NonNull Consumer<Location> consumer) {
            locationManager.getCurrentLocation(provider,
                    (android.os.CancellationSignal) cancellationSignal.getCancellationSignalObject(),
                    executor,
                    consumer::accept);
        }
    }

    private static final class CancellableLocationListener implements LocationListener {

        private final LocationManager mLocationManager;
        private final Executor mExecutor;
        private final Handler mTimeoutHandler;

        private Consumer<Location> mConsumer;

        private boolean mTriggered;

        @Nullable Runnable mTimeoutRunnable;

        CancellableLocationListener(LocationManager locationManager,
                                    Executor executor, Consumer<Location> consumer) {
            mLocationManager = locationManager;
            mExecutor = executor;
            mTimeoutHandler = new Handler(Looper.getMainLooper());

            mConsumer = consumer;
        }

        @RequiresPermission(anyOf = {ACCESS_COARSE_LOCATION, ACCESS_FINE_LOCATION})
        public void cancel() {
            Log.d("BLARG", "Canceling listener");
            synchronized (this) {
                if (mTriggered) {
                    return;
                }
                mTriggered = true;
            }

            cleanup();
        }

        @SuppressLint("MissingPermission") // Can't annotate a lambda
        public void startTimeout(long timeoutMs) {
            synchronized (this) {
                if (mTriggered) {
                    return;
                }

                // ideally this would be a wakeup alarm, but that would require another compat layer
                // to deal with translating pending intent alarms into listeners which doesn't exist
                // at the moment, so this should be sufficient to prevent extreme battery drain
                mTimeoutRunnable = () -> {
                    Log.d("BLARG", "Timed out!");
                    mTimeoutRunnable = null;
                    onLocationChanged((Location) null);
                };
                mTimeoutHandler.postDelayed(mTimeoutRunnable, timeoutMs);
            }
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {}

        @Override
        public void onProviderEnabled(@NonNull String provider) {}

        @RequiresPermission(anyOf = {ACCESS_COARSE_LOCATION, ACCESS_FINE_LOCATION})
        @Override
        public void onProviderDisabled(@NonNull String p) {
            onLocationChanged((Location) null);
        }

        @RequiresPermission(anyOf = {ACCESS_COARSE_LOCATION, ACCESS_FINE_LOCATION})
        @Override
        public void onLocationChanged(final @Nullable Location location) {
            Log.d("BLARG", "Got a location in the listener");
            synchronized (this) {
                if (mTriggered) {
                    return;
                }
                mTriggered = true;
            }

            final Consumer<Location> consumer = mConsumer;
            mExecutor.execute(() -> consumer.accept(location));

            cleanup();
        }

        @RequiresPermission(anyOf = {ACCESS_COARSE_LOCATION, ACCESS_FINE_LOCATION})
        private void cleanup() {
            mConsumer = null;
            mLocationManager.removeUpdates(this);
            if (mTimeoutRunnable != null) {
                mTimeoutHandler.removeCallbacks(mTimeoutRunnable);
                mTimeoutRunnable = null;
            }
        }
    }
}

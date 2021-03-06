package com.lc.puppet.service.providers;

import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.IInterface;
import android.os.RemoteException;
import android.text.TextUtils;
import android.util.Log;

import com.lc.puppet.service.providers.base.HookDataProvider;
import com.lc.puppet.service.providers.base.PatchHookProvider;
import com.lc.puppet.storage.IObFlowBase;
import com.lc.puppet.storage.IObIndex;
import com.lody.virtual.client.hook.base.MethodInvocationProxy;
import com.lody.virtual.client.hook.proxies.location.LocationManagerStub;
import com.lody.virtual.helper.utils.Reflect;

import java.util.HashMap;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import mirror.android.location.ILocationListener;
import mirror.android.location.LocationRequestL;

/**
 * @author legency
 */
public class LocationManagerProvider extends PatchHookProvider {

    public static final String TAG = "P_L_M";
    public static final String TAG2 = "P_L_M_ThreadPool";

    private static HashMap<IBinder, LocationRunnable> listeners;
    private static ThreadPoolExecutor fixedThreadPool;


    @Override
    public Class<? extends MethodInvocationProxy> getDelegatePatch() {
        return LocationManagerStub.class;
    }

    @Override
    protected void addHookDataProviders() {
        super.addHookDataProviders();
        addProvider(new requestLocationUpdates());
        addProvider(new removeUpdates());
    }

    private class requestLocationUpdates extends HookDataProvider {

        @Override
        public String getName() {
            return "requestLocationUpdates";
        }

        @Override
        public Object exec(Object... args) {
            requestLocationUpdates(args);
            return null;
        }
    }

    private class removeUpdates extends HookDataProvider {

        @Override
        public String getName() {
            return "removeUpdates";
        }

        @Override
        public Object exec(Object... args) {
            removeUpdates(args);
            return null;
        }
    }

    /**
     * @param args
     * @see com.android.server.LocationManagerService#requestLocationUpdates (LocationRequest request, ILocationListener listener, PendingIntent intent, String packageName)
     * @see com.lc.puppet.client.hook.patch.hook.location.Interceptor_RequestLocationUpdates
     */
    @ForReflect
    public void requestLocationUpdates(Object[] args) {
        if (args.length < 2 || !(args[1] instanceof IBinder)) {
            return;
        }
        final IBinder iLocationListener = (IBinder) args[1];
        final IInterface i = ILocationListener.Stub.asInterface.call(iLocationListener);
        Object locationObject = args[0];
        String provider = null;

        if (LocationRequestL.TYPE.isInstance(locationObject)) {
            provider = LocationRequestL.getProvider.call(locationObject);
        }
        final Location fakeLocation = getLocation();
        if (!TextUtils.isEmpty(provider)) {
            fakeLocation.setProvider(provider);
        }
        fakeLocation.setTime(System.currentTimeMillis());

        if (listeners == null) {
            listeners = new HashMap<>();
        }
        if (listeners.containsKey(iLocationListener)) {
            Log.e(TAG, "listener exist");
            return;
        }
        final LocationRunnable thread = new LocationRunnable() {
            @Override
            public void run() {
                while (running && IObFlowBase.get().enabled) {
                    try {
                        Thread.sleep(3000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    //fixme 该方法 存在 线程杀不死的问题
                    try {
                        Log.e(TAG, "ILocationListener.onLocationChanged start call");
                        ILocationListener.onLocationChanged.callWithException(i, fakeLocation);
                    } catch (Throwable e) {
                        Log.e(TAG, "send location failed kill thread", e);
                        running = false;
                    }
                    Log.d(TAG, "thread running pool size :" + fixedThreadPool.getPoolSize() + "index" + index);
                }
            }
        };
        try {
            iLocationListener.linkToDeath(() -> {
                Log.d(TAG, "binder died");
                thread.running = false;
                listeners.remove(iLocationListener);
            }, 0);
        } catch (RemoteException e) {
            e.printStackTrace();
            return;
        }

        listeners.put(iLocationListener, thread);
        if (fixedThreadPool == null) {
            fixedThreadPool = new ThreadPoolExecutor(0, Integer.MAX_VALUE,
                    60L, TimeUnit.SECONDS,
                    new SynchronousQueue<>());
        }
        thread.index = fixedThreadPool.getPoolSize();
        thread.running = true;
        fixedThreadPool.submit(thread);
        Log.d(TAG2, "new thread added pool size :" + fixedThreadPool.getPoolSize() + " hash :" + iLocationListener.hashCode());
    }

    /**
     * @param args
     * @see com.lc.puppet.client.hook.patch.hook.location.Interceptor_RemoveUpdates
     */
    @ForReflect
    public void removeUpdates(Object[] args) {

        if (args.length <= 2 || !(args[0] instanceof IBinder)) {
            return;
        }
        IBinder iLocationListener = (IBinder) args[0];
        final IInterface i = ILocationListener.Stub.asInterface.call(iLocationListener);
        IBinder binder = i.asBinder();
        if (listeners != null) {
            LocationRunnable a = listeners.remove(iLocationListener);
            if (a != null) {
                a.running = false;
                Log.d(TAG2, "remove location succeed hash:" + iLocationListener.hashCode());
            } else {
                Log.e(TAG, "remove location target listener not found ");
            }
            fixedThreadPool.remove(a);

        } else {
            Log.e(TAG, "remove location failed listeners is empty");
        }
    }


    static abstract class LocationRunnable implements Runnable {
        boolean running = true;
        public int index;
    }

    Location getLocation() {
        return callDataWithCreator(IObIndex.LOCATION, new PaperDataCreator<Location>() {
            @Override
            public Location createFakeData() {
                return createFakeLocation(null);
            }
        });
    }

    /**
     * @param provider
     * @return
     * @see LocationManager#GPS_PROVIDER;
     * @see LocationManager#NETWORK_PROVIDER;
     * @see LocationManager#PASSIVE_PROVIDER;
     */
    private static Location createFakeLocation(String provider) {

        if (TextUtils.isEmpty(provider)) {
            provider = LocationManager.GPS_PROVIDER;
        }
        Location location = new Location(provider);

        //测试经纬度 天安门
        location.setLongitude(116.403958);
        location.setLatitude(39.915049);

        location.setAccuracy(32.0F);
        location.setAltitude(0.0);
        location.setBearing(0.0F);
        Bundle bundle = new Bundle();
        bundle.putInt("satellites", 5);
        location.setExtras(bundle);
        Reflect.on(location).call("setIsFromMockProvider", false);
        location.setTime(System.currentTimeMillis());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            location.setElapsedRealtimeNanos(317611316791260L);
        }
        return location;
    }


}

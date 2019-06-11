package com.cyberpunk.ble.beat.connect.ble;

import android.bluetooth.BluetoothGatt;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.util.Log;

import com.cyberpunk.ble.beat.connect.ble.central.BlePeripheralUart;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Semaphore;

public class UartPacketManagerBase implements BlePeripheralUart.UartRxHandler {
    // Log
    private final static String TAG = UartPacketManagerBase.class.getSimpleName();

    // Listener
    public interface Listener {
        void onUartPacket(UartPacket packet);
    }

    // Data
    //private boolean mIsEnabled = false;
    protected final Handler mMainHandler = new Handler(Looper.getMainLooper());
    protected WeakReference<Listener> mWeakListener;
    protected List<UartPacket> mPackets = new ArrayList<>();
    protected Semaphore mPacketsSemaphore = new Semaphore(1, true);
    private boolean mIsPacketCacheEnabled;
    protected Context mContext;

    protected long mReceivedBytes = 0;
    protected long mSentBytes = 0;

    public UartPacketManagerBase(@NonNull Context context, @Nullable Listener listener, boolean isPacketCacheEnabled) {
        mContext = context.getApplicationContext();
        mIsPacketCacheEnabled = isPacketCacheEnabled;
        mWeakListener = new WeakReference<>(listener);
    }

    // region Received data: UartRxHandler

    @Override
    public void onRxDataReceived(@NonNull byte[] data, @Nullable String identifier, int status) {
        if (status != BluetoothGatt.GATT_SUCCESS) {
            Log.w(TAG, "onRxDataReceived error:" + status);
            return;
        }

        UartPacket uartPacket = new UartPacket(identifier, UartPacket.TRANSFERMODE_RX, data);

        try {
            mPacketsSemaphore.acquire();
        } catch (InterruptedException e) {
            Log.w(TAG, "InterruptedException: " + e.toString());
        }
        mReceivedBytes += data.length;
        if (mIsPacketCacheEnabled) {
            mPackets.add(uartPacket);
        }

        // Send data to delegate
        Listener listener = mWeakListener.get();
        if (listener != null) {
            mMainHandler.post(() -> listener.onUartPacket(uartPacket));
        }

        mPacketsSemaphore.release();
    }

    public List<UartPacket> getPacketsCache() {
        return mPackets;
    }

    // region Counters
    public void resetCounters() {
        mReceivedBytes = 0;
        mSentBytes = 0;
    }

    public long getReceivedBytes() {
        return mReceivedBytes;
    }

    public long getSentBytes() {
        return mSentBytes;
    }

    // endregion
}

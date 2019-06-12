package com.cyberpunk.ble.beat.connect.app;

import android.app.Service;
import android.bluetooth.BluetoothGatt;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import com.cyberpunk.ble.beat.connect.ble.UartPacket;
import com.cyberpunk.ble.beat.connect.ble.UartPacketManagerBase;
import com.cyberpunk.ble.beat.connect.ble.central.BlePeripheral;
import com.cyberpunk.ble.beat.connect.ble.central.BlePeripheralUart;
import com.cyberpunk.ble.beat.connect.ble.central.BleScanner;
import com.cyberpunk.ble.beat.connect.ble.central.UartDataManager;
import com.cyberpunk.ble.beat.connect.ble.central.UartPacketManager;

import java.util.List;


public class SpotifyBLE extends Service implements BleScanner.BleScannerListener, UartPacketManagerBase.Listener {
    // Constants
    private final static String TAG = BleScanner.class.getSimpleName();

    private String identifier;
    private BlePeripheral peripheral;
    private BlePeripheralUart uartPeripheral;
    BleScanner mScanner;
    private UartPacketManager mUartData;

    public SpotifyBLE() {
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId){
        identifier = intent.getStringExtra("identifier");
        mScanner = BleScanner.getInstance();
        mScanner.setListener(this);
        mUartData = new UartPacketManager(this, this, true);           // Note: mqttmanager should have been initialized previously


        if (!mScanner.isScanning()) {
            Log.d(TAG, "start scanning");
            mScanner.start();
        } else {
            Log.d(TAG, "start scanning: already was scanning");
        }


        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    public void onDestroy (){

    }

    @Override
    public void onScanPeripheralsUpdated(List<BlePeripheral> scanResults) {
        peripheral = mScanner.getPeripheralWithIdentifier(identifier);
        if (peripheral != null && peripheral.getConnectionState() != BlePeripheral.STATE_CONNECTED || peripheral.getConnectionState() != BlePeripheral.STATE_CONNECTING){
            peripheral.connect(this);
            uartPeripheral = new BlePeripheralUart(peripheral);
            uartPeripheral.uartEnable(mUartData, status -> {
                Log.d(TAG, "GATT:" + status);
            });
            mUartData.send(uartPeripheral, "BEAT\n");
            mUartData.send(uartPeripheral, "10\n");
            mUartData.send(uartPeripheral, "12\n");
            mUartData.send(uartPeripheral, "17\n");

        }
    }

    @Override
    public void onScanPeripheralsFailed(int errorCode) {
        Log.e(TAG, "scan peripherals failed: " + errorCode);
    }

    @Override
    public void onScanStatusChanged(boolean isScanning) {

    }

    @Override
    public void onUartPacket(UartPacket packet) {
        Log.d(TAG, "Uart packet: " + packet.getTimestamp() + " : " + packet.getData());
    }
}

package com.cyberpunk.ble.beat.connect.app;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;

import com.cyberpunk.ble.beat.connect.ble.UartPacket;
import com.cyberpunk.ble.beat.connect.ble.UartPacketManagerBase;
import com.cyberpunk.ble.beat.connect.ble.central.BlePeripheral;
import com.cyberpunk.ble.beat.connect.ble.central.BlePeripheralUart;
import com.cyberpunk.ble.beat.connect.ble.central.BleScanner;
import com.cyberpunk.ble.beat.connect.ble.central.UartPacketManager;
import com.spotify.android.appremote.api.ConnectionParams;
import com.spotify.android.appremote.api.Connector;
import com.spotify.android.appremote.api.SpotifyAppRemote;
import com.spotify.protocol.types.Track;

import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;


public class SpotifyBLEService extends Service implements BleScanner.BleScannerListener, UartPacketManagerBase.Listener {
    // Constants
    private final static String TAG = SpotifyBLEService.class.getSimpleName();

    private String identifier;
    private String mAccessToken;
    private BlePeripheral peripheral;
    private BlePeripheralUart uartPeripheral;
    private OkHttpClient mOkHttpClient;
    private Track track;
    private Call mCall;
    private BleScanner mScanner;
    private UartPacketManager mUartData;
    private static final String CLIENT_ID = "9996d1c0a32c49399cec02544beca87b";
    private static final String REDIRECT_URI = "app://blebeat";
    private SpotifyAppRemote mSpotifyAppRemote;
    private ConnectionParams mConnectionParams;


    public SpotifyBLEService() {
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId){
        super.onStartCommand(intent, flags, startId);
        if (intent != null) {
            identifier = intent.getStringExtra("identifier");
            mAccessToken = intent.getStringExtra("token");
        }
        Log.d(TAG, "Search bluetooth identifier: " + identifier);
        mScanner = BleScanner.getInstance();
        mScanner.setListener(this);
        mUartData = new UartPacketManager(this, this, true);           // Note: mqttmanager should have been initialized previously

        mConnectionParams =
                new ConnectionParams.Builder(CLIENT_ID)
                        .setRedirectUri(REDIRECT_URI)
                        .showAuthView(true)
                        .build();

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
        super.onDestroy();
        SpotifyAppRemote.disconnect(mSpotifyAppRemote);
    }

    @Override
    public void onScanPeripheralsUpdated(List<BlePeripheral> scanResults) {
        if(peripheral == null) {
            peripheral = mScanner.getPeripheralWithIdentifier(identifier);
            if (peripheral == null) {
                Log.e(TAG, "Device not found");
            } else {
                mScanner.stop();
                SpotifyAppRemote.connect(this.getApplicationContext(), mConnectionParams,
                        new Connector.ConnectionListener() {
                            @Override
                            public void onConnected(SpotifyAppRemote spotifyAppRemote) {
                                mSpotifyAppRemote = spotifyAppRemote;
                                Log.d(TAG, "Connected with Spotify");

                                // Now you can start interacting with App Remote
                                connected();
                            }

                            @Override
                            public void onFailure(Throwable throwable) {
                                Log.e(TAG, throwable.getMessage(), throwable);
                            }
                        });

                uartPeripheral = new BlePeripheralUart(peripheral);
                uartPeripheral.uartEnable(mUartData, status -> {
                    Log.d(TAG, "GATT:" + status);
                });
            }
        }




        /*if (peripheral != null && (peripheral.getConnectionState() != BlePeripheral.STATE_CONNECTED || peripheral.getConnectionState() != BlePeripheral.STATE_CONNECTING)){
            peripheral.connect(this);
            uartPeripheral = new BlePeripheralUart(peripheral);
            uartPeripheral.uartEnable(mUartData, status -> {
                Log.d(TAG, "GATT:" + status);
            });
            mUartData.send(uartPeripheral, "BEAT\n");
            mUartData.send(uartPeripheral, "10\n");
            mUartData.send(uartPeripheral, "12\n");
            mUartData.send(uartPeripheral, "17\n");

        } */
    }

    // Bluetooth Handler
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

    // Spotify region
    private void connected() {
        mSpotifyAppRemote.getPlayerApi()
                .subscribeToPlayerState()
                .setEventCallback(playerState -> {
                    track = playerState.track;
                    if (track != null && !playerState.isPaused) {
                        Log.d(TAG, track.uri + " " + track.name + " by " + track.artist.name);
                        if (mOkHttpClient == null) {
                            mOkHttpClient = new OkHttpClient();
                        }
                        Request request = new Request.Builder()
                                .url("https://api.spotify.com/v1/audio-analysis/" + track.uri.split(":")[2])
                                .addHeader("Authorization","Bearer " + mAccessToken)
                                .build();

                        if (mCall != null) {
                            mCall.cancel();
                        }
                        mCall = mOkHttpClient.newCall(request);
                        mCall.enqueue(new Callback() {
                            @Override
                            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                                Log.e(TAG, "Failed to fetch data: " + e);
                            }

                            @Override
                            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                                try {
                                    String currentTrack = track.uri;
                                    final JSONObject jsonObject = new JSONObject(response.body().string());
                                    response.close();
                                    JSONArray beats = jsonObject.getJSONArray("beats");
                                    StringBuilder sendString = new StringBuilder();

                                    for(int i = 0; i < beats.length() && !call.isCanceled(); i++) {
                                        double duration = beats.getJSONObject(i).getDouble("duration");
                                        duration *= 1000;
                                        Thread.sleep((int)duration);
                                        if(track == null || !track.uri.equals(currentTrack)) {
                                            return;
                                        }

                                        sendString.append("BEAT " + (int)duration);
                                        sendString.append('\n');
                                        mUartData.send(uartPeripheral, sendString.toString());
                                        sendString.setLength(0);
                                        Thread.sleep((int)duration);
                                    }
                                    sendString.append('\n');
                                    //mUartData.send(uartPeripheral, sendString.toString());
                                    //Thread.sleep(1000);

                                    Log.d(TAG, "send string: " + sendString);
                                } catch (JSONException e) {
                                    Log.e(TAG, "Failed to parse data: " + e);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                            }
                        });
                    } else if(playerState.isPaused){
                        track = null;
                    }
                });
    }
    // End Spotify Region
}

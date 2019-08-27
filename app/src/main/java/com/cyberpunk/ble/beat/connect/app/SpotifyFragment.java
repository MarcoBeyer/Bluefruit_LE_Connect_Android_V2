package com.cyberpunk.ble.beat.connect.app;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.cyberpunk.ble.beat.connect.R;
import com.spotify.sdk.android.authentication.AuthenticationClient;
import com.spotify.sdk.android.authentication.AuthenticationRequest;
import com.spotify.sdk.android.authentication.AuthenticationResponse;

public class SpotifyFragment extends ConnectedPeripheralFragment {
    private static final int SPOTIFY_REQUEST_CODE = 1337;
    private static final String CLIENT_ID = "9996d1c0a32c49399cec02544beca87b";
    private static final String REDIRECT_URI = "app://blebeat";

    public static SpotifyFragment newInstance(@Nullable String singlePeripheralIdentifier) {
        SpotifyFragment fragment = new SpotifyFragment();
        fragment.setArguments(createFragmentArgs(singlePeripheralIdentifier));
        return fragment;
    }

    public SpotifyFragment() {
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // Check if result comes from the correct activity
        AuthenticationResponse response = AuthenticationClient.getResponse(resultCode, data);
        switch (response.getType()) {
            // Response was successful and contains auth token
            case TOKEN:
                Log.d("SpotifyFragment", "token: " + response.getAccessToken());
                Intent intent = new Intent(this.getContext(), SpotifyBLEService.class);
                intent.putExtra("token", response.getAccessToken());
                intent.putExtra("identifier", mBlePeripheral.getIdentifier());
                this.requireContext().stopService(intent);
                this.requireContext().startService(intent);
                break;

            case ERROR:
                Log.e("SpotifyFragment", "token: " + response.getError());
                break;

            // Most likely auth flow was cancelled
            default:
                // Handle other cases
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_spotify, container, false);
        Button button = view.findViewById(R.id.spotify_button_start);
        button.setOnClickListener(v -> {
            AuthenticationRequest.Builder builder =
                    new AuthenticationRequest.Builder(CLIENT_ID, AuthenticationResponse.Type.TOKEN, REDIRECT_URI);
            AuthenticationRequest request = builder.build();
            AuthenticationClient.openLoginActivity(this.requireActivity(), SPOTIFY_REQUEST_CODE, request);
        });
        Button button_stop = view.findViewById(R.id.spotify_button_stop);
        button_stop.setOnClickListener(v -> {
            Intent intent = new Intent(this.getContext(), SpotifyBLEService.class);
            this.requireContext().stopService(intent);
        });
        return view;
    }


}

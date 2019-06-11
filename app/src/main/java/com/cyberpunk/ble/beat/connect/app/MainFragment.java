package com.cyberpunk.ble.beat.connect.app;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.cyberpunk.ble.beat.connect.R;

public class MainFragment extends Fragment {
    // UI
    private ScannerFragment scannerFragment;

    // region Fragment Lifecycle
    public MainFragment() {
        // Required empty public constructor
    }

    public static MainFragment newInstance() {
        return new MainFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Retain this fragment across configuration changes
        setRetainInstance(true);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment

        return inflater.inflate(R.layout.fragment_main, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        scannerFragment = ScannerFragment.newInstance();
        FragmentManager fragmentManager = getChildFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.replace(R.id.navigationContentLayout, scannerFragment)
                .commit();
        setActionBarTitle(getString(R.string.main_tabbar_centralmode));
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        FragmentActivity activity = getActivity();
        if (activity != null) {
            // update options menu with current values
            activity.invalidateOptionsMenu();
        }
    }

    // endregion

    private void setActionBarTitle(String title) {
        AppCompatActivity activity = ((AppCompatActivity) getActivity());
        if (activity != null) {
            ActionBar actionBar = activity.getSupportActionBar();
            if (actionBar != null) {
                actionBar.setTitle(title);
                actionBar.setDisplayHomeAsUpEnabled(false);     // Don't show caret for MainFragment
            }
        }
    }

    // endregion

    // region Actions
    void startScanning() {
        // Send the message to the peripheral mode fragment, or ignore it if is not selected
       scannerFragment.startScanning();
    }

    void disconnectAllPeripherals() {
        scannerFragment.disconnectAllPeripherals();
    }

    // endregion
}
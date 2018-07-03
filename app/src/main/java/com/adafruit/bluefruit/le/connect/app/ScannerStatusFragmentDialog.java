package com.adafruit.bluefruit.le.connect.app;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatDialog;
import android.support.v7.app.AppCompatDialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

import com.adafruit.bluefruit.le.connect.R;

public class ScannerStatusFragmentDialog extends AppCompatDialogFragment {
    // Params
    protected final static String kParamMessage = "message";
    protected final static String kParamBlePeripheralId = "blePeripheralIdentifier";

    // Inteface
    interface onScannerStatusCancelListener {
        void scannerStatusCancelled(@NonNull String blePeripheralIdentifier);
    }

    // UI
    private TextView mMessageTextView;

    // Data
    private String mMessage;
    private String mBlePeripheralIdentifier;
    private onScannerStatusCancelListener mCancelListener;

    // region Fragment Lifecycle
    public static ScannerStatusFragmentDialog newInstance(@Nullable String message, @NonNull String blePeripheralIdentifier) {
        ScannerStatusFragmentDialog fragment = new ScannerStatusFragmentDialog();
        Bundle args = new Bundle();
        args.putString(kParamMessage, message);
        args.putString(kParamBlePeripheralId, blePeripheralIdentifier);
        fragment.setArguments(args);
        return fragment;
    }


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle bundle = getArguments();
        if (bundle != null) {
            mMessage = bundle.getString(kParamMessage);
            mBlePeripheralIdentifier = bundle.getString(kParamBlePeripheralId);
        }
    }

    @SuppressWarnings("UnnecessaryLocalVariable")
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Remove title
        AppCompatDialog dialog = (AppCompatDialog) getDialog();
        dialog.supportRequestWindowFeature(Window.FEATURE_NO_TITLE);

        View view = inflater.inflate(R.layout.layout_common_message_dialog, container, false);
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mMessageTextView = view.findViewById(R.id.messageTextView);
        mMessageTextView.setText(mMessage);
    }

    @Override
    public void onStart() {
        super.onStart();

        // https://stackoverflow.com/questions/42501704/how-to-make-dialogfragment-width-match-parent
        Window window = getDialog().getWindow();
        if (window != null) {
            window.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT);
        }
    }

    @Override
    public void onDestroyView() {
        Dialog dialog = getDialog();

        // Work around bug: http://code.google.com/p/android/issues/detail?id=17423
        if ((dialog != null) && getRetainInstance())
            dialog.setDismissMessage(null);

        super.onDestroyView();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof onScannerStatusCancelListener) {
            mCancelListener = (onScannerStatusCancelListener) context;
        } else if (getTargetFragment() instanceof onScannerStatusCancelListener) {
            mCancelListener = (onScannerStatusCancelListener) getTargetFragment();
        } /*else if (getTargetFragment() != null && getTargetFragment().getActivity() instanceof Listener) {
            mListener = (Listener) getTargetFragment().getActivity();
        } */ else {
            throw new RuntimeException(context.toString() + " must implement Listener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mCancelListener = null;
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        super.onCancel(dialog);

        mCancelListener.scannerStatusCancelled(mBlePeripheralIdentifier);
    }

    public void setMessage(int messageId) {
        Context context = getContext();
        if (context != null) {
            setMessage(context.getString(messageId));
        }
    }

    public void setMessage(String message) {
        mMessage = message;
        mMessageTextView.setText(mMessage);
    }

    public boolean isInitialized() {
        return mMessageTextView != null;
    }
}

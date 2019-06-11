package com.cyberpunk.ble.beat.connect.style;

import android.content.Context;
import com.google.android.material.snackbar.Snackbar;
import androidx.core.content.ContextCompat;
import android.widget.TextView;

import com.cyberpunk.ble.beat.connect.R;

public class StyledSnackBar {

    public static void styleSnackBar(Snackbar snackbar, Context context) {
        snackbar.getView().setBackgroundColor(ContextCompat.getColor(context, R.color.colorAccent));
        TextView textView = snackbar.getView().findViewById(com.google.android.material.R.id.snackbar_text);     // Manually changing colors till theme styling is available for snackbars
        textView.setTextColor(ContextCompat.getColor(context, R.color.infotext));
    }
}

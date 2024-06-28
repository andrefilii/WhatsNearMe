package it.andreafilippi.whatsnearme.ui.dialogs;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

public class BluetoothDeviceListDialog extends DialogFragment {
    private final DialogInterface.OnClickListener callback;
    private ArrayAdapter<String> devicesAdapter;

    public BluetoothDeviceListDialog(ArrayAdapter<String> adapter, DialogInterface.OnClickListener listener) {
        devicesAdapter = adapter;
        callback = listener;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        super.onCreateDialog(savedInstanceState);

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Dispositivi Bluetooth nelle vicinanze");
        builder.setAdapter(devicesAdapter, (dialog, which) -> {
            if (callback != null)
                callback.onClick(dialog, which);
        });

        return builder.create();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }
}

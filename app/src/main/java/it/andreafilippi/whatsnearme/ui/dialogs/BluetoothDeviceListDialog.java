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
    private final OnClickListener listener;
    private final ArrayAdapter<String> devicesAdapter;

    public interface OnClickListener {
        void onClick(DialogInterface dialogInterface, int which);
    }

    public BluetoothDeviceListDialog(ArrayAdapter<String> adapter, OnClickListener listener) {
        devicesAdapter = adapter;
        this.listener = listener;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        super.onCreateDialog(savedInstanceState);

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Dispositivi Bluetooth nelle vicinanze");
        builder.setAdapter(devicesAdapter, (dialog, which) -> {
            if (listener != null)
                listener.onClick(dialog, which);
        });

        return builder.create();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }
}

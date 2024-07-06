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
    private final OnClickListener onClickListener;
    private final OnCancelListener onCancelListener;
    private final ArrayAdapter<String> devicesAdapter;

    public interface OnClickListener {
        void onClick(DialogInterface dialogInterface, int which);
    }

    public interface OnCancelListener {
        void onCancel(DialogInterface dialog);
    }

    public BluetoothDeviceListDialog(ArrayAdapter<String> adapter, OnClickListener onClickListener, OnCancelListener cancelListener) {
        devicesAdapter = adapter;
        this.onClickListener = onClickListener;
        this.onCancelListener = cancelListener;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        super.onCreateDialog(savedInstanceState);

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Dispositivi Bluetooth nelle vicinanze");
        builder.setAdapter(devicesAdapter, (dialog, which) -> {
            if (onClickListener != null)
                onClickListener.onClick(dialog, which);
        });

        return builder.create();
    }

    @Override
    public void onCancel(@NonNull DialogInterface dialog) {
        super.onCancel(dialog);
        if (onCancelListener != null) onCancelListener.onCancel(dialog);
    }

    @Override
    public void onDismiss(@NonNull DialogInterface dialog) {
        super.onDismiss(dialog);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }
}

package it.andreafilippi.whatsnearme.ui.dialogs;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import java.io.Serializable;
import java.util.ArrayList;

public class BluetoothDeviceListDialog extends DialogFragment {
    private static final String ARG_ITEMS = "arg_items";
    private static final String ARG_ONCLICK = "arg_onclick";
    private static final String ARG_ONCANCEL = "arg_oncancel";
    private static final String ARG_ADDRESSES = "arg_addresses";


    private OnClickListener onClickListener;
    private OnCancelListener onCancelListener;
    private ArrayAdapter<String> devicesAdapter;
    private ArrayList<String> adaperItems;

    private ArrayList<String> devicesAddresses;

    public interface OnClickListener extends Serializable {
        void onClick(DialogInterface dialogInterface, int which, String deviceInfo);
    }

    public interface OnCancelListener extends Serializable{
        void onCancel(DialogInterface dialog);
    }


    public static BluetoothDeviceListDialog newInstance(OnClickListener onClickListener, OnCancelListener cancelListener) {

        Bundle args = new Bundle();
        args.putSerializable(ARG_ONCLICK, onClickListener);
        args.putSerializable(ARG_ONCANCEL, cancelListener);

        BluetoothDeviceListDialog fragment = new BluetoothDeviceListDialog();
        fragment.setArguments(args);
        return fragment;
    }

    public BluetoothDeviceListDialog() {

    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        super.onCreateDialog(savedInstanceState);

        if (getArguments() != null) {
            this.onClickListener = getArguments().getSerializable(ARG_ONCLICK, OnClickListener.class);
            this.onCancelListener = getArguments().getSerializable(ARG_ONCANCEL, OnCancelListener.class);
        }

        devicesAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_list_item_single_choice);

        if (savedInstanceState != null) {
            adaperItems = savedInstanceState.getStringArrayList(ARG_ITEMS);
            devicesAddresses = savedInstanceState.getStringArrayList(ARG_ADDRESSES);

            devicesAdapter.addAll(adaperItems);
            devicesAdapter.notifyDataSetChanged();
        } else {
            adaperItems = new ArrayList<>();
            devicesAddresses = new ArrayList<>();
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Dispositivi Bluetooth nelle vicinanze");
        builder.setAdapter(devicesAdapter, (dialog, which) -> {
            if (onClickListener != null)
                onClickListener.onClick(dialog, which, devicesAdapter.getItem(which));
        });

        return builder.create();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (adaperItems != null && devicesAddresses != null) {
            outState.putStringArrayList(ARG_ITEMS, adaperItems);
            outState.putStringArrayList(ARG_ADDRESSES, devicesAddresses);
        }
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

    public void addDevice(String name, String address) {
        if (!devicesAddresses.contains(address)) {
            String item = name + "\n" + address;
            devicesAdapter.add(item);
            devicesAdapter.notifyDataSetChanged();
            adaperItems.add(item);
            devicesAddresses.add(address);
        }
    }
}

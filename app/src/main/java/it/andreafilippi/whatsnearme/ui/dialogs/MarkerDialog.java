package it.andreafilippi.whatsnearme.ui.dialogs;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.nfc.NfcAdapter;
import android.os.Bundle;
import android.os.ParcelUuid;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.PopupMenu;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.DialogFragment;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipDrawable;
import com.google.android.material.chip.ChipGroup;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import it.andreafilippi.whatsnearme.R;
import it.andreafilippi.whatsnearme.databinding.DialogMarkerBinding;
import it.andreafilippi.whatsnearme.entities.Place;

public class MarkerDialog extends DialogFragment {
    private static final int REQUEST_ENABLE_BT_CODE = 69;

    private DialogMarkerBinding binding;
    private Place place;
    private PopupMenu shareMenu;
    private NfcAdapter nfcAdapter;
    private BluetoothAdapter bluetoothAdapter;
    private ArrayAdapter<String> devicesArrayAdapter;

    private final BroadcastReceiver deviceFoundReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // trovato un dispositivo
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice.class);
                if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(requireActivity(),
                            new String[]{Manifest.permission.BLUETOOTH_CONNECT}, REQUEST_ENABLE_BT_CODE);
                    return;
                }
                String deviceName = device.getName();
                String deviceHardwareAddress = device.getAddress(); // MAC
                Log.d("BT DISCOVER", deviceName + " " + deviceHardwareAddress);
                if (deviceName != null) {
                    devicesArrayAdapter.add(deviceName + "\n" + deviceHardwareAddress);
                }
            }
        }
    };

    private final BroadcastReceiver bondChangedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(action)) {
                int bondState = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.ERROR);
                if (bondState == BluetoothDevice.BOND_BONDED) {
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice.class);
                    if (device != null) {
                        startBluetoothConnection(device);
                    }
                }
            }
        }
    };

    public MarkerDialog(Place place) {
        this.place = place;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        binding = DialogMarkerBinding.inflate(inflater, container, false);

        binding.title.setText(place.getName());

        shareMenu = new PopupMenu(requireContext(), binding.condividiBtn);
        shareMenu.getMenuInflater().inflate(R.menu.share_location_menu, shareMenu.getMenu());
        shareMenu.setOnMenuItemClickListener(this::onMenuItemClickListener);

        binding.indicazioniBtn.setOnClickListener(this::onIndicazioniBtnClick);
        binding.segnaVisitatoBtn.setOnClickListener(this::onSegnaVisitatoBtnClick);
        binding.condividiBtn.setOnClickListener(this::onCondividiBtnClick);

        if (place.getTags() != null) {
            //creazione chips
            ChipGroup chipGroup = binding.chipGroup;
            ViewGroup.LayoutParams layoutParams =
                    new ChipGroup.LayoutParams(ChipGroup.LayoutParams.WRAP_CONTENT, ChipGroup.LayoutParams.WRAP_CONTENT);
            for (String tag : place.getTags()) {
                Chip chip = new Chip(requireContext());
                chip.setLayoutParams(layoutParams);
                chip.setClickable(false);
                chip.setText(tag);
                chip.setChipDrawable(ChipDrawable.createFromAttributes(requireContext(), null, 0, com.google.android.material.R.style.Widget_MaterialComponents_Chip_Filter));

                chipGroup.addView(chip);
            }
        }

        getDialog().setCanceledOnTouchOutside(true);

        nfcAdapter = NfcAdapter.getDefaultAdapter(requireContext());
        if (nfcAdapter == null) {
            // nfc non supportato
            shareMenu.getMenu().findItem(R.id.option_nfc).setEnabled(false);
        }

        BluetoothManager bluetoothManager = requireActivity().getSystemService(BluetoothManager.class);
        bluetoothAdapter = bluetoothManager.getAdapter();
        if (bluetoothAdapter == null) {
            // il dispositivo non supporta il bluetooth
            shareMenu.getMenu().findItem(R.id.option_bluetooth).setEnabled(false);
        } else {
            IntentFilter f = new IntentFilter(BluetoothDevice.ACTION_FOUND);
            requireActivity().registerReceiver(deviceFoundReceiver, f);

            IntentFilter f2 = new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
            requireActivity().registerReceiver(bondChangedReceiver, f2);
        }

        return binding.getRoot();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (nfcAdapter != null)
            nfcAdapter.disableForegroundDispatch(getActivity());
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        requireActivity().unregisterReceiver(deviceFoundReceiver);
        requireActivity().unregisterReceiver(bondChangedReceiver);
    }

    private void onCondividiBtnClick(View view) {
        shareMenu.show();
    }

    private void onSegnaVisitatoBtnClick(View view) {
    }

    private void onIndicazioniBtnClick(View view) {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(getPlaceLocationUri()));
        intent.setPackage(null);
        startActivity(intent);
    }

    private boolean onMenuItemClickListener(MenuItem menuItem) {
        int menuItemId = menuItem.getItemId();

        if (menuItemId == R.id.option_nfc) {
            onShareMenuNfcClick();
            return true;
        }
        if (menuItemId == R.id.option_bluetooth) {
            onShareMenuBluetoothClick();
            return true;
        }
        if (menuItemId == R.id.option_other) {
            onShareMenuOtherClick();
        }

        return false;
    }

    private void onShareMenuNfcClick() {
//        NdefRecord record = NdefRecord.createUri(getPlaceLocationUri());
//        NdefMessage message = new NdefMessage(new NdefRecord[]{record});
//
//        if (nfcAdapter != null && nfcAdapter.isEnabled()) {
//            Intent intent = new Intent(requireContext(), getClass());
//            intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
//            nfcAdapter.enableForegroundDispatch(requireActivity(), intent, null, null);
//
//            Toast.makeText(requireContext(), "Tocca un altro dispositivo per condividere la posizione", Toast.LENGTH_LONG).show();
//        } else {
//            Toast.makeText(requireContext(), "NFC non è abilitato", Toast.LENGTH_LONG).show();
//        }
    }

    private void onShareMenuBluetoothClick() {
        if (!bluetoothAdapter.isEnabled()) {
            Toast.makeText(requireContext(), "Bluetooth non attivo!", Toast.LENGTH_SHORT).show();
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivity(enableBtIntent);
            return;
        }
        // è abilitato, inizio a cercare dispositivi vicini
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(requireActivity(),
                    new String[]{Manifest.permission.BLUETOOTH_SCAN}, REQUEST_ENABLE_BT_CODE);
            return;
        }
        devicesArrayAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_list_item_single_choice);
        BluetoothDeviceListDialog dialog = new BluetoothDeviceListDialog(devicesArrayAdapter, this::onBtListClick);
        dialog.show(getParentFragmentManager(), "bt_list_dialog");
        bluetoothAdapter.startDiscovery();
    }

    private void onBtListClick(DialogInterface dialog, int which) {
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED) {
            bluetoothAdapter.cancelDiscovery();
        }

        dialog.cancel();
        String deviceInfo = devicesArrayAdapter.getItem(which);
        String deviceMAC = deviceInfo.substring(deviceInfo.length() - 17);

        // provo a fare il pairing tra dispositivi
        BluetoothDevice device = bluetoothAdapter.getRemoteDevice(deviceMAC);
        if(BluetoothDevice.BOND_BONDED == device.getBondState()) {
            // già connesso, inizio il trasferimento
            new Thread(() -> startBluetoothConnection(device)).start();
        } else {
            if (device.createBond())
                Toast.makeText(requireContext(), "Inizio pairing", Toast.LENGTH_SHORT).show();
            else
                Toast.makeText(requireContext(), "Errore pairing", Toast.LENGTH_SHORT).show();
        }
    }

    private void onShareMenuOtherClick() {
        String uri = getPlaceLocationUri();

        Intent sharingIntent = new Intent(android.content.Intent.ACTION_SEND);
        sharingIntent.setType("text/plain");
        String ShareSub = "Ti ho condiviso questa posizione!";
        sharingIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, ShareSub);
        sharingIntent.putExtra(android.content.Intent.EXTRA_TEXT, uri);
        startActivity(Intent.createChooser(sharingIntent, "Condividi con"));
    }

    private String getPlaceLocationUri() {
        return "https://maps.google.com/maps?q=" + place.getLat() + "," + place.getLng() + "+(" + place.getName().replace(" ", "+") + ")";
    }

    private void startBluetoothConnection(BluetoothDevice device) {
        Log.d("BT DISCOVER", "Device selezionato: " + device);

        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(requireActivity(),
                    new String[]{Manifest.permission.BLUETOOTH_CONNECT}, REQUEST_ENABLE_BT_CODE);
            return;
        }

        ParcelUuid[] uuids = device.getUuids();
        try {
            //            UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
            BluetoothSocket bs = device.createInsecureRfcommSocketToServiceRecord(UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"));
            //            Method m = device.getClass().getMethod("createRfcommSocket", int.class);
            //            BluetoothSocket bs =  (BluetoothSocket) m.invoke(device, 1);
            // FINALMENTE, invio i dati
            bs.connect();
            OutputStream os = bs.getOutputStream();
            os.write(getPlaceLocationUri().getBytes(StandardCharsets.UTF_8));
            os.flush();
            os.close();
        } catch (IOException e) {
            Log.e("BT CONNECT", e.toString());
            requireActivity().runOnUiThread(() -> {
                Toast.makeText(requireContext(), "Trasferimento via Bluetooth fallito", Toast.LENGTH_SHORT).show();
            });
        }
    }
}

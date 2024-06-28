package it.andreafilippi.whatsnearme.ui.dialogs;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.nfc.NfcAdapter;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.PopupMenu;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.DialogFragment;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipDrawable;
import com.google.android.material.chip.ChipGroup;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import it.andreafilippi.whatsnearme.R;
import it.andreafilippi.whatsnearme.databinding.DialogMarkerBinding;
import it.andreafilippi.whatsnearme.entities.Place;
import it.andreafilippi.whatsnearme.utils.DatabaseHelper;

public class MarkerDialog extends DialogFragment {

    private ActivityResultLauncher<String[]> bluetoothScanPermissionLauncher;

    private DialogMarkerBinding binding;
    private Place place;
    private PopupMenu shareMenu;
    private NfcAdapter nfcAdapter;
    private BluetoothAdapter bluetoothAdapter;
    private ArrayAdapter<String> devicesArrayAdapter;
    private DatabaseHelper databaseHelper;

    private final BroadcastReceiver deviceFoundReceiver = new BroadcastReceiver() {
        @SuppressWarnings("MissingPermission")
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // trovato un dispositivo
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice.class);
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
                        startBtConnection(device);
                    }
                }
            }
        }
    };

    public MarkerDialog(Place place) {
        this.place = place;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        bluetoothScanPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestMultiplePermissions(),
                result -> {
                    Boolean btScanGranted = result.getOrDefault(Manifest.permission.BLUETOOTH_SCAN, false);
                    Boolean btConnectGranted = result.getOrDefault(Manifest.permission.BLUETOOTH_CONNECT, false);
                    if (btScanGranted && btConnectGranted) {
                        startBtDiscovery();
                    } else {
                        Toast.makeText(getContext(), "Permesso negato :(", Toast.LENGTH_SHORT).show();
                    }
                });
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

        databaseHelper = new DatabaseHelper(requireContext());
        // TODO controllare se il luogo è già segnato come visitato, e nel caso cambiare icona

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
            IntentFilter f1 = new IntentFilter(BluetoothDevice.ACTION_FOUND);
            requireActivity().registerReceiver(deviceFoundReceiver, f1);

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

        databaseHelper.close();
    }

    /* -- BUTTON LISTENERS -- */

    private void onCondividiBtnClick(View view) {
        shareMenu.show();
    }

    private void onSegnaVisitatoBtnClick(View view) {
        // TODO se il luogo è già visitato il pulsante serve a rimuoverlo dal db
        // if <luogo gia visitato> then database.removeLuogo(place.getId)

        // TODO far scattare la foto

        new Thread(() -> {
            if (databaseHelper.addLuogo(place, null) != 1) {
                requireActivity().runOnUiThread(() -> Toast.makeText(requireContext(), "Luogo salvato con successo nel diario!", Toast.LENGTH_SHORT).show());
            } else {
                // TODO gestire errore oltre al dialog immagino eliminare la foto salvata
            }

        }).start();
    }

    private void onIndicazioniBtnClick(View view) {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(getPlaceLocationUri()));
        intent.setPackage(null);
        startActivity(intent);
    }

    /* ---------------------- */

    /* -- GESTIONE MENU CONDIVISIONE -- */

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
            return true;
        }

        return false;
    }

    private void onShareMenuNfcClick() {
        // TODO, forse non si può fare
    }

    private void onShareMenuBluetoothClick() {
        if (!bluetoothAdapter.isEnabled()) {
            Toast.makeText(requireContext(), "Bluetooth non attivo!", Toast.LENGTH_SHORT).show();
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivity(enableBtIntent);
            return;
        }
        // è abilitato, inizio a cercare dispositivi vicini
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(requireContext(), "Per favore accettare e riprovare", Toast.LENGTH_SHORT).show();
            bluetoothScanPermissionLauncher.launch(new String[]{Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT});
        } else {
            startBtDiscovery();
        }
    }

    @SuppressWarnings("MissingPermission")
    private void startBtDiscovery() {
        devicesArrayAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_list_item_single_choice);
        BluetoothDeviceListDialog dialog = new BluetoothDeviceListDialog(devicesArrayAdapter, this::onBtListClick);
        dialog.show(getParentFragmentManager(), "bt_list_dialog");
        bluetoothAdapter.startDiscovery();
    }

    private void onBtListClick(DialogInterface dialog, int which) {
        // ho selezionato un dispositivo, disattivo la ricerca
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED) {
            bluetoothAdapter.cancelDiscovery();
        }

        // recupero l'indirizzo MAC (è scritto nella lista)
        dialog.cancel();
        String deviceInfo = devicesArrayAdapter.getItem(which);
        String deviceMAC = deviceInfo.substring(deviceInfo.length() - 17);

        // provo a fare il pairing tra dispositivi
        BluetoothDevice device = bluetoothAdapter.getRemoteDevice(deviceMAC);
        if(BluetoothDevice.BOND_BONDED == device.getBondState()) {
            // già connesso, inizio il trasferimento
            startBtConnection(device);
        } else {
            // provo a fare il pairing, se va a buon fine verrà chiamato il receiver definito sopra
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

    private void startBtConnection(BluetoothDevice device) {
        new Thread(() -> connectAndSendDataBluetooth(device)).start();
    }

    @SuppressWarnings("MissingPermission")
    private void connectAndSendDataBluetooth(BluetoothDevice device) {
        Log.d("BT DISCOVER", "Device selezionato: " + device);

        try (BluetoothSocket bs = device.createInsecureRfcommSocketToServiceRecord(UUID.fromString(getString(R.string.btConnectionUID)))) {
            // FINALMENTE, invio i dati
            bs.connect();
            OutputStream os = bs.getOutputStream();
            os.write(getPlaceLocationUri().getBytes(StandardCharsets.UTF_8));
            os.flush();

        } catch (IOException e) {
            Log.e("BT CONNECT", e.toString());
            requireActivity().runOnUiThread(() -> {
                Toast.makeText(requireContext(), "Trasferimento via Bluetooth fallito", Toast.LENGTH_SHORT).show();
            });
        }
    }
}

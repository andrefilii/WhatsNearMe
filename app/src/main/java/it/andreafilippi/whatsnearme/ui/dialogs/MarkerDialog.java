package it.andreafilippi.whatsnearme.ui.dialogs;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
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
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.PopupMenu;

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
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import it.andreafilippi.whatsnearme.R;
import it.andreafilippi.whatsnearme.databinding.DialogMarkerBinding;
import it.andreafilippi.whatsnearme.entities.MyPlace;
import it.andreafilippi.whatsnearme.ui.activities.CameraActivity;
import it.andreafilippi.whatsnearme.utils.DatabaseHelper;
import it.andreafilippi.whatsnearme.utils.Utils;

public class MarkerDialog extends DialogFragment {
    public static final String ARG_PLACE = "place";

    private static final int VISITATO_IDK = -1;
    private static final int VISITATO_YES = 1;
    private static final int VISITATO_NO = 0;

    private DialogMarkerBinding binding;

    private ActivityResultLauncher<String[]> bluetoothScanPermissionLauncher;
    private ActivityResultLauncher<Intent> cameraLauncher;

    private MyPlace place;
    private PopupMenu shareMenu;
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothDeviceListDialog btDeviceDialog;
    private DatabaseHelper databaseHelper;
    private AtomicInteger isLuogoGiaVisitato;

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
                if (btDeviceDialog != null && deviceName != null) {
                    btDeviceDialog.addDevice(deviceName, deviceHardwareAddress);
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
                        Utils.makeToastShort(requireContext(), "Pairing effettuato, invio il link");
                        startBtConnection(device);
                    }
                }
            }
        }
    };


    public static MarkerDialog newInstance(MyPlace place) {
        Bundle args = new Bundle();
        args.putParcelable(ARG_PLACE, place);
        MarkerDialog md = new MarkerDialog();
        md.setArguments(args);
        return md;
    }

    public MarkerDialog() {

    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        if (getArguments() != null)
            this.place = getArguments().getParcelable(ARG_PLACE, MyPlace.class);

        bluetoothScanPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestMultiplePermissions(),
                result -> {
                    Boolean btScanGranted = result.getOrDefault(Manifest.permission.BLUETOOTH_SCAN, false);
                    Boolean btConnectGranted = result.getOrDefault(Manifest.permission.BLUETOOTH_CONNECT, false);
                    if (Boolean.TRUE.equals(btScanGranted) && Boolean.TRUE.equals(btConnectGranted)) {
                        startBtDiscovery();
                    } else {
                        Utils.makeToastShort(requireContext(), "Permessi non concessi");
                    }
                });

        cameraLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == CameraActivity.RESULT_OK) {
                        Intent data = result.getData();
                        if (data != null) {
                            salvaLuogoNelDiario(data.getStringExtra(CameraActivity.EXTRA_IMAGE_PATH));
                        }
                    } else {
                        Utils.makeToastShort(requireContext(), "Errore durante lo scatto");
                    }
                });

        binding = DialogMarkerBinding.inflate(inflater, container, false);

        binding.title.setText(place.getName());

        // creo il menu per la condivisione
        shareMenu = new PopupMenu(requireContext(), binding.condividiBtn);
        shareMenu.getMenuInflater().inflate(R.menu.share_location_menu, shareMenu.getMenu());
        shareMenu.setOnMenuItemClickListener(this::onMenuItemClickListener);

        // associazione listener ai pulsanti
        binding.indicazioniBtn.setOnClickListener(this::onIndicazioniBtnClick);
        binding.segnaVisitatoBtn.setOnClickListener(this::onSegnaVisitatoBtnClick);
        binding.condividiBtn.setOnClickListener(this::onCondividiBtnClick);

        // setting del pulsante per il diario di viaggio
        databaseHelper = new DatabaseHelper(requireContext());
        isLuogoGiaVisitato = new AtomicInteger(VISITATO_IDK);
        new Thread(() -> {
            if(databaseHelper.doesLuogoExist(place.getId())) {
                isLuogoGiaVisitato.set(VISITATO_YES);
            } else {
                isLuogoGiaVisitato.set(VISITATO_NO);
            }
            requireActivity().runOnUiThread(() -> {
                if (isLuogoGiaVisitato.get() == VISITATO_YES) {
                    binding.segnaVisitatoBtn.setBackgroundResource(R.drawable.rounded_background_red);
                } else {
                    binding.segnaVisitatoBtn.setBackgroundResource(R.drawable.rounded_background_grey);
                }
            });
        }).start();

        if (place.getTags() != null) {
            //creazione chips con i tags del luogo
            ChipGroup chipGroup = binding.chipGroup;
            ViewGroup.LayoutParams layoutParams =
                    new ChipGroup.LayoutParams(ChipGroup.LayoutParams.WRAP_CONTENT, ChipGroup.LayoutParams.WRAP_CONTENT);
            for (String tag : place.getTags()) {
                Chip chip = new Chip(requireContext());
                chip.setLayoutParams(layoutParams);
                chip.setClickable(false);
                chip.setText(tag.replace("_", " "));
                chip.setChipDrawable(ChipDrawable.createFromAttributes(requireContext(), null, 0, com.google.android.material.R.style.Widget_MaterialComponents_Chip_Filter));

                chipGroup.addView(chip);
            }
        }

        // il dialog può essere chiuso cliccando all'esterno
        getDialog().setCanceledOnTouchOutside(true);

        // setting per la condivisione bluetooth
        BluetoothManager bluetoothManager = requireActivity().getSystemService(BluetoothManager.class);
        bluetoothAdapter = bluetoothManager.getAdapter();
        if (bluetoothAdapter == null) {
            // il dispositivo non supporta il bluetooth
            shareMenu.getMenu().findItem(R.id.option_bluetooth).setEnabled(false);
        } else {
            requireActivity().registerReceiver(deviceFoundReceiver,
                    new IntentFilter(BluetoothDevice.ACTION_FOUND));

            requireActivity().registerReceiver(bondChangedReceiver,
                    new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED));
        }

        return binding.getRoot();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
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
        if (isLuogoGiaVisitato.get() == VISITATO_IDK) {
            // ancora in caricamento
            Utils.makeToastShort(requireContext(), "Attendere il caricamento dei dati");
        } else if (isLuogoGiaVisitato.get() == VISITATO_YES) {
            // è già visitato, mostro dialog per rimozione
            new AlertDialog.Builder(requireContext())
                    .setTitle("Conferma rimozione")
                    .setMessage("Sei sicuro di voler rimuovere il luogo dal diario di viaggio?")
                    .setPositiveButton("Si", (dialog, which) -> {
                        isLuogoGiaVisitato.set(VISITATO_IDK);
                        new Thread(() -> {
                            if (databaseHelper.removeLuogo(place.getId())) {
                                // è andato a buon fine
                                isLuogoGiaVisitato.set(VISITATO_NO);
                                requireActivity().runOnUiThread(() -> {
                                    binding.segnaVisitatoBtn.setBackgroundResource(R.drawable.rounded_background_grey);
                                    Utils.makeToastShort(requireContext(), "Luogo rimosso dal diario");
                                });
                            } else {
                                isLuogoGiaVisitato.set(VISITATO_YES);
                                requireActivity().runOnUiThread(() -> Utils.makeToastShort(requireContext(), "Errore durante la rimozione"));
                            }
                        }).start();
                        dialog.dismiss();
                    })
                    .setNegativeButton("No", (dialog, which) -> {
                        dialog.dismiss();
                    })
                    .create()
                    .show();
        } else {
            // non è visitato, mostro dialog per aggiungere una foto
            new AlertDialog.Builder(requireContext())
                    .setTitle("Vuoi aggiungere una foto al luogo?")
                    .setPositiveButton("Si", (dialog, which) -> {
                        Intent intent = new Intent(requireActivity(), CameraActivity.class);
                        cameraLauncher.launch(intent);
                        dialog.dismiss();
                    })
                    .setNegativeButton("No", (dialog, which) -> {
                        salvaLuogoNelDiario(null);
                        dialog.dismiss();
                    }).show();
        }
    }

    private void salvaLuogoNelDiario(String pathFoto) {
        if (pathFoto != null) Log.d("MD-SAVE-PHOTO", pathFoto);
        new Thread(() -> {
            if (databaseHelper.addLuogo(place, pathFoto) != -1) {
                isLuogoGiaVisitato.set(VISITATO_YES);
                requireActivity().runOnUiThread(() -> {
                    binding.segnaVisitatoBtn.setBackgroundResource(R.drawable.rounded_background_red);
                    Utils.makeToastShort(requireContext(), "Luogo salvato nel diario!");
                });
            } else {
                requireActivity().runOnUiThread(() -> Utils.makeToastShort(requireContext(), "Errore durante il salvataggio"));
            }

        }).start();
    }

    private void onIndicazioniBtnClick(View view) {
        // apro il link di google maps, se è presente sul telefono apre direttamente l'app
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(Utils.getPlaceLocationUrl(place)));
        intent.setPackage(null);
        startActivity(intent);
    }


    /* -- GESTIONE MENU CONDIVISIONE -- */

    private boolean onMenuItemClickListener(MenuItem menuItem) {
        int menuItemId = menuItem.getItemId();

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

    private void onShareMenuOtherClick() {
        String uri = Utils.getPlaceLocationUrl(place);

        Intent sharingIntent = new Intent(android.content.Intent.ACTION_SEND);
        sharingIntent.setType("text/plain");
        String ShareSub = "Ti ho condiviso questa posizione!";
        sharingIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, ShareSub);
        sharingIntent.putExtra(android.content.Intent.EXTRA_TEXT, uri);
        startActivity(Intent.createChooser(sharingIntent, "Condividi con"));
    }

    private void onShareMenuBluetoothClick() {
        if (!bluetoothAdapter.isEnabled()) {
            Utils.makeToastShort(requireContext(), "Bluetooth non attivo!");
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivity(enableBtIntent);
            return;
        }

        // è abilitato, inizio a cercare dispositivi vicini
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            bluetoothScanPermissionLauncher.launch(new String[]{Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT});
        } else {
            startBtDiscovery();
        }
    }

    @SuppressWarnings("MissingPermission")
    private void startBtDiscovery() {
        btDeviceDialog = BluetoothDeviceListDialog.newInstance(this::onBtListClick, this::onBtDeviceCancel);
        btDeviceDialog.show(getParentFragmentManager(), "bt_list_dialog");
        bluetoothAdapter.startDiscovery();
    }

    @SuppressLint("MissingPermission")
    private void onBtDeviceCancel(DialogInterface dialog) {
        bluetoothAdapter.cancelDiscovery();
    }

    @SuppressLint("MissingPermission")
    private void onBtListClick(DialogInterface dialog, int which, String deviceInfo) {
        // ho selezionato un dispositivo, disattivo la ricerca
        bluetoothAdapter.cancelDiscovery();

        // recupero l'indirizzo MAC (è scritto nella entry)
        dialog.dismiss();
        String deviceMAC = deviceInfo.substring(deviceInfo.length() - 17);

        // provo a fare il pairing tra dispositivi
        BluetoothDevice device = bluetoothAdapter.getRemoteDevice(deviceMAC);
        if(BluetoothDevice.BOND_BONDED == device.getBondState()) {
            // già connesso, inizio il trasferimento
            startBtConnection(device);
        } else {
            // provo a fare il pairing, se va a buon fine verrà chiamato il receiver definito sopra
            if (device.createBond())
                Utils.makeToastShort(requireContext(), "Inizio pairing");
            else
                Utils.makeToastShort(requireContext(), "Errore pairing");
        }
    }

    private void startBtConnection(BluetoothDevice device) {
        new Thread(() -> connectAndSendDataBluetooth(device)).start();
    }

    @SuppressWarnings("MissingPermission")
    private void connectAndSendDataBluetooth(BluetoothDevice device) {
        Log.d("BT DISCOVER", "Device selezionato: " + device);
        try {
            BluetoothSocket bs = device.createInsecureRfcommSocketToServiceRecord(UUID.fromString(getString(R.string.btConnectionUID)));
            // FINALMENTE, invio i dati
            bs.connect();
            OutputStream os = bs.getOutputStream();
            os.write(Utils.getPlaceLocationUrl(place).getBytes(StandardCharsets.UTF_8));
            os.flush();

            requireActivity().runOnUiThread(() -> {
                Utils.makeToastShort(requireContext(), "Trasferimento andato a buon fine");
            });

            // attendo di riceve un singolo byte di modo da attendere che l'altro dispositivo
            //  abbia ricevuto il messaggio
            bs.getInputStream().read();
            bs.close();

        } catch (IOException e) {
            Log.e("BT CONNECT", e.toString());
            requireActivity().runOnUiThread(() -> {
                Utils.makeToastShort(requireContext(), "Trasferimento via Bluetooth fallito");
            });
        }
    }
}

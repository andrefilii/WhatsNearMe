package it.andreafilippi.whatsnearme.ui.activities;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.text.style.UnderlineSpan;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import java.util.UUID;

import it.andreafilippi.whatsnearme.R;
import it.andreafilippi.whatsnearme.databinding.ActivityMainBinding;
import it.andreafilippi.whatsnearme.databinding.DialogBtReceiverBinding;
import it.andreafilippi.whatsnearme.ui.fragments.DiarioFragment;
import it.andreafilippi.whatsnearme.ui.fragments.MapsFragment;
import it.andreafilippi.whatsnearme.ui.fragments.SettingsFragment;
import it.andreafilippi.whatsnearme.utils.BtReceiverTask;
import it.andreafilippi.whatsnearme.utils.Utils;

public class MainActivity extends AppCompatActivity implements BtReceiverTask.BtCallback {
    private static final String MAP_FRAG = "map_frag";
    private static final String DIARY_FRAG = "diary_frag";
    private static final String SETTINGS_FRAG = "settings_frag";
    private static final String CUR_FRAG = "cur_frag";

    private ActivityResultLauncher<String[]> locationPermissionLauncher;
    private ActivityResultLauncher<String[]> btConnectionPermissionLauncher;
    private ActivityResultLauncher<Intent> discoverableBtLauncher;

    private ActivityMainBinding binding;

    private FragmentManager fragmentManager;

    private MapsFragment mapsFragment;
    private DiarioFragment diarioFragment;
    private SettingsFragment settingsFragment;

    private Fragment currentFragment;

    private BluetoothAdapter btAdapter;
    private BtReceiverTask btReceiverTask;

    private AlertDialog btDialog;
    private ProgressBar btProgressBar;
    private TextView btMsgReceived;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        /* Creazione launcher per permessi a runtime */
        locationPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestMultiplePermissions(),
                result -> {
                    Boolean fineLocationGranted = result.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false);
                    Boolean coarseLocationGranted = result.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false);
                    firstLoad(Boolean.TRUE.equals(fineLocationGranted) || Boolean.TRUE.equals(coarseLocationGranted));
                });

        btConnectionPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestMultiplePermissions(),
                result -> {
                    Boolean bluetoothConnection = result.getOrDefault(Manifest.permission.BLUETOOTH_CONNECT, false);
                    Boolean bluetoothDiscoverable = result.getOrDefault(Manifest.permission.BLUETOOTH_SCAN, false);
                    if (Boolean.TRUE.equals(bluetoothDiscoverable) && Boolean.TRUE.equals(bluetoothConnection)) {
                        ensureBtDiscoverable();
                    } else {
                        Utils.makeToastLong(this, "Necessari i permessi per ricevere i dati!");
                    }
                });

        discoverableBtLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_CANCELED) {
                        // L'utente ha rifiutato la visibilità
                        Utils.makeToastShort(this, "Visibilità Bluetooth rifiutata");
                    } else {
                        openBtConnectionDialog();
                    }
                });

        // configurazione btn per ricevere i link condivisi
        BluetoothManager btManager = getSystemService(BluetoothManager.class);
        btAdapter = btManager.getAdapter();
        if (btAdapter == null) {
            // il dispositivo non supporta il bluetooth
            binding.btButton.setEnabled(false);
        } else {
            binding.btButton.setOnClickListener(this::onBtBtnClick);
        }

        // configurazione fragment e barra di navigazione
        binding.navigationBar.setOnItemSelectedListener(this::navigationListener);

        fragmentManager = getSupportFragmentManager();

        if (savedInstanceState != null) {
            // riprendo i fragment salvati
            mapsFragment = (MapsFragment) fragmentManager.getFragment(savedInstanceState, MAP_FRAG);
            diarioFragment = (DiarioFragment) fragmentManager.getFragment(savedInstanceState, DIARY_FRAG);
            settingsFragment = (SettingsFragment) fragmentManager.getFragment(savedInstanceState, SETTINGS_FRAG);
            switch (savedInstanceState.getInt(CUR_FRAG)) {
                case 0:
                    currentFragment = mapsFragment;
                    binding.navigationBar.setSelectedItemId(R.id.navigation_map);
                    break;
                case 1:
                    currentFragment = diarioFragment;
                    binding.navigationBar.setSelectedItemId(R.id.navigation_diary);
                    break;
                case 2:
                    currentFragment = settingsFragment;
                    binding.navigationBar.setSelectedItemId(R.id.navigation_settings);
                    break;
            }
        } else {
            // prima volta che apro la activity
            // verifico permessi per la posizione
            if (checkLocationPermission()) {
                firstLoad(true);
            } else {
                // non ho i permessi, chiedo a runtime
                locationPermissionLauncher.launch(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION});
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (btReceiverTask != null) {
            btReceiverTask.cancel(true);
            btReceiverTask = null;
        }
    }

    private void firstLoad(boolean withPositionPermission) {
        // creo i fragment
        mapsFragment = MapsFragment.newInstance();
        diarioFragment = DiarioFragment.newInstance();
        settingsFragment = SettingsFragment.newInstance();
        if (withPositionPermission) {
            // se ho i permessi per la posizione, visualizzo subito la mappa
            fragmentManager.beginTransaction()
                    .add(R.id.fragment_container, mapsFragment, MAP_FRAG)
                    .add(R.id.fragment_container, diarioFragment, DIARY_FRAG)
                    .add(R.id.fragment_container, settingsFragment, SETTINGS_FRAG)
                    .hide(diarioFragment)
                    .hide(settingsFragment)
                    .show(mapsFragment)
                    .commit();
            currentFragment = mapsFragment;

            binding.navigationBar.setSelectedItemId(R.id.navigation_map);
        } else {
            // non ho i permessi, mostro direttamente il diario
            fragmentManager.beginTransaction()
                    .add(R.id.fragment_container, mapsFragment, MAP_FRAG)
                    .add(R.id.fragment_container, diarioFragment, DIARY_FRAG)
                    .add(R.id.fragment_container, settingsFragment, SETTINGS_FRAG)
                    .hide(mapsFragment)
                    .hide(settingsFragment)
                    .show(diarioFragment)
                    .commit();
            currentFragment = diarioFragment;

            binding.navigationBar.setSelectedItemId(R.id.navigation_diary);
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        // recupero i fragment nel manager
        Fragment mapsFrag = fragmentManager.findFragmentByTag(MAP_FRAG);
        Fragment diarioFrag = fragmentManager.findFragmentByTag(DIARY_FRAG);
        Fragment settingsFrag = fragmentManager.findFragmentByTag(SETTINGS_FRAG);
        if (mapsFrag != null && diarioFrag != null && settingsFrag != null) {
            // se i fragment sono presenti, li salvo nel bundle
            fragmentManager.putFragment(outState, MAP_FRAG, mapsFrag);
            fragmentManager.putFragment(outState, DIARY_FRAG, diarioFrag);
            fragmentManager.putFragment(outState, SETTINGS_FRAG, settingsFrag);

            int curFrag = 0;
            if (currentFragment != null) {
                if (currentFragment instanceof MapsFragment) curFrag = 0;
                else if (currentFragment instanceof DiarioFragment) curFrag = 1;
                else curFrag = 2;
            }
            outState.putInt(CUR_FRAG, curFrag);
        }
    }

    /**
     * Recupera il fragment che voglio aprire e lo carica
     * @param menuItem l'elemento del menu selezionato
     */
    private boolean navigationListener(MenuItem menuItem) {
        Fragment selected = null;

        int itemId = menuItem.getItemId();
        if (itemId == R.id.navigation_map) {
            selected = mapsFragment;
        } else if (itemId == R.id.navigation_diary) {
            selected = diarioFragment;
        } else if (itemId == R.id.navigation_settings) {
            selected = settingsFragment;
        }

        loadFragment(selected);

        return true;
    }

    /**
     * Mostra il fragment passato e lo savla in currentFragment
     * @param fragment il fragment da mostrare
     */
    private void loadFragment(Fragment fragment) {
        if (fragment != null && fragment != currentFragment) {
            FragmentTransaction transaction = fragmentManager.beginTransaction();
            if (currentFragment != null)
                transaction.hide(currentFragment);
            transaction.show(fragment)
                    .commit();
            currentFragment = fragment;
        }
    }

    private boolean checkLocationPermission() {
        int fineLocationPermission =
                ContextCompat.checkSelfPermission(this,
                        android.Manifest.permission.ACCESS_FINE_LOCATION);
        int coarseLocationPermission =
                ContextCompat.checkSelfPermission(this,
                        android.Manifest.permission.ACCESS_COARSE_LOCATION);
        return (fineLocationPermission == PackageManager.PERMISSION_GRANTED) ||
                (coarseLocationPermission == PackageManager.PERMISSION_GRANTED);
    }


    /* GESTIONE RICEVITORE BT */

    private void onBtBtnClick(View view) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
            btConnectionPermissionLauncher.launch(new String[]{Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN});
        } else {
            ensureBtDiscoverable();
        }
    }

    @SuppressWarnings("MissingPermission")
    private void ensureBtDiscoverable() {
        // prima di iniziare a riceve i dati, controllo che il dispositivo sia discoverable
        if (btAdapter.getScanMode() != BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
            Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            intent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
            discoverableBtLauncher.launch(intent);
        } else {
            openBtConnectionDialog();
        }
    }

    private void openBtConnectionDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Attendi...");

        DialogBtReceiverBinding binding = DialogBtReceiverBinding.inflate(getLayoutInflater(), null, false);
        builder.setView(binding.getRoot());

        btProgressBar = binding.progressBar;
        btMsgReceived = binding.msgReceived;

        builder.setNegativeButton("Annulla", (d, which) -> {
                if (btReceiverTask != null) {
                    btReceiverTask.cancel(true);
                    btReceiverTask = null;
                }
                d.dismiss();
            });

        btDialog = builder.create();
        btDialog.setCancelable(true);
        btDialog.setOnCancelListener((d) -> {
                if (btReceiverTask != null) {
                    btReceiverTask.cancel(true);
                    btReceiverTask = null;
                }
                d.dismiss();
            });

        btDialog.show();

        btReceiverTask = new BtReceiverTask(btAdapter, getString(R.string.btConnectionName), UUID.fromString(getString(R.string.btConnectionUID)), this);
        btReceiverTask.execute();
    }

    @Override
    public void onUpdate(String msg) {
        if (btProgressBar != null) btProgressBar.setVisibility(View.GONE);
        if (btMsgReceived != null && btDialog != null) {
            if (msg != null && !msg.equals("CANCELLED")) {
                btDialog.setTitle("Link ricevuto!");

                // stile di un link
                SpannableString spannableString = new SpannableString(msg);
                spannableString.setSpan(new UnderlineSpan(), 0, msg.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                spannableString.setSpan(new ForegroundColorSpan(Color.BLUE), 0, msg.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                btMsgReceived.setText(spannableString);
                btMsgReceived.setVisibility(View.VISIBLE);

                btMsgReceived.setOnClickListener(v -> {
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(btMsgReceived.getText().toString()));
                    intent.setPackage(null);
                    startActivity(intent);
                });
                btMsgReceived.setOnLongClickListener(v -> {
                    ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                    ClipData clip = ClipData.newPlainText("Link Maps", msg);
                    clipboard.setPrimaryClip(clip);
                    Utils.makeToastShort(this, "Messaggio copiato negli appunti");

                    return true;
                });
            } else if ("CANCELLED".equals(msg)) {
                btMsgReceived.setText("Connessione annullata.");
                btMsgReceived.setVisibility(View.VISIBLE);
            } else {
                btDialog.setTitle("Errore");
                btMsgReceived.setText("Errore nella ricezione del messaggio.");
                btMsgReceived.setVisibility(View.VISIBLE);
            }
        }
    }

    @Override
    public void onProgress(int p) {
        if (btProgressBar != null) {
            if (p >= 100) {
                btProgressBar.setVisibility(View.GONE);
            } else {
                btProgressBar.setVisibility(View.VISIBLE);
                btProgressBar.setIndeterminate(p < 0);
                if (p >= 0) btProgressBar.setProgress(p);
            }
        }
    }
}
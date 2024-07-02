package it.andreafilippi.whatsnearme.ui;

import android.Manifest;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.UUID;

import it.andreafilippi.whatsnearme.R;
import it.andreafilippi.whatsnearme.databinding.ActivityMainBinding;
import it.andreafilippi.whatsnearme.ui.fragments.DiarioFragment;
import it.andreafilippi.whatsnearme.ui.fragments.MapsFragment;
import it.andreafilippi.whatsnearme.ui.fragments.SettingsFragment;

public class MainActivity extends AppCompatActivity {
    private static final String MAP_FRAG = "map_frag";
    private static final String DIARY_FRAG = "diary_frag";
    private static final String SETTINGS_FRAG = "settings_frag";
    private static final String CUR_FRAG = "cur_frag";

    private ActivityResultLauncher<String> locationPermissionLauncher;
    private ActivityResultLauncher<String> btConnectionPermissionLauncher;

    private ActivityMainBinding binding;

    private FragmentManager fragmentManager;

    private MapsFragment mapsFragment;
    private DiarioFragment diarioFragment;
    private SettingsFragment settingsFragment;

    private Fragment currentFragment;

    private BluetoothAdapter btAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        /* Creazione launcher per permessi a runtime */
        locationPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    firstLoad(isGranted);
//                    if (isGranted) {
//                        loadFragment(mapsFragment);
////                        firstLoad(true);
//                    } else {
//                        loadFragment(diarioFragment);
//                    }
                });

        btConnectionPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        openBtConnectionDialog();
                    } else {
                        Toast.makeText(this, "Necessari i permessi per ricevere i dati!", Toast.LENGTH_LONG).show();
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
//            mapsFragment = savedInstanceState.getSerializable("map_frag", MapsFragment.class);
//            diarioFragment = savedInstanceState.getSerializable("diario_frag", DiarioFragment.class);
//            settingsFragment = savedInstanceState.getSerializable("settings_frag", SettingsFragment.class);
//
//            switch (savedInstanceState.getInt("cur_frag")) {
//                case 0:
//                    currentFragment = mapsFragment;
//                    binding.navigationBar.setSelectedItemId(R.id.navigation_map);
//                    break;
//                case 1:
//                    currentFragment = diarioFragment;
//                    binding.navigationBar.setSelectedItemId(R.id.navigation_diary);
//                    break;
//                case 2:
//                    currentFragment = settingsFragment;
//                    binding.navigationBar.setSelectedItemId(R.id.navigation_settings);
//                    break;
//            }
            mapsFragment = (MapsFragment) getSupportFragmentManager().getFragment(savedInstanceState, MAP_FRAG);
            diarioFragment = (DiarioFragment) getSupportFragmentManager().getFragment(savedInstanceState, DIARY_FRAG);
            settingsFragment = (SettingsFragment) getSupportFragmentManager().getFragment(savedInstanceState, SETTINGS_FRAG);

//            currentFragment = getSupportFragmentManager().getFragment(savedInstanceState, CUR_FRAG);
//            if (currentFragment != null) {
//                if (currentFragment instanceof MapsFragment)
//                    binding.navigationBar.setSelectedItemId(R.id.navigation_map);
//                else if (currentFragment instanceof DiarioFragment)
//                    binding.navigationBar.setSelectedItemId(R.id.navigation_diary);
//                else
//                    binding.navigationBar.setSelectedItemId(R.id.navigation_settings);
//            }
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
            mapsFragment = MapsFragment.newInstance();
            diarioFragment = DiarioFragment.newInstance();
            settingsFragment = SettingsFragment.newInstance();

            // verifico permessi per la posizione
            if (checkLocationPermission()) {
//                loadFragment(mapsFragment);
                firstLoad(true);
            } else {
                // non ho i permessi, chiedo a runtime
                locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
            }
        }
    }

    private void firstLoad(boolean withPositionPermission) {
        if (withPositionPermission) {
            fragmentManager.beginTransaction()
                    .add(R.id.fragment_container, mapsFragment, MAP_FRAG)
                    .add(R.id.fragment_container, diarioFragment, DIARY_FRAG)
                    .add(R.id.fragment_container, settingsFragment, SETTINGS_FRAG)
                    .hide(diarioFragment)
                    .hide(settingsFragment)
                    .show(mapsFragment)
                    .commit();
            currentFragment = mapsFragment;
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
//        outState.putSerializable("map_frag", mapsFragment);
//        outState.putSerializable("diario_frag", diarioFragment);
//        outState.putSerializable("settings_frag", settingsFragment);
//        int curFrag = -1;
//        if (currentFragment != null) {
//            if (currentFragment instanceof MapsFragment) curFrag = 0;
//            else if (currentFragment instanceof DiarioFragment) curFrag = 1;
//            else curFrag = 2;
//        }
//        outState.putInt("cur_frag", curFrag);

        getSupportFragmentManager().putFragment(outState, MAP_FRAG, mapsFragment);
        getSupportFragmentManager().putFragment(outState, DIARY_FRAG, diarioFragment);
        getSupportFragmentManager().putFragment(outState, SETTINGS_FRAG, settingsFragment);
//        if (currentFragment != null) {
//            getSupportFragmentManager().putFragment(outState, CUR_FRAG, currentFragment);
//        }
        int curFrag = -1;
        if (currentFragment != null) {
            if (currentFragment instanceof MapsFragment) curFrag = 0;
            else if (currentFragment instanceof DiarioFragment) curFrag = 1;
            else curFrag = 2;
        }
        outState.putInt(CUR_FRAG, curFrag);

    }

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
        return fineLocationPermission == PackageManager.PERMISSION_GRANTED;
    }


    /* GESTIONE RICEVITORE BT */

    private void onBtBtnClick(View view) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            btConnectionPermissionLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT);
        } else {
            openBtConnectionDialog();
        }
    }

    private void openBtConnectionDialog() {
        // TODO creare un dialog o qualcosa del genere per eventualmente interrompere la connessione
        //  e mostrare un timer massimo di attesa aggiornato dalla funzione di connessione
        //  la funzione scrive anche il risultato su questo dialog
        new Thread(this::connectAndReceiveData).start();
    }

    @SuppressWarnings("MissingPermission")
    private void connectAndReceiveData() {
        // TODO forse aprire un dialog che indica che si è in ascolto e il timer di massima attesa che scende
        try (BluetoothServerSocket bss =
                     btAdapter.listenUsingRfcommWithServiceRecord(
                             getString(R.string.btConnectionName),
                             UUID.fromString(getString(R.string.btConnectionUID)))) {
            try (BluetoothSocket bs = bss.accept(60000)) {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(bs.getInputStream()))) {
                    StringBuilder builder = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        builder.append(line).append("\n");
                    }
                    runOnUiThread(() -> {
                        // TODO far diventare una notifica o qualcosa del genere
                        Toast.makeText(this, "Messaggio ricevuto: " + builder, Toast.LENGTH_LONG).show();
                        Log.d("BT RECEIVED", builder.toString());
                    });
                } catch (IOException e) {
                    Log.e("BT CONNECTION", e.toString());
                }
            } catch (IOException e) {
                Log.e("BT CONNECTION", e.toString());
            }
        } catch (IOException e) {
            Log.e("BT CONNECTION", e.toString());
            runOnUiThread(() -> {
                Toast.makeText(this, "Errore in fase di connessione", Toast.LENGTH_SHORT).show();
            });
        }
    }
}
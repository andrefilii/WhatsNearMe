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
                    if (isGranted) {
                        loadFragment(mapsFragment);
                    } else {
                        loadFragment(diarioFragment);
                    }
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

        mapsFragment = new MapsFragment();
        diarioFragment = new DiarioFragment();
        settingsFragment = new SettingsFragment();

        // verifico permessi per la posizione
        if (checkLocationPermission()) {
            loadFragment(mapsFragment);
        } else {
            // non ho i permessi, chiedo a runtime
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
        }
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
            transaction.replace(binding.fragmentContainer.getId(), fragment)
                    .show(fragment)
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
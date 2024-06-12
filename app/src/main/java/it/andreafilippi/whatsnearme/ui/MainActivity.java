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
import it.andreafilippi.whatsnearme.ui.fragments.MapsFragment;
import it.andreafilippi.whatsnearme.ui.fragments.SettingsFragment;

public class MainActivity extends AppCompatActivity {
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 420;
    private static final int BT_CONNECTION_REQUEST_CODE = 111;

    private ActivityMainBinding binding;

    private FragmentManager fragmentManager;

    private MapsFragment mapsFragment;
    private SettingsFragment settingsFragment;

    private Fragment currentFragment;

    private BluetoothAdapter btAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

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
        // diarioFragment
        settingsFragment = new SettingsFragment();

        // verifico permessi per la posizione
        if (checkLocationPermission()) {
            currentFragment = mapsFragment;
            fragmentManager.beginTransaction()
                    .add(binding.fragmentContainer.getId(), mapsFragment)
                    .commit();
        } else {
            // non ho i permessi, chiedo a runtime
            requestLocationPermission();
        }
    }

    private boolean navigationListener(MenuItem menuItem) {
        Fragment selected = null;

        int itemId = menuItem.getItemId();
        if (itemId == R.id.navigation_map) {
            selected = mapsFragment;
        } else if (itemId == R.id.navigation_other) {
//            selected = otherFragment;
        } else if (itemId == R.id.navigation_settings) {
            selected = settingsFragment;
        }

        loadFragment(selected);

        return true;
    }

    private void onBtBtnClick(View view) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.BLUETOOTH_CONNECT},
                    BT_CONNECTION_REQUEST_CODE);
            return;
        }
        new Thread(() -> connectAndReceiveData()).start();
    }

    private void connectAndReceiveData() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
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

    private void requestLocationPermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                android.Manifest.permission.ACCESS_FINE_LOCATION)) {
            new AlertDialog.Builder(this)
                    .setTitle("Permesso di accesso alla posizione rischiesto")
                    .setMessage("Questa applicazione richiede là accesso alla posizione per " +
                            "funzionare correttamente.Se si nega l'accesso le funzioni " +
                            "disponiibili saranno limitate al solo diario di viaggio.")
                    .setPositiveButton("OK", this::onDialogPositiveBtnClick)
                    .setNegativeButton("Annulla", this::onDialogNegativeBtnClick)
                    .create()
                    .show();
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        }
    }

    private void onDialogPositiveBtnClick(DialogInterface dialog, int which) {
        // chiedo i permessi
        ActivityCompat.requestPermissions(this,
                new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                LOCATION_PERMISSION_REQUEST_CODE);
    }

    private void onDialogNegativeBtnClick(DialogInterface dialog, int which) {
        dialog.dismiss();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            // ho ricevuto la risposta alla richiesta del permesso sulla localizzazione
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // permessi concessi
                loadFragment(mapsFragment);
            } else {
                // permessi non concessi, disattivo le mappe
                loadFragment(null); // TODO sostituire null con diarioFragment
            }
        } else if (requestCode == BT_CONNECTION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // permesso di connessione concesso
                new Thread(() -> connectAndReceiveData()).start();
            }
        }
    }
}
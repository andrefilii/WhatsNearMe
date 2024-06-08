package it.andreafilippi.whatsnearme.ui;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import it.andreafilippi.whatsnearme.R;
import it.andreafilippi.whatsnearme.databinding.ActivityMainBinding;
import it.andreafilippi.whatsnearme.ui.fragments.MapsFragment;
import it.andreafilippi.whatsnearme.ui.fragments.SettingsFragment;

public class MainActivity extends AppCompatActivity {
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 420;

    private ActivityMainBinding binding;

    private FragmentManager fragmentManager;

    private MapsFragment mapsFragment;
    private SettingsFragment settingsFragment;

    private Fragment currentFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

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
        }
    }
}
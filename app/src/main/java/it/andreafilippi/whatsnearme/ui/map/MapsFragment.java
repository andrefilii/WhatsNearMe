package it.andreafilippi.whatsnearme.ui.map;

import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;

import it.andreafilippi.whatsnearme.R;
import it.andreafilippi.whatsnearme.databinding.FragmentMapsBinding;

/*
* !!!!
*  SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getContext());
                boolean x = settings.getBoolean("sync", false);
           usare questo per prendere le impostazioini
* */

public class MapsFragment extends Fragment implements OnMapReadyCallback {
    private static final String CAMERA_POSITION = "camera_position";

    private final LatLng italy = new LatLng(42.5, 12.5);

    private FragmentMapsBinding binding;

    private GoogleMap myMap;

    private Location currentLocation;
    private LocationManager locManager;

    private CameraPosition cameraPosition;
//    private LatLngBounds mapLocation;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentMapsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        currentLocation = null;
        locManager = (LocationManager) getActivity().getSystemService(Context.LOCATION_SERVICE);

        SupportMapFragment mapFragment =
                (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        if (checkLocationPermission()) {
            // ho i permessi per mostrate la posizione dell'utente
            // TODO ottengo la posizione
        } else {
            // non ho i permessi, vedo la mappa e basta senza poterci fare niente
            // TODO disattivazione pulsanti e altra UI
        }

        // salvo lo stato nel caso ad esempio venga ruotato lo schermo
        if (savedInstanceState != null) {
            cameraPosition =
                    savedInstanceState.getParcelable(CAMERA_POSITION, CameraPosition.class);
        }

    }

    @Override
    public void onResume() {
        super.onResume();

    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);

        if (myMap != null) {
            outState.putParcelable(CAMERA_POSITION, myMap.getCameraPosition());
//            outState.putParcelable(MAP_LOCATION, myMap.getProjection().getVisibleRegion().latLngBounds);
        }
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        myMap = googleMap;

        if (checkLocationPermission()) {
            myMap.setMyLocationEnabled(true);
        }
        myMap.getUiSettings().setZoomControlsEnabled(true);

        if (cameraPosition != null) {
            myMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
        } else {
            // appena creata. Mi posiziono sulla mia posizione, o se non c'è una vista standard
            if (checkLocationPermission()) {
                // TODO recupero mia posizione e mi ci metto
                showCurrentLocation();
            } else {
                myMap.animateCamera(CameraUpdateFactory.newLatLngZoom(italy, 3));
            }
        }
    }

    private void showCurrentLocation() {
        // TODO
    }

    private boolean checkLocationPermission() {
        int fineLocationPermission =
                ContextCompat.checkSelfPermission(getContext(),
                        android.Manifest.permission.ACCESS_FINE_LOCATION);
        return fineLocationPermission == PackageManager.PERMISSION_GRANTED;
    }
}
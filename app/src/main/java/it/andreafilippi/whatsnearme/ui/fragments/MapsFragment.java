package it.andreafilippi.whatsnearme.ui.fragments;

import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.Serializable;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import it.andreafilippi.whatsnearme.BuildConfig;
import it.andreafilippi.whatsnearme.R;
import it.andreafilippi.whatsnearme.databinding.FragmentMapsBinding;
import it.andreafilippi.whatsnearme.entities.Place;
import it.andreafilippi.whatsnearme.params.MarkerData;
import it.andreafilippi.whatsnearme.params.PlacesTaskParam;
import it.andreafilippi.whatsnearme.ui.dialogs.MarkerDialog;
import it.andreafilippi.whatsnearme.utils.FetchPlaces;
import it.andreafilippi.whatsnearme.utils.Utils;

public class MapsFragment extends Fragment implements OnMapReadyCallback {
    private static final String ARG_MARKERS = "markers_list";

    private final LatLng italy = new LatLng(42.5, 12.5);

    private SharedPreferences settings;

    private FragmentMapsBinding binding;

    private GoogleMap myMap;

    private FusedLocationProviderClient flpc;
    private LocationCallback locationUpdateCallback;
    private Marker currentLocationMarker;

    private CameraPosition cameraPosition;

    private boolean isFirstLocationUpdate = true;

    private Circle searchRadiusCircle;

    private List<Marker> mapMarkers;

    private BitmapDescriptor userLocationIcon;
    private ArrayList<MarkerData> savedMarkerData;


    public static MapsFragment newInstance() {
        MapsFragment mf = new MapsFragment();
        return mf;
    }

    public MapsFragment() {

    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentMapsBinding.inflate(inflater, container, false);

        // creazione della bitmap per l'icona della posizione utente
        userLocationIcon = creaIconaPosizione();

        // salvo il preference manager
        settings = PreferenceManager.getDefaultSharedPreferences(requireContext());

        // prendo il client per la posizione
        flpc = LocationServices.getFusedLocationProviderClient(requireContext());

        // creo la callback per la posizione
        locationUpdateCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                for (Location location : locationResult.getLocations()) {
                    if (location != null) updateUserLocationOnMap(location);
                }
            }
        };

        binding.btnRestaurants.setOnClickListener(this::onBtnRestaurantsClick);
        binding.btnMuseums.setOnClickListener(this::onBtnMuseumsClick);
        binding.btnATM.setOnClickListener(this::onBtnATMClick);

        return binding.getRoot();
    }

    private BitmapDescriptor creaIconaPosizione() {
        Bitmap icona = BitmapFactory.decodeResource(getResources(), R.drawable.ic_user_location);
        Bitmap resisezIcon = Bitmap.createScaledBitmap(icona, (int) (icona.getWidth()*0.45), (int) (icona.getHeight()*0.45), false);
        return BitmapDescriptorFactory.fromBitmap(resisezIcon);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (savedInstanceState != null) {
            this.savedMarkerData = savedInstanceState.getParcelableArrayList(ARG_MARKERS, MarkerData.class);
        }

        SupportMapFragment mapFragment =
                (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        if (!checkLocationPermission()) {
            // non ho i permessi, vedo la mappa e basta senza poterci fare niente
            binding.btnRestaurants.setEnabled(false);
            binding.btnMuseums.setEnabled(false);
            binding.btnATM.setEnabled(false);

//            Toast.makeText(requireContext(),
//                    "Abilita la posizione per utilizzare la funzione di ricerca",
//                    Toast.LENGTH_LONG).show();
            Utils.makeToastShort(requireContext(), "Abilita la posizione per utilizzare la funzione di ricerca");
        }

    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mapMarkers != null)
            outState.putParcelableArrayList(ARG_MARKERS,
                    new ArrayList<>(
                            mapMarkers.stream()
                                    .map(m -> new MarkerData(m.getPosition(), m.getTitle(), (Place) m.getTag()))
                                    .collect(Collectors.toList())
                    ));
    }

    @Override
    public void onPause() {
        super.onPause();
        flpc.removeLocationUpdates(locationUpdateCallback);
        currentLocationMarker = null;
    }

    @Override
    public void onResume() {
        super.onResume();
        isFirstLocationUpdate = true;
        startLocationUpdated();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        myMap = googleMap;

        myMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(requireContext(), R.raw.map_style));

        myMap.setOnMarkerClickListener(this::onMarkerClick);

        boolean isLocationEnabled = checkLocationPermission();
        if (isLocationEnabled) {
            // inizia l'update della posizione utente
            startLocationUpdated();
        }

        myMap.getUiSettings().setZoomControlsEnabled(true);
        myMap.getUiSettings().setMapToolbarEnabled(false);

        // se non ho accesso alla posizione, metto una vista standard
        if (!isLocationEnabled) {
            myMap.animateCamera(CameraUpdateFactory.newLatLngZoom(italy, 3));
        }

        if (this.mapMarkers != null) {
            ArrayList<Marker> mapMarkersNew = new ArrayList<>(mapMarkers.size());
            for (Marker marker : mapMarkers) {
                Marker m = myMap.addMarker(new MarkerOptions().position(marker.getPosition()).title(marker.getTitle()));
                if (m != null) {
                    m.setTag(marker.getTag());
                    mapMarkersNew.add(m);
                }
            }
            this.mapMarkers = mapMarkersNew;
        }
        else if (savedMarkerData != null) {
            this.mapMarkers = new ArrayList<>(savedMarkerData.size());
            for (MarkerData marker : savedMarkerData) {
                Marker m = myMap.addMarker(new MarkerOptions().position(marker.getPosition()).title(marker.getTitle()));
                if (m != null) {
                    m.setTag(marker.getPlace());
                    this.mapMarkers.add(m);
                }
            }
        }
    }

    private boolean onMarkerClick(Marker marker) {
        if (marker.getTag() != null) {
            // se ha il campo tag, significa che è un luogo di interesse e non la posizione
            Place place = (Place) marker.getTag();
            Log.d("MARKER CLICK", marker.toString());

            MarkerDialog.newInstance(place).show(requireActivity().getSupportFragmentManager(), "markerDialog");

            return true;
        }

        // comportamento standard
        return false;
    }

    private void startLocationUpdated() {
        LocationRequest locationRequest = LocationRequest.create()
                .setInterval(10000)
                .setFastestInterval(5000)
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        if (!checkLocationPermission()) return;

        flpc.requestLocationUpdates(locationRequest, locationUpdateCallback, null);
    }

    private void updateUserLocationOnMap(Location location) {
        LatLng position = new LatLng(location.getLatitude(), location.getLongitude());

        if (searchRadiusCircle != null)
            searchRadiusCircle.remove();

        if (currentLocationMarker == null) {
            currentLocationMarker =
                    myMap.addMarker(new MarkerOptions()
                            .position(position)
                            .icon(userLocationIcon)
                            .title("Tu sei qui"));
        } else {
            currentLocationMarker.setPosition(position);
        }

        searchRadiusCircle = myMap.addCircle(new CircleOptions()
                .center(position)
                .radius(getSearchRadius())
        );

        Log.d("MAP", "Marker aggiornato: " + currentLocationMarker.getPosition());

        if (isFirstLocationUpdate) {
            // se è la prima volta che prendo la posizione significa che ho riaperto ora il fragment
            //  mi posiziono sulla posizione dell'utente
            myMap.animateCamera(CameraUpdateFactory.newLatLngZoom(position, getZoomLevel()));

            isFirstLocationUpdate = false;
        }
    }

    /* Btn listeners */

    private void onBtnATMClick(View view) {
        if (currentLocationMarker == null) {
//            Toast.makeText(requireContext(),
//                    "Per favore attendere il caricamento della posizione",
//                    Toast.LENGTH_SHORT).show();
            Utils.makeToastShort(requireContext(), "Per favore attendi il caricamento della posizione");
        } else {
            fetchPlaces(Place.Category.ATM);
        }
    }

    private void onBtnMuseumsClick(View view) {
        if (currentLocationMarker == null) {
//            Toast.makeText(requireContext(),
//                    "Per favore attendere il caricamento della posizione",
//                    Toast.LENGTH_SHORT).show();
            Utils.makeToastShort(requireContext(), "Per favore attendi il caricamento della posizione");
        } else {
            fetchPlaces(Place.Category.MUSEUM);
        }
    }

    private void onBtnRestaurantsClick(View view) {
        if (currentLocationMarker == null) {
//            Toast.makeText(requireContext(),
//                    "Per favore attendere il caricamento della posizione",
//                    Toast.LENGTH_SHORT).show();
            Utils.makeToastShort(requireContext(), "Per favore attendi il caricamento della posizione");
        } else {
            fetchPlaces(Place.Category.RESTAURANT);
        }
    }

    /* --- UTILS --- */

    private void fetchPlaces(Place.Category category) {
        PlacesTaskParam params = new PlacesTaskParam()
                .setMap(myMap)
                .setRadius(getSearchRadius())
                .setCenter(currentLocationMarker.getPosition())
                .setCategory(category)
                .setApiKey(BuildConfig.MAPS_API_KEY);

        new FetchPlaces(params, mapMarkers, this::onFetchPlacesCompleted).execute();
    }

    private void onFetchPlacesCompleted(List<Marker> markers) {
        // mi salvo i nuovi marker: questo serve per poi passarli di nuovo al Task in caso di una
        //  nuova ricerca
        this.mapMarkers = markers;
    }

    private boolean checkLocationPermission() {
        int fineLocationPermission =
                ContextCompat.checkSelfPermission(requireContext(),
                        android.Manifest.permission.ACCESS_FINE_LOCATION);
        int coarseLocationPermission =
                ContextCompat.checkSelfPermission(requireContext(),
                        android.Manifest.permission.ACCESS_COARSE_LOCATION);
        return (fineLocationPermission == PackageManager.PERMISSION_GRANTED) ||
                (coarseLocationPermission == PackageManager.PERMISSION_GRANTED);
    }

    private Integer getSearchRadius() {
        return settings.getInt("search_radius", 1500);
    }

    private int getZoomLevel() {
        int searchRadius = getSearchRadius();
        double radius = searchRadius + searchRadius/2.0;
        double scale = radius / 500.0;
        return (int) (16 - Math.log(scale) / Math.log(2));
    }
}
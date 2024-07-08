package it.andreafilippi.whatsnearme.ui.fragments;

import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

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
import com.google.android.gms.location.Priority;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.CircularBounds;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.android.libraries.places.api.net.SearchNearbyRequest;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import it.andreafilippi.whatsnearme.BuildConfig;
import it.andreafilippi.whatsnearme.R;
import it.andreafilippi.whatsnearme.databinding.FragmentMapsBinding;
import it.andreafilippi.whatsnearme.entities.MyPlace;
import it.andreafilippi.whatsnearme.params.MarkerData;
import it.andreafilippi.whatsnearme.ui.dialogs.MarkerDialog;
import it.andreafilippi.whatsnearme.utils.PlacesUtils;
import it.andreafilippi.whatsnearme.utils.Utils;

public class MapsFragment extends Fragment implements OnMapReadyCallback {
    private static final String ARG_MARKERS = "markers_list";
    private static final String ARG_CATEGORY = "category";

    private final LatLng italy = new LatLng(42.5, 12.5);

    private SharedPreferences settings;

    private FragmentMapsBinding binding;

    private GoogleMap myMap;

    private FusedLocationProviderClient flpc;
    private LocationCallback locationUpdateCallback;
    private Marker currentLocationMarker;

    private boolean isFirstLocationUpdate = true;

    private Circle searchRadiusCircle;

    private PlacesUtils.Category curCategory;
    private List<Marker> mapMarkers;

    private BitmapDescriptor userLocationIcon;
    private ArrayList<MarkerData> savedMarkerData;

    private PlacesClient placesClient;


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

        // recupero la api key per google places (stessa di maps)
        String apiKey = BuildConfig.MAPS_API_KEY;

        if (TextUtils.isEmpty(apiKey) || apiKey.equals("DEAFULT_API_KEY")) {
            // se non ho specificato una key, disattivo la ricerca
            Log.e("PLACE API", "Nessuna API key specificata");
            binding.btnRestaurants.setEnabled(false);
            binding.btnMuseums.setEnabled(false);
            binding.btnATM.setEnabled(false);
            Utils.makeToastShort(requireContext(), "Errore caricamento places key");
        } else {
            // inizializzo places con la key
            Places.initializeWithNewPlacesApiEnabled(requireActivity().getApplicationContext(), apiKey);
            // creazione nuovo client places
            this.placesClient = Places.createClient(requireContext());

            binding.btnRestaurants.setOnClickListener(this::onBtnRestaurantsClick);
            binding.btnMuseums.setOnClickListener(this::onBtnMuseumsClick);
            binding.btnATM.setOnClickListener(this::onBtnATMClick);
        }

        return binding.getRoot();
    }

    private BitmapDescriptor creaIconaPosizione() {
        Bitmap icona = BitmapFactory.decodeResource(getResources(), R.drawable.ic_user_location);
        Bitmap resisezIcon = Bitmap.createScaledBitmap(icona, (int) (icona.getWidth()*0.40), (int) (icona.getHeight()*0.40), false);
        return BitmapDescriptorFactory.fromBitmap(resisezIcon);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (savedInstanceState != null) {
            // ho dei dati salvati (i marker), li ripristino
            this.savedMarkerData = savedInstanceState.getParcelableArrayList(ARG_MARKERS, MarkerData.class);
            this.curCategory = PlacesUtils.Category.getCategoryByString(savedInstanceState.getString(ARG_CATEGORY));
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

            Utils.makeToastShort(requireContext(), "Abilita la posizione per utilizzare la funzione di ricerca");
        }

    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mapMarkers != null && !mapMarkers.isEmpty()) {
            // mi salvo i marker sulla mappa per riposizionarli più tardi
            outState.putParcelableArrayList(ARG_MARKERS,
                    new ArrayList<>(
                            mapMarkers.stream()
                                    .map(m -> new MarkerData(m.getPosition().latitude, m.getPosition().longitude, m.getTitle(), (MyPlace) m.getTag()))
                                    .collect(Collectors.toList())
                    ));
            outState.putString(ARG_CATEGORY, curCategory.getDescription());
        }
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (!hidden) {
            // sono tornato sulla schermata, ridisegno la posizione e mi ci centro
            if (checkLocationPermission() && currentLocationMarker != null) {
                isFirstLocationUpdate = true;
                Location location = new Location("gps");
                location.setLatitude(currentLocationMarker.getPosition().latitude);
                location.setLongitude(currentLocationMarker.getPosition().longitude);
                updateUserLocationOnMap(location);
            }
        }
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

        // cambio il tema della mappa in base a quello di sistema
        if (Utils.isNightMode(requireContext()))
            myMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(requireContext(), R.raw.map_style_dark));
        else
            myMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(requireContext(), R.raw.map_style_light));

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
            // ho dei markers salvati ma la mappa è stata ricreata, li rimetto sulla mappa
            BitmapDescriptor icon = Utils.creaIconaMarker(requireContext(), this.curCategory);
            ArrayList<Marker> mapMarkersNew = new ArrayList<>(mapMarkers.size());
            for (Marker marker : mapMarkers) {
                Marker m = myMap.addMarker(
                        new MarkerOptions()
                                .position(marker.getPosition())
                                .title(marker.getTitle())
                                .icon(icon)
                        );
                if (m != null) {
                    m.setTag(marker.getTag());
                    mapMarkersNew.add(m);
                }
            }
            this.mapMarkers = mapMarkersNew;
        }
        else if (savedMarkerData != null) {
            // mi ero salvato dei marker nel bundle, li rimetto sulla mappa
            BitmapDescriptor icon = Utils.creaIconaMarker(requireContext(), this.curCategory);
            this.mapMarkers = new ArrayList<>(savedMarkerData.size());
            for (MarkerData marker : savedMarkerData) {
                Marker m = myMap.addMarker(
                        new MarkerOptions()
                                .position(new LatLng(marker.getLat(), marker.getLng()))
                                .title(marker.getTitle())
                                .icon(icon)
                        );
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
            MyPlace place = (MyPlace) marker.getTag();
            Log.d("MARKER CLICK", marker.toString());

            // apro il dialog per visualizzare le informazioni
            MarkerDialog.newInstance(place).show(requireActivity().getSupportFragmentManager(), "markerDialog");

            return true;
        }

        // comportamento standard
        return false;
    }

    private void startLocationUpdated() {
        if (!checkLocationPermission()) return;

        // richiesta di localizzazione bilanciata, ogni 10 secondi con un minimo di 5
        LocationRequest locationRequest = new LocationRequest.Builder(
                Priority.PRIORITY_BALANCED_POWER_ACCURACY, 10000)
                .setMinUpdateIntervalMillis(5000)
                .build();

        // inizio con l'ultima posizione in cache, poi ottengo quella esatta
        flpc.getLastLocation()
                .addOnSuccessListener(location -> {
                    if (location != null) {
                        // se ho una posizione salvata parto con quella
                        updateUserLocationOnMap(location);
                    }
                    flpc.requestLocationUpdates(locationRequest, locationUpdateCallback, null);
                })
                .addOnFailureListener(e -> {
                    // recupero dalla cache fallito, inizio con l'aggiornamento classico
                    Log.e("MAP", e.toString());
                    flpc.requestLocationUpdates(locationRequest, locationUpdateCallback, null);
                });

    }

    private void updateUserLocationOnMap(Location location) {
        // ottengo le coordinate dalla posizione ricevuta
        LatLng position = new LatLng(location.getLatitude(), location.getLongitude());

        if (searchRadiusCircle == null) {
            // creo il cerchio che indica il raggio di ricerca
            searchRadiusCircle = myMap.addCircle(new CircleOptions()
                    .center(position)
                    .radius(getSearchRadius())
            );
        } else {
            searchRadiusCircle.setCenter(position);
            searchRadiusCircle.setRadius(getSearchRadius());
        }

        if (currentLocationMarker == null) {
            // creo il marker della posizione utente
            currentLocationMarker =
                    myMap.addMarker(new MarkerOptions()
                            .position(position)
                            .icon(userLocationIcon)
                            .title("Tu sei qui"));
        } else {
            currentLocationMarker.setPosition(position);
        }

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
            Utils.makeToastShort(requireContext(), "Per favore attendi il caricamento della posizione");
        } else {
            fetchPlaces(PlacesUtils.Category.ATM);
        }
    }

    private void onBtnMuseumsClick(View view) {
        if (currentLocationMarker == null) {
            Utils.makeToastShort(requireContext(), "Per favore attendi il caricamento della posizione");
        } else {
            fetchPlaces(PlacesUtils.Category.MUSEUM);
        }
    }

    private void onBtnRestaurantsClick(View view) {
        if (currentLocationMarker == null) {
            Utils.makeToastShort(requireContext(), "Per favore attendi il caricamento della posizione");
        } else {
            fetchPlaces(PlacesUtils.Category.RESTAURANT);
        }
    }

    /* --- UTILS --- */

    private void fetchPlaces(PlacesUtils.Category category) {
        // rimuovo i marker presenti, se ci sono
        if (mapMarkers != null) {
            mapMarkers.forEach(Marker::remove);
            mapMarkers.clear();
        } else {
            mapMarkers = new ArrayList<>();
        }

        // definisco il raggio di ricerca
        CircularBounds circle = CircularBounds.newInstance(currentLocationMarker.getPosition(), getSearchRadius());

        // lista di categorie da includere nella ricerca
        List<String> includedTypes = PlacesUtils.getPlaceTypesByCategory(category);

        // creo la richiesta
        SearchNearbyRequest searchNearbyRequest =
                SearchNearbyRequest.builder(circle, PlacesUtils.placeFields)
                        .setIncludedTypes(includedTypes)
                        .build();

        // effettuo la ricerca
        placesClient.searchNearby(searchNearbyRequest)
                .addOnSuccessListener(response -> onFetchPlacesCompleted(category, response.getPlaces()))
                .addOnFailureListener(e -> {
                    Log.e("PLACES API", e.toString());
                    Utils.makeToastShort(requireContext(), "Errore caricamento risultati");
                });
    }

    private void onFetchPlacesCompleted(PlacesUtils.Category category, List<Place> places) {
        this.curCategory = category;

        BitmapDescriptor icon = Utils.creaIconaMarker(requireContext(), category);
        for (Place place : places) {
            Marker marker = myMap.addMarker(new MarkerOptions()
                    .title(place.getName())
                    .icon(icon)
                    .position(place.getLatLng()));
            if (marker != null) {
                // informazioni poi da usare nel dialog
                MyPlace myPlace = MyPlace.fromGooglePlace(place);
                marker.setTag(myPlace);
                mapMarkers.add(marker);
            }
        }
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
package it.andreafilippi.whatsnearme.utils;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import it.andreafilippi.whatsnearme.R;
import it.andreafilippi.whatsnearme.entities.Place;
import it.andreafilippi.whatsnearme.params.PlacesTaskParam;

public class FetchPlaces extends AsyncTask<Void, Place, List<Marker>> {
    /* DA RIMUOVERE, PER TEST */
    private static final Place[] testPlaces = {
            new Place().setName("Museo Città di Livorno").setLat(43.5551451).setLng(10.3071166),
            new Place().setName("Museo Fortezza Vecchia").setLat(43.5527328).setLng(10.3021199),
            new Place().setName("Museo Piazza Grande").setLat(43.5506926).setLng(10.3089171)
    };

    private static final String ENDPOINT = "https://maps.googleapis.com/maps/api/place/nearbysearch/json?";

    private final PlacesTaskParam taskParam;
    private final Consumer<List<Marker>> callback;
    private List<Marker> markers;
    private GoogleMap map;

    public FetchPlaces(PlacesTaskParam param, List<Marker> oldMarkers, Consumer<List<Marker>> callback) {
        this.markers = oldMarkers;
        this.callback = callback;
        taskParam = param;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();

        map = taskParam.getMap();
        if (markers != null)
            markers.forEach(Marker::remove);
    }

    @Override
    protected List<Marker> doInBackground(Void... params) {
        if (markers == null)
            markers = new ArrayList<>();
        else
            markers.clear();

        boolean x = false;
        if (x) {
            try {
                String jsonData = DownloadUrl.performRequest(createRequest());

                try {
                    JSONObject response = new JSONObject(jsonData);
                    JSONArray jsonArray = response.getJSONArray("results");

                    for (int i = 0; i < jsonArray.length(); i++) {
                        Place place = new Place();

                        JSONObject result = jsonArray.getJSONObject(i);
                        JSONObject location = result.getJSONObject("geometry").getJSONObject("location");

                        place.setName(result.getString("name"));

                        place.setLat(location.getDouble("lat"));
                        place.setLng(location.getDouble("lng"));

                        List<String> tags = new ArrayList<>();
                        JSONArray types = result.getJSONArray("types");
                        for (int j = 0; j < types.length(); j++) {
                            tags.add(types.getString(j));
                        }
                        place.setTags(tags);

                        publishProgress(place);
                    }

                } catch (JSONException e) {
                    Log.e("FETCH PLACES - JSON", e.toString());
                }
            } catch (IOException e) {
                Log.e("FETCH PLACES", e.toString());
            }
        } else {
            // TESTING
            for (Place place : testPlaces) publishProgress(place);
        }

        return markers;
    }

    @Override
    protected void onProgressUpdate(Place... values) {
        super.onProgressUpdate(values);

        Place place = values[0];

        LatLng latLng = new LatLng(place.getLat(), place.getLng());
        markers.add(map.addMarker(new MarkerOptions()
                .title(place.getName())
                .position(latLng)));
    }

    @Override
    protected void onPostExecute(List<Marker> markers) {
        super.onPostExecute(markers);
        // se specificata, chiamo la callback
        if (callback != null) callback.accept(markers);
    }

    private String createRequest() {
        String requestBuilder = ENDPOINT +
                "location=" + taskParam.getCenter().latitude + "," + taskParam.getCenter().longitude +
                "&radius=" + taskParam.getRadius() +
                "&type=" + taskParam.getCategory().getDescription() +
                "&key=" + taskParam.getApiKey();
        return requestBuilder;
    }
}
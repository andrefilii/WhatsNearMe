package it.andreafilippi.whatsnearme.params;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;

import it.andreafilippi.whatsnearme.utils.PlacesUtils;

public class PlacesTaskParam {
    GoogleMap map;
    Integer radius;
    LatLng center;
    PlacesUtils.Category category;
    String apiKey;

    public GoogleMap getMap() {
        return map;
    }

    public PlacesTaskParam setMap(GoogleMap map) {
        this.map = map;
        return this;
    }

    public Integer getRadius() {
        return radius;
    }

    public PlacesTaskParam setRadius(Integer radius) {
        this.radius = radius;
        return this;
    }

    public LatLng getCenter() {
        return center;
    }

    public PlacesTaskParam setCenter(LatLng center) {
        this.center = center;
        return this;
    }

    public PlacesUtils.Category getCategory() {
        return category;
    }

    public PlacesTaskParam setCategory(PlacesUtils.Category category) {
        this.category = category;
        return this;
    }

    public String getApiKey() {
        return apiKey;
    }

    public PlacesTaskParam setApiKey(String apiKey) {
        this.apiKey = apiKey;
        return this;
    }
}

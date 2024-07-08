package it.andreafilippi.whatsnearme.entities;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.android.libraries.places.api.model.Place;

import java.io.Serializable;
import java.util.List;

public class MyPlace implements Parcelable {
    private String id;
    private String name;
    private List<String> tags;
    private Double lat;
    private Double lng;


    public static MyPlace fromGooglePlace(Place place) {
        return new MyPlace()
                .setId(place.getId())
                .setName(place.getName())
                .setLat(place.getLatLng().latitude)
                .setLng(place.getLatLng().longitude)
                .setTags(place.getPlaceTypes());
    }

    public MyPlace() {
    }

    public MyPlace(String nome, Double lat, Double lng) {
        this.name = nome;
        this.lat = lat;
        this.lng = lng;
    }

    protected MyPlace(Parcel in) {
        id = in.readString();
        name = in.readString();
        tags = in.createStringArrayList();
        lat = in.readDouble();
        lng = in.readDouble();
    }

    public static final Creator<MyPlace> CREATOR = new Creator<MyPlace>() {
        @Override
        public MyPlace createFromParcel(Parcel in) {
            return new MyPlace(in);
        }

        @Override
        public MyPlace[] newArray(int size) {
            return new MyPlace[size];
        }
    };

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(id);
        dest.writeString(name);
        dest.writeStringList(tags);
        dest.writeDouble(lat);
        dest.writeDouble(lng);
    }

    @Override
    public int describeContents() {
        return 0;
    }


    public String getId() {
        return id;
    }

    public MyPlace setId(String id) {
        this.id = id;
        return this;
    }

    public String getName() {
        return name;
    }

    public MyPlace setName(String name) {
        this.name = name;
        return this;
    }

    public List<String> getTags() {
        return tags;
    }

    public MyPlace setTags(List<String> tags) {
        this.tags = tags;
        return this;
    }

    public Double getLat() {
        return lat;
    }

    public MyPlace setLat(Double lat) {
        this.lat = lat;
        return this;
    }

    public Double getLng() {
        return lng;
    }

    public MyPlace setLng(Double lng) {
        this.lng = lng;
        return this;
    }
}

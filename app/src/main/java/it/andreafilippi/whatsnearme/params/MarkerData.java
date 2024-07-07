package it.andreafilippi.whatsnearme.params;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.android.gms.maps.model.LatLng;

import it.andreafilippi.whatsnearme.entities.MyPlace;

public class MarkerData implements Parcelable {
    private Double lat;
    private Double lng;
    private String title;
    private MyPlace place;


    public MarkerData(Double lat, Double lng, String title, MyPlace place) {
        this.lat = lat;
        this.lng = lng;
        this.title = title;
        this.place = place;
    }

    protected MarkerData(Parcel in) {
        lat = in.readDouble();
        lng = in.readDouble();
        title = in.readString();
        place = in.readParcelable(MyPlace.class.getClassLoader(), MyPlace.class);
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeDouble(lat);
        dest.writeDouble(lng);
        dest.writeString(title);
        dest.writeParcelable(place, flags);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<MarkerData> CREATOR = new Creator<MarkerData>() {
        @Override
        public MarkerData createFromParcel(Parcel in) {
            return new MarkerData(in);
        }

        @Override
        public MarkerData[] newArray(int size) {
            return new MarkerData[size];
        }
    };


    public Double getLat() {
        return lat;
    }

    public void setLat(Double lat) {
        this.lat = lat;
    }

    public Double getLng() {
        return lng;
    }

    public void setLng(Double lng) {
        this.lng = lng;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public MyPlace getPlace() {
        return place;
    }

    public void setPlace(MyPlace place) {
        this.place = place;
    }

}

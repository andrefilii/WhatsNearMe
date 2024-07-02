package it.andreafilippi.whatsnearme.params;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.android.gms.maps.model.LatLng;

import it.andreafilippi.whatsnearme.entities.Place;
import it.andreafilippi.whatsnearme.ui.dialogs.MarkerDialog;

public class MarkerData implements Parcelable {
    private LatLng position;
    private String title;
    private Place place;

    public MarkerData(LatLng position, String title, Place place) {
        this.position = position;
        this.title = title;
        this.place = place;
    }

    protected MarkerData(Parcel in) {
        position = in.readParcelable(LatLng.class.getClassLoader(), LatLng.class);
        title = in.readString();
        place = in.readSerializable(Place.class.getClassLoader(), Place.class);
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

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(position, flags);
        dest.writeString(title);
        dest.writeSerializable(place);
    }

    public LatLng getPosition() {
        return position;
    }

    public MarkerData setPosition(LatLng position) {
        this.position = position;
        return this;
    }

    public String getTitle() {
        return title;
    }

    public MarkerData setTitle(String title) {
        this.title = title;
        return this;
    }

    public Place getPlace() {
        return place;
    }

    public MarkerData setPlace(Place place) {
        this.place = place;
        return this;
    }
}

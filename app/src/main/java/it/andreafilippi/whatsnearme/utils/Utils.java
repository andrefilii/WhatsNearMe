package it.andreafilippi.whatsnearme.utils;

import android.content.Context;
import android.widget.Toast;

import it.andreafilippi.whatsnearme.entities.Place;

public class Utils {

    public static void makeToastShort(Context context, String msg) {
        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();
    }

    public static void makeToastLong(Context context, String msg) {
        Toast.makeText(context, msg, Toast.LENGTH_LONG).show();
    }

    public static String getPlaceLocationUri(Place place) {
        return "https://maps.google.com/maps?q=" + place.getLat() + "," + place.getLng() + "+(" + place.getName().replace(" ", "+") + ")";
    }

}

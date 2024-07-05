package it.andreafilippi.whatsnearme.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.widget.Toast;

import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;

import it.andreafilippi.whatsnearme.R;
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

    public static BitmapDescriptor creaIconaMarker(Context context, Place.Category category) {
        int resId = -1;
        switch (category) {
            case RESTAURANT:
                resId = R.drawable.ic_restaurant_png;
                break;
            case MUSEUM:
                resId = R.drawable.ic_museum_png;
                break;
            case ATM:
                resId = R.drawable.ic_atm_png;
                break;
        }

        if (resId != -1) {
            Bitmap icona = BitmapFactory.decodeResource(context.getResources(), resId);
            Bitmap resisezIcon = Bitmap.createScaledBitmap(icona, (int) (icona.getWidth() * 0.35), (int) (icona.getHeight() * 0.35), false);
            return BitmapDescriptorFactory.fromBitmap(resisezIcon);
        } else {
            return BitmapDescriptorFactory.defaultMarker();
        }
    }

}

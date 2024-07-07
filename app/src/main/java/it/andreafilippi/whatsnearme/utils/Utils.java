package it.andreafilippi.whatsnearme.utils;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.widget.Toast;

import androidx.appcompat.content.res.AppCompatResources;

import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;

import it.andreafilippi.whatsnearme.R;
import it.andreafilippi.whatsnearme.entities.MyPlace;

public class Utils {

    public static void makeToastShort(Context context, String msg) {
        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();
    }

    public static void makeToastLong(Context context, String msg) {
        Toast.makeText(context, msg, Toast.LENGTH_LONG).show();
    }

    public static String getPlaceLocationUri(MyPlace place) {
        return "https://maps.google.com/maps?q=" + place.getLat() + "," + place.getLng() + "+(" + place.getName().replace(" ", "+") + ")";
    }

    public static BitmapDescriptor creaIconaMarker(Context context, PlacesUtils.Category category) {
        int resId = -1;
        switch (category) {
            case RESTAURANT:
                resId = R.drawable.ic_marker_restaurant;
                break;
            case MUSEUM:
                resId = R.drawable.ic_marker_museum;
                break;
            case ATM:
                resId = R.drawable.ic_marker_atm;
                break;
        }

        if (resId != -1) {
            Drawable vectorDrawable = AppCompatResources.getDrawable(context, resId);
            assert vectorDrawable != null;
            Bitmap bitmap = Bitmap.createBitmap(vectorDrawable.getIntrinsicWidth(),
                    vectorDrawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            vectorDrawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
            vectorDrawable.draw(canvas);
            Bitmap resisedIcon = Bitmap.createScaledBitmap(bitmap, (int) (bitmap.getWidth() * 0.30), (int) (bitmap.getHeight() * 0.30), false);
            return BitmapDescriptorFactory.fromBitmap(resisedIcon);
        } else {
            return BitmapDescriptorFactory.defaultMarker();
        }
    }

    public static boolean isNightMode(Context context) {
        int nightModeFlags = context.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        return nightModeFlags == Configuration.UI_MODE_NIGHT_YES;
    }

}

package it.andreafilippi.whatsnearme.utils;

import com.google.android.libraries.places.api.model.Place;

import java.util.Arrays;
import java.util.List;

public class PlacesUtils {

    public static final List<Place.Field> placeFields = Arrays.asList(Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG, Place.Field.TYPES);

    public static enum Category {
        RESTAURANT("restaurant"),
        MUSEUM("museum"),
        ATM("atm");

        private final String description;

        Category(String description) {
            this.description = description;
        }

        public static Category getCategoryByString(String description) {
            for (Category c : Category.values()) {
                if (c.description.equals(description)) return c;
            }
            return null;
        }

        public String getDescription() {
            return this.description;
        }
    }

}

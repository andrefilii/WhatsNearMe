package it.andreafilippi.whatsnearme.utils;

import com.google.android.libraries.places.api.model.Place;

import java.util.Arrays;
import java.util.List;

public class PlacesUtils {

    public static final List<Place.Field> placeFields = Arrays.asList(Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG, Place.Field.TYPES);

    public static List<String> getPlaceTypesByCategory(Category category) {
        switch (category) {
            case RESTAURANT:
                return List.of("restaurant", "fast_food_restaurant", "bar", "cafe");
            case MUSEUM:
                return List.of("art_gallery", "museum");
            case ATM:
                return List.of("atm");
            default:
                return List.of();
        }
    }

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

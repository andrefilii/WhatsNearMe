package it.andreafilippi.whatsnearme.entities;

import java.io.Serializable;
import java.util.List;

public class Place implements Serializable {
    private String id;
    private String name;
    private List<String> tags;
    private Double lat;
    private Double lng;

    public Place() {

    }

    public Place(String nome, Double lat, Double lng) {
        this.name = nome;
        this.lat = lat;
        this.lng = lng;
    }

    public String getId() {
        return id;
    }

    public Place setId(String id) {
        this.id = id;
        return this;
    }

    public String getName() {
        return name;
    }

    public Place setName(String name) {
        this.name = name;
        return this;
    }

    public List<String> getTags() {
        return tags;
    }

    public Place setTags(List<String> tags) {
        this.tags = tags;
        return this;
    }

    public Double getLat() {
        return lat;
    }

    public Place setLat(Double lat) {
        this.lat = lat;
        return this;
    }

    public Double getLng() {
        return lng;
    }

    public Place setLng(Double lng) {
        this.lng = lng;
        return this;
    }

    public enum Category {
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

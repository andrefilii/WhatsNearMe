package it.andreafilippi.whatsnearme.entities;

import java.util.List;

public class Place {
    private String id;
    private String name;
    private List<String> tags;
    private Double lat;
    private Double lng;

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

        // Costruttore
        private Category(String description) {
            this.description = description;
        }

        // Metodo per ottenere la descrizione
        public String getDescription() {
            return this.description;
        }
    }
}

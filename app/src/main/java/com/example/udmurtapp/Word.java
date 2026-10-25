package com.example.udmurtapp;

import java.io.Serializable;

public class Word implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String id;
    private final String udmurt;
    private final String russian;
    private final String category;
    private final String imageUrl;
    private final String audioUrl;
    private final String example;
    private final String exampleRus;
    private boolean favorite;

    public Word(String id, String udmurt, String russian, String category, String imageUrl, String audioUrl, String example, String exampleRus) {
        this.id = id;
        this.udmurt = udmurt;
        this.russian = russian;
        this.category = category;
        this.imageUrl = imageUrl;
        this.audioUrl = audioUrl;
        this.example = example;
        this.exampleRus = exampleRus;
        this.favorite = false;
    }

    public String getId() { return id; }
    public String getUdmurt() { return udmurt; }
    public String getRussian() { return russian; }
    public String getCategory() { return category; }
    public String getImageUrl() { return imageUrl; }

    public String getAudioUrl() { return audioUrl; }

    public String getExample() { return example; }
    public String getExampleRus() { return exampleRus; }
    public boolean isFavorite() { return favorite; }
    public void setFavorite(boolean favorite) { this.favorite = favorite; }
}
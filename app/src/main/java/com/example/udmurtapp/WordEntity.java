package com.example.udmurtapp;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "words")
public class WordEntity {

    @PrimaryKey(autoGenerate = true)
    public int id;

    public String serverId;
    public String udmurt;
    public String russian;
    public String category;
    public String imageUrl;
    public String audioUrl;
    public String example;
    public String exampleRus;
}
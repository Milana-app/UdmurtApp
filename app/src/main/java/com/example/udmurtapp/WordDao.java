package com.example.udmurtapp;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.OnConflictStrategy;

import java.util.List;

@Dao
public interface WordDao {

    @Query("SELECT * FROM words")
    List<WordEntity> getAllWords();

    @Query("SELECT * FROM words WHERE category = :category")
    List<WordEntity> getWordsByCategory(String category);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<WordEntity> words);

    @Query("DELETE FROM words")
    void deleteAll();

    @Query("SELECT COUNT(*) FROM words")
    int getCount();
}
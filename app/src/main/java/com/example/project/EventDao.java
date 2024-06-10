package com.example.project;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface EventDao {
    @Query("SELECT * FROM Event")
    List<Event> getAll();

    @Query("SELECT * FROM Event WHERE id = :eventId")
    Event getById(int eventId);

    @Insert
    void insertAll(Event... events);

    @Delete
    void delete(Event event);
}
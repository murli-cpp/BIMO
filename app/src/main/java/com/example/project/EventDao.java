package com.example.project;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.TypeConverters;

import java.util.Date;
import java.util.List;

@Dao
public interface EventDao {
    @Query("SELECT MAX(updated_dt) FROM Event")
    @TypeConverters({TimestampConverter.class})
    Date getMaxUpdated();

    @Query("SELECT * FROM Event ORDER BY id DESC LIMIT :limit")
    List<Event> getAll(int limit);

    @Query("SELECT * FROM Event WHERE id = :eventId")
    Event getById(int eventId);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<Event> events);

    @Delete
    void delete(Event event);

    @Query("DELETE FROM Event")
    void Clear();
}
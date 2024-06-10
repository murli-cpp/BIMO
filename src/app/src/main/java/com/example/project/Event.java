package com.example.project;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters;

import java.util.Date;

@Entity
public class Event {
    @PrimaryKey
    public int id;

    @ColumnInfo(name = "title")
    public String title;

    @ColumnInfo(name = "updated_dt")
    @TypeConverters({TimestampConverter.class})
    public Date updated;

    @ColumnInfo(name = "start_dt")
    @TypeConverters({TimestampConverter.class})
    public Date start;

    @ColumnInfo(name = "end_dt")
    @TypeConverters({TimestampConverter.class})
    public Date end;

    @ColumnInfo(name = "duration")
    public int duration;

    @ColumnInfo(name = "type")
    public String type;

    @ColumnInfo(name = "object")
    public String object;

    @ColumnInfo(name = "file_audio")
    public String fileAudio;

    @ColumnInfo(name = "file_video")
    public String fileVideo;

    @ColumnInfo(name = "file_photo")
    public String filePhoto;
}
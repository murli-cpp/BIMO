package com.example.project;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters;

import java.util.Date;

import javax.annotation.Nullable;

@Entity(indices = {
        @Index("updated_dt")
})

public class Event {
    @PrimaryKey
    public int id;

    @Nullable
    @ColumnInfo(name = "title")
    public String title;

    @ColumnInfo(name = "updated_dt")
    @TypeConverters({TimestampConverter.class})
    public Date updated;

    @ColumnInfo(name = "start_dt")
    @TypeConverters({TimestampConverter.class})
    public Date start;

    @Nullable
    @ColumnInfo(name = "end_dt")
    @TypeConverters({TimestampConverter.class})
    public Date end;

    @Nullable
    @ColumnInfo(name = "duration")
    public int duration;

    @ColumnInfo(name = "type")
    public String type;

    @ColumnInfo(name = "object")
    public String object;

    @Nullable
    @ColumnInfo(name = "file_audio")
    public String fileAudio;

    @Nullable
    @ColumnInfo(name = "file_video")
    public String fileVideo;

    @Nullable
    @ColumnInfo(name = "file_photo")
    public String filePhoto;
}
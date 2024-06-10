package com.example.project;

import androidx.room.TypeConverter;

import java.util.Date;
public class TimestampConverter {
    @TypeConverter
    public static long  from(Date value) {
        return value.getTime();
    }
    @TypeConverter
    public static Date to(long value) {
        return new Date(value);
    }
}

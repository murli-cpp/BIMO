package com.example.project;

import androidx.room.TypeConverter;

import java.util.Date;
public class TimestampConverter {
    @TypeConverter
    public static long from(Date value) {
        if (value == null)
            return 0;
        return value.getTime();
    }
    @TypeConverter
    public static Date to(long value) {
        return new Date(value);
    }

    @TypeConverter
    public static Date to(Object value) {
        if (value == null)
            return null;
        return new Date((long) value);
    }
}

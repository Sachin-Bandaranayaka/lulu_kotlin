package com.example.luluuu.db

import androidx.room.TypeConverter
import com.example.luluuu.model.InvoiceItem
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.Date

class Converters {
    @TypeConverter
    fun fromTimestamp(value: Long?): Date? {
        return value?.let { Date(it) }
    }

    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? {
        return date?.time
    }

    @TypeConverter
    fun fromJson(value: String): List<InvoiceItem> {
        val listType = object : TypeToken<List<InvoiceItem>>() {}.type
        return Gson().fromJson(value, listType)
    }

    @TypeConverter
    fun toJson(list: List<InvoiceItem>): String {
        return Gson().toJson(list)
    }
} 
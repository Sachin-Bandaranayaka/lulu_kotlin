package com.example.luluuu.model

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.Date

class Converters {
    private val gson = Gson()

    @TypeConverter
    fun fromTimestamp(value: Long?): Date? {
        return value?.let { Date(it) }
    }

    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? {
        return date?.time
    }

    @TypeConverter
    fun fromItemsList(value: List<InvoiceItem>?): String {
        return gson.toJson(value ?: emptyList<InvoiceItem>())
    }

    @TypeConverter
    fun toItemsList(value: String?): List<InvoiceItem> {
        if (value.isNullOrEmpty()) return emptyList()
        val listType = object : TypeToken<List<InvoiceItem>>() {}.type
        return try {
            gson.fromJson(value, listType)
        } catch (e: Exception) {
            emptyList()
        }
    }
}

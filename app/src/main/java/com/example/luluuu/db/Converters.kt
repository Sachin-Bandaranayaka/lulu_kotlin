package com.example.luluuu.db

import androidx.room.TypeConverter
import com.example.luluuu.model.InvoiceItem
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.*

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
    fun fromInvoiceItemList(value: List<InvoiceItem>?): String {
        return gson.toJson(value ?: emptyList<InvoiceItem>())
    }

    @TypeConverter
    fun toInvoiceItemList(value: String?): List<InvoiceItem> {
        if (value.isNullOrEmpty()) return emptyList()
        val type = object : TypeToken<ArrayList<InvoiceItem>>() {}.type
        return try {
            gson.fromJson<ArrayList<InvoiceItem>>(value, type).orEmpty()
        } catch (e: Exception) {
            emptyList()
        }
    }
}
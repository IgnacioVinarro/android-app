package com.ignaciovinarro.stayfocused.core.database

import androidx.room.TypeConverter
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset

object Converters {
    @TypeConverter
    fun fromLocalDate(value: Long?): LocalDate? = value?.let { LocalDate.ofEpochDay(it) }

    @TypeConverter
    fun toLocalDate(date: LocalDate?): Long? = date?.toEpochDay()

    @TypeConverter
    fun fromLocalDateTime(value: Long?): LocalDateTime? = value?.let { LocalDateTime.ofEpochSecond(it / 1000, 0, ZoneOffset.UTC) }

    @TypeConverter
    fun toLocalDateTime(dateTime: LocalDateTime?): Long? = dateTime?.toEpochSecond(ZoneOffset.UTC)?.times(1000)
}
package mirujam.nekomemo.data.local

import androidx.room.TypeConverter

class Converters {

    @TypeConverter
    fun fromStringList(value: List<String>): String = ListJsonConverter.fromStringList(value)

    @TypeConverter
    fun toStringList(value: String): List<String> = ListJsonConverter.toStringList(value)

    @TypeConverter
    fun fromIntList(value: List<Int>): String = ListJsonConverter.fromIntList(value)

    @TypeConverter
    fun toIntList(value: String): List<Int> = ListJsonConverter.toIntList(value)
}

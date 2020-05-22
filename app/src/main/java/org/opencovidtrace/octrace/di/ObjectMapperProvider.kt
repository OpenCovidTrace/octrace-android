package org.opencovidtrace.octrace.di

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.TypeAdapter
import com.google.gson.reflect.TypeToken
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.google.gson.stream.JsonWriter
import org.opencovidtrace.octrace.storage.LocationIndex
import java.util.*

object ObjectMapperProvider : IndependentProvider<Gson>() {


    override fun initInstance(): Gson = GsonBuilder()
        .registerTypeAdapter(
            object : TypeToken<HashMap<LocationIndex, Long>>() {}.type,
            LocationIndexHashMapTypeAdapter()
        )
        .create()

    private class LocationIndexHashMapTypeAdapter : TypeAdapter<HashMap<LocationIndex, Long>>() {

        override fun write(writer: JsonWriter?, value: HashMap<LocationIndex, Long>?) {
            if (value == null) writer?.nullValue()
            else {
                writer?.value(toJson(value))
            }
        }

        override fun read(jsonReader: JsonReader?): HashMap<LocationIndex, Long> {
            if (jsonReader?.peek() == JsonToken.NULL) {
                jsonReader.nextNull()
                return hashMapOf()
            }
            if (jsonReader?.peek() == JsonToken.BEGIN_OBJECT) {
                (Gson().fromJson(jsonReader,
                    object :
                        TypeToken<HashMap<String, Long>>() {}.type
                ) as? HashMap<String, Long>)?.let {
                    val result: HashMap<LocationIndex, Long> = hashMapOf()
                    for ((key, value) in it) {
                        (Gson().fromJson(
                            key,
                            object : TypeToken<LocationIndex>() {}.type
                        ) as? LocationIndex)?.let { locationIndex ->
                            result.put(locationIndex, value)
                        }
                    }
                    return result
                } ?: kotlin.run { return hashMapOf() }
            }
            return hashMapOf()
        }
    }

}

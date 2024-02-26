package com.github.neapovil.elevators.object;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.Location;

import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.annotations.JsonAdapter;

public final class ChunkElevators
{
    @JsonAdapter(ElevatorsAdapter.class)
    public List<Location> elevators = new ArrayList<>();

    public boolean has(Location location)
    {
        return this.elevators.stream()
                .anyMatch(i -> i.getBlockX() == location.getBlockX() && i.getBlockY() == location.getBlockY() && i.getBlockZ() == location.getBlockZ());
    }

    class ElevatorsAdapter implements JsonSerializer<List<Location>>, JsonDeserializer<List<Location>>
    {
        @Override
        public JsonElement serialize(List<Location> src, Type typeOfSrc, JsonSerializationContext context)
        {
            final JsonArray jsonarray = new JsonArray();

            for (Location location : src)
            {
                final JsonObject jsonobject = new JsonObject();

                for (Map.Entry<String, Object> i : location.serialize().entrySet())
                {
                    jsonobject.add(i.getKey(), new JsonPrimitive(i.getValue().toString()));
                }

                jsonarray.add(jsonobject);
            }

            return jsonarray;
        }

        @Override
        public List<Location> deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException
        {
            final List<Location> locations = new ArrayList<>();

            for (JsonElement jsonelement : json.getAsJsonArray())
            {
                final Map<String, Object> map = new HashMap<>();

                for (Map.Entry<String, JsonElement> i : jsonelement.getAsJsonObject().entrySet())
                {
                    map.put(i.getKey(), i.getValue().getAsString());
                }

                locations.add(Location.deserialize(map));
            }

            return locations;
        }
    }
}

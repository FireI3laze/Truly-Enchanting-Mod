package com.fireblaze.truly_enchanting.runes;

import com.google.gson.*;
import net.minecraft.resources.ResourceLocation;

import java.lang.reflect.Type;

public class ResourceLocationAdapter implements JsonSerializer<ResourceLocation>, JsonDeserializer<ResourceLocation> {

    @Override
    public JsonElement serialize(ResourceLocation src, Type typeOfSrc, JsonSerializationContext context) {
        // ResourceLocation als String speichern
        return new JsonPrimitive(src.toString());
    }

    @Override
    public ResourceLocation deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        // String aus JSON wieder in ResourceLocation umwandeln
        return ResourceLocation.tryParse(json.getAsString());
    }
}

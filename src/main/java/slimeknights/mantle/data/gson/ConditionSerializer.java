package slimeknights.mantle.data.gson;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.mojang.serialization.JsonOps;
import net.fabricmc.fabric.api.resource.conditions.v1.ResourceCondition;

import java.lang.reflect.Type;

/** Gson adapter for Fabric resource conditions. */
public class ConditionSerializer implements JsonDeserializer<ResourceCondition>, JsonSerializer<ResourceCondition> {
  public static final ConditionSerializer INSTANCE = new ConditionSerializer();

  private ConditionSerializer() {}

  @Override
  public ResourceCondition deserialize(JsonElement json, Type type, JsonDeserializationContext context) throws JsonParseException {
    return ResourceCondition.CODEC.parse(JsonOps.INSTANCE, json)
      .getOrThrow(message -> new JsonParseException("Failed to parse resource condition: " + message));
  }

  @Override
  public JsonElement serialize(ResourceCondition condition, Type type, JsonSerializationContext context) {
    return ResourceCondition.CODEC.encodeStart(JsonOps.INSTANCE, condition)
      .getOrThrow(message -> new JsonParseException("Failed to serialize resource condition: " + message));
  }
}

package slimeknights.mantle.client.book.data.deserializer;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.mojang.serialization.JsonOps;
import net.fabricmc.fabric.api.resource.conditions.v1.ResourceCondition;
import net.fabricmc.fabric.api.resource.conditions.v1.ResourceConditions;
import net.minecraft.util.GsonHelper;
import slimeknights.mantle.client.book.data.JsonCondition;

import java.lang.reflect.Type;

/** Serializer/deserializer for book resource conditions. */
public interface ConditionDeserializer {
  ConditionDeserializer.Deserializer DESERIALIZER = new ConditionDeserializer.Deserializer();
  ConditionDeserializer.Serializer SERIALIZER = new ConditionDeserializer.Serializer();

  class Deserializer implements JsonDeserializer<JsonCondition> {
    @Override
    public JsonCondition deserialize(JsonElement json, Type type, JsonDeserializationContext context) throws JsonParseException {
      JsonObject jsonObject = GsonHelper.convertToJsonObject(json, "book condition");
      if (!jsonObject.has(ResourceConditions.CONDITIONS_KEY)) {
        return new JsonCondition();
      }

      JsonElement encodedCondition = jsonObject.get(ResourceConditions.CONDITIONS_KEY);
      ResourceCondition condition = ResourceCondition.CONDITION_CODEC.parse(JsonOps.INSTANCE, encodedCondition)
        .resultOrPartial(message -> {})
        .orElse(null);
      return new JsonCondition(condition);
    }
  }

  class Serializer implements JsonSerializer<JsonCondition> {
    @Override
    public JsonElement serialize(JsonCondition src, Type typeOfSrc, JsonSerializationContext context) {
      if (src.getCondition() == null) {
        return JsonNull.INSTANCE;
      }

      JsonElement encoded = ResourceCondition.CONDITION_CODEC.encodeStart(JsonOps.INSTANCE, src.getCondition())
        .resultOrPartial(message -> {})
        .orElse(JsonNull.INSTANCE);
      JsonObject wrapper = new JsonObject();
      wrapper.add(ResourceConditions.CONDITIONS_KEY, encoded);
      return wrapper;
    }
  }
}

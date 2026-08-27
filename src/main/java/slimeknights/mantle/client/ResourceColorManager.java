package slimeknights.mantle.client;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.ResourceManager;
import slimeknights.mantle.Mantle;
import slimeknights.mantle.data.listener.ISafeManagerReloadListener;
import slimeknights.mantle.util.JsonHelper;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/** Class allowing the resource pack to set colors for various things. */
public class ResourceColorManager implements ISafeManagerReloadListener, IdentifiableResourceReloadListener {
  private static final String COLORS_PATH = "mantle/colors.json";
  private static final String FALLBACK_PATH = "tinkering/colors.json";
  public static final TextColor WHITE = TextColor.fromRgb(-1);
  public static final ResourceColorManager INSTANCE = new ResourceColorManager();

  private static Map<String,TextColor> COLORS = Collections.emptyMap();

  private ResourceColorManager() {}

  public static void init() {
    ResourceManagerHelper.get(PackType.CLIENT_RESOURCES).registerReloadListener(INSTANCE);
  }

  private static void parseRecursive(String prefix, JsonObject json, Map<String,TextColor> colors) {
    for (Entry<String,JsonElement> entry : json.entrySet()) {
      String key = entry.getKey();
      JsonElement element = entry.getValue();
      if (element.isJsonObject()) {
        parseRecursive(prefix + key + ".", element.getAsJsonObject(), colors);
      } else if (element.isJsonPrimitive()) {
        String fullPath = prefix + key;
        if (!colors.containsKey(fullPath)) {
          String text = element.getAsString();
          TextColor color = TextColor.parseColor(text).result().orElse(null);
          if (color == null) {
            Mantle.logger.error("Color at key '{}' could not be parsed, got '{}'", fullPath, text);
          } else {
            colors.put(fullPath, color);
          }
        }
      } else if (!element.isJsonNull()) {
        Mantle.logger.error("Skipping color key '{}' as the value is not a string", key);
      }
    }
  }

  @Override
  public void onReloadSafe(ResourceManager manager) {
    Map<String,TextColor> colors = new HashMap<>();
    List<JsonObject> jsonFiles = JsonHelper.getFileInAllDomainsAndPacks(manager, COLORS_PATH, null);
    for (int i = jsonFiles.size() - 1; i >= 0; i--) {
      parseRecursive("", jsonFiles.get(i), colors);
    }
    jsonFiles = JsonHelper.getFileInAllDomainsAndPacks(manager, FALLBACK_PATH, COLORS_PATH);
    for (int i = jsonFiles.size() - 1; i >= 0; i--) {
      parseRecursive("", jsonFiles.get(i), colors);
    }
    COLORS = colors;
  }

  @Nullable
  public static TextColor getOrNull(String path) {
    return COLORS.get(path);
  }

  public static TextColor getTextColor(String path) {
    return COLORS.getOrDefault(path, WHITE);
  }

  public static int getColor(String path) {
    return getTextColor(path).getValue();
  }

  @Override
  public ResourceLocation getFabricId() {
    return Mantle.getResource("resource_color_manager");
  }
}

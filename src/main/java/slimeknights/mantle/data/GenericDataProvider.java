package slimeknights.mantle.data;

import com.google.common.hash.Hashing;
import com.google.common.hash.HashingOutputStream;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.stream.JsonWriter;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import lombok.RequiredArgsConstructor;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.util.GsonHelper;
import slimeknights.mantle.Mantle;
import slimeknights.mantle.util.JsonHelper;

import javax.annotation.Nullable;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;

/** Generic logic to convert any serializable object into JSON. */
@SuppressWarnings({"unused", "SameParameterValue"})
@RequiredArgsConstructor
public abstract class GenericDataProvider implements DataProvider {
  protected final FabricDataOutput output;
  private final PackType type;
  private final String folder;
  private final Gson gson;

  public GenericDataProvider(FabricDataOutput output, PackType type, String folder) {
    this(output, type, folder, JsonHelper.DEFAULT_GSON);
  }

  protected void saveJson(CachedOutput output, ResourceLocation location, Object object, @Nullable Comparator<String> keyComparator) {
    try {
      Path path = this.output.getOutputFolder().resolve(Paths.get(type.getDirectory(), location.getNamespace(), folder, location.getPath() + ".json"));
      saveStable(output, gson.toJsonTree(object), path, keyComparator);
    } catch (IOException e) {
      Mantle.logger.error("Couldn't create data for {}", location, e);
    }
  }

  protected void saveJson(CachedOutput output, ResourceLocation location, Object object) {
    saveJson(output, location, object, DataProvider.KEY_COMPARATOR);
  }

  protected <T> void saveJson(CachedOutput output, ResourceLocation location, Codec<T> codec, T object) {
    saveJson(output, location, codec.encodeStart(JsonOps.INSTANCE, object).getOrThrow(false, Mantle.logger::error));
  }

  @SuppressWarnings("UnstableApiUsage")
  static void saveStable(CachedOutput cache, JsonElement json, Path path, @Nullable Comparator<String> keyComparator) throws IOException {
    ByteArrayOutputStream byteOutput = new ByteArrayOutputStream();
    HashingOutputStream hashingOutput = new HashingOutputStream(Hashing.sha1(), byteOutput);
    JsonWriter writer = new JsonWriter(new OutputStreamWriter(hashingOutput, StandardCharsets.UTF_8));
    writer.setSerializeNulls(false);
    writer.setIndent("  ");
    GsonHelper.writeValue(writer, json, keyComparator);
    writer.close();
    cache.writeIfNeeded(path, byteOutput.toByteArray(), hashingOutput.hash());
  }
}

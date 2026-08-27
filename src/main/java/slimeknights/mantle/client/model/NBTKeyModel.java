package slimeknights.mantle.client.model;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Pair;
import com.mojang.math.Transformation;
import io.github.fabricators_of_create.porting_lib.models.CompositeModel;
import io.github.fabricators_of_create.porting_lib.models.geometry.BlockGeometryBakingContext;
import io.github.fabricators_of_create.porting_lib.models.geometry.IGeometryBakingContext;
import io.github.fabricators_of_create.porting_lib.models.geometry.IGeometryLoader;
import io.github.fabricators_of_create.porting_lib.models.geometry.IUnbakedGeometry;
import lombok.RequiredArgsConstructor;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.client.resources.model.UnbakedModel;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import slimeknights.mantle.client.model.util.MantleItemLayerModel;
import slimeknights.mantle.client.model.util.ModelTextureIteratable;
import slimeknights.mantle.util.JsonHelper;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Function;

/** Model which uses a key in item custom data to select which texture variant to load. */
@RequiredArgsConstructor
public class NBTKeyModel implements IUnbakedGeometry<NBTKeyModel> {
  public static final IGeometryLoader<NBTKeyModel> LOADER = NBTKeyModel::deserialize;
  private static final Multimap<ResourceLocation, Pair<String, ResourceLocation>> EXTRA_TEXTURES = HashMultimap.create();

  public static void registerExtraTexture(ResourceLocation key, String textureName, ResourceLocation texture) {
    EXTRA_TEXTURES.put(key, Pair.of(textureName, texture));
  }

  private final String nbtKey;
  @Nullable
  private final ResourceLocation extraTexturesKey;
  private Map<String, Material> textures = Collections.emptyMap();

  @Override
  public void resolveParents(Function<ResourceLocation, UnbakedModel> modelGetter, IGeometryBakingContext owner) {
    textures = new HashMap<>();
    textures.put("default", owner.getMaterial("default"));

    if (owner instanceof BlockGeometryBakingContext blockContext) {
      ModelTextureIteratable iterable = new ModelTextureIteratable(null, blockContext.owner);
      for (Map<String, Either<Material, String>> map : iterable) {
        for (String key : map.keySet()) {
          if (!textures.containsKey(key) && owner.hasMaterial(key)) {
            textures.put(key, owner.getMaterial(key));
          }
        }
      }
    }

    if (extraTexturesKey != null) {
      for (Pair<String, ResourceLocation> extra : EXTRA_TEXTURES.get(extraTexturesKey)) {
        textures.putIfAbsent(extra.getFirst(), new Material(InventoryMenu.BLOCK_ATLAS, extra.getSecond()));
      }
    }
  }

  private static BakedModel bakeModel(IGeometryBakingContext owner, Material texture,
                                      Function<Material, TextureAtlasSprite> spriteGetter,
                                      Transformation rotation, ItemOverrides overrides) {
    TextureAtlasSprite sprite = spriteGetter.apply(texture);
    CompositeModel.Baked.Builder builder = CompositeModel.Baked.builder(owner, sprite, overrides, owner.getTransforms());
    builder.addQuads(MantleItemLayerModel.getDefaultRenderType(owner),
      MantleItemLayerModel.getQuadsForSprite(-1, -1, sprite, rotation, 0));
    return builder.build();
  }

  @Override
  public BakedModel bake(IGeometryBakingContext owner, ModelBaker baker,
                         Function<Material, TextureAtlasSprite> spriteGetter,
                         ModelState modelTransform, ItemOverrides overrides) {
    Transformation transform = MantleItemLayerModel.applyTransform(modelTransform, owner.getRootTransform()).getRotation();
    Map<String, BakedModel> variants = new HashMap<>(textures.size());
    for (Entry<String, Material> entry : textures.entrySet()) {
      if (!entry.getKey().equals("default")) {
        variants.put(entry.getKey(), bakeModel(owner, entry.getValue(), spriteGetter, transform, ItemOverrides.EMPTY));
      }
    }
    return bakeModel(owner, textures.get("default"), spriteGetter, transform,
      new Overrides(nbtKey, textures, Map.copyOf(variants)));
  }

  @RequiredArgsConstructor
  public static class Overrides extends ItemOverrides {
    private final String nbtKey;
    private final Map<String, Material> textures;
    private final Map<String, BakedModel> variants;

    @Override
    public BakedModel resolve(BakedModel model, ItemStack stack, @Nullable ClientLevel world,
                              @Nullable LivingEntity livingEntity, int seed) {
      CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
      if (customData != null) {
        CompoundTag nbt = customData.copyTag();
        if (nbt.contains(nbtKey)) {
          return variants.getOrDefault(nbt.getString(nbtKey), model);
        }
      }
      return model;
    }

    public Material getTexture(String name) {
      Material texture = textures.get(name);
      return texture != null ? texture : textures.get("default");
    }
  }

  public static NBTKeyModel deserialize(JsonObject json, JsonDeserializationContext context) {
    String key = GsonHelper.getAsString(json, "nbt_key");
    ResourceLocation extraTexturesKey = json.has("extra_textures_key")
      ? JsonHelper.getResourceLocation(json, "extra_textures_key") : null;
    return new NBTKeyModel(key, extraTexturesKey);
  }
}

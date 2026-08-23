package slimeknights.mantle.client.model;

import com.google.common.collect.Sets;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.mojang.datafixers.util.Either;
import io.github.fabricators_of_create.porting_lib.models.CustomParticleIconModel;
import io.github.fabricators_of_create.porting_lib.models.data.ModelData;
import io.github.fabricators_of_create.porting_lib.models.geometry.IGeometryBakingContext;
import io.github.fabricators_of_create.porting_lib.models.geometry.IGeometryLoader;
import io.github.fabricators_of_create.porting_lib.models.geometry.IUnbakedGeometry;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import net.fabricmc.fabric.api.renderer.v1.render.RenderContext;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.client.resources.model.UnbakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import slimeknights.mantle.client.model.util.ColoredBlockModel;
import slimeknights.mantle.client.model.util.DynamicBakedWrapper;
import slimeknights.mantle.client.model.util.GeometryContextWrapper;
import slimeknights.mantle.client.model.util.ModelHelper;
import slimeknights.mantle.client.model.util.ModelTextureIteratable;
import slimeknights.mantle.client.model.util.SimpleBlockModel;
import slimeknights.mantle.util.RetexturedHelper;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Model that dynamically retextures a list of textures based on data from {@link RetexturedHelper}.
 */
@SuppressWarnings("WeakerAccess")
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public class RetexturedModel implements IUnbakedGeometry<RetexturedModel> {
  /** Loader instance */
  public static final IGeometryLoader<RetexturedModel> LOADER = RetexturedModel::deserialize;

  private final SimpleBlockModel model;
  private final Set<String> retextured;

  @Override
  public void resolveParents(Function<ResourceLocation, UnbakedModel> modelGetter, IGeometryBakingContext context) {
    model.resolveParents(modelGetter, context);
  }

  @Override
  public BakedModel bake(IGeometryBakingContext owner, ModelBaker baker,
                         Function<Material, TextureAtlasSprite> spriteGetter, ModelState transform,
                         ItemOverrides overrides) {
    BakedModel baked = model.bake(owner, baker, spriteGetter, transform, overrides);
    return new Baked(baked, owner, model, transform, getAllRetextured(owner, model, retextured));
  }

  /** Gets all texture aliases which ultimately reference a retextured texture. */
  public static Set<String> getAllRetextured(IGeometryBakingContext owner, SimpleBlockModel model, Set<String> originalSet) {
    Set<String> retextured = Sets.newHashSet(originalSet);
    for (Map<String, Either<Material, String>> textures : ModelTextureIteratable.of(owner, model)) {
      textures.forEach((name, either) -> either.ifRight(parent -> {
        if (retextured.contains(parent)) {
          retextured.add(name);
        }
      }));
    }
    return Set.copyOf(retextured);
  }

  /** Deserializes a retextured model from JSON. */
  public static RetexturedModel deserialize(JsonObject json, JsonDeserializationContext context) {
    ColoredBlockModel model = ColoredBlockModel.deserialize(json, context);
    return new RetexturedModel(model, getRetexturedNames(json));
  }

  /** Gets the list of texture keys which may be dynamically replaced. */
  public static Set<String> getRetexturedNames(JsonObject json) {
    if (json.has("retextured")) {
      JsonElement retextured = json.get("retextured");
      if (retextured.isJsonArray()) {
        JsonArray array = retextured.getAsJsonArray();
        if (array.isEmpty()) {
          throw new JsonSyntaxException("Must have at least one texture in retextured");
        }
        List<String> builder = new ArrayList<>(array.size());
        for (int i = 0; i < array.size(); i++) {
          builder.add(GsonHelper.convertToString(array.get(i), "retextured[" + i + "]"));
        }
        return Set.copyOf(builder);
      }
      if (retextured.isJsonPrimitive()) {
        return Set.of(retextured.getAsString());
      }
    }
    throw new JsonSyntaxException("Missing retextured, expected to find a String or a JsonArray");
  }

  /** Baked variant which swaps the underlying model using block entity render data or item custom data. */
  public static class Baked extends DynamicBakedWrapper<BakedModel> implements CustomParticleIconModel {
    private final Map<ResourceLocation, BakedModel> cache = new ConcurrentHashMap<>();
    private final IGeometryBakingContext owner;
    private final SimpleBlockModel model;
    private final ModelState transform;
    private final Set<String> retextured;

    public Baked(BakedModel baked, IGeometryBakingContext owner, SimpleBlockModel model,
                 ModelState transform, Set<String> retextured) {
      super(baked);
      this.owner = owner;
      this.model = model;
      this.transform = transform;
      this.retextured = retextured;
    }

    private BakedModel getRetexturedModel(ResourceLocation name) {
      return model.bakeDynamic(new RetexturedContext(owner, retextured, name), transform);
    }

    private BakedModel getCachedModel(Block block) {
      return cache.computeIfAbsent(ModelHelper.getParticleTexture(block), this::getRetexturedModel);
    }

    @Override
    public TextureAtlasSprite getParticleIcon(Object dataObject) {
      if (retextured.contains("particle") && dataObject instanceof ModelData data) {
        Block block = data.get(RetexturedHelper.BLOCK_PROPERTY);
        if (block != null) {
          BakedModel cached = getCachedModel(block);
          if (cached instanceof CustomParticleIconModel customParticle) {
            return customParticle.getParticleIcon(data);
          }
          return cached.getParticleIcon();
        }
      }
      if (wrapped instanceof CustomParticleIconModel customParticle) {
        return customParticle.getParticleIcon(dataObject);
      }
      return wrapped.getParticleIcon();
    }

    @Override
    public void emitBlockQuads(BlockAndTintGetter blockView, BlockState state, BlockPos pos,
                               Supplier<RandomSource> randomSupplier, RenderContext context) {
      if (blockView.getBlockEntityRenderData(pos) instanceof ModelData data) {
        Block block = data.get(RetexturedHelper.BLOCK_PROPERTY);
        if (block != null) {
          getCachedModel(block).emitBlockQuads(blockView, state, pos, randomSupplier, context);
          return;
        }
      }
      super.emitBlockQuads(blockView, state, pos, randomSupplier, context);
    }

    @Override
    public void emitItemQuads(ItemStack stack, Supplier<RandomSource> randomSupplier, RenderContext context) {
      if (stack.isEmpty() || RetexturedHelper.getTextureName(stack).isEmpty()) {
        super.emitItemQuads(stack, randomSupplier, context);
        return;
      }
      Block block = RetexturedHelper.getTexture(stack);
      if (block == Blocks.AIR) {
        super.emitItemQuads(stack, randomSupplier, context);
        return;
      }
      getCachedModel(block).emitItemQuads(stack, randomSupplier, context);
    }

    @Override
    public ItemOverrides getOverrides() {
      return RetexturedOverride.INSTANCE;
    }
  }

  /** Model context which substitutes the configured dynamic texture. */
  public static class RetexturedContext extends GeometryContextWrapper {
    private final Set<String> retextured;
    private final Material texture;

    public RetexturedContext(IGeometryBakingContext base, Set<String> retextured, ResourceLocation texture) {
      super(base);
      this.retextured = retextured;
      this.texture = new Material(InventoryMenu.BLOCK_ATLAS, texture);
    }

    @Override
    public boolean hasMaterial(String name) {
      if (retextured.contains(name)) {
        return !MissingTextureAtlasSprite.getLocation().equals(texture.texture());
      }
      return super.hasMaterial(name);
    }

    @Override
    public Material getMaterial(String name) {
      if (retextured.contains(name)) {
        return texture;
      }
      return super.getMaterial(name);
    }
  }

  /** Vanilla item override fallback for render paths which resolve overrides before Fabric quad emission. */
  private static class RetexturedOverride extends ItemOverrides {
    private static final RetexturedOverride INSTANCE = new RetexturedOverride();

    @Nullable
    @Override
    public BakedModel resolve(BakedModel originalModel, ItemStack stack, @Nullable ClientLevel level,
                              @Nullable LivingEntity entity, int seed) {
      if (stack.isEmpty() || RetexturedHelper.getTextureName(stack).isEmpty()) {
        return originalModel;
      }
      Block block = RetexturedHelper.getTexture(stack);
      if (block == Blocks.AIR) {
        return originalModel;
      }
      BakedModel unwrapped = ModelHelper.unwrap(originalModel, Baked.class);
      if (unwrapped instanceof Baked baked) {
        return baked.getCachedModel(block);
      }
      return originalModel;
    }
  }
}

package slimeknights.mantle.client.model.util;

import com.google.common.collect.ImmutableList;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.mojang.math.Transformation;
import io.github.fabricators_of_create.porting_lib.models.CompositeModel;
import io.github.fabricators_of_create.porting_lib.models.ExtraFaceData;
import io.github.fabricators_of_create.porting_lib.models.UnbakedGeometryHelper;
import io.github.fabricators_of_create.porting_lib.models.geometry.IGeometryBakingContext;
import io.github.fabricators_of_create.porting_lib.models.geometry.IGeometryLoader;
import io.github.fabricators_of_create.porting_lib.models.geometry.IUnbakedGeometry;
import io.github.fabricators_of_create.porting_lib.models.geometry.SimpleModelState;
import io.github.fabricators_of_create.porting_lib.render_types.PortingLibRenderTypes;
import io.github.fabricators_of_create.porting_lib.render_types.RenderTypeGroup;
import lombok.RequiredArgsConstructor;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import slimeknights.mantle.data.loadable.common.ColorLoadable;
import slimeknights.mantle.util.ItemLayerPixels;
import slimeknights.mantle.util.JsonHelper;
import slimeknights.mantle.util.LogicHelper;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * Item-layer model with Mantle's static color, tint, luminosity, and per-layer render-type support.
 * Uses Porting Lib's 1.21 item element generator instead of the removed Forge vertex pipeline.
 */
@RequiredArgsConstructor
public class MantleItemLayerModel implements IUnbakedGeometry<MantleItemLayerModel> {
  public static final IGeometryLoader<MantleItemLayerModel> LOADER = MantleItemLayerModel::deserialize;

  private final List<LayerData> layers;
  private List<Material> textures = List.of();

  private LayerData getLayer(int index) {
    return LogicHelper.getOrDefault(layers, index, LayerData.DEFAULT);
  }

  /** Gets the normal render types for generated item layers. */
  public static RenderTypeGroup getDefaultRenderType(IGeometryBakingContext context) {
    ResourceLocation renderTypeHint = context.getRenderTypeHint();
    if (renderTypeHint != null) {
      return context.getRenderType(renderTypeHint);
    }
    return new RenderTypeGroup(RenderType.translucent(), PortingLibRenderTypes.ITEM_UNSORTED_TRANSLUCENT.get());
  }

  /** Applies Mantle/Porting Lib root transformation semantics to a model state. */
  public static ModelState applyTransform(ModelState modelState, Transformation transformation) {
    if (transformation.isIdentity()) {
      return modelState;
    }
    return UnbakedGeometryHelper.composeRootTransformIntoModelState(modelState, transformation);
  }

  @Override
  public BakedModel bake(IGeometryBakingContext owner, ModelBaker baker,
                         Function<Material, TextureAtlasSprite> spriteGetter,
                         ModelState modelTransform, ItemOverrides overrides) {
    List<Material> resolvedTextures = new ArrayList<>();
    for (int i = 0; owner.hasMaterial("layer" + i); i++) {
      resolvedTextures.add(owner.getMaterial("layer" + i));
    }
    textures = List.copyOf(resolvedTextures);
    if (textures.isEmpty()) {
      throw new IllegalStateException("Empty textures list");
    }

    TextureAtlasSprite particle = spriteGetter.apply(owner.hasMaterial("particle")
      ? owner.getMaterial("particle") : textures.get(0));
    modelTransform = applyTransform(modelTransform, owner.getRootTransform());

    RenderTypeGroup normalRenderTypes = getDefaultRenderType(owner);
    CompositeModel.Baked.Builder modelBuilder = CompositeModel.Baked.builder(owner, particle, overrides, owner.getTransforms());

    for (int i = 0; i < textures.size(); i++) {
      TextureAtlasSprite sprite = spriteGetter.apply(textures.get(i));
      LayerData data = getLayer(i);
      int tint = data.noTint() ? -1 : i;
      List<BakedQuad> quads = getQuadsForSprite(data.color(), tint, sprite,
        modelTransform.getRotation(), data.luminosity());
      modelBuilder.addQuads(data.getRenderType(owner, normalRenderTypes), quads);
    }
    return modelBuilder.build();
  }

  /** Builds all quads for one generated-item sprite layer. */
  public static ImmutableList<BakedQuad> getQuadsForSprite(int color, int tint, TextureAtlasSprite sprite,
                                                            Transformation transform, int luminosity) {
    return getQuadsForSprite(color, tint, sprite, transform, luminosity, null);
  }

  /**
   * Builds all quads for one generated-item sprite layer.
   * The legacy pixel tracker is accepted for API compatibility; vanilla/Porting Lib 1.21 performs the geometry generation.
   */
  public static ImmutableList<BakedQuad> getQuadsForSprite(int color, int tint, TextureAtlasSprite sprite,
                                                            Transformation transform, int luminosity,
                                                            @Nullable ItemLayerPixels pixels) {
    int resolvedColor = color == -1 ? 0xFFFFFFFF : color;
    int light = Math.max(0, Math.min(15, luminosity));
    ExtraFaceData faceData = new ExtraFaceData(resolvedColor, light, light, true);
    List<net.minecraft.client.renderer.block.model.BlockElement> elements =
      UnbakedGeometryHelper.createUnbakedItemElements(tint, sprite, faceData);
    List<BakedQuad> quads = UnbakedGeometryHelper.bakeElements(elements, material -> sprite, new SimpleModelState(transform));
    return ImmutableList.copyOf(quads);
  }

  /** Returns the south/front quad used by GUI-only render paths. */
  public static BakedQuad getQuadForGui(int color, int tint, TextureAtlasSprite sprite,
                                        Transformation transform, int emissivity) {
    List<BakedQuad> quads = getQuadsForSprite(color, tint, sprite, transform, emissivity);
    for (BakedQuad quad : quads) {
      if (quad.getDirection() == Direction.SOUTH) {
        return quad;
      }
    }
    if (quads.isEmpty()) {
      throw new IllegalStateException("Generated item layer produced no quads");
    }
    return quads.get(0);
  }

  /** Per-layer options read from Mantle model JSON. */
  private record LayerData(int color, int luminosity, boolean noTint, @Nullable ResourceLocation renderType) {
    private static final LayerData DEFAULT = new LayerData(-1, 0, false, null);

    public RenderTypeGroup getRenderType(IGeometryBakingContext context, RenderTypeGroup defaultType) {
      return renderType == null ? defaultType : context.getRenderType(renderType);
    }

    public static LayerData fromJson(JsonObject json) {
      int color = ColorLoadable.ALPHA.getOrDefault(json, "color", -1);
      int luminosity = GsonHelper.getAsInt(json, "luminosity", 0);
      boolean noTint = GsonHelper.getAsBoolean(json, "no_tint", false);
      ResourceLocation renderType = JsonHelper.getResourceLocation(json, "render_type", null);
      return new LayerData(color, luminosity, noTint, renderType);
    }
  }

  /** Deserializes this model from JSON. */
  public static MantleItemLayerModel deserialize(JsonObject json, JsonDeserializationContext context) {
    return new MantleItemLayerModel(JsonHelper.parseList(json, "layers", LayerData::fromJson));
  }
}

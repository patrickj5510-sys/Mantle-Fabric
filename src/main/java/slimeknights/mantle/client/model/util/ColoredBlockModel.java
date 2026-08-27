package slimeknights.mantle.client.model.util;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.mojang.datafixers.util.Either;
import com.mojang.math.Transformation;
import io.github.fabricators_of_create.porting_lib.models.IModelBuilder;
import io.github.fabricators_of_create.porting_lib.models.IQuadTransformer;
import io.github.fabricators_of_create.porting_lib.models.QuadTransformers;
import io.github.fabricators_of_create.porting_lib.models.geometry.IGeometryBakingContext;
import io.github.fabricators_of_create.porting_lib.models.geometry.IGeometryLoader;
import lombok.Getter;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.BlockElement;
import net.minecraft.client.renderer.block.model.BlockElementFace;
import net.minecraft.client.renderer.block.model.BlockModel;
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
import slimeknights.mantle.util.JsonHelper;
import slimeknights.mantle.util.LogicHelper;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Block model supporting a static color, luminosity, and per-element UV lock.
 */
@SuppressWarnings("unused")
public class ColoredBlockModel extends SimpleBlockModel {
  public static final IGeometryLoader<SimpleBlockModel> LOADER = ColoredBlockModel::deserialize;

  @Getter
  private final List<ColorData> colorData;

  public ColoredBlockModel(@Nullable ResourceLocation parentLocation, Map<String, Either<Material, String>> textures,
                           List<BlockElement> parts, List<ColorData> colorData) {
    super(parentLocation, textures, parts);
    this.colorData = colorData;
  }

  public ColoredBlockModel(SimpleBlockModel base, List<ColorData> colorData) {
    super(base);
    this.colorData = colorData;
  }

  /** Creates a ModelState with the same rotation but a per-element UV-lock setting. */
  private static ModelState withUvLock(ModelState base, boolean uvLock) {
    return new ModelState() {
      @Override
      public Transformation getRotation() {
        return base.getRotation();
      }

      @Override
      public boolean isUvLocked() {
        return uvLock;
      }
    };
  }

  private static void bakePart(IModelBuilder<?> builder, IGeometryBakingContext owner, BlockElement part,
                               ColorData colors, Function<Material, TextureAtlasSprite> spriteGetter,
                               ModelState modelState, IQuadTransformer rootTransformer) {
    ModelState faceState = withUvLock(modelState, colors.isUvLock(modelState.isUvLocked()));

    for (Map.Entry<Direction, BlockElementFace> entry : part.faces.entrySet()) {
      Direction direction = entry.getKey();
      BlockElementFace face = entry.getValue();
      String texture = face.texture();
      if (!texture.isEmpty() && texture.charAt(0) == '#') {
        texture = texture.substring(1);
      }

      TextureAtlasSprite sprite = spriteGetter.apply(owner.getMaterial(texture));
      BakedQuad quad = BlockModel.bakeFace(part, face, sprite, direction, faceState);

      rootTransformer.processInPlace(quad);
      if (colors.color() != -1) {
        QuadTransformers.applyingColor(colors.color()).processInPlace(quad);
      }
      if (colors.luminosity() > 0) {
        QuadTransformers.settingEmissivity(colors.luminosity()).processInPlace(quad);
      }

      if (face.cullForDirection() == null) {
        builder.addUnculledFace(quad);
      } else {
        builder.addCulledFace(modelState.getRotation().rotateTransform(face.cullForDirection()), quad);
      }
    }
  }

  public static BakedModel bakeModel(IGeometryBakingContext owner, List<BlockElement> elements,
                                     List<ColorData> colorData,
                                     Function<Material, TextureAtlasSprite> spriteGetter,
                                     ModelState transform, ItemOverrides overrides) {
    TextureAtlasSprite particle = spriteGetter.apply(owner.getMaterial("particle"));
    IModelBuilder<?> builder = IModelBuilder.of(owner.useAmbientOcclusion(), owner.useBlockLight(), owner.isGui3d(),
      owner.getTransforms(), overrides, particle, getRenderTypeGroup(owner));
    IQuadTransformer rootTransformer = applyTransform(transform, owner.getRootTransform());

    for (int i = 0; i < elements.size(); i++) {
      ColorData colors = LogicHelper.getOrDefault(colorData, i, ColorData.DEFAULT);
      bakePart(builder, owner, elements.get(i), colors, spriteGetter, transform, rootTransformer);
    }
    return builder.build();
  }

  @Override
  public BakedModel bake(IGeometryBakingContext owner, ModelBaker baker,
                         Function<Material, TextureAtlasSprite> spriteGetter,
                         ModelState modelTransform, ItemOverrides overrides) {
    return bakeModel(owner, getElements(), colorData, spriteGetter, modelTransform, overrides);
  }

  @Override
  public BakedModel bakeWithElements(IGeometryBakingContext owner, List<BlockElement> elements, ModelState transform) {
    return bakeModel(owner, elements, colorData, Material::sprite, transform, ItemOverrides.EMPTY);
  }

  /** Data class for per-element color properties. */
  public record ColorData(int color, int luminosity, @Nullable Boolean uvlock) {
    public static final ColorData DEFAULT = new ColorData(-1, -1, null);

    public boolean isUvLock(boolean defaultLock) {
      return uvlock == null ? defaultLock : uvlock;
    }

    public static ColorData fromJson(JsonObject json) {
      int color = ColorLoadable.ALPHA.getOrDefault(json, "color", -1);
      int luminosity = GsonHelper.getAsInt(json, "luminosity", -1);
      Boolean uvlock = json.has("uvlock") ? GsonHelper.getAsBoolean(json, "uvlock") : null;
      return new ColorData(color, luminosity, uvlock);
    }
  }

  public static ColoredBlockModel deserialize(JsonObject json, JsonDeserializationContext context) {
    SimpleBlockModel model = SimpleBlockModel.deserialize(json, context);
    List<ColorData> colors = json.has("colors")
      ? JsonHelper.parseList(json, "colors", ColorData::fromJson)
      : Collections.emptyList();
    return new ColoredBlockModel(model, colors);
  }

  /** Backwards-compatible helper retained for API callers. */
  public static int swapColorRedBlue(int color) {
    return QuadTransformers.toABGR(color);
  }

  /** Backwards-compatible color transformer helper. */
  public static IQuadTransformer applyColorQuadTransformer(int color) {
    return QuadTransformers.applyingColor(color);
  }
}

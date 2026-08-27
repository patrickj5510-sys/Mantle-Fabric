package slimeknights.mantle.client.model.util;

import com.google.common.collect.Sets;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSyntaxException;
import com.mojang.datafixers.util.Either;
import com.mojang.math.Transformation;
import io.github.fabricators_of_create.porting_lib.models.IModelBuilder;
import io.github.fabricators_of_create.porting_lib.models.IQuadTransformer;
import io.github.fabricators_of_create.porting_lib.models.QuadTransformers;
import io.github.fabricators_of_create.porting_lib.models.UnbakedGeometryHelper;
import io.github.fabricators_of_create.porting_lib.models.geometry.IGeometryBakingContext;
import io.github.fabricators_of_create.porting_lib.models.geometry.IGeometryLoader;
import io.github.fabricators_of_create.porting_lib.models.geometry.IUnbakedGeometry;
import io.github.fabricators_of_create.porting_lib.render_types.RenderTypeGroup;
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
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.client.resources.model.UnbakedModel;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.inventory.InventoryMenu;
import slimeknights.mantle.Mantle;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Simpler version of {@link BlockModel} for use in a custom geometry. */
@SuppressWarnings("WeakerAccess")
public class SimpleBlockModel implements IUnbakedGeometry<SimpleBlockModel> {
  public static final IGeometryLoader<SimpleBlockModel> LOADER = SimpleBlockModel::deserialize;

  @Getter
  @Nullable
  private ResourceLocation parentLocation;
  private final List<BlockElement> parts;
  @Getter
  private final Map<String, Either<Material, String>> textures;
  @Getter
  private BlockModel parent;

  public SimpleBlockModel(@Nullable ResourceLocation parentLocation, Map<String, Either<Material, String>> textures, List<BlockElement> parts) {
    this.parts = parts;
    this.textures = textures;
    this.parentLocation = parentLocation;
  }

  public SimpleBlockModel(SimpleBlockModel base) {
    this.parts = base.parts;
    this.textures = base.textures;
    this.parentLocation = base.parentLocation;
    this.parent = base.parent;
  }

  @SuppressWarnings("deprecation")
  public List<BlockElement> getElements() {
    return parts.isEmpty() && parent != null ? parent.getElements() : parts;
  }

  @Override
  public void resolveParents(Function<ResourceLocation, UnbakedModel> modelGetter, IGeometryBakingContext owner) {
    fetchParent(owner, modelGetter);
  }

  /** Loads this model's parent chain. Kept as a public helper for Mantle dynamic model callers. */
  public void fetchParent(IGeometryBakingContext owner, Function<ResourceLocation, UnbakedModel> modelGetter) {
    if (parent != null || parentLocation == null) {
      return;
    }

    Set<UnbakedModel> chain = Sets.newLinkedHashSet();
    parent = getParent(modelGetter, chain, parentLocation, owner.getModelName());
    if (parent == null) {
      parent = getMissing(modelGetter);
      parentLocation = ModelBakery.MISSING_MODEL_LOCATION;
    }

    for (BlockModel link = parent; link.parentLocation != null && link.parent == null; link = link.parent) {
      chain.add(link);
      link.parent = getParent(modelGetter, chain, link.parentLocation, link.name);
      if (link.parent == null) {
        link.parent = getMissing(modelGetter);
        link.parentLocation = ModelBakery.MISSING_MODEL_LOCATION;
      }
    }
  }

  @Nullable
  private static BlockModel getParent(Function<ResourceLocation, UnbakedModel> modelGetter, Set<UnbakedModel> chain, ResourceLocation location, String name) {
    UnbakedModel unbaked = modelGetter.apply(location);
    if (unbaked == null) {
      Mantle.logger.warn("No parent '{}' while loading model '{}'", location, name);
      return null;
    }
    if (chain.contains(unbaked)) {
      Mantle.logger.warn("Found 'parent' loop while loading model '{}' in chain: {} -> {}", name,
        chain.stream().map(Object::toString).collect(Collectors.joining(" -> ")), location);
      return null;
    }
    if (!(unbaked instanceof BlockModel blockModel)) {
      throw new IllegalStateException("BlockModel parent has to be a block model.");
    }
    return blockModel;
  }

  @Nonnull
  private static BlockModel getMissing(Function<ResourceLocation, UnbakedModel> modelGetter) {
    UnbakedModel model = modelGetter.apply(ModelBakery.MISSING_MODEL_LOCATION);
    if (!(model instanceof BlockModel blockModel)) {
      throw new IllegalStateException("Failed to load missing model");
    }
    return blockModel;
  }

  public static RenderTypeGroup getRenderTypeGroup(IGeometryBakingContext owner) {
    ResourceLocation renderTypeHint = owner.getRenderTypeHint();
    return renderTypeHint != null ? owner.getRenderType(renderTypeHint) : RenderTypeGroup.EMPTY;
  }

  public static IQuadTransformer applyTransform(ModelState modelState, Transformation transformation) {
    if (transformation.isIdentity()) {
      return QuadTransformers.empty();
    }
    return UnbakedGeometryHelper.applyRootTransform(modelState, transformation);
  }

  /** Bakes one vanilla block element into Porting Lib's Fabric model builder. */
  public static void bakePart(IModelBuilder<?> builder, IGeometryBakingContext owner, BlockElement part,
                              Function<Material, TextureAtlasSprite> spriteGetter, ModelState transform,
                              IQuadTransformer quadTransformer) {
    for (Direction direction : part.faces.keySet()) {
      BlockElementFace face = part.faces.get(direction);
      String texture = face.texture();
      if (!texture.isEmpty() && texture.charAt(0) == '#') {
        texture = texture.substring(1);
      }
      TextureAtlasSprite sprite = spriteGetter.apply(owner.getMaterial(texture));
      BakedQuad bakedQuad = BlockModel.bakeFace(part, face, sprite, direction, transform);
      quadTransformer.processInPlace(bakedQuad);
      if (face.cullForDirection() == null) {
        builder.addUnculledFace(bakedQuad);
      } else {
        builder.addCulledFace(transform.getRotation().rotateTransform(face.cullForDirection()), bakedQuad);
      }
    }
  }

  public static BakedModel bakeModel(IGeometryBakingContext owner, List<BlockElement> elements,
                                     Function<Material, TextureAtlasSprite> spriteGetter, ModelState transform,
                                     ItemOverrides overrides) {
    TextureAtlasSprite particle = spriteGetter.apply(owner.getMaterial("particle"));
    IModelBuilder<?> builder = IModelBuilder.of(owner.useAmbientOcclusion(), owner.useBlockLight(), owner.isGui3d(),
      owner.getTransforms(), overrides, particle, getRenderTypeGroup(owner));
    IQuadTransformer quadTransformer = applyTransform(transform, owner.getRootTransform());
    for (BlockElement part : elements) {
      bakePart(builder, owner, part, spriteGetter, transform, quadTransformer);
    }
    return builder.build();
  }

  @Override
  public BakedModel bake(IGeometryBakingContext owner, ModelBaker baker,
                         Function<Material, TextureAtlasSprite> spriteGetter, ModelState transform,
                         ItemOverrides overrides) {
    return bakeModel(owner, this.getElements(), spriteGetter, transform, overrides);
  }

  public BakedModel bakeWithElements(IGeometryBakingContext owner, List<BlockElement> elements, ModelState transform) {
    return bakeModel(owner, elements, Material::sprite, transform, ItemOverrides.EMPTY);
  }

  public BakedModel bakeDynamic(IGeometryBakingContext owner, ModelState transform) {
    return bakeWithElements(owner, this.getElements(), transform);
  }

  private static Either<Material, String> parseTextureLocationOrReference(ResourceLocation atlas, String name) {
    if (name.charAt(0) == '#') {
      return Either.right(name.substring(1));
    }
    ResourceLocation location = ResourceLocation.tryParse(name);
    if (location == null) {
      throw new JsonParseException(name + " is not valid resource location");
    }
    return Either.left(new Material(atlas, location));
  }

  public static SimpleBlockModel deserialize(JsonObject json, JsonDeserializationContext context) {
    String parentName = GsonHelper.getAsString(json, "parent", "");
    ResourceLocation parent = parentName.isEmpty() ? null : ResourceLocation.parse(parentName);

    Map<String, Either<Material, String>> textureMap;
    if (json.has("textures")) {
      ResourceLocation atlas = InventoryMenu.BLOCK_ATLAS;
      JsonObject textures = GsonHelper.getAsJsonObject(json, "textures");
      Map<String, Either<Material, String>> builder = new HashMap<>(textures.size());
      for (Entry<String, JsonElement> entry : textures.entrySet()) {
        builder.put(entry.getKey(), parseTextureLocationOrReference(atlas, entry.getValue().getAsString()));
      }
      textureMap = Map.copyOf(builder);
    } else {
      textureMap = Map.of();
    }

    List<BlockElement> parts;
    if (json.has("elements")) {
      parts = getModelElements(context, json.get("elements"), "elements");
    } else {
      parts = List.of();
    }
    return new SimpleBlockModel(parent, textureMap, parts);
  }

  public static List<BlockElement> getModelElements(JsonDeserializationContext context, JsonElement element, String name) {
    if (element.isJsonObject()) {
      return List.of((BlockElement) context.deserialize(element.getAsJsonObject(), BlockElement.class));
    }
    if (element.isJsonArray()) {
      JsonArray array = element.getAsJsonArray();
      List<BlockElement> builder = new ArrayList<>(array.size());
      for (JsonElement json : array) {
        builder.add(context.deserialize(json, BlockElement.class));
      }
      return List.copyOf(builder);
    }
    throw new JsonSyntaxException("Missing " + name + ", expected to find a JsonArray or JsonObject");
  }
}

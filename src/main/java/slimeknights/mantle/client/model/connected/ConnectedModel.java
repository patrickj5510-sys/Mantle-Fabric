package slimeknights.mantle.client.model.connected;

import com.google.common.collect.Lists;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSyntaxException;
import com.mojang.datafixers.util.Either;
import com.mojang.math.Transformation;
import io.github.fabricators_of_create.porting_lib.models.geometry.IGeometryBakingContext;
import io.github.fabricators_of_create.porting_lib.models.geometry.IGeometryLoader;
import io.github.fabricators_of_create.porting_lib.models.geometry.IUnbakedGeometry;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import net.fabricmc.fabric.api.renderer.v1.render.RenderContext;
import net.minecraft.client.renderer.block.model.BlockElement;
import net.minecraft.client.renderer.block.model.BlockElementFace;
import net.minecraft.client.renderer.block.model.BlockFaceUV;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.client.resources.model.UnbakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.Direction.Plane;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import slimeknights.mantle.client.model.util.ColoredBlockModel;
import slimeknights.mantle.client.model.util.DynamicBakedWrapper;
import slimeknights.mantle.client.model.util.ExtraTextureContext;
import slimeknights.mantle.client.model.util.ModelTextureIteratable;
import slimeknights.mantle.client.model.util.SimpleBlockModel;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

/** Model that handles generating variants for connected textures. */
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public class ConnectedModel implements IUnbakedGeometry<ConnectedModel> {
  public static final IGeometryLoader<ConnectedModel> LOADER = ConnectedModel::deserialize;

  private final SimpleBlockModel model;
  /** Map of texture name to suffix table indexed as 0bENWS. */
  private final Map<String, String[]> connectedTextures;
  private final BiPredicate<BlockState, BlockState> connectionPredicate;
  private final Set<Direction> sides;

  /** Extra connected texture materials, populated while resolving parents. */
  private Map<String, Material> extraTextures = Map.of();

  @Override
  public void resolveParents(Function<ResourceLocation, UnbakedModel> modelGetter, IGeometryBakingContext owner) {
    model.resolveParents(modelGetter, owner);

    Map<String, Material> resolved = new HashMap<>();
    for (Entry<String, String[]> entry : connectedTextures.entrySet()) {
      String name = entry.getKey();
      if (!owner.hasMaterial(name)) {
        continue;
      }
      Material base = owner.getMaterial(name);
      ResourceLocation atlas = base.atlasLocation();
      ResourceLocation texture = base.texture();
      String namespace = texture.getNamespace();
      String path = texture.getPath();

      for (String suffix : entry.getValue()) {
        if (suffix.isEmpty()) {
          continue;
        }
        String suffixedName = name + "_" + suffix;
        if (!resolved.containsKey(suffixedName)) {
          Material material = owner.hasMaterial(suffixedName)
            ? owner.getMaterial(suffixedName)
            : new Material(atlas, ResourceLocation.fromNamespaceAndPath(namespace, path + "/" + suffix));
          resolved.put(suffixedName, material);
        }
      }
    }
    this.extraTextures = Map.copyOf(resolved);
  }

  @Override
  public BakedModel bake(IGeometryBakingContext owner, ModelBaker baker,
                         Function<Material, TextureAtlasSprite> spriteGetter,
                         ModelState transform, ItemOverrides overrides) {
    BakedModel baked = model.bake(owner, baker, spriteGetter, transform, overrides);
    return new Baked(this, new ExtraTextureContext(owner, extraTextures), transform, baked);
  }

  /** Fabric baked model. Connections are derived directly from the block view at render time. */
  protected static class Baked extends DynamicBakedWrapper<BakedModel> {
    private final ConnectedModel parent;
    private final IGeometryBakingContext owner;
    private final ModelState transforms;
    private final BakedModel[] cache = new BakedModel[64];
    private final Map<String, String> nameMappingCache = new ConcurrentHashMap<>();
    private final ModelTextureIteratable modelTextures;

    public Baked(ConnectedModel parent, IGeometryBakingContext owner, ModelState transforms, BakedModel baked) {
      super(baked);
      this.parent = parent;
      this.owner = owner;
      this.transforms = transforms;
      this.modelTextures = ModelTextureIteratable.of(owner, parent.model);
      this.cache[0] = baked;
    }

    private static Direction rotateDirection(Direction direction, Direction rotation) {
      if (rotation == Direction.UP) {
        return direction;
      }
      if (rotation == Direction.DOWN) {
        return direction.getAxis() == Axis.Z ? direction.getOpposite() : direction;
      }
      return switch (direction) {
        case NORTH -> Direction.UP;
        case SOUTH -> Direction.DOWN;
        case EAST -> rotation.getCounterClockWise();
        case WEST -> rotation.getClockWise();
        default -> throw new IllegalArgumentException("Direction must be horizontal axis");
      };
    }

    private static Function<Direction, Direction> getTransform(Direction face, BlockFaceUV uv) {
      Function<Direction, Direction> transform = direction -> rotateDirection(direction, face);

      boolean flipV = uv.uvs[1] > uv.uvs[3];
      if (uv.uvs[0] > uv.uvs[2]) {
        if (flipV) {
          transform = transform.compose(Direction::getOpposite);
        } else {
          transform = transform.compose(direction -> direction.getAxis() == Axis.X ? direction.getOpposite() : direction);
        }
      } else if (flipV) {
        transform = transform.compose(direction -> direction.getAxis() == Axis.Z ? direction.getOpposite() : direction);
      }

      return switch (uv.rotation) {
        case 90 -> transform.compose(Direction::getClockWise);
        case 180 -> transform.compose(Direction::getOpposite);
        case 270 -> transform.compose(Direction::getCounterClockWise);
        default -> transform;
      };
    }

    private String getConnectedNameUncached(String key) {
      String check = key;
      String found = "";
      for (Map<String, Either<Material, String>> textures : modelTextures) {
        Either<Material, String> either = textures.get(check);
        if (either != null) {
          Optional<String> newName = either.right();
          if (newName.isEmpty()) {
            break;
          }
          check = newName.get();
          if (parent.connectedTextures.containsKey(check)) {
            found = check;
            break;
          }
        }
      }
      return found;
    }

    private String getConnectedName(String key) {
      if (!key.isEmpty() && key.charAt(0) == '#') {
        key = key.substring(1);
      }
      if (parent.connectedTextures.containsKey(key)) {
        return key;
      }
      return nameMappingCache.computeIfAbsent(key, this::getConnectedNameUncached);
    }

    private String getTextureSuffix(String texture, byte connections, Function<Direction, Direction> transform) {
      int key = 0;
      for (Direction dir : Plane.HORIZONTAL) {
        int flag = 1 << transform.apply(dir).get3DDataValue();
        if ((connections & flag) == flag) {
          key |= 1 << dir.get2DDataValue();
        }
      }
      String[] suffixes = parent.connectedTextures.get(texture);
      if (suffixes == null) {
        return "";
      }
      String suffix = suffixes[key];
      return suffix.isEmpty() ? "" : "_" + suffix;
    }

    private BakedModel applyConnections(byte connections) {
      List<BlockElement> elements = Lists.newArrayList();
      for (BlockElement part : parent.model.getElements()) {
        Map<Direction, BlockElementFace> partFaces = new EnumMap<>(Direction.class);
        for (Map.Entry<Direction, BlockElementFace> entry : part.faces.entrySet()) {
          Direction dir = entry.getKey();
          BlockElementFace original = entry.getValue();
          BlockElementFace face = original;

          String connectedTexture = getConnectedName(original.texture());
          if (!connectedTexture.isEmpty()) {
            String suffix = getTextureSuffix(connectedTexture, connections, getTransform(dir, original.uv()));
            if (!suffix.isEmpty()) {
              face = new BlockElementFace(original.cullForDirection(), original.tintIndex(),
                "#" + connectedTexture + suffix, original.uv());
            }
          }
          partFaces.put(dir, face);
        }
        elements.add(new BlockElement(part.from, part.to, partFaces, part.rotation, part.shade));
      }
      return parent.model.bakeWithElements(owner, elements, transforms);
    }

    private static byte getConnections(Predicate<Direction> predicate) {
      byte connections = 0;
      for (Direction dir : Direction.values()) {
        if (predicate.test(dir)) {
          connections |= 1 << dir.get3DDataValue();
        }
      }
      return connections;
    }

    private synchronized BakedModel getCachedModel(byte connections) {
      int index = connections & 0x3F;
      if (cache[index] == null) {
        cache[index] = applyConnections(connections);
      }
      return cache[index];
    }

    @Override
    public void emitBlockQuads(BlockAndTintGetter world, BlockState state, BlockPos pos,
                               Supplier<RandomSource> randomSupplier, RenderContext context) {
      Transformation rotation = transforms.getRotation();
      byte connections = getConnections(dir -> parent.sides.contains(dir)
        && parent.connectionPredicate.test(state, world.getBlockState(pos.relative(rotation.rotateTransform(dir)))));
      getCachedModel(connections).emitBlockQuads(world, state, pos, randomSupplier, context);
    }
  }

  /** Deserializes a connected model from JSON. */
  public static ConnectedModel deserialize(JsonObject json, JsonDeserializationContext context) {
    ColoredBlockModel model = ColoredBlockModel.deserialize(json, context);
    JsonObject data = GsonHelper.getAsJsonObject(json, "connection");

    JsonObject connected = GsonHelper.getAsJsonObject(data, "textures");
    if (connected.isEmpty()) {
      throw new JsonSyntaxException("Must have at least one texture in connected");
    }

    Map<String, String[]> connectedTextures = new HashMap<>(connected.size());
    for (Entry<String, JsonElement> entry : connected.entrySet()) {
      String name = entry.getKey();
      connectedTextures.put(name, ConnectedModelRegistry.deserializeType(entry.getValue(), "textures[" + name + "]"));
    }

    Set<Direction> sides;
    if (data.has("sides")) {
      JsonArray array = GsonHelper.getAsJsonArray(data, "sides");
      sides = EnumSet.noneOf(Direction.class);
      for (int i = 0; i < array.size(); i++) {
        String side = GsonHelper.convertToString(array.get(i), "sides[" + i + "]");
        Direction dir = Direction.byName(side);
        if (dir == null) {
          throw new JsonParseException("Invalid side " + side);
        }
        sides.add(dir);
      }
    } else {
      sides = EnumSet.allOf(Direction.class);
    }

    BiPredicate<BlockState, BlockState> predicate = ConnectedModelRegistry.deserializePredicate(data, "predicate");
    return new ConnectedModel(model, Map.copyOf(connectedTextures), predicate, sides);
  }
}

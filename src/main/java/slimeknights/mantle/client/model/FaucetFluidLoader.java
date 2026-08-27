package slimeknights.mantle.client.model;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.fabricators_of_create.porting_lib.models.mixin.client.ModelBakeryAccessor;
import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import org.joml.Vector3f;
import slimeknights.mantle.Mantle;
import slimeknights.mantle.client.model.fluid.FluidCuboid;
import slimeknights.mantle.client.render.FluidRenderer;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

/**
 * Shared logic for fluids rendering in blocks below between Ceramics and Tinkers Construct
 */
public class FaucetFluidLoader extends SimpleJsonResourceReloadListener implements IdentifiableResourceReloadListener {
  private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
  private static final FaucetFluidLoader INSTANCE = new FaucetFluidLoader();
  private static final ResourceLocation DEFAULT_NAME = Mantle.getResource("_default");
  private final Map<BlockState,FaucetFluid> fluidMap = new HashMap<>();
  private static boolean initialized = false;

  public static void initialize() {
    if (initialized) {
      return;
    }
    initialized = true;
    ResourceManagerHelper.get(PackType.CLIENT_RESOURCES).registerReloadListener(INSTANCE);
  }

  private FaucetFluid defaultFluid;
  private FaucetFluidLoader() {
    super(GSON, "models/faucet_fluid");
    defaultFluid = FaucetFluid.EMPTY;
  }

  @Override
  protected void apply(Map<ResourceLocation,JsonElement> map, ResourceManager resourceManager, ProfilerFiller profiler) {
    fluidMap.clear();
    JsonElement def = map.get(DEFAULT_NAME);
    if (def == null || !def.isJsonObject()) {
      Mantle.logger.warn("Found no default fluid model, this is likely a problem with the resource pack");
      defaultFluid = FaucetFluid.EMPTY;
    } else {
      try {
        defaultFluid = FaucetFluid.parseDefault(def.getAsJsonObject());
      } catch (Exception exception) {
        Mantle.logger.error("Failed to load default faucet fluid model {}", DEFAULT_NAME, exception);
      }
    }

    for (Entry<ResourceLocation,JsonElement> entry : map.entrySet()) {
      ResourceLocation location = entry.getKey();
      if (location.equals(DEFAULT_NAME) || !entry.getValue().isJsonObject()) {
        continue;
      }

      try {
        JsonObject json = GsonHelper.convertToJsonObject(entry.getValue(), "");
        JsonObject variants = GsonHelper.getAsJsonObject(json, "variants");
        Block block = BuiltInRegistries.BLOCK.get(location);
        if (block != Blocks.AIR) {
          StateDefinition<Block,BlockState> container = block.getStateDefinition();
          List<BlockState> validStates = container.getPossibleStates();
          for (Entry<String, JsonElement> variant : variants.entrySet()) {
            FaucetFluid fluid = FaucetFluid.fromJson(GsonHelper.convertToJsonObject(variant.getValue(), variant.getKey()), defaultFluid);
            validStates.stream().filter(ModelBakeryAccessor.port_lib$predicate(container, variant.getKey())).forEach(state -> fluidMap.put(state, fluid));
          }
        } else {
          Mantle.logger.debug("Skipping loading faucet fluid model '{}' as no corresponding block exists", location);
        }
      } catch (Exception e) {
        Mantle.logger.warn("Exception loading faucet fluid model '{}': {}", location, e.getMessage());
      }
    }
  }

  public static FaucetFluid get(BlockState state) {
    return INSTANCE.fluidMap.getOrDefault(state, INSTANCE.defaultFluid);
  }

  public static void renderFaucetFluids(LevelAccessor world, BlockPos pos, Direction direction, PoseStack matrices,
                                        VertexConsumer buffer, TextureAtlasSprite still, TextureAtlasSprite flowing,
                                        int color, int light) {
    int i = 0;
    FaucetFluid faucetFluid;
    do {
      i++;
      faucetFluid = FaucetFluidLoader.get(world.getBlockState(pos.below(i)));
      matrices.pushPose();
      matrices.translate(0, -i, 0);
      for (FluidCuboid cube : faucetFluid.getFluids(direction)) {
        FluidRenderer.renderCuboid(matrices, buffer, cube, still, flowing, cube.getFromScaled(), cube.getToScaled(), color, light, false);
      }
      matrices.popPose();
    } while (faucetFluid.isContinued());
  }

  @Override
  public ResourceLocation getFabricId() {
    return Mantle.getResource("faucet_fluid_loader");
  }

  public static class FaucetFluid {
    private static final FaucetFluid EMPTY = new FaucetFluid(Collections.emptyList(), Collections.emptyList(), false);
    private final List<FluidCuboid> side;
    private final List<FluidCuboid> center;
    private final boolean cont;

    public FaucetFluid(List<FluidCuboid> side, List<FluidCuboid> center, boolean cont) {
      this.side = side;
      this.center = center;
      this.cont = cont;
    }

    public List<FluidCuboid> getFluids(Direction dir) {
      return dir.getAxis() == Axis.Y ? center : side;
    }

    public boolean isContinued() {
      return cont;
    }

    protected static FaucetFluid parseDefault(JsonObject json) {
      List<FluidCuboid> side = FluidCuboid.listFromJson(json, "side");
      List<FluidCuboid> center = FluidCuboid.listFromJson(json, "center");
      return new FaucetFluid(side, center, false);
    }

    protected static FaucetFluid fromJson(JsonObject json, FaucetFluid def) {
      List<FluidCuboid> side = parseFluids(json, "side", def.side);
      List<FluidCuboid> center = parseFluids(json, "center", def.center);
      boolean cont = GsonHelper.getAsBoolean(json, "continue", false);
      return new FaucetFluid(side, center, cont);
    }

    private static List<FluidCuboid> parseFluids(JsonObject json, String tag, List<FluidCuboid> def) {
      JsonElement element;
      if (json.has(tag)) {
        element = json.get(tag);
      } else if (json.has("bottom") && json.get("bottom").isJsonPrimitive()) {
        element = json.get("bottom");
      } else {
        return def;
      }
      if (element.isJsonPrimitive()) {
        int value = element.getAsInt();
        return def.stream().map(cuboid -> {
          Vector3f from = new Vector3f(cuboid.getFrom());
          from.y = value;
          return new FluidCuboid(from, cuboid.getTo(), cuboid.getFaces());
        }).collect(Collectors.toList());
      }
      return FluidCuboid.listFromJson(json, tag);
    }
  }
}

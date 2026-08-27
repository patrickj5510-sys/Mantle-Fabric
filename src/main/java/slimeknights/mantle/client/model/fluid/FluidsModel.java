package slimeknights.mantle.client.model.fluid;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import io.github.fabricators_of_create.porting_lib.models.geometry.IGeometryBakingContext;
import io.github.fabricators_of_create.porting_lib.models.geometry.IGeometryLoader;
import io.github.fabricators_of_create.porting_lib.models.geometry.IUnbakedGeometry;
import lombok.AllArgsConstructor;
import lombok.Getter;
import net.fabricmc.fabric.api.renderer.v1.model.ForwardingBakedModel;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.client.resources.model.UnbakedModel;
import net.minecraft.resources.ResourceLocation;
import slimeknights.mantle.client.model.util.ColoredBlockModel;
import slimeknights.mantle.client.model.util.SimpleBlockModel;

import java.util.List;
import java.util.function.Function;

/** Contains fluid cuboids used by block entity renderers such as the faucet. */
@AllArgsConstructor
public class FluidsModel implements IUnbakedGeometry<FluidsModel> {
  public static final IGeometryLoader<FluidsModel> LOADER = FluidsModel::deserialize;

  private final SimpleBlockModel model;
  private final List<FluidCuboid> fluids;

  @Override
  public void resolveParents(Function<ResourceLocation, UnbakedModel> modelGetter, IGeometryBakingContext owner) {
    model.resolveParents(modelGetter, owner);
  }

  @Override
  public BakedModel bake(IGeometryBakingContext owner, ModelBaker baker,
                         Function<Material, TextureAtlasSprite> spriteGetter, ModelState transform,
                         ItemOverrides overrides) {
    return new Baked(model.bake(owner, baker, spriteGetter, transform, overrides), fluids);
  }

  /** Baked model, mostly a data wrapper around a normal model. */
  @SuppressWarnings("WeakerAccess")
  public static class Baked extends ForwardingBakedModel {
    @Getter
    private final List<FluidCuboid> fluids;

    public Baked(BakedModel originalModel, List<FluidCuboid> fluids) {
      this.wrapped = originalModel;
      this.fluids = fluids;
    }
  }

  /** Deserializes the model from JSON. */
  public static FluidsModel deserialize(JsonObject json, JsonDeserializationContext context) {
    ColoredBlockModel model = ColoredBlockModel.deserialize(json, context);
    return new FluidsModel(model, FluidCuboid.listFromJson(json, "fluids"));
  }
}

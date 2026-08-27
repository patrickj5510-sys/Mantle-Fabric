package slimeknights.mantle.registration.adapter;

import io.github.fabricators_of_create.porting_lib.fluids.BaseFlowingFluid;
import io.github.fabricators_of_create.porting_lib.fluids.BaseFlowingFluid.Properties;
import net.minecraft.core.Registry;
import net.minecraft.world.level.material.Fluid;
import slimeknights.mantle.registration.DelayedSupplier;
import slimeknights.mantle.registration.FluidBuilder;

import java.util.function.Function;

/** Registry adapter for registering fluids. */
@SuppressWarnings("unused")
public class FluidRegistryAdapter extends RegistryAdapter<Fluid> {
  public FluidRegistryAdapter(Registry<Fluid> registry, String modId) {
    super(registry, modId);
  }

  /** Registers a new fluid with both source and flowing variants. */
  public <F extends BaseFlowingFluid> F register(FluidBuilder<?> builder,
                                                 Function<Properties,F> still,
                                                 Function<Properties,F> flowing,
                                                 String name) {
    DelayedSupplier<Fluid> stillDelayed = new DelayedSupplier<>();
    DelayedSupplier<Fluid> flowingDelayed = new DelayedSupplier<>();

    Properties props = builder.build(builder.getType(), stillDelayed, flowingDelayed);

    F stillFluid = register(still.apply(props), name);
    stillDelayed.setSupplier(() -> stillFluid);
    F flowingFluid = register(flowing.apply(props), "flowing_" + name);
    flowingDelayed.setSupplier(() -> flowingFluid);

    return stillFluid;
  }

  /** Registers a fluid using Porting Lib's default 1.21 source/flowing implementations. */
  public BaseFlowingFluid register(FluidBuilder<?> builder, String name) {
    return register(builder, BaseFlowingFluid.Source::new, BaseFlowingFluid.Flowing::new, name);
  }
}

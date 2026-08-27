package slimeknights.mantle.fluid.texture;

import io.github.fabricators_of_create.porting_lib.fluids.FluidType;

/**
 * Legacy compatibility wrapper retained for downstream source compatibility.
 * Fluid visuals are registered through Fabric's fluid render handler API on 1.21.
 */
@Deprecated(forRemoval = true)
public record ClientTextureFluidType(FluidType type) {}

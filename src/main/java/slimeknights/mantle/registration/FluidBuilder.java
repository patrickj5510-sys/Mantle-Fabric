package slimeknights.mantle.registration;

import io.github.fabricators_of_create.porting_lib.fluids.BaseFlowingFluid;
import io.github.fabricators_of_create.porting_lib.fluids.FluidType;
import lombok.Getter;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.material.Fluid;

import javax.annotation.Nullable;
import java.util.function.Supplier;

/**
 * Fluid properties builder used by Mantle's registration helpers.
 */
@Getter
public class FluidBuilder<T extends FluidBuilder<T>> {
  protected Supplier<? extends FluidType> type;
  @Nullable
  protected Supplier<? extends Item> bucket;
  @Nullable
  protected Supplier<? extends LiquidBlock> block;
  private int slopeFindDistance = 4;
  private int levelDecreasePerBlock = 1;
  private float explosionResistance = 1;
  private int tickRate = 5;

  protected FluidBuilder() {}

  /** Creates a new builder instance. */
  public static FluidBuilder<?> create(Supplier<? extends FluidType> type) {
    FluidBuilder<?> builder = new FluidBuilder<>();
    builder.type = type;
    return builder;
  }

  @SuppressWarnings("unchecked")
  private T self() {
    return (T)this;
  }

  public T bucket(Supplier<? extends Item> value) {
    this.bucket = value;
    return self();
  }

  public T block(Supplier<? extends LiquidBlock> value) {
    this.block = value;
    return self();
  }

  public T slopeFindDistance(int value) {
    this.slopeFindDistance = value;
    return self();
  }

  public T levelDecreasePerBlock(int value) {
    this.levelDecreasePerBlock = value;
    return self();
  }

  public T explosionResistance(int value) {
    this.explosionResistance = value;
    return self();
  }

  public T tickRate(int value) {
    this.tickRate = value;
    return self();
  }

  /** Builds Porting Lib 1.21 flowing-fluid properties. */
  public BaseFlowingFluid.Properties build(Supplier<? extends FluidType> type,
                                           Supplier<? extends Fluid> still,
                                           Supplier<? extends Fluid> flowing) {
    BaseFlowingFluid.Properties properties = new BaseFlowingFluid.Properties(type, still, flowing)
      .slopeFindDistance(this.slopeFindDistance)
      .levelDecreasePerBlock(this.levelDecreasePerBlock)
      .explosionResistance(this.explosionResistance)
      .tickRate(this.tickRate);
    if (this.block != null) {
      properties.block(this.block);
    }
    if (this.bucket != null) {
      properties.bucket(this.bucket);
    }
    return properties;
  }
}

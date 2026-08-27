package slimeknights.mantle.registration.deferred;

import com.google.common.base.Suppliers;
import io.github.fabricators_of_create.porting_lib.fluids.BaseFlowingFluid;
import io.github.fabricators_of_create.porting_lib.fluids.BaseFlowingFluid.Properties;
import io.github.fabricators_of_create.porting_lib.fluids.FluidType;
import io.github.fabricators_of_create.porting_lib.registry.DeferredHolder;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import slimeknights.mantle.fluid.TextureFluidType;
import slimeknights.mantle.fluid.UnplaceableFluid;
import slimeknights.mantle.registration.DelayedSupplier;
import slimeknights.mantle.registration.FluidBuilder;
import slimeknights.mantle.registration.ItemProperties;
import slimeknights.mantle.registration.object.FlowingFluidObject;
import slimeknights.mantle.registration.object.FluidObject;

import javax.annotation.Nullable;
import java.util.function.Function;
import java.util.function.Supplier;

/** Deferred registration helper for Mantle fluids on Fabric. */
@SuppressWarnings({"unused", "WeakerAccess"})
public class FluidDeferredRegister extends DeferredRegisterWrapper<Fluid> {
  private final SynchronizedDeferredRegister<Block> blockRegister;
  private final SynchronizedDeferredRegister<Item> itemRegister;

  public FluidDeferredRegister(String modID) {
    super(Registries.FLUID, modID);
    this.blockRegister = SynchronizedDeferredRegister.create(Registries.BLOCK, modID);
    this.itemRegister = SynchronizedDeferredRegister.create(Registries.ITEM, modID);
  }

  @Override
  public void register() {
    super.register();
    blockRegister.register();
    itemRegister.register();
  }

  /**
   * Creates a memoized fluid-type supplier. Porting Lib 1.21 stores the type on the flowing fluid
   * rather than in a separate registry.
   */
  public <I extends FluidType> Supplier<I> registerType(String name, Supplier<? extends I> supplier) {
    return Suppliers.memoize(supplier::get);
  }

  /** Registers a fluid to the vanilla fluid registry. */
  public <I extends Fluid> DeferredHolder<Fluid,I> registerFluid(String name, Supplier<? extends I> supplier) {
    return register.register(name, supplier);
  }

  /** Starts a builder for a fluid. */
  public Builder register(String name) {
    return new Builder(name);
  }

  @Accessors(fluent = true)
  @Setter
  public class Builder extends FluidBuilder<Builder> {
    private final String name;
    private final DelayedSupplier<Fluid> stillDelayed = new DelayedSupplier<>();
    /** Name to use for the common tag, defaults to the fluid name. */
    private String tagName;

    private Builder(String name) {
      this.name = name;
      this.tagName = name;
    }

    /* Fluid type */

    public Builder type(Supplier<? extends FluidType> supplier) {
      if (this.type != null) {
        throw new IllegalStateException("Type already created for " + name);
      }
      this.type = Suppliers.memoize(supplier::get);
      return this;
    }

    public Builder type(FluidType.Properties properties) {
      return type(() -> new TextureFluidType(properties));
    }

    public Builder type() {
      return type(FluidType.Properties.create());
    }

    /* Bucket */

    public Builder bucket(Function<Supplier<? extends Fluid>,Item> constructor) {
      if (this.bucket != null) {
        throw new IllegalStateException("Bucket already created for " + name);
      }
      return bucket(itemRegister.register(name + "_bucket", () -> constructor.apply(stillDelayed)));
    }

    public Builder bucket() {
      return bucket(itemRegister.register(name + "_bucket", () -> new BucketItem(stillDelayed.get(), ItemProperties.BUCKET_PROPS)));
    }

    /* Block */

    @SuppressWarnings({"unchecked", "rawtypes"})
    public Builder block(Function<Supplier<? extends FlowingFluid>,LiquidBlock> constructor) {
      if (this.block != null) {
        throw new IllegalStateException("Block already created for " + name);
      }
      return block(blockRegister.register(name + "_fluid", () -> constructor.apply((Supplier<FlowingFluid>)(Supplier)stillDelayed)));
    }

    /** Creates the default liquid block with 1.21 block properties. */
    public Builder block(MapColor color, int lightLevel) {
      return block(supplier -> new LiquidBlock(supplier.get(), createProperties(color, lightLevel)));
    }

    public Builder block(MapColor color) {
      return block(color, 0);
    }

    /* Final fluid */

    public FluidObject<UnplaceableFluid> unplacable() {
      return unplacable(UnplaceableFluid::new);
    }

    public <F extends Fluid> FluidObject<F> unplacable(Function<FluidBuilder<?>,F> constructor) {
      if (block != null) {
        throw new IllegalStateException("Cannot build an unplacable fluid with a block form");
      }
      if (type == null) {
        this.type();
      }
      DeferredHolder<Fluid,F> fluid = registerFluid(name, () -> constructor.apply(this));
      stillDelayed.setSupplier(fluid);
      return new FluidObject<>(resource(name), tagName, type, fluid);
    }

    public FlowingFluidObject<BaseFlowingFluid> flowing() {
      return flowing(BaseFlowingFluid.Source::new, BaseFlowingFluid.Flowing::new);
    }

    public <F extends FlowingFluid> FlowingFluidObject<F> flowing(Function<Properties,? extends F> createStill,
                                                                  Function<Properties,? extends F> createFlowing) {
      if (type == null) {
        this.type();
      }

      DelayedSupplier<FlowingFluid> flowingDelayed = new DelayedSupplier<>();
      Properties properties = build(type, stillDelayed, flowingDelayed);

      DeferredHolder<Fluid,F> still = registerFluid(name, () -> createStill.apply(properties));
      stillDelayed.setSupplier(still);
      DeferredHolder<Fluid,F> flowing = registerFluid("flowing_" + name, () -> createFlowing.apply(properties));
      flowingDelayed.setSupplier(flowing);

      return new FlowingFluidObject<>(resource(name), tagName, type, still, flowing, this.block);
    }
  }

  /** Creates the standard 1.21 liquid-block properties. */
  public static BlockBehaviour.Properties createProperties(MapColor color, int lightLevel) {
    return BlockBehaviour.Properties.of()
      .mapColor(color)
      .replaceable()
      .noCollission()
      .randomTicks()
      .strength(100.0F)
      .lightLevel(state -> lightLevel)
      .pushReaction(PushReaction.DESTROY)
      .noLootTable()
      .liquid()
      .sound(SoundType.EMPTY);
  }
}

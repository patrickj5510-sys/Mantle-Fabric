package slimeknights.mantle.data.loadable.common;

import io.github.fabricators_of_create.porting_lib.fluids.FluidStack;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import slimeknights.mantle.data.loadable.ErrorFactory;
import slimeknights.mantle.data.loadable.Loadable;
import slimeknights.mantle.data.loadable.Loadables;
import slimeknights.mantle.data.loadable.field.LoadableField;
import slimeknights.mantle.data.loadable.primitive.IntLoadable;
import slimeknights.mantle.data.loadable.record.RecordLoadable;

import javax.annotation.Nullable;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;

/** Loadable for a Porting Lib fluid stack on Fabric 1.21. */
@SuppressWarnings("unused")
public class FluidStackLoadable {
  private FluidStackLoadable() {}

  private static final Function<FluidStack,Fluid> FLUID_GETTER = FluidStack::getFluid;
  /** Legacy NBT payloads are preserved in 1.21 as the stack's custom-data component. */
  private static final Predicate<FluidStack> COMPACT_NBT = stack -> stack.get(DataComponents.CUSTOM_DATA) == null;
  private static final BiFunction<FluidStack,ErrorFactory,FluidStack> NOT_EMPTY = (stack, error) -> {
    if (stack.isEmpty()) {
      throw error.create("FluidStack cannot be empty");
    }
    return stack;
  };

  private static final LoadableField<Fluid,FluidStack> FLUID = Loadables.FLUID.defaultField("fluid", Fluids.EMPTY, false, FLUID_GETTER);
  private static final LoadableField<Integer,FluidStack> AMOUNT = IntLoadable.FROM_ZERO.requiredField("amount", stack -> Math.toIntExact(stack.getAmount()));
  private static final LoadableField<CompoundTag,FluidStack> NBT = NBTLoadable.ALLOW_STRING.nullableField("nbt", stack -> {
    CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
    return customData == null ? null : customData.copyTag();
  });

  public static final Loadable<FluidStack> OPTIONAL_BUCKET = fixedSize(FluidConstants.BUCKET);
  public static final RecordLoadable<FluidStack> OPTIONAL_STACK = RecordLoadable.create(FLUID, AMOUNT, (fluid, count) -> makeStack(fluid, count, null));
  public static final RecordLoadable<FluidStack> OPTIONAL_BUCKET_NBT = fixedSizeNBT(FluidConstants.BUCKET);
  public static final RecordLoadable<FluidStack> OPTIONAL_STACK_NBT = RecordLoadable.create(FLUID, AMOUNT, NBT, FluidStackLoadable::makeStack);

  public static final Loadable<FluidStack> REQUIRED_BUCKET = notEmpty(OPTIONAL_BUCKET);
  public static final RecordLoadable<FluidStack> REQUIRED_STACK = notEmpty(OPTIONAL_STACK);
  public static final RecordLoadable<FluidStack> REQUIRED_BUCKET_NBT = notEmpty(OPTIONAL_BUCKET_NBT);
  public static final RecordLoadable<FluidStack> REQUIRED_STACK_NBT = notEmpty(OPTIONAL_STACK_NBT);

  private static FluidStack makeStack(Fluid fluid, long amount, @Nullable CompoundTag nbt) {
    if (fluid == Fluids.EMPTY || amount <= 0) {
      return FluidStack.EMPTY;
    }
    FluidStack stack = new FluidStack(fluid, amount);
    if (nbt != null && !nbt.isEmpty()) {
      stack.set(DataComponents.CUSTOM_DATA, CustomData.of(nbt.copy()));
    }
    return stack;
  }

  public static Loadable<FluidStack> fixedSize(long amount) {
    if (amount <= 0 || amount > Integer.MAX_VALUE) {
      throw new IllegalArgumentException("Amount must be between 1 and Integer.MAX_VALUE, received " + amount);
    }
    return Loadables.FLUID.flatXmap(fluid -> makeStack(fluid, amount, null), FLUID_GETTER);
  }

  public static RecordLoadable<FluidStack> fixedSizeNBT(long amount) {
    if (amount <= 0 || amount > Integer.MAX_VALUE) {
      throw new IllegalArgumentException("Amount must be between 1 and Integer.MAX_VALUE, received " + amount);
    }
    return RecordLoadable.create(FLUID, NBT, (fluid, tag) -> makeStack(fluid, amount, tag))
      .compact(OPTIONAL_BUCKET, COMPACT_NBT);
  }

  public static Loadable<FluidStack> notEmpty(Loadable<FluidStack> loadable) {
    return loadable.validate(NOT_EMPTY);
  }

  public static RecordLoadable<FluidStack> notEmpty(RecordLoadable<FluidStack> loadable) {
    return loadable.validate(NOT_EMPTY);
  }
}

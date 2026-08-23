package slimeknights.mantle.recipe.ingredient;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.github.fabricators_of_create.porting_lib.fluids.FluidStack;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import slimeknights.mantle.Mantle;
import slimeknights.mantle.data.loadable.IAmLoadable;
import slimeknights.mantle.data.loadable.Loadable;
import slimeknights.mantle.data.loadable.Loadables;
import slimeknights.mantle.data.loadable.common.FluidStackLoadable;
import slimeknights.mantle.data.loadable.mapping.EitherLoadable;
import slimeknights.mantle.data.loadable.primitive.IntLoadable;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.mantle.util.RegistryHelper;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/** Simple displayable ingredient type for fluids. */
@SuppressWarnings("unused")
public abstract class FluidIngredient implements IAmLoadable {
  public static final FluidMatch EMPTY = new FluidMatch(Fluids.EMPTY, 0);

  private static EitherLoadable.TypedBuilder<FluidIngredient> loadableBuilder() {
    return EitherLoadable.<FluidIngredient>typed().key("fluid", FLUID_MATCH).key("tag", TAG_MATCH).key("name", NAME_MATCH);
  }

  private static final Loadable<FluidIngredient> NETWORK = FluidStackLoadable.REQUIRED_STACK.list(0)
    .flatXmap(fluids -> FluidIngredient.of(fluids.stream().map(FluidIngredient::of).toList()), FluidIngredient::getFluids);
  private static final RecordLoadable<FluidMatch> FLUID_MATCH = RecordLoadable.create(
    Loadables.FLUID.requiredField("fluid", i -> i.fluid),
    IntLoadable.FROM_ONE.requiredField("amount", i -> Math.toIntExact(i.amount)),
    (fluid, amount) -> FluidIngredient.of(fluid, amount.longValue()));
  @Deprecated(forRemoval = true)
  private static final RecordLoadable<FluidMatch> NAME_MATCH = RecordLoadable.create(
    Loadables.FLUID.requiredField("name", i -> i.fluid),
    IntLoadable.FROM_ONE.requiredField("amount", i -> Math.toIntExact(i.amount)),
    (fluid, amount) -> {
      Mantle.logger.warn("Using deprecated key 'name' for fluid ingredient, use 'fluid' instead. This will be removed in the future");
      return FluidIngredient.of(fluid, amount.longValue());
    });
  private static final RecordLoadable<TagMatch> TAG_MATCH = RecordLoadable.create(
    Loadables.FLUID_TAG.requiredField("tag", i -> i.tag),
    IntLoadable.FROM_ONE.requiredField("amount", i -> Math.toIntExact(i.amount)),
    (tag, amount) -> FluidIngredient.of(tag, amount.longValue()));
  private static final Loadable<Compound> COMPOUND = loadableBuilder().build(NETWORK).list(2)
    .flatXmap(Compound::new, c -> c.ingredients);
  public static final Loadable<FluidIngredient> LOADABLE = loadableBuilder().array(COMPOUND).build(NETWORK);

  public static FluidMatch of(Fluid fluid, int amount) {
    return of(fluid, (long) amount);
  }

  public static FluidMatch of(Fluid fluid, long amount) {
    if (fluid == Fluids.EMPTY || amount <= 0) {
      return EMPTY;
    }
    return new FluidMatch(fluid, amount);
  }

  public static FluidIngredient of(FluidStack stack) {
    return of(stack.getFluid(), stack.getAmount());
  }

  public static TagMatch of(TagKey<Fluid> fluid, int amount) {
    return of(fluid, (long) amount);
  }

  public static TagMatch of(TagKey<Fluid> fluid, long amount) {
    return new TagMatch(fluid, amount);
  }

  public static FluidIngredient of(FluidIngredient... ingredients) {
    return of(List.of(ingredients));
  }

  public static FluidIngredient of(List<FluidIngredient> ingredients) {
    if (ingredients.size() == 1) {
      return ingredients.get(0);
    }
    return new Compound(ingredients);
  }

  private List<FluidStack> displayFluids;

  public abstract boolean test(Fluid fluid);

  public abstract long getAmount(Fluid fluid);

  public boolean test(FluidStack stack) {
    Fluid fluid = stack.getFluid();
    return stack.getAmount() >= getAmount(fluid) && test(fluid);
  }

  public List<FluidStack> getFluids() {
    if (displayFluids == null) {
      displayFluids = getAllFluids().stream().filter(stack -> {
        Fluid fluid = stack.getFluid();
        return fluid.isSource(fluid.defaultFluidState());
      }).collect(Collectors.toList());
    }
    return displayFluids;
  }

  protected abstract List<FluidStack> getAllFluids();

  public JsonElement serialize() {
    return LOADABLE.serialize(this);
  }

  public static FluidIngredient deserialize(JsonObject parent, String key) {
    return LOADABLE.getIfPresent(parent, key);
  }

  @Deprecated
  public static FluidIngredient deserialize(JsonElement element, String key) {
    return LOADABLE.convert(element, key);
  }

  public void write(FriendlyByteBuf buffer) {
    NETWORK.encode(buffer, this);
  }

  public static FluidIngredient read(FriendlyByteBuf buffer) {
    return NETWORK.decode(buffer);
  }

  @AllArgsConstructor(access=AccessLevel.PRIVATE)
  private static class FluidMatch extends FluidIngredient {
    private final Fluid fluid;
    private final long amount;

    @Override
    public Loadable<?> loadable() {
      return FLUID_MATCH;
    }

    @Override
    public boolean test(Fluid fluid) {
      return fluid == this.fluid;
    }

    @Override
    public long getAmount(Fluid fluid) {
      return amount;
    }

    @Override
    public List<FluidStack> getAllFluids() {
      return Collections.singletonList(new FluidStack(fluid, amount));
    }
  }

  @AllArgsConstructor
  private static class TagMatch extends FluidIngredient {
    private final TagKey<Fluid> tag;
    private final long amount;

    @Override
    public Loadable<?> loadable() {
      return TAG_MATCH;
    }

    @Override
    public boolean test(Fluid fluid) {
      return fluid.is(tag);
    }

    @Override
    public long getAmount(Fluid fluid) {
      return amount;
    }

    @Override
    public List<FluidStack> getAllFluids() {
      return RegistryHelper.getTagValueStream(BuiltInRegistries.FLUID, tag)
        .map(fluid -> new FluidStack(fluid, amount))
        .toList();
    }
  }

  @RequiredArgsConstructor
  private static class Compound extends FluidIngredient {
    private final List<FluidIngredient> ingredients;

    @Override
    public Loadable<?> loadable() {
      return COMPOUND;
    }

    @Override
    public boolean test(Fluid fluid) {
      for (FluidIngredient ingredient : ingredients) {
        if (ingredient.test(fluid)) {
          return true;
        }
      }
      return false;
    }

    @Override
    public boolean test(FluidStack stack) {
      for (FluidIngredient ingredient : ingredients) {
        if (ingredient.test(stack)) {
          return true;
        }
      }
      return false;
    }

    @Override
    public long getAmount(Fluid fluid) {
      for (FluidIngredient ingredient : ingredients) {
        if (ingredient.test(fluid)) {
          return ingredient.getAmount(fluid);
        }
      }
      return 0;
    }

    @Override
    public List<FluidStack> getAllFluids() {
      return ingredients.stream().flatMap(ingredient -> ingredient.getFluids().stream()).collect(Collectors.toList());
    }
  }
}

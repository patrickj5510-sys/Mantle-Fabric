package slimeknights.mantle.registration.object;

import io.github.fabricators_of_create.porting_lib.fluids.FluidType;
import lombok.Getter;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.material.Fluid;
import slimeknights.mantle.recipe.ingredient.FluidIngredient;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Objects;
import java.util.function.Supplier;

/** Object containing registry entries for a fluid with no flowing form. */
@SuppressWarnings("WeakerAccess")
public class FluidObject<F extends Fluid> implements Supplier<F>, ItemLike, IdAwareObject {
  @Getter @Nonnull
  protected final ResourceLocation id;
  @Getter @Nonnull
  private final TagKey<Fluid> forgeTag;
  private final Supplier<? extends FluidType> type;
  private final Supplier<? extends F> still;

  public FluidObject(ResourceLocation id, String tagName, Supplier<? extends FluidType> type, Supplier<? extends F> still) {
    this.id = id;
    this.forgeTag = TagKey.create(Registries.FLUID, ResourceLocation.fromNamespaceAndPath("c", tagName));
    this.type = type;
    this.still = still;
  }

  public FluidType getType() {
    return type.get();
  }

  @Override
  public F get() {
    return Objects.requireNonNull(still.get(), "Fluid object missing still fluid");
  }

  @Nullable
  public Item getBucket() {
    Item bucket = still.get().getBucket();
    return bucket == Items.AIR ? null : bucket;
  }

  @Override
  public Item asItem() {
    return still.get().getBucket();
  }

  public FluidIngredient ingredient(int amount, boolean commonTag) {
    if (commonTag) {
      return FluidIngredient.of(get(), amount);
    }
    return FluidIngredient.of(getForgeTag(), amount);
  }
}

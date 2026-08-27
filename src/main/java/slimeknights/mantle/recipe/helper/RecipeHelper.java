package slimeknights.mantle.recipe.helper;

import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import io.github.fabricators_of_create.porting_lib.fluids.FluidStack;
import io.netty.handler.codec.DecoderException;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import slimeknights.mantle.recipe.IMultiRecipe;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/** Helpers used in creation and display of recipes. */
@SuppressWarnings({"WeakerAccess", "unused"})
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class RecipeHelper {
  /** Gets a recipe of a specific class type by name from the manager. */
  public static <C extends Recipe<?>> Optional<C> getRecipe(RecipeManager manager, ResourceLocation name, Class<C> clazz) {
    return manager.byKey(name).map(RecipeHolder::value).filter(clazz::isInstance).map(clazz::cast);
  }

  /** Gets all recipes of a type, safely cast to the requested class. */
  public static <I extends RecipeInput, T extends Recipe<I>, C extends T> List<C> getRecipes(RecipeManager manager, RecipeType<T> type, Class<C> clazz) {
    return manager.getAllRecipesFor(type).stream()
      .map(RecipeHolder::value)
      .filter(clazz::isInstance)
      .map(clazz::cast)
      .collect(Collectors.toList());
  }

  /** Gets a stable, filtered list of recipes for a UI. */
  public static <I extends RecipeInput, T extends Recipe<I>, C extends T> List<C> getUIRecipes(RecipeManager manager, RecipeType<T> type, Class<C> clazz, Predicate<? super C> filter) {
    return manager.getAllRecipesFor(type).stream()
      .sorted(Comparator.comparing(RecipeHolder::id))
      .map(RecipeHolder::value)
      .filter(clazz::isInstance)
      .map(clazz::cast)
      .filter(filter)
      .collect(Collectors.toList());
  }

  /** Gets all recipes from holders, expanding Mantle multi-recipes for recipe viewers. */
  public static <C> List<C> getJEIRecipes(Stream<? extends RecipeHolder<?>> recipes, Class<C> clazz) {
    return recipes
      .sorted((r1, r2) -> {
        boolean m1 = r1.value() instanceof IMultiRecipe<?>;
        boolean m2 = r2.value() instanceof IMultiRecipe<?>;
        if (m1 && !m2) return 1;
        if (!m1 && m2) return -1;
        return r1.id().compareTo(r2.id());
      })
      .flatMap(holder -> {
        Recipe<?> recipe = holder.value();
        if (recipe instanceof IMultiRecipe<?> multi) {
          return multi.getRecipes().stream();
        }
        return Stream.of(recipe);
      })
      .filter(clazz::isInstance)
      .map(clazz::cast)
      .collect(Collectors.toList());
  }

  /** Gets all recipes of a type from a manager, expanding Mantle multi-recipes. */
  public static <I extends RecipeInput, T extends Recipe<I>, C> List<C> getJEIRecipes(RecipeManager manager, RecipeType<T> type, Class<C> clazz) {
    return getJEIRecipes(manager.getAllRecipesFor(type).stream(), clazz);
  }

  /** Serializes a fluid stack into Mantle's compact JSON form. */
  public static JsonObject serializeFluidStack(FluidStack stack) {
    JsonObject json = new JsonObject();
    json.addProperty("fluid", BuiltInRegistries.FLUID.getKey(stack.getFluid()).toString());
    json.addProperty("amount", stack.getAmount());
    return json;
  }

  /** Deserializes a fluid stack from Mantle's compact JSON form. */
  public static FluidStack deserializeFluidStack(JsonObject json) {
    String fluidName = GsonHelper.getAsString(json, "fluid");
    ResourceLocation id = ResourceLocation.parse(fluidName);
    Fluid fluid = BuiltInRegistries.FLUID.get(id);
    if (fluid == null || fluid == Fluids.EMPTY) {
      throw new JsonSyntaxException("Unknown fluid " + fluidName);
    }
    long amount = GsonHelper.getAsLong(json, "amount");
    return new FluidStack(fluid, amount);
  }

  /** Gets an item from JSON and validates its class type. */
  public static <C> C deserializeItem(String name, String key, Class<C> clazz) {
    ResourceLocation id = ResourceLocation.parse(name);
    Item item = BuiltInRegistries.ITEM.get(id);
    if (item == null) {
      throw new JsonSyntaxException("Invalid " + key + ": Unknown item " + name + "'");
    }
    if (!clazz.isInstance(item)) {
      throw new JsonSyntaxException("Invalid " + key + ": must be " + clazz.getSimpleName());
    }
    return clazz.cast(item);
  }

  /** Reads an item from the packet buffer. */
  public static Item readItem(FriendlyByteBuf buffer) {
    return Item.byId(buffer.readVarInt());
  }

  /** Reads an item from the packet buffer and validates its class type. */
  public static <T> T readItem(FriendlyByteBuf buffer, Class<T> clazz) {
    Item item = readItem(buffer);
    if (!clazz.isInstance(item)) {
      throw new DecoderException("Invalid item '" + BuiltInRegistries.ITEM.getKey(item) + "', must be " + clazz.getSimpleName());
    }
    return clazz.cast(item);
  }

  /** Writes an item to the packet buffer. */
  public static void writeItem(FriendlyByteBuf buffer, ItemLike item) {
    buffer.writeVarInt(Item.getId(item.asItem()));
  }
}

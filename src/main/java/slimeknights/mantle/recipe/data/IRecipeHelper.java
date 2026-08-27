package slimeknights.mantle.recipe.data;

import io.github.fabricators_of_create.porting_lib.registry.DeferredHolder;
import net.fabricmc.fabric.api.resource.conditions.v1.ResourceCondition;
import net.fabricmc.fabric.api.resource.conditions.v1.ResourceConditions;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.material.Fluid;
import org.jetbrains.annotations.ApiStatus;
import slimeknights.mantle.registration.object.IdAwareObject;
import slimeknights.mantle.util.IdExtender.LocationExtender;

import java.util.Objects;

/** Interface for common resource location and condition methods. */
@SuppressWarnings("unused")
public interface IRecipeHelper extends LocationExtender {
  /** Gets the ID of the mod adding recipes. */
  String getModId();

  /** Use {@link #location(String)}, this method just exists to simplify implementation. */
  @ApiStatus.Internal
  @Override
  default ResourceLocation location(String namespace, String path) {
    return location(path);
  }

  /** Gets a resource location for the mod. */
  default ResourceLocation location(String name) {
    return ResourceLocation.fromNamespaceAndPath(getModId(), name);
  }

  /** Gets a resource location string for your mod. */
  default String prefix(String id) {
    return getModId() + ":" + id;
  }

  /** Gets a registry ID for the given item in this mod's namespace. */
  default ResourceLocation id(ItemLike item) {
    return location(Objects.requireNonNull(BuiltInRegistries.ITEM.getKey(item.asItem())).getPath());
  }

  /** Gets a registry ID for the given value in this mod's namespace. */
  default <T> ResourceLocation id(Registry<T> registry, T value) {
    return location(Objects.requireNonNull(registry.getKey(value)).getPath());
  }

  /* Deferred holder location helpers */
  default ResourceLocation wrap(DeferredHolder<?, ?> location, String prefix, String suffix) {
    return wrap(location.getId(), prefix, suffix);
  }

  default ResourceLocation prefix(DeferredHolder<?, ?> location, String prefix) {
    return prefix(location.getId(), prefix);
  }

  default ResourceLocation suffix(DeferredHolder<?, ?> location, String suffix) {
    return suffix(location.getId(), suffix);
  }

  /* Other named object location helpers */
  default ResourceLocation wrap(IdAwareObject location, String prefix, String suffix) {
    return wrap(location.getId(), prefix, suffix);
  }

  default ResourceLocation prefix(IdAwareObject location, String prefix) {
    return prefix(location.getId(), prefix);
  }

  default ResourceLocation suffix(IdAwareObject location, String suffix) {
    return suffix(location.getId(), suffix);
  }

  /** Gets an item tag by name. */
  default TagKey<Item> getItemTag(String modId, String name) {
    return TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(modId, name));
  }

  /** Gets a fluid tag by name. */
  default TagKey<Fluid> getFluidTag(String modId, String name) {
    return TagKey.create(Registries.FLUID, ResourceLocation.fromNamespaceAndPath(modId, name));
  }

  /** Creates a condition requiring the common item tag to be populated. */
  default ResourceCondition tagCondition(String name) {
    return ResourceConditions.tagsPopulated(getItemTag("c", name));
  }

  /** Creates a RecipeOutput instance with the added Fabric resource conditions. */
  default RecipeOutput withCondition(RecipeOutput output, ResourceCondition... conditions) {
    ConsumerWrapperBuilder builder = ConsumerWrapperBuilder.wrap();
    for (ResourceCondition condition : conditions) {
      builder.addCondition(condition);
    }
    return builder.build(output);
  }
}

package slimeknights.mantle.registration.adapter;

import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;

import java.util.Objects;

/**
 * Convenience wrapper for registering entries in a vanilla registry under a fixed mod namespace.
 */
@SuppressWarnings("WeakerAccess")
public class RegistryAdapter<T> {
  private final Registry<T> registry;
  private final String modId;

  public RegistryAdapter(Registry<T> registry, String modId) {
    this.registry = registry;
    this.modId = modId;
  }

  /**
   * Construct a resource location that belongs to the given namespace. Usually your mod.
   * @param name  Name for location
   */
  public ResourceLocation getResource(String name) {
    return ResourceLocation.fromNamespaceAndPath(modId, name);
  }

  /**
   * Construct a resource location string that belongs to the given namespace. Usually your mod.
   * @param name  Name for location
   */
  public String resourceName(String name) {
    return modId + ":" + name;
  }

  /**
   * General purpose registration method. Just pass the name you want your thing registered as.
   * @param entry  Entry to register
   * @param name   Registry name
   * @return Registry entry
   */
  public <I extends T> I register(I entry, String name) {
    return this.register(entry, this.getResource(name));
  }

  /**
   * Registers an entry using the name from another entry
   * @param entry  Entry to register
   * @param name   Entry name to copy
   * @param <I>    Value type
   * @return  Registered entry
   */
  public <I extends T> I register(I entry, T name) {
    return this.register(entry, Objects.requireNonNull(registry.getKey(name)));
  }

  /**
   * General purpose backup registration method. In case you want to set a very specific resource location.
   * @param entry     Entry to register
   * @param location  Registry name
   * @return Registry entry
   */
  public <I extends T> I register(I entry, ResourceLocation location) {
    return Registry.register(registry, location, entry);
  }
}

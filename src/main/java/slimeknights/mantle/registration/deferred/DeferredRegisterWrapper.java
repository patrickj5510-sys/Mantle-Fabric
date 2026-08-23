package slimeknights.mantle.registration.deferred;

import io.github.fabricators_of_create.porting_lib.registry.DeferredRegister;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.StringRepresentable;
import slimeknights.mantle.registration.object.EnumObject;

import java.util.function.BiFunction;
import java.util.function.Supplier;

/**
 * Base logic for a deferred register wrapper.
 * @param <T> registry type
 */
@SuppressWarnings("WeakerAccess")
public abstract class DeferredRegisterWrapper<T> {
  /** Registry instance, use this to provide register methods. */
  protected final SynchronizedDeferredRegister<T> register;
  /** Mod ID for registration. */
  protected final String modID;

  protected DeferredRegisterWrapper(ResourceKey<? extends Registry<T>> registryKey, String modID) {
    this(DeferredRegister.create(registryKey, modID), modID);
  }

  protected DeferredRegisterWrapper(Registry<T> registry, String modID) {
    this(DeferredRegister.create(registry, modID), modID);
  }

  protected DeferredRegisterWrapper(DeferredRegister<T> register, String modID) {
    this.register = SynchronizedDeferredRegister.create(register);
    this.modID = modID;
  }

  /** Initializes this registry wrapper. Needs to be called during mod construction. */
  public void register() {
    register.register();
  }

  /** Gets a resource location object for the given name. */
  protected ResourceLocation resource(String name) {
    return ResourceLocation.fromNamespaceAndPath(modID, name);
  }

  /** Gets a resource location string for the given name. */
  protected String resourceName(String name) {
    return modID + ":" + name;
  }

  /** Registers an enum-backed group, prefixing each name with the serialized enum value. */
  protected static <E extends Enum<E> & StringRepresentable, V extends T, T> EnumObject<E,V> registerEnum(E[] values, String name, BiFunction<String,E,Supplier<? extends V>> register) {
    if (values.length == 0) {
      throw new IllegalArgumentException("Must have at least one value");
    }
    EnumObject.Builder<E,V> builder = new EnumObject.Builder<>(values[0].getDeclaringClass());
    for (E value : values) {
      builder.put(value, register.apply(value.getSerializedName() + "_" + name, value));
    }
    return builder.build();
  }

  /** Registers an enum-backed group, suffixing each name with the serialized enum value. */
  protected static <E extends Enum<E> & StringRepresentable, V extends T, T> EnumObject<E,V> registerEnum(String name, E[] values, BiFunction<String,E,Supplier<? extends V>> register) {
    if (values.length == 0) {
      throw new IllegalArgumentException("Must have at least one value");
    }
    EnumObject.Builder<E,V> builder = new EnumObject.Builder<>(values[0].getDeclaringClass());
    for (E value : values) {
      builder.put(value, register.apply(name + "_" + value.getSerializedName(), value));
    }
    return builder.build();
  }
}

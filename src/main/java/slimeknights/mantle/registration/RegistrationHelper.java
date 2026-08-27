package slimeknights.mantle.registration;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraft.core.DefaultedRegistry;
import net.minecraft.world.level.block.state.properties.WoodType;
import slimeknights.mantle.util.RegistryHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class RegistrationHelper {
  /** Wood types registered by Mantle consumers for client-side atlas/model setup. */
  private static final List<WoodType> WOOD_TYPES = new ArrayList<>();

  /**
   * Gets a holder for a registry object.
   * @param registry  Registry instance
   * @param entry     Entry to fetch holder
   * @param <T>       Registry type
   * @param <R>       Return type, typically but not strictly registry type
   * @return Supplier for the given registry cast to the requested type
   */
  @SuppressWarnings("unchecked")
  public static <T, R extends T> Supplier<R> getCastedHolder(DefaultedRegistry<T> registry, T entry) {
    Supplier<T> holder = RegistryHelper.getHolder(registry, entry);
    return () -> (R) holder.get();
  }

  /** Registers a wood type with vanilla and records it for Mantle client setup. */
  public static void registerWoodType(WoodType type) {
    synchronized (WOOD_TYPES) {
      WOOD_TYPES.add(type);
      WoodType.register(type);
    }
  }

  /** Runs the given consumer for each registered wood type. */
  public static void forEachWoodType(Consumer<WoodType> consumer) {
    WOOD_TYPES.forEach(consumer);
  }
}

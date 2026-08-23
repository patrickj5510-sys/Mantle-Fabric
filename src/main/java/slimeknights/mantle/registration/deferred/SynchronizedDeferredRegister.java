package slimeknights.mantle.registration.deferred;

import io.github.fabricators_of_create.porting_lib.registry.DeferredHolder;
import io.github.fabricators_of_create.porting_lib.registry.DeferredRegister;
import lombok.RequiredArgsConstructor;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;

import java.util.function.Supplier;

/** Deferred register instance that synchronizes register calls. */
@RequiredArgsConstructor(staticName = "create")
public class SynchronizedDeferredRegister<T> {
  private final DeferredRegister<T> internal;

  /** Creates a new instance for the given resource key. */
  public static <T> SynchronizedDeferredRegister<T> create(ResourceKey<? extends Registry<T>> key, String modid) {
    return create(DeferredRegister.create(key, modid));
  }

  /** Creates a new instance for the given registry. */
  public static <B> SynchronizedDeferredRegister<B> create(Registry<B> registry, String modid) {
    return create(DeferredRegister.create(registry, modid));
  }

  /** Registers the given object, synchronized over the internal register. */
  public <I extends T> DeferredHolder<T, I> register(final String name, final Supplier<? extends I> supplier) {
    synchronized (internal) {
      return internal.register(name, supplier);
    }
  }

  /** Registers all queued entries with the target registry. */
  public void register() {
    internal.register();
  }
}

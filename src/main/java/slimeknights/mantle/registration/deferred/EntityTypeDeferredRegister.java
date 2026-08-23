package slimeknights.mantle.registration.deferred;

import io.github.fabricators_of_create.porting_lib.registry.DeferredHolder;
import io.github.fabricators_of_create.porting_lib.util.DeferredSpawnEggItem;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;

import java.util.function.Supplier;

/** Deferred register for entity types, with optional spawn eggs. */
@SuppressWarnings("unused")
public class EntityTypeDeferredRegister extends DeferredRegisterWrapper<EntityType<?>> {
  private final SynchronizedDeferredRegister<Item> itemRegistry;

  public EntityTypeDeferredRegister(String modID) {
    super(Registries.ENTITY_TYPE, modID);
    itemRegistry = SynchronizedDeferredRegister.create(Registries.ITEM, modID);
  }

  @Override
  public void register() {
    super.register();
    itemRegistry.register();
  }

  /** Registers an entity type from a Fabric entity type builder. */
  public <T extends Entity> DeferredHolder<EntityType<?>, EntityType<T>> register(String name, Supplier<FabricEntityTypeBuilder<T>> supplier) {
    return register.register(name, () -> supplier.get().build());
  }

  /** Registers an entity type and a deferred spawn egg for it. */
  public <T extends Mob> DeferredHolder<EntityType<?>, EntityType<T>> registerWithEgg(String name, Supplier<FabricEntityTypeBuilder<T>> supplier, int primary, int secondary) {
    DeferredHolder<EntityType<?>, EntityType<T>> object = register(name, supplier);
    var spawnEgg = itemRegistry.register(name + "_spawn_egg", () -> new DeferredSpawnEggItem(object, primary, secondary, new Item.Properties()));
    ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.SPAWN_EGGS).register(entries -> entries.accept(spawnEgg.get()));
    return object;
  }
}

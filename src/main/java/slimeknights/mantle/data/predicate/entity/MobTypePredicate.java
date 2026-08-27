package slimeknights.mantle.data.predicate.entity;

import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.entity.LivingEntity;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.mantle.data.registry.GenericLoaderRegistry.IGenericLoader;
import slimeknights.mantle.data.registry.NamedComponentRegistry;

/** Predicate matching the legacy Mantle mob classifications using vanilla 1.21 entity type tags. */
public record MobTypePredicate(MobClass type) implements LivingEntityPredicate {
  /** Registry of mob classifications, preserving the existing serialized names used by Mantle/Tinkers data. */
  public static final NamedComponentRegistry<MobClass> MOB_TYPES = new NamedComponentRegistry<>("Unknown mob type");
  /** Loader for a mob type predicate. */
  public static final RecordLoadable<MobTypePredicate> LOADER = RecordLoadable.create(MOB_TYPES.requiredField("mobs", MobTypePredicate::type), MobTypePredicate::new);

  /** Replacement for the MobType enum removed by Minecraft 1.21. */
  public enum MobClass {
    UNDEFINED,
    UNDEAD,
    ARTHROPOD,
    ILLAGER,
    WATER
  }

  @Override
  public boolean matches(LivingEntity input) {
    return switch (type) {
      case UNDEAD -> input.getType().is(EntityTypeTags.UNDEAD);
      case ARTHROPOD -> input.getType().is(EntityTypeTags.ARTHROPOD);
      case ILLAGER -> input.getType().is(EntityTypeTags.ILLAGER);
      case WATER -> input.getType().is(EntityTypeTags.AQUATIC);
      case UNDEFINED -> !input.getType().is(EntityTypeTags.UNDEAD)
        && !input.getType().is(EntityTypeTags.ARTHROPOD)
        && !input.getType().is(EntityTypeTags.ILLAGER)
        && !input.getType().is(EntityTypeTags.AQUATIC);
    };
  }

  @Override
  public IGenericLoader<? extends LivingEntityPredicate> getLoader() {
    return LOADER;
  }
}

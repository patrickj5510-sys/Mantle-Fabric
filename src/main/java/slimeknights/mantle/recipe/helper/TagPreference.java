package slimeknights.mantle.recipe.helper;

import net.fabricmc.fabric.api.event.lifecycle.v1.CommonLifecycleEvents;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import slimeknights.mantle.config.Config;
import slimeknights.mantle.util.LogicHelper;
import slimeknights.mantle.util.RegistryHelper;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/** Utility that helps get the preferred item from a tag based on mod ID. */
public class TagPreference {
  /** Just an alphabetically late ID to simplify null checks. */
  private static final ResourceLocation DEFAULT_ID = ResourceLocation.fromNamespaceAndPath("zzzzz", "zzzzz");

  /** Cache from any tag key to its preferred value. */
  private static final Map<TagKey<?>, Optional<?>> PREFERENCE_CACHE = new ConcurrentHashMap<>();
  /** Cache of comparator instances. */
  private static final Map<ResourceKey<?>, RegistryComparator<?>> COMPARATOR_CACHE = new HashMap<>();

  /** Registers cache invalidation whenever tags are loaded or synchronized. */
  public static void init() {
    CommonLifecycleEvents.TAGS_LOADED.register((registries, client) -> {
      PREFERENCE_CACHE.clear();
      COMPARATOR_CACHE.clear();
    });
  }

  @SuppressWarnings("unchecked")
  private static <T> Comparator<T> getComparator(Registry<T> registry) {
    return (Comparator<T>) COMPARATOR_CACHE.computeIfAbsent(registry.key(), k -> new RegistryComparator<>(registry));
  }

  /** Gets the preference from a tag without going through the cache. */
  private static <T> Optional<T> getUncachedPreference(TagKey<T> tag) {
    Registry<T> registry = RegistryHelper.getRegistry(tag.registry());
    if (registry == null) {
      return Optional.empty();
    }
    return RegistryHelper.getTagValueStream(tag).min(getComparator(registry));
  }

  private static final Function<TagKey<?>, Optional<?>> PREFERENCE_LOOKUP = TagPreference::getUncachedPreference;

  /** Gets the preferred value from a tag based on configured mod-ID preference. */
  @SuppressWarnings("unchecked")
  public static <T> Optional<T> getPreference(TagKey<T> tag) {
    return (Optional<T>) PREFERENCE_CACHE.computeIfAbsent(tag, PREFERENCE_LOOKUP);
  }

  /** Logic to compare two registry values. */
  private record RegistryComparator<T>(Registry<T> registry) implements Comparator<T> {
    @Override
    public int compare(T a, T b) {
      ResourceLocation idA = Objects.requireNonNullElse(registry.getKey(a), DEFAULT_ID);
      ResourceLocation idB = Objects.requireNonNullElse(registry.getKey(b), DEFAULT_ID);
      List<? extends String> entries = Config.TAG_PREFERENCES.get();
      int size = entries.size();
      int indexA = LogicHelper.defaultIf(entries.indexOf(idA.getNamespace()), -1, size);
      int indexB = LogicHelper.defaultIf(entries.indexOf(idB.getNamespace()), -1, size);
      if (indexA != indexB) {
        return Integer.compare(indexA, indexB);
      }
      return idA.compareNamespaced(idB);
    }
  }
}

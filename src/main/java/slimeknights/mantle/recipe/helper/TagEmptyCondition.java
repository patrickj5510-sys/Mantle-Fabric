package slimeknights.mantle.recipe.helper;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.fabricmc.fabric.api.resource.conditions.v1.ResourceCondition;
import net.fabricmc.fabric.api.resource.conditions.v1.ResourceConditionType;
import net.fabricmc.fabric.api.resource.conditions.v1.ResourceConditions;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import org.jetbrains.annotations.Nullable;
import slimeknights.mantle.Mantle;

/**
 * Resource condition used for fallback data when a tag is unavailable.
 *
 * <p>Fabric evaluates resource conditions before tag contents are bound, so its public API can tell whether a tag
 * was loaded but cannot distinguish a declared zero-entry tag from a populated one at this phase. In practice this
 * preserves Mantle's fallback behavior for absent compatibility tags, which is the primary use of this condition.</p>
 */
public class TagEmptyCondition<T> implements ResourceCondition {
  private static final ResourceLocation NAME = Mantle.getResource("tag_empty");

  public static final MapCodec<TagEmptyCondition<?>> CODEC = RecordCodecBuilder.<TagEmptyCondition<?>>mapCodec(instance -> instance.group(
    ResourceLocation.CODEC.fieldOf("registry").forGetter(condition -> condition.tag.registry().location()),
    ResourceLocation.CODEC.fieldOf("tag").forGetter(condition -> condition.tag.location())
  ).apply(instance, TagEmptyCondition::fromIds));

  public static final ResourceConditionType<TagEmptyCondition<?>> TYPE = ResourceConditionType.create(NAME, CODEC);

  private final TagKey<T> tag;

  public TagEmptyCondition(TagKey<T> tag) {
    this.tag = tag;
  }

  public TagEmptyCondition(ResourceKey<? extends Registry<T>> registry, ResourceLocation name) {
    this(TagKey.create(registry, name));
  }

  private static TagEmptyCondition<?> fromIds(ResourceLocation registry, ResourceLocation tag) {
    return create(ResourceKey.createRegistryKey(registry), tag);
  }

  private static <T> TagEmptyCondition<T> create(ResourceKey<? extends Registry<T>> registry, ResourceLocation tag) {
    return new TagEmptyCondition<>(registry, tag);
  }

  @Override
  public ResourceConditionType<?> getType() {
    return TYPE;
  }

  @Override
  public boolean test(@Nullable HolderLookup.Provider registries) {
    return ResourceConditions.not(ResourceConditions.tagsPopulated(tag)).test(registries);
  }

  @Override
  public String toString() {
    return "tag_empty(\"" + tag + "\")";
  }
}

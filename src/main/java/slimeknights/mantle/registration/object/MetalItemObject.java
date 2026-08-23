package slimeknights.mantle.registration.object;

import lombok.Getter;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

import java.util.function.Supplier;

/** Object wrapper containing ingots, nuggets, and blocks. */
public class MetalItemObject extends ItemObject<Block> {
  private final Supplier<? extends Item> ingot;
  private final Supplier<? extends Item> nugget;
  @Getter private final TagKey<Block> blockTag;
  @Getter private final TagKey<Item> blockItemTag;
  @Getter private final TagKey<Item> ingotTag;
  @Getter private final TagKey<Item> nuggetTag;

  public MetalItemObject(String tagName, ItemObject<? extends Block> block, Supplier<? extends Item> ingot, Supplier<? extends Item> nugget) {
    super(block);
    this.ingot = ingot;
    this.nugget = nugget;
    this.blockTag = TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath("c", tagName + "_blocks"));
    this.blockItemTag = getTag(tagName + "_blocks");
    this.ingotTag = getTag(tagName + "_ingots");
    this.nuggetTag = getTag(tagName + "_nuggets");
  }

  public Item getIngot() { return ingot.get(); }
  public Item getNugget() { return nugget.get(); }

  private static TagKey<Item> getTag(String name) {
    return TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("c", name));
  }
}

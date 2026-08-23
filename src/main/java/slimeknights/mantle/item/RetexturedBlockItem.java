package slimeknights.mantle.item;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import slimeknights.mantle.block.RetexturedBlock;
import slimeknights.mantle.block.entity.IRetexturedBlockEntity;
import slimeknights.mantle.util.RetexturedHelper;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Logic for a dynamically retexturable block item.
 * Use alongside {@link IRetexturedBlockEntity} and {@link RetexturedBlock}.
 */
@SuppressWarnings("WeakerAccess")
public class RetexturedBlockItem extends BlockTooltipItem {
  /** Tag used for getting the texture */
  protected final TagKey<Item> textureTag;

  public RetexturedBlockItem(Block block, TagKey<Item> textureTag, Properties builder) {
    super(block, builder);
    this.textureTag = textureTag;
  }

  public void fillItemCategory(CreativeModeTab.Output items) {
    addTagVariants(this.getBlock(), textureTag, items, true);
  }

  @Override
  public void appendHoverText(ItemStack stack, @Nullable Level worldIn, List<Component> tooltip, TooltipFlag flagIn) {
    addTooltip(stack, tooltip);
    super.appendHoverText(stack, worldIn, tooltip, flagIn);
  }

  /* Utils */

  /** Gets the texture name from a stack. */
  public static String getTextureName(ItemStack stack) {
    return RetexturedHelper.getTextureName(stack);
  }

  /** Gets the texture block from a stack. */
  public static Block getTexture(ItemStack stack) {
    return RetexturedHelper.getTexture(stack);
  }

  /** Adds the texture block to the tooltip. */
  public static void addTooltip(ItemStack stack, List<Component> tooltip) {
    RetexturedHelper.addTooltip(stack, tooltip);
  }

  /** Sets or clears the texture name using 1.21 item data components. */
  public static ItemStack setTexture(ItemStack stack, String name) {
    return RetexturedHelper.setTexture(stack, name);
  }

  /** Sets or clears the texture block using 1.21 item data components. */
  public static ItemStack setTexture(ItemStack stack, @Nullable Block block) {
    return RetexturedHelper.setTexture(stack, block);
  }

  /**
   * Adds all blocks from the block tag to the specified block for fillItemGroup.
   */
  public static void addTagVariants(ItemLike block, TagKey<Item> tag, CreativeModeTab.Output list, boolean showAllVariants) {
    boolean added = false;
    Class<?> clazz = block.getClass();
    for (Holder<Item> candidate : BuiltInRegistries.ITEM.getTagOrEmpty(tag)) {
      if (!candidate.isBound()) {
        continue;
      }
      Item item = candidate.value();
      if (!(item instanceof BlockItem blockItem)) {
        continue;
      }
      Block textureBlock = blockItem.getBlock();
      if (clazz.isInstance(textureBlock)) {
        continue;
      }
      added = true;
      list.accept(setTexture(new ItemStack(block), textureBlock));
      if (!showAllVariants) {
        return;
      }
    }
    if (!added) {
      list.accept(new ItemStack(block));
    }
  }
}

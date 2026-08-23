package slimeknights.mantle.util;

import io.github.fabricators_of_create.porting_lib.models.data.ModelData;
import io.github.fabricators_of_create.porting_lib.models.data.ModelProperty;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import slimeknights.mantle.Mantle;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

/**
 * This utility contains helpers to handle the NBT for retexturable blocks
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class RetexturedHelper {
  /** Translation key for the texture ID in advanced tooltips. */
  public static final String KEY_ID = Mantle.makeDescriptionId("block", "retextured.id");
  /** Tag name for texture blocks. Should not be used directly, use the utils to interact */
  public static final String TAG_TEXTURE = "texture";
  /** Property for tile entities containing a texture block */
  public static final ModelProperty<Block> BLOCK_PROPERTY = new ModelProperty<>(block -> block != Blocks.AIR);

  /* Texture name */

  /** Gets the name of the texture from NBT, or empty if none. */
  public static String getTextureName(@Nullable CompoundTag nbt) {
    if (nbt == null) {
      return "";
    }
    return nbt.getString(TAG_TEXTURE);
  }

  /** Gets the texture name from an item stack. */
  public static String getTextureName(ItemStack stack) {
    CustomData customData = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
    return getTextureName(customData.copyTag());
  }

  /** Gets the registry name for a texture block. */
  public static String getTextureName(Block block) {
    if (block == Blocks.AIR) {
      return "";
    }
    return Objects.requireNonNull(BuiltInRegistries.BLOCK.getKey(block)).toString();
  }

  /* Texture */

  /** Gets a block for the given registry name, or air if invalid. */
  public static Block getBlock(String name) {
    if (!name.isEmpty()) {
      ResourceLocation location = ResourceLocation.tryParse(name);
      if (location != null) {
        return BuiltInRegistries.BLOCK.get(location);
      }
    }
    return Blocks.AIR;
  }

  /** Gets the texture block from a stack. */
  public static Block getTexture(ItemStack stack) {
    return getBlock(getTextureName(stack));
  }

  /* Setting */

  /** Sets the texture in an NBT instance. */
  public static void setTexture(@Nullable CompoundTag nbt, String texture) {
    if (nbt != null) {
      if (texture.isEmpty()) {
        nbt.remove(TAG_TEXTURE);
      } else {
        nbt.putString(TAG_TEXTURE, texture);
      }
    }
  }

  /** Sets or clears the texture on an item stack using 1.21 custom data components. */
  public static ItemStack setTexture(ItemStack stack, String name) {
    if (!name.isEmpty()) {
      stack.update(DataComponents.CUSTOM_DATA, CustomData.EMPTY, data -> {
        CompoundTag tag = data.copyTag();
        tag.putString(TAG_TEXTURE, name);
        return CustomData.of(tag);
      });
    } else if (stack.has(DataComponents.CUSTOM_DATA)) {
      stack.update(DataComponents.CUSTOM_DATA, CustomData.EMPTY, data -> {
        CompoundTag tag = data.copyTag();
        tag.remove(TAG_TEXTURE);
        return CustomData.of(tag);
      });
    }
    return stack;
  }

  /** Sets the texture block on an item stack. */
  public static ItemStack setTexture(ItemStack stack, @Nullable Block block) {
    if (block == null || block == Blocks.AIR) {
      return setTexture(stack, "");
    }
    return setTexture(stack, BuiltInRegistries.BLOCK.getKey(block).toString());
  }

  /* Block entity */

  /** Refreshes client model data after the texture changes. */
  public static void onTextureUpdated(BlockEntity self) {
    Level level = self.getLevel();
    if (level != null && level.isClientSide) {
      self.requestModelDataUpdate();
      BlockState state = self.getBlockState();
      level.sendBlockUpdated(self.getBlockPos(), state, state, 0);
    }
  }

  /** Creates a builder with the block property as specified. */
  public static ModelData.Builder getModelDataBuilder(Block block) {
    if (block == Blocks.AIR) {
      block = null;
    }
    return ModelData.builder().with(BLOCK_PROPERTY, block);
  }

  /** Creates model data with the block property as specified. */
  public static ModelData getModelData(Block block) {
    return getModelDataBuilder(block).build();
  }

  /* Tooltip / variants */

  /** Adds the texture block to the tooltip. */
  public static void addTooltip(ItemStack stack, List<Component> tooltip, TooltipFlag flag) {
    Block block = getTexture(stack);
    if (block != Blocks.AIR) {
      tooltip.add(block.getName().withStyle(ChatFormatting.GRAY));
      if (flag.isAdvanced()) {
        tooltip.add(Component.translatable(KEY_ID, BuiltInRegistries.BLOCK.getKey(block)).withStyle(ChatFormatting.DARK_GRAY));
      }
    }
  }

  /** @deprecated use {@link #addTooltip(ItemStack, List, TooltipFlag)} */
  @Deprecated(forRemoval = true)
  public static void addTooltip(ItemStack stack, List<Component> tooltip) {
    addTooltip(stack, tooltip, TooltipFlag.NORMAL);
  }

  /** Adds all block items in a tag as retextured variants. */
  @SuppressWarnings("deprecation")
  public static boolean addTagVariants(Predicate<ItemStack> tab, ItemLike block, TagKey<Item> tag) {
    boolean added = false;
    for (Holder<Item> candidate : BuiltInRegistries.ITEM.getTagOrEmpty(tag)) {
      if (!candidate.isBound()) {
        continue;
      }
      Item item = candidate.value();
      if (item == block.asItem()) {
        continue;
      }
      if (!(item instanceof BlockItem blockItem)) {
        continue;
      }
      added = true;
      if (tab.test(setTexture(new ItemStack(block), blockItem.getBlock()))) {
        break;
      }
    }
    return added;
  }
}

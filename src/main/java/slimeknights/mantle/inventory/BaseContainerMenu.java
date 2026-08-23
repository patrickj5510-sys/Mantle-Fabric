package slimeknights.mantle.inventory;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;

/** Base container menu with Mantle's slot movement behavior. */
public abstract class BaseContainerMenu extends AbstractContainerMenu {
  protected BaseContainerMenu(@Nullable MenuType<?> menuType, int containerId) {
    super(menuType, containerId);
  }

  /** Adds the player's inventory slots to this menu. */
  protected void addInventorySlots(Inventory inventory, int startX, int startY) {
    for (int row = 0; row < 3; row++) {
      for (int column = 0; column < 9; column++) {
        this.addSlot(new Slot(inventory, column + row * 9 + 9, startX + column * 18, startY + row * 18));
      }
    }
    for (int column = 0; column < 9; column++) {
      this.addSlot(new Slot(inventory, column, startX + column * 18, startY + 58));
    }
  }

  /** Moves an item stack between menu regions. */
  @Override
  protected boolean moveItemStackTo(ItemStack stack, int startIndex, int endIndex, boolean useEndIndex) {
    boolean ret = this.mergeItemStackRefill(stack, startIndex, endIndex, useEndIndex);
    if (!stack.isEmpty() && stack.getCount() > 0) {
      ret |= this.mergeItemStackMove(stack, startIndex, endIndex, useEndIndex);
    }
    return ret;
  }

  /** Tries to merge into existing compatible stacks first. */
  protected boolean mergeItemStackRefill(ItemStack stack, int startIndex, int endIndex, boolean useEndIndex) {
    if (stack.getCount() <= 0) {
      return false;
    }
    boolean changed = false;
    int index = useEndIndex ? endIndex - 1 : startIndex;
    if (stack.isStackable()) {
      while (stack.getCount() > 0 && (!useEndIndex && index < endIndex || useEndIndex && index >= startIndex)) {
        Slot slot = this.slots.get(index);
        ItemStack existing = slot.getItem();
        if (!existing.isEmpty() && ItemStack.isSameItemSameComponents(stack, existing) && this.canTakeItemForPickAll(stack, slot)) {
          int combined = existing.getCount() + stack.getCount();
          int limit = Math.min(stack.getMaxStackSize(), slot.getMaxStackSize(stack));
          if (combined <= limit) {
            stack.setCount(0);
            existing.setCount(combined);
            slot.setChanged();
            changed = true;
          } else if (existing.getCount() < limit) {
            stack.shrink(limit - existing.getCount());
            existing.setCount(limit);
            slot.setChanged();
            changed = true;
          }
        }
        index += useEndIndex ? -1 : 1;
      }
    }
    return changed;
  }

  /** Moves into empty compatible slots after refill attempts. */
  protected boolean mergeItemStackMove(ItemStack stack, int startIndex, int endIndex, boolean useEndIndex) {
    if (stack.getCount() <= 0) {
      return false;
    }
    boolean changed = false;
    int index = useEndIndex ? endIndex - 1 : startIndex;
    while (!useEndIndex && index < endIndex || useEndIndex && index >= startIndex) {
      Slot slot = this.slots.get(index);
      ItemStack existing = slot.getItem();
      if (existing.isEmpty() && slot.mayPlace(stack) && this.canTakeItemForPickAll(stack, slot)) {
        int limit = slot.getMaxStackSize(stack);
        ItemStack moved = stack.copy();
        moved.setCount(Math.min(stack.getCount(), limit));
        slot.set(moved);
        stack.shrink(moved.getCount());
        slot.setChanged();
        changed = true;
        if (stack.isEmpty()) {
          break;
        }
      }
      index += useEndIndex ? -1 : 1;
    }
    return changed;
  }

  @Override
  public abstract ItemStack quickMoveStack(Player player, int index);
}

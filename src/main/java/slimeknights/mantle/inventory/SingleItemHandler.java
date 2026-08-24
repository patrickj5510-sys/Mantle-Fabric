package slimeknights.mantle.inventory;

import io.github.fabricators_of_create.porting_lib.transfer.item.SlottedStackStorage;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.item.base.SingleStackStorage;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleSlotStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import slimeknights.mantle.block.entity.MantleBlockEntity;

import javax.annotation.Nonnull;

/** Item handler containing exactly one item. */
@SuppressWarnings("unused")
@RequiredArgsConstructor
public abstract class SingleItemHandler<T extends MantleBlockEntity> implements SlottedStackStorage {
  protected final T parent;
  private final int maxStackSize;

  @Getter
  private ItemStack stack = ItemStack.EMPTY;

  public void setStack(ItemStack newStack) {
    this.stack = newStack;
    parent.setChangedFast();
  }

  protected abstract boolean isItemValid(ItemVariant stack);

  /** Compatibility helper retained for callers that validate by slot number. */
  public boolean isItemValid(int slot, ItemVariant stack) {
    return slot == 0 && isItemValid(stack);
  }

  @Override
  public int getSlotCount() {
    return 1;
  }

  @Override
  public int getSlotLimit(int slot) {
    return maxStackSize;
  }

  @Nonnull
  @Override
  public ItemStack getStackInSlot(int slot) {
    return slot == 0 ? stack : ItemStack.EMPTY;
  }

  @Override
  public void setStackInSlot(int slot, ItemStack stack) {
    if (slot == 0) {
      setStack(stack);
    }
  }

  @Override
  public SingleSlotStorage<ItemVariant> getSlot(int slot) {
    return new SingleStackStorage() {
      @Override
      protected ItemStack getStack() {
        return SingleItemHandler.this.getStackInSlot(slot);
      }

      @Override
      protected void setStack(ItemStack stack) {
        SingleItemHandler.this.setStackInSlot(slot, stack);
      }
    };
  }

  @Override
  public long insert(ItemVariant resource, long maxAmount, TransactionContext transaction) {
    return getSlot(0).insert(resource, maxAmount, transaction);
  }

  @Override
  public long extract(ItemVariant resource, long maxAmount, TransactionContext transaction) {
    return getSlot(0).extract(resource, maxAmount, transaction);
  }

  public CompoundTag writeToNBT(HolderLookup.Provider registries) {
    if (!stack.isEmpty()) {
      return (CompoundTag) stack.save(registries);
    }
    return new CompoundTag();
  }

  public void readFromNBT(CompoundTag nbt, HolderLookup.Provider registries) {
    stack = ItemStack.parseOptional(registries, nbt);
  }
}

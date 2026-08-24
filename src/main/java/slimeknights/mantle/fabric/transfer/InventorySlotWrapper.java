package slimeknights.mantle.fabric.transfer;

import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.item.base.SingleStackStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.fabricmc.fabric.impl.transfer.DebugMessages;
import net.fabricmc.fabric.impl.transfer.item.ItemVariantImpl;
import net.fabricmc.fabric.impl.transfer.item.SpecialLogicInventory;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.BrewingStandBlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.world.level.block.state.properties.ChestType;

class InventorySlotWrapper extends SingleStackStorage {
  private final InventoryStorage storage;
  final int slot;
  private final SpecialLogicInventory specialInv;
  private ItemStack lastReleasedSnapshot = null;

  InventorySlotWrapper(InventoryStorage storage, int slot) {
    this.storage = storage;
    this.slot = slot;
    this.specialInv = storage.inventory instanceof SpecialLogicInventory specialInv ? specialInv : null;
  }

  @Override
  protected ItemStack getStack() {
    return storage.inventory.getItem(slot);
  }

  @Override
  protected void setStack(ItemStack stack) {
    if (specialInv == null) {
      storage.inventory.setItem(slot, stack);
    } else {
      specialInv.fabric_setSuppress(true);
      try {
        storage.inventory.setItem(slot, stack);
      } finally {
        specialInv.fabric_setSuppress(false);
      }
    }
  }

  @Override
  public long insert(ItemVariant insertedVariant, long maxAmount, TransactionContext transaction) {
    if (!canInsert(slot, ((ItemVariantImpl) insertedVariant).getCachedStack())) {
      return 0;
    }
    long ret = super.insert(insertedVariant, maxAmount, transaction);
    if (specialInv != null && ret > 0) specialInv.fabric_onTransfer(slot, transaction);
    return ret;
  }

  private boolean canInsert(int slot, ItemStack stack) {
    if (storage.inventory instanceof ShulkerBoxBlockEntity shulker) {
      return shulker.canPlaceItemThroughFace(slot, stack, null);
    }
    return storage.inventory.canPlaceItem(slot, stack);
  }

  @Override
  public long extract(ItemVariant variant, long maxAmount, TransactionContext transaction) {
    long ret = super.extract(variant, maxAmount, transaction);
    if (specialInv != null && ret > 0) specialInv.fabric_onTransfer(slot, transaction);
    return ret;
  }

  @Override
  public int getCapacity(ItemVariant variant) {
    if (storage.inventory instanceof AbstractFurnaceBlockEntity && slot == 1 && variant.isOf(Items.BUCKET)) {
      return 1;
    }
    if (storage.inventory instanceof BrewingStandBlockEntity && slot < 3) {
      return 1;
    }
    return Math.min(storage.inventory.getMaxStackSize(), variant.toStack(1).getMaxStackSize());
  }

  @Override
  public void updateSnapshots(TransactionContext transaction) {
    storage.markDirtyParticipant.updateSnapshots(transaction);
    super.updateSnapshots(transaction);
    if (storage.inventory instanceof ChestBlockEntity chest && chest.getBlockState().getValue(ChestBlock.TYPE) != ChestType.SINGLE) {
      BlockPos otherChestPos = chest.getBlockPos().relative(ChestBlock.getConnectedDirection(chest.getBlockState()));
      if (chest.getLevel().getBlockEntity(otherChestPos) instanceof ChestBlockEntity otherChest) {
        ((InventoryStorage) InventoryStorage.of(otherChest, null)).markDirtyParticipant.updateSnapshots(transaction);
      }
    }
  }

  @Override
  protected void releaseSnapshot(ItemStack snapshot) {
    lastReleasedSnapshot = snapshot;
  }

  @Override
  protected void onFinalCommit() {
    ItemStack original = lastReleasedSnapshot;
    ItemStack currentStack = getStack();

    if (storage.inventory instanceof SpecialLogicInventory specialLogicInv) {
      specialLogicInv.fabric_onFinalCommit(slot, original, currentStack);
    }

    if (!original.isEmpty() && original.getItem() == currentStack.getItem()) {
      original.setCount(currentStack.getCount());
      original.setTag(currentStack.hasTag() ? currentStack.getTag().copy() : null);
      setStack(original);
    } else {
      original.setCount(0);
    }
  }

  @Override
  public String toString() {
    return "InventorySlotWrapper[%s#%d]".formatted(DebugMessages.forInventory(storage.inventory), slot);
  }
}

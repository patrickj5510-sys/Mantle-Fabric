package slimeknights.mantle.fabric.transfer;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.StoragePreconditions;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageUtil;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleSlotStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.fabricmc.fabric.api.transfer.v1.transaction.base.SnapshotParticipant;
import net.fabricmc.fabric.impl.transfer.DebugMessages;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

class PlayerInventoryStorage extends InventoryStorage {
  private final DroppedStacks droppedStacks;
  private final Inventory playerInventory;

  PlayerInventoryStorage(Inventory playerInventory) {
    super(playerInventory);
    this.droppedStacks = new DroppedStacks();
    this.playerInventory = playerInventory;
  }

  @Override
  public long insert(ItemVariant resource, long maxAmount, TransactionContext transaction) {
    return offer(resource, maxAmount, transaction);
  }

  public long offer(ItemVariant resource, long amount, TransactionContext tx) {
    StoragePreconditions.notBlankNotNegative(resource, amount);
    long initialAmount = amount;

    List<SingleSlotStorage<ItemVariant>> mainSlots = getSlots().subList(0, Inventory.INVENTORY_SIZE);

    for (InteractionHand hand : InteractionHand.values()) {
      SingleSlotStorage<ItemVariant> handSlot = getHandSlot(hand);
      if (handSlot.getResource().equals(resource)) {
        amount -= handSlot.insert(resource, amount, tx);
        if (amount == 0) return initialAmount;
      }
    }

    amount -= StorageUtil.insertStacking(mainSlots, resource, amount, tx);
    return initialAmount - amount;
  }

  public void drop(ItemVariant variant, long amount, boolean throwRandomly, boolean retainOwnership, TransactionContext transaction) {
    StoragePreconditions.notBlankNotNegative(variant, amount);
    if (amount > 0 && !playerInventory.player.level().isClientSide()) {
      droppedStacks.addDrop(variant, amount, throwRandomly, retainOwnership, transaction);
    }
  }

  public SingleSlotStorage<ItemVariant> getHandSlot(InteractionHand hand) {
    if (Objects.requireNonNull(hand) == InteractionHand.MAIN_HAND) {
      if (Inventory.isHotbarSlot(playerInventory.selected)) {
        return getSlot(playerInventory.selected);
      } else {
        throw new RuntimeException("Unexpected player selected slot: " + playerInventory.selected);
      }
    } else if (hand == InteractionHand.OFF_HAND) {
      return getSlot(Inventory.SLOT_OFFHAND);
    } else {
      throw new UnsupportedOperationException("Unknown hand: " + hand);
    }
  }

  @Override
  public String toString() {
    return "PlayerInventoryStorage[" + DebugMessages.forInventory(playerInventory) + "]";
  }

  private class DroppedStacks extends SnapshotParticipant<Integer> {
    final List<Entry> entries = new ArrayList<>();

    void addDrop(ItemVariant key, long amount, boolean throwRandomly, boolean retainOwnership, TransactionContext transaction) {
      updateSnapshots(transaction);
      entries.add(new Entry(key, amount, throwRandomly, retainOwnership));
    }

    @Override
    protected Integer createSnapshot() {
      return entries.size();
    }

    @Override
    protected void readSnapshot(Integer snapshot) {
      int previousSize = snapshot;
      while (entries.size() > previousSize) {
        entries.remove(entries.size() - 1);
      }
    }

    @Override
    protected void onFinalCommit() {
      for (Entry entry : entries) {
        long remainder = entry.amount;
        int maxStackSize = entry.key.toStack(1).getMaxStackSize();
        while (remainder > 0) {
          int dropped = (int) Math.min(maxStackSize, remainder);
          playerInventory.player.drop(entry.key.toStack(dropped), entry.throwRandomly, entry.retainOwnership);
          remainder -= dropped;
        }
      }
      entries.clear();
    }

    private record Entry(ItemVariant key, long amount, boolean throwRandomly, boolean retainOwnership) {}
  }

  public void drop(ItemVariant variant, long amount, boolean retainOwnership, TransactionContext transaction) {
    drop(variant, amount, false, retainOwnership, transaction);
  }

  public void drop(ItemVariant variant, long amount, TransactionContext transaction) {
    drop(variant, amount, false, transaction);
  }
}

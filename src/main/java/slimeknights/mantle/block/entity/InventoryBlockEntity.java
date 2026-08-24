package slimeknights.mantle.block.entity;

import io.github.fabricators_of_create.porting_lib.transfer.item.SlottedStackStorage;
import lombok.Getter;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SidedStorageBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.Nameable;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import slimeknights.mantle.fabric.transfer.InventoryStorage;
import slimeknights.mantle.util.ItemStackList;

import javax.annotation.Nonnull;

/** Updated Mantle inventory block entity with Fabric transfer storage support. */
public abstract class InventoryBlockEntity extends NameableBlockEntity implements Container, MenuProvider, Nameable, SidedStorageBlockEntity {
  private static final String TAG_INVENTORY_SIZE = "InventorySize";
  private static final String TAG_ITEMS = "Items";
  private static final String TAG_SLOT = "Slot";

  private NonNullList<ItemStack> inventory;
  private final boolean saveSizeToNBT;
  protected int stackSizeLimit;
  @Getter
  protected SlottedStackStorage itemHandler;

  public InventoryBlockEntity(BlockEntityType<?> tileEntityTypeIn, BlockPos pos, BlockState state, Component name, boolean saveSizeToNBT, int inventorySize) {
    this(tileEntityTypeIn, pos, state, name, saveSizeToNBT, inventorySize, 64);
  }

  public InventoryBlockEntity(BlockEntityType<?> tileEntityTypeIn, BlockPos pos, BlockState state, Component name, boolean saveSizeToNBT, int inventorySize, int maxStackSize) {
    super(tileEntityTypeIn, pos, state, name);
    this.saveSizeToNBT = saveSizeToNBT;
    this.inventory = NonNullList.withSize(inventorySize, ItemStack.EMPTY);
    this.stackSizeLimit = maxStackSize;
    this.itemHandler = InventoryStorage.of(this, null);
  }

  @Nonnull
  @Override
  public Storage<ItemVariant> getItemStorage(@Nullable Direction direction) {
    return this.itemHandler;
  }

  @Override
  public ItemStack getItem(int slot) {
    if (slot < 0 || slot >= this.inventory.size()) {
      return ItemStack.EMPTY;
    }
    return this.inventory.get(slot);
  }

  public boolean isStackInSlot(int slot) {
    return !this.getItem(slot).isEmpty();
  }

  private void resizeInternal(int size) {
    if (size == this.inventory.size()) {
      return;
    }
    ItemStackList newInventory = ItemStackList.withSize(size);
    for (int i = 0; i < size && i < this.inventory.size(); i++) {
      newInventory.set(i, this.inventory.get(i));
    }
    this.inventory = newInventory;
  }

  public void resize(int size) {
    this.resizeInternal(size);
    this.setChangedFast();
  }

  @Override
  public int getContainerSize() {
    return this.inventory.size();
  }

  @Override
  public int getMaxStackSize() {
    return this.stackSizeLimit;
  }

  @Override
  public void setItem(int slot, ItemStack itemstack) {
    if (slot < 0 || slot >= this.inventory.size()) {
      return;
    }
    ItemStack current = this.inventory.get(slot);
    this.inventory.set(slot, itemstack);
    if (!itemstack.isEmpty() && itemstack.getCount() > this.getMaxStackSize()) {
      itemstack.setCount(this.getMaxStackSize());
    }
    if (!ItemStack.matches(current, itemstack)) {
      this.setChangedFast();
    }
  }

  @Override
  public ItemStack removeItem(int slot, int quantity) {
    if (quantity <= 0) {
      return ItemStack.EMPTY;
    }
    ItemStack itemStack = this.getItem(slot);
    if (itemStack.isEmpty()) {
      return ItemStack.EMPTY;
    }
    if (itemStack.getCount() <= quantity) {
      this.setItem(slot, ItemStack.EMPTY);
      this.setChangedFast();
      return itemStack;
    }
    itemStack = itemStack.split(quantity);
    if (this.getItem(slot).getCount() == 0) {
      this.setItem(slot, ItemStack.EMPTY);
    }
    this.setChangedFast();
    return itemStack;
  }

  @Override
  public ItemStack removeItemNoUpdate(int slot) {
    ItemStack itemStack = this.getItem(slot);
    this.setItem(slot, ItemStack.EMPTY);
    return itemStack;
  }

  @Override
  public boolean canPlaceItem(int slot, ItemStack itemstack) {
    return slot < this.getContainerSize() && (this.inventory.get(slot).isEmpty() || itemstack.getCount() + this.inventory.get(slot).getCount() <= this.getMaxStackSize());
  }

  @Override
  public void clearContent() {
    for (int i = 0; i < this.inventory.size(); i++) {
      this.inventory.set(i, ItemStack.EMPTY);
    }
  }

  @Override
  public boolean stillValid(Player entityplayer) {
    if (level == null || this.level.getBlockEntity(this.worldPosition) != this || this.getBlockState().getBlock() == Blocks.AIR) {
      return false;
    }
    return entityplayer.distanceToSqr(this.worldPosition.getX() + 0.5D, this.worldPosition.getY() + 0.5D, this.worldPosition.getZ() + 0.5D) <= 64D;
  }

  @Override
  public void startOpen(Player player) {}

  @Override
  public void stopOpen(Player player) {}

  @Override
  protected void loadAdditional(CompoundTag tags, HolderLookup.Provider registries) {
    super.loadAdditional(tags, registries);
    if (saveSizeToNBT) {
      this.resizeInternal(tags.getInt(TAG_INVENTORY_SIZE));
    }
    this.readInventoryFromNBT(tags, registries);
  }

  @Override
  protected void saveSynced(CompoundTag tags, HolderLookup.Provider registries) {
    super.saveSynced(tags, registries);
    if (saveSizeToNBT) {
      tags.putInt(TAG_INVENTORY_SIZE, this.inventory.size());
    }
  }

  @Override
  protected void saveAdditional(CompoundTag tags, HolderLookup.Provider registries) {
    super.saveAdditional(tags, registries);
    this.writeInventoryToNBT(tags, registries);
  }

  public void writeInventoryToNBT(CompoundTag tag, HolderLookup.Provider registries) {
    ListTag nbttaglist = new ListTag();
    for (int i = 0; i < getContainerSize(); i++) {
      ItemStack stack = getItem(i);
      if (!stack.isEmpty()) {
        CompoundTag itemTag = (CompoundTag) stack.save(registries);
        itemTag.putByte(TAG_SLOT, (byte) i);
        nbttaglist.add(itemTag);
      }
    }
    tag.put(TAG_ITEMS, nbttaglist);
  }

  public void readInventoryFromNBT(CompoundTag tag, HolderLookup.Provider registries) {
    ListTag list = tag.getList(TAG_ITEMS, Tag.TAG_COMPOUND);
    int limit = this.getMaxStackSize();
    for (int i = 0; i < list.size(); ++i) {
      CompoundTag itemTag = list.getCompound(i);
      int slot = itemTag.getByte(TAG_SLOT) & 255;
      if (slot < this.inventory.size()) {
        ItemStack stack = ItemStack.parseOptional(registries, itemTag);
        if (!stack.isEmpty() && stack.getCount() > limit) {
          stack.setCount(limit);
        }
        this.inventory.set(slot, stack);
      }
    }
  }

  @Override
  public boolean isEmpty() {
    for (ItemStack itemstack : this.inventory) {
      if (!itemstack.isEmpty()) {
        return false;
      }
    }
    return true;
  }
}

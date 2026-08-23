package slimeknights.mantle.inventory;

import io.github.fabricators_of_create.porting_lib.common.util.EnvExecutor;
import net.fabricmc.api.EnvType;
import net.minecraft.client.Minecraft;
import net.minecraft.core.NonNullList;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import slimeknights.mantle.util.BlockEntityHelper;

import javax.annotation.Nullable;

public class BaseContainerMenu<TILE extends BlockEntity> extends AbstractContainerMenu {

  public static double MAX_DISTANCE = 64;
  public static int BASE_Y_OFFSET = 84;

  @Nullable
  protected final TILE tile;

  @Nullable
  protected final Inventory inv;

  protected BaseContainerMenu(MenuType<?> type, int id, @Nullable Inventory inv, @Nullable TILE tile) {
    super(type, id);
    this.inv = inv;
    this.tile = tile;
  }

  @Nullable
  public TILE getTile() {
    return this.tile;
  }

  public void syncOnOpen(ServerPlayer playerOpened) {
    ServerLevel server = playerOpened.serverLevel();
    for (Player player : server.players()) {
      if (player == playerOpened) {
        continue;
      }
      if (player.containerMenu instanceof BaseContainerMenu) {
        if (this.sameGui((BaseContainerMenu) player.containerMenu)) {
          this.syncWithOtherContainer((BaseContainerMenu) player.containerMenu, playerOpened);
          return;
        }
      }
    }
    this.syncNewContainer(playerOpened);
  }

  protected void syncWithOtherContainer(BaseContainerMenu otherContainer, ServerPlayer player) {}

  protected void syncNewContainer(ServerPlayer player) {}

  public boolean sameGui(BaseContainerMenu otherContainer) {
    if (this.tile == null) {
      return false;
    }
    return this.tile == otherContainer.tile;
  }

  @Override
  public boolean stillValid(Player playerIn) {
    if (this.tile == null) {
      return true;
    }
    if (!tile.isRemoved()) {
      Level world = tile.getLevel();
      if (world == null) {
        return false;
      }
      return world.isLoaded(tile.getBlockPos());
    }
    return false;
  }

  @Override
  public NonNullList<ItemStack> getItems() {
    return super.getItems();
  }

  protected void addInventorySlots() {
    if (this.inv != null) {
      this.addInventorySlots(this.inv);
    }
  }

  protected int playerInventoryStart = -1;

  protected int getInventoryXOffset() {
    return 8;
  }

  protected int getInventoryYOffset() {
    return BASE_Y_OFFSET;
  }

  protected void addInventorySlots(Inventory inv) {
    int yOffset = this.getInventoryYOffset();
    int xOffset = this.getInventoryXOffset();
    int start = this.slots.size();
    for (int slotY = 0; slotY < 3; slotY++) {
      for (int slotX = 0; slotX < 9; slotX++) {
        addSlot(new Slot(inv, slotX + slotY * 9 + 9, xOffset + slotX * 18, yOffset + slotY * 18));
      }
    }
    yOffset += 58;
    for (int slotY = 0; slotY < 9; slotY++) {
      addSlot(new Slot(inv, slotY, xOffset + slotY * 18, yOffset));
    }
    this.playerInventoryStart = start;
  }

  @Override
  protected Slot addSlot(Slot slotIn) {
    if (this.playerInventoryStart >= 0) {
      throw new IllegalStateException("BaseContainer: Player inventory has to be last slots. Add all slots before adding the player inventory.");
    }
    return super.addSlot(slotIn);
  }

  @Override
  public ItemStack quickMoveStack(Player playerIn, int index) {
    if (this.playerInventoryStart < 0) {
      return ItemStack.EMPTY;
    }
    ItemStack itemstack = ItemStack.EMPTY;
    Slot slot = this.slots.get(index);
    if (slot != null && slot.hasItem()) {
      ItemStack itemstack1 = slot.getItem();
      itemstack = itemstack1.copy();
      int end = this.slots.size();
      if (index < this.playerInventoryStart) {
        if (!this.moveItemStackTo(itemstack1, this.playerInventoryStart, end, true)) {
          return ItemStack.EMPTY;
        }
      } else if (!this.moveItemStackTo(itemstack1, 0, this.playerInventoryStart, false)) {
        return ItemStack.EMPTY;
      }
      if (itemstack1.isEmpty()) {
        slot.set(ItemStack.EMPTY);
      } else {
        slot.setChanged();
      }
    }
    return itemstack;
  }

  @Override
  protected boolean moveItemStackTo(ItemStack stack, int startIndex, int endIndex, boolean useEndIndex) {
    boolean ret = this.mergeItemStackRefill(stack, startIndex, endIndex, useEndIndex);
    if (!stack.isEmpty() && stack.getCount() > 0) {
      ret |= this.mergeItemStackMove(stack, startIndex, endIndex, useEndIndex);
    }
    return ret;
  }

  protected boolean mergeItemStackRefill(ItemStack stack, int startIndex, int endIndex, boolean useEndIndex) {
    if (stack.getCount() <= 0) {
      return false;
    }
    boolean flag1 = false;
    int k = useEndIndex ? endIndex - 1 : startIndex;
    Slot slot;
    ItemStack itemstack1;
    if (stack.isStackable()) {
      while (stack.getCount() > 0 && (!useEndIndex && k < endIndex || useEndIndex && k >= startIndex)) {
        slot = this.slots.get(k);
        itemstack1 = slot.getItem();
        if (!itemstack1.isEmpty() && itemstack1.getItem() == stack.getItem() && ItemStack.isSameItemSameTags(stack, itemstack1) && this.canTakeItemForPickAll(stack, slot)) {
          int l = itemstack1.getCount() + stack.getCount();
          int limit = Math.min(stack.getMaxStackSize(), slot.getMaxStackSize(stack));
          if (l <= limit) {
            stack.setCount(0);
            itemstack1.setCount(l);
            slot.setChanged();
            flag1 = true;
          } else if (itemstack1.getCount() < limit) {
            stack.shrink(limit - itemstack1.getCount());
            itemstack1.setCount(limit);
            slot.setChanged();
            flag1 = true;
          }
        }
        k += useEndIndex ? -1 : 1;
      }
    }
    return flag1;
  }

  protected boolean mergeItemStackMove(ItemStack stack, int startIndex, int endIndex, boolean useEndIndex) {
    if (stack.getCount() <= 0) {
      return false;
    }
    boolean flag1 = false;
    int k = useEndIndex ? endIndex - 1 : startIndex;
    while (!useEndIndex && k < endIndex || useEndIndex && k >= startIndex) {
      Slot slot = this.slots.get(k);
      ItemStack itemstack1 = slot.getItem();
      if (itemstack1.isEmpty() && slot.mayPlace(stack) && this.canTakeItemForPickAll(stack, slot)) {
        int limit = slot.getMaxStackSize(stack);
        ItemStack stack2 = stack.copy();
        if (stack2.getCount() > limit) {
          stack2.setCount(limit);
          stack.shrink(limit);
        } else {
          stack.setCount(0);
        }
        slot.set(stack2);
        slot.setChanged();
        flag1 = true;
        if (stack.isEmpty()) {
          break;
        }
      }
      k += useEndIndex ? -1 : 1;
    }
    return flag1;
  }

  @Nullable
  public static <TILE extends BlockEntity> TILE getTileEntityFromBuf(@Nullable FriendlyByteBuf buf, Class<TILE> type) {
    if (buf == null) {
      return null;
    }
    return EnvExecutor.callWhenOn(EnvType.CLIENT, () -> () -> BlockEntityHelper.get(type, Minecraft.getInstance().level, buf.readBlockPos()).orElse(null));
  }
}

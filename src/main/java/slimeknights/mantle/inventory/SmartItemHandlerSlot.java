package slimeknights.mantle.inventory;

import io.github.fabricators_of_create.porting_lib.transfer.item.SlotItemHandler;
import io.github.fabricators_of_create.porting_lib.transfer.item.SlottedStackStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.SlottedStorage;
import net.minecraft.world.item.ItemStack;

/** Uses the transfer storage slot limit when determining stack capacity. */
public class SmartItemHandlerSlot extends SlotItemHandler {
  private final int slotIndex;

  public SmartItemHandlerSlot(SlottedStorage<ItemVariant> itemHandler, int index, int xPosition, int yPosition) {
    super(itemHandler, index, xPosition, yPosition);
    this.slotIndex = index;
  }

  @Override
  public int getMaxStackSize(ItemStack stack) {
    var storage = getItemHandler();
    return (int) Math.min(stack.getMaxStackSize(), storage instanceof SlottedStackStorage slottedStackStorage
      ? slottedStackStorage.getSlotLimit(slotIndex)
      : storage.getSlot(slotIndex).getCapacity());
  }
}
